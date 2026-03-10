#include <Arduino.h>

void setup() {
    Serial.begin(115200);
    Serial.println("ESP32 connected");
}

void loop() {
    Serial.println("running...");
    delay(1000);
}