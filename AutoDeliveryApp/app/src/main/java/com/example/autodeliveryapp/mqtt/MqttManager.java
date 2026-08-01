package com.example.autodeliveryapp.mqtt;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.autodeliveryapp.model.RobotAck;
import com.example.autodeliveryapp.model.RobotCommand;
import com.example.autodeliveryapp.model.RobotStatusMessage;
import com.example.autodeliveryapp.model.RobotTelemetry;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MqttManager {
    private static final String TAG = "MqttManager";
    private static MqttManager instance;
    private Mqtt3AsyncClient client;
    private MqttListener listener;
    private final Handler mainHandler;

    private MqttManager() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized MqttManager getInstance() {
        if (instance == null) {
            instance = new MqttManager();
        }
        return instance;
    }

    public void setListener(MqttListener listener) {
        this.listener = listener;
    }

    public void connect(String uid) {
        if (!MqttConfig.isConfigUsable()) {
            notifyError("MQTT config is not usable", null);
            return;
        }

        if (client != null && client.getState().isConnected()) {
            return;
        }

        String clientId = uid != null && !uid.isEmpty() ?
                "android_" + uid + "_" + System.currentTimeMillis() :
                "android_guest_" + System.currentTimeMillis();

        Mqtt3ClientBuilder builder = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(MqttConfig.HOST)
                .serverPort(MqttConfig.PORT);

        if (MqttConfig.USE_SSL) {
            builder.sslWithDefaultConfig();
        }

        builder.simpleAuth()
                .username(MqttConfig.USERNAME)
                .password(MqttConfig.PASSWORD.getBytes(StandardCharsets.UTF_8))
                .applySimpleAuth();

        // Automatic reconnect basic support
        builder.automaticReconnectWithDefaultConfig();

        client = builder.buildAsync();

        client.connectWith()
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "MQTT Connection failed", throwable);
                        notifyDisconnected(throwable);
                    } else {
                        Log.i(TAG, "MQTT Connected successfully");
                        client.publishes(MqttGlobalPublishFilter.ALL, this::handleMessage);
                        notifyConnected();
                    }
                });
    }

    public void disconnect() {
        if (client != null && client.getState().isConnected()) {
            client.disconnect().whenComplete((ignored, throwable) -> {
                Log.i(TAG, "MQTT Disconnected");
                client = null;
                notifyDisconnected(null);
            });
        }
    }

    public boolean isConnected() {
        return client != null && client.getState().isConnected();
    }

    public void subscribeRobot(String robotId) {
        if (!isConnected()) return;

        client.subscribeWith()
                .topicFilter(MqttTopics.telemetry(robotId))
                .qos(MqttQos.AT_MOST_ONCE)
                .send();

        client.subscribeWith()
                .topicFilter(MqttTopics.status(robotId))
                .qos(MqttQos.AT_LEAST_ONCE)
                .send();

        client.subscribeWith()
                .topicFilter(MqttTopics.ack(robotId))
                .qos(MqttQos.AT_LEAST_ONCE)
                .send();

        client.subscribeWith()
                .topicFilter(MqttTopics.availability(robotId))
                .qos(MqttQos.AT_LEAST_ONCE)
                .send();
    }

    private void handleMessage(Mqtt3Publish publish) {
        String topic = publish.getTopic().toString();
        String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);

        if (topic.endsWith("/telemetry")) {
            RobotTelemetry telemetry = MqttPayloadParser.parseTelemetry(payload, topic);
            if (telemetry != null) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onRobotTelemetry(telemetry);
                });
            }
        } else if (topic.endsWith("/status")) {
            RobotStatusMessage status = MqttPayloadParser.parseStatus(payload, topic);
            if (status != null) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onRobotStatus(status);
                });
            }
        } else if (topic.endsWith("/ack")) {
            RobotAck ack = MqttPayloadParser.parseAck(payload, topic);
            if (ack != null) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onRobotAck(ack);
                });
            }
        } else if (topic.endsWith("/availability")) {
            String[] parts = topic.split("/");
            if (parts.length >= 2) {
                String robotId = parts[parts.length - 2];
                boolean online = "online".equalsIgnoreCase(payload.trim());
                mainHandler.post(() -> {
                    if (listener != null) listener.onRobotAvailability(robotId, online, System.currentTimeMillis());
                });
            }
        }
    }

    public void unsubscribeRobot(String robotId) {
        if (!isConnected()) return;
        client.unsubscribeWith()
                .topicFilter(MqttTopics.telemetry(robotId))
                .addTopicFilter(MqttTopics.status(robotId))
                .addTopicFilter(MqttTopics.ack(robotId))
                .addTopicFilter(MqttTopics.availability(robotId))
                .send();
    }

    public void publishStartDelivery(String robotId, RobotCommand command) {
        if (!isConnected()) return;
        String payload = MqttPayloadParser.toCommandJson(command).toString();
        publishMessage(MqttTopics.command(robotId), payload, MqttQos.AT_LEAST_ONCE, false);
    }

    public void publishCancelTask(String robotId, String taskId, String reason) {
        if (!isConnected()) return;
        RobotCommand command = new RobotCommand();
        command.commandId = UUID.randomUUID().toString();
        command.command = "CANCEL_TASK";
        command.taskId = taskId;
        command.robotId = robotId;
        command.timestamp = System.currentTimeMillis();
        // Reason could be passed if there was a field for it
        
        String payload = MqttPayloadParser.toCommandJson(command).toString();
        publishMessage(MqttTopics.command(robotId), payload, MqttQos.AT_LEAST_ONCE, false);
    }

    private void publishMessage(String topic, String payload, MqttQos qos, boolean retain) {
        client.publishWith()
                .topic(topic)
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .qos(qos)
                .retain(retain)
                .send()
                .whenComplete((publishResult, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Failed to publish to " + topic, throwable);
                        notifyError("Failed to publish", throwable);
                    }
                });
    }

    private void notifyConnected() {
        mainHandler.post(() -> {
            if (listener != null) listener.onMqttConnected();
        });
    }

    private void notifyDisconnected(Throwable error) {
        mainHandler.post(() -> {
            if (listener != null) listener.onMqttDisconnected(error);
        });
    }

    private void notifyError(String message, Throwable error) {
        mainHandler.post(() -> {
            if (listener != null) listener.onMqttError(message, error);
        });
    }
}
