package com.example.autodeliveryapp.mqtt;

import com.example.autodeliveryapp.BuildConfig;

public class MqttConfig {
    public static final String HOST = BuildConfig.MQTT_HOST;
    public static final int PORT = BuildConfig.MQTT_PORT;
    public static final String USERNAME = BuildConfig.MQTT_USERNAME;
    public static final String PASSWORD = BuildConfig.MQTT_PASSWORD;
    public static final boolean USE_SSL = BuildConfig.MQTT_USE_SSL;
    public static final String ENV = BuildConfig.MQTT_ENV;
    public static final String DEFAULT_ROBOT_ID = BuildConfig.MQTT_DEFAULT_ROBOT_ID;

    public static boolean isConfigUsable() {
        return HOST != null && !HOST.trim().isEmpty() &&
               USERNAME != null && !USERNAME.trim().isEmpty() &&
               PASSWORD != null && !PASSWORD.trim().isEmpty() &&
               ENV != null && !ENV.trim().isEmpty();
    }
}
