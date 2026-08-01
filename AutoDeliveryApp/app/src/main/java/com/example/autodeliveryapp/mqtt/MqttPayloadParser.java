package com.example.autodeliveryapp.mqtt;

import android.util.Log;
import com.example.autodeliveryapp.model.RobotAck;
import com.example.autodeliveryapp.model.RobotCommand;
import com.example.autodeliveryapp.model.RobotStatusMessage;
import com.example.autodeliveryapp.model.RobotTelemetry;
import org.json.JSONException;
import org.json.JSONObject;

public class MqttPayloadParser {
    private static final String TAG = "MqttPayloadParser";

    public static RobotTelemetry parseTelemetry(String payload, String topic) {
        try {
            JSONObject json = new JSONObject(payload);
            RobotTelemetry telemetry = new RobotTelemetry();
            telemetry.robotId = json.optString("robotId", "");
            telemetry.taskId = json.optString("taskId", "");
            telemetry.lat = json.optDouble("lat", 0.0);
            telemetry.lng = json.optDouble("lng", 0.0);
            telemetry.battery = json.optInt("battery", 0);
            telemetry.speed = json.optDouble("speed", 0.0);
            telemetry.heading = json.optDouble("heading", 0.0);
            telemetry.obstacle = json.optBoolean("obstacle", false);
            telemetry.timestamp = json.optLong("timestamp", 0L);
            telemetry.seq = json.optLong("seq", 0L);
            
            if (telemetry.robotId.isEmpty() || telemetry.timestamp == 0L) {
                Log.w(TAG, "Missing required fields in telemetry: " + topic);
                return null;
            }
            return telemetry;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing telemetry from topic: " + topic, e);
            return null;
        }
    }

    public static RobotStatusMessage parseStatus(String payload, String topic) {
        try {
            JSONObject json = new JSONObject(payload);
            RobotStatusMessage status = new RobotStatusMessage();
            status.robotId = json.optString("robotId", "");
            status.taskId = json.optString("taskId", "");
            status.status = json.optString("status", "");
            status.slotId = json.optString("slotId", "");
            status.message = json.optString("message", "");
            status.timestamp = json.optLong("timestamp", 0L);
            
            if (status.robotId.isEmpty() || status.status.isEmpty()) {
                Log.w(TAG, "Missing required fields in status: " + topic);
                return null;
            }
            return status;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing status from topic: " + topic, e);
            return null;
        }
    }

    public static RobotAck parseAck(String payload, String topic) {
        try {
            JSONObject json = new JSONObject(payload);
            RobotAck ack = new RobotAck();
            ack.commandId = json.optString("commandId", "");
            ack.taskId = json.optString("taskId", "");
            ack.robotId = json.optString("robotId", "");
            
            if (json.has("accepted")) {
                ack.accepted = json.optBoolean("accepted", false);
            } else if (json.has("status")) {
                String statusStr = json.optString("status", "");
                ack.accepted = "accepted".equalsIgnoreCase(statusStr);
            } else {
                ack.accepted = false;
            }
            
            ack.message = json.optString("message", "");
            ack.timestamp = json.optLong("timestamp", 0L);
            
            if (ack.commandId.isEmpty() || ack.robotId.isEmpty()) {
                Log.w(TAG, "Missing required fields in ack: " + topic);
                return null;
            }
            return ack;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing ack from topic: " + topic, e);
            return null;
        }
    }
    
    public static JSONObject toCommandJson(RobotCommand cmd) {
        JSONObject json = new JSONObject();
        try {
            json.put("commandId", cmd.commandId);
            json.put("command", cmd.command);
            json.put("taskId", cmd.taskId);
            json.put("orderId", cmd.orderId);
            json.put("robotId", cmd.robotId);
            json.put("slotId", cmd.slotId);
            json.put("pickupLat", cmd.pickupLat);
            json.put("pickupLng", cmd.pickupLng);
            if (cmd.pickupAddress != null) json.put("pickupAddress", cmd.pickupAddress);
            json.put("dropoffLat", cmd.dropoffLat);
            json.put("dropoffLng", cmd.dropoffLng);
            if (cmd.dropoffAddress != null) json.put("dropoffAddress", cmd.dropoffAddress);
            if (cmd.bleToken != null) json.put("bleToken", cmd.bleToken);
            json.put("timestamp", cmd.timestamp);
        } catch (JSONException e) {
            Log.e(TAG, "Error generating command JSON", e);
        }
        return json;
    }
}
