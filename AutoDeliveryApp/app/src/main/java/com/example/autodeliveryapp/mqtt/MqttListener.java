package com.example.autodeliveryapp.mqtt;

import com.example.autodeliveryapp.model.RobotAck;
import com.example.autodeliveryapp.model.RobotStatusMessage;
import com.example.autodeliveryapp.model.RobotTelemetry;

public interface MqttListener {
    void onMqttConnected();
    void onMqttDisconnected(Throwable error);
    void onRobotTelemetry(RobotTelemetry telemetry);
    void onRobotStatus(RobotStatusMessage status);
    void onRobotAck(RobotAck ack);
    void onRobotAvailability(String robotId, boolean online, long timestamp);
    void onMqttError(String message, Throwable error);
}
