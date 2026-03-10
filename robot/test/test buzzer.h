//GPIO25 → buzzer → GND

#include <Arduino.h>

#define BUZZER 25

void setup() {
    pinMode(BUZZER, OUTPUT);
}

void loop() {
    digitalWrite(BUZZER, HIGH);
    delay(500);

    digitalWrite(BUZZER, LOW);
    delay(500);
}