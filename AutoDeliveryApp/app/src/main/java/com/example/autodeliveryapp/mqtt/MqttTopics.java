package com.example.autodeliveryapp.mqtt;

public class MqttTopics {
    
    private static String getEnv() {
        String env = MqttConfig.ENV;
        if (env == null || env.trim().isEmpty()) return "dev";
        return env.trim();
    }
    
    private static String sanitize(String robotId) {
        if (robotId == null || robotId.trim().isEmpty()) {
            throw new IllegalArgumentException("robotId cannot be null or empty");
        }
        return robotId.trim();
    }

    public static String command(String robotId) {
        return "autodelivery/" + getEnv() + "/robots/" + sanitize(robotId) + "/command";
    }

    public static String telemetry(String robotId) {
        return "autodelivery/" + getEnv() + "/robots/" + sanitize(robotId) + "/telemetry";
    }

    public static String status(String robotId) {
        return "autodelivery/" + getEnv() + "/robots/" + sanitize(robotId) + "/status";
    }

    public static String ack(String robotId) {
        return "autodelivery/" + getEnv() + "/robots/" + sanitize(robotId) + "/ack";
    }

    public static String event(String robotId) {
        return "autodelivery/" + getEnv() + "/robots/" + sanitize(robotId) + "/event";
    }

    public static String availability(String robotId) {
        return "autodelivery/" + getEnv() + "/robots/" + sanitize(robotId) + "/availability";
    }
}
