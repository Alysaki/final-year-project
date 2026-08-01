package com.example.autodeliveryapp.mqtt;

import android.util.Log;
import com.example.autodeliveryapp.Constants;
import com.example.autodeliveryapp.model.RobotAck;
import com.example.autodeliveryapp.model.RobotStatusMessage;
import com.example.autodeliveryapp.model.RobotTelemetry;
import com.example.autodeliveryapp.utils.NotificationUtils;
import com.example.autodeliveryapp.utils.SlotUtils;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MqttFirebaseBridge {
    private static final String TAG = "MqttFirebaseBridge";
    
    private final String senderUid;
    private final String receiverUid;
    private final String taskId;
    private final String robotId;
    private final String slotId;

    private long lastTelemetryWriteTime = 0;
    private long lastTelemetrySeq = -1;
    private String lastStatus = "";

    public MqttFirebaseBridge(String senderUid, String receiverUid, String taskId, String robotId, String slotId) {
        this.senderUid = senderUid;
        this.receiverUid = receiverUid;
        this.taskId = taskId;
        this.robotId = robotId;
        this.slotId = slotId;
    }

    public void handleTelemetry(RobotTelemetry telemetry) {
        if (senderUid == null || taskId == null) {
            Log.w(TAG, "Missing senderUid or taskId, cannot mirror telemetry to Firebase");
            return;
        }

        if (!taskId.equals(telemetry.taskId)) {
            Log.w(TAG, "Telemetry taskId mismatch, ignoring. Expected: " + taskId + ", got: " + telemetry.taskId);
            return;
        }

        if (telemetry.robotId != null && !telemetry.robotId.isEmpty() && robotId != null && !robotId.equals(telemetry.robotId)) {
            Log.w(TAG, "Telemetry robotId mismatch, ignoring. Expected: " + robotId + ", got: " + telemetry.robotId);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTelemetryWriteTime < 5000) {
            return; // Throttling: Max 1 write per 5 seconds
        }
        
        if (telemetry.seq > 0 && telemetry.seq <= lastTelemetrySeq) {
            return; // Sequence guard
        }

        DatabaseReference taskRef = FirebaseDatabase.getInstance(Constants.DB_URL)
                .getReference("tasks")
                .child(senderUid)
                .child(taskId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("robotLat", telemetry.lat);
        updates.put("robotLng", telemetry.lng);
        updates.put("robotBattery", telemetry.battery);
        updates.put("robotSpeed", telemetry.speed);
        updates.put("robotHeading", telemetry.heading);
        updates.put("lastMqttAt", ServerValue.TIMESTAMP);
        updates.put("lastTelemetrySeq", telemetry.seq);

        taskRef.updateChildren(updates).addOnFailureListener(e -> Log.w(TAG, "Permission denied or network error updating telemetry", e));
        
        lastTelemetryWriteTime = now;
        if (telemetry.seq > 0) {
            lastTelemetrySeq = telemetry.seq;
        }
    }

    public void handleStatus(RobotStatusMessage statusMessage) {
        if (senderUid == null || taskId == null) {
            Log.w(TAG, "Missing senderUid or taskId, cannot mirror status to Firebase");
            return;
        }

        if (!taskId.equals(statusMessage.taskId)) {
            Log.w(TAG, "Status taskId mismatch, ignoring. Expected: " + taskId + ", got: " + statusMessage.taskId);
            return;
        }

        if (statusMessage.robotId != null && !statusMessage.robotId.isEmpty() && robotId != null && !robotId.equals(statusMessage.robotId)) {
            Log.w(TAG, "Status robotId mismatch, ignoring.");
            return;
        }

        String newStatus = statusMessage.status;
        if (newStatus == null || newStatus.isEmpty()) return;

        boolean statusChanged = !newStatus.equals(lastStatus);

        // Derive activeLeg from status for TrackingActivity route rendering
        String activeLeg = deriveActiveLeg(newStatus);

        FirebaseDatabase db = FirebaseDatabase.getInstance(Constants.DB_URL);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("robotStatus", newStatus);
        updates.put("activeLeg", activeLeg);
        updates.put("lastMqttAt", ServerValue.TIMESTAMP);
        if (statusChanged) {
            updates.put("lastNotifiedStatus", newStatus);
        }
        
        db.getReference("tasks").child(senderUid).child(taskId).updateChildren(updates)
            .addOnFailureListener(e -> Log.w(TAG, "Permission denied or network error updating status", e));

        if (receiverUid != null && !receiverUid.isEmpty()) {
            db.getReference("recipientTasks").child(receiverUid).child(taskId).updateChildren(updates)
                .addOnFailureListener(e -> Log.w(TAG, "Permission denied updating recipientTasks", e));
        }

        if (statusChanged) {
            lastStatus = newStatus;
            
            if (robotId != null && slotId != null) {
                SlotUtils.releaseSlotIfTerminal(newStatus, robotId, slotId);
            }
            
            sendNotification(newStatus);
        }
    }

    public void handleAck(RobotAck ack) {
        if (senderUid == null || taskId == null) {
            Log.w(TAG, "Missing senderUid or taskId, cannot mirror ack to Firebase");
            return;
        }

        if (!taskId.equals(ack.taskId)) {
            Log.w(TAG, "Ack taskId mismatch, ignoring. Expected: " + taskId + ", got: " + ack.taskId);
            return;
        }

        if (ack.robotId != null && !ack.robotId.isEmpty() && robotId != null && !robotId.equals(ack.robotId)) {
            Log.w(TAG, "Ack robotId mismatch, ignoring.");
            return;
        }

        DatabaseReference taskRef = FirebaseDatabase.getInstance(Constants.DB_URL)
                .getReference("tasks")
                .child(senderUid)
                .child(taskId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("mqttAck", ack.accepted);
        updates.put("mqttAckMessage", ack.message);
        updates.put("mqttCommandId", ack.commandId);
        updates.put("lastMqttAt", ServerValue.TIMESTAMP);

        taskRef.updateChildren(updates).addOnFailureListener(e -> Log.w(TAG, "Permission denied or network error updating ack", e));
    }

    /**
     * Maps robot status strings to activeLeg values used by TrackingActivity
     * to determine which route segment to draw (C→A vs A/C→B).
     */
    private String deriveActiveLeg(String status) {
        if (status == null) return "robot_to_pickup";
        switch (status) {
            case "going_to_pickup":
                return "robot_to_pickup";
            case "arrived_pickup":
            case "waiting_sender_load":
                return "at_pickup";
            case "sender_loaded":
            case "picked_up":
            case "going_to_destination":
                return "pickup_to_delivery";
            case "arrived_dropoff":
            case "waiting_receiver_unlock":
                return "at_delivery";
            case "delivered":
            case "cancelled":
                return "completed";
            default:
                return "robot_to_pickup";
        }
    }

    private void sendNotification(String statusType) {
        if (senderUid == null || receiverUid == null) {
            Log.w(TAG, "Missing senderUid or receiverUid, cannot send notification");
            return;
        }

        String title = NotificationUtils.buildTitle(statusType);
        String message = NotificationUtils.buildMessage(statusType, taskId, slotId, null, null);
        String icon = NotificationUtils.getIcon(statusType);
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        FirebaseDatabase db = FirebaseDatabase.getInstance(Constants.DB_URL);
        
        String senderNotifId = db.getReference("notifications").child(senderUid).push().getKey();
        if (senderNotifId != null) {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("icon", icon);
            notifData.put("title", title);
            notifData.put("message", message);
            notifData.put("time", time);
            notifData.put("orderId", taskId);
            notifData.put("taskId", taskId);
            notifData.put("type", statusType);
            notifData.put("read", false);
            notifData.put("createdAt", ServerValue.TIMESTAMP);
            notifData.put("slotId", slotId);
            notifData.put("robotId", robotId);

            db.getReference("notifications").child(senderUid).child(senderNotifId).setValue(notifData);
        }

        String receiverNotifId = db.getReference("notifications").child(receiverUid).push().getKey();
        if (receiverNotifId != null) {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("icon", icon);
            notifData.put("title", title);
            notifData.put("message", message);
            notifData.put("time", time);
            notifData.put("orderId", taskId);
            notifData.put("taskId", taskId);
            notifData.put("type", statusType);
            notifData.put("read", false);
            notifData.put("createdAt", ServerValue.TIMESTAMP);
            notifData.put("slotId", slotId);
            notifData.put("robotId", robotId);
            
            db.getReference("notifications").child(receiverUid).child(receiverNotifId).setValue(notifData);
        }
    }
}
