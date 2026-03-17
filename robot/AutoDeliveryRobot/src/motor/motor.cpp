#include <Arduino.h>
#include "motor.h"
#include "config.h"

void motor_init() {
    int pins[] = {IN1,IN2,IN3,IN4,IN5,IN6,IN7,IN8};
    for (int i=0;i<8;i++) pinMode(pins[i], OUTPUT);

    ledcSetup(0, 1000, 8);
    ledcSetup(1, 1000, 8);
    ledcSetup(2, 1000, 8);
    ledcSetup(3, 1000, 8);

    ledcAttachPin(ENA1, 0);
    ledcAttachPin(ENB1, 1);
    ledcAttachPin(ENA2, 2);
    ledcAttachPin(ENB2, 3);
}

void setSpeed(int spd){
    ledcWrite(0, spd);
    ledcWrite(1, spd);
    ledcWrite(2, spd);
    ledcWrite(3, spd);
}

void move_forward() {
    setSpeed(200);
    digitalWrite(IN1, HIGH); digitalWrite(IN2, LOW);
    digitalWrite(IN3, HIGH); digitalWrite(IN4, LOW);
    digitalWrite(IN5, HIGH); digitalWrite(IN6, LOW);
    digitalWrite(IN7, HIGH); digitalWrite(IN8, LOW);
}

void move_backward() {
    setSpeed(200);
    digitalWrite(IN1, LOW); digitalWrite(IN2, HIGH);
    digitalWrite(IN3, LOW); digitalWrite(IN4, HIGH);
    digitalWrite(IN5, LOW); digitalWrite(IN6, HIGH);
    digitalWrite(IN7, LOW); digitalWrite(IN8, HIGH);
}

void turn_left() {
    setSpeed(180);
    digitalWrite(IN1, LOW); digitalWrite(IN2, HIGH);
    digitalWrite(IN3, HIGH); digitalWrite(IN4, LOW);
}

void turn_right() {
    setSpeed(180);
    digitalWrite(IN1, HIGH); digitalWrite(IN2, LOW);
    digitalWrite(IN3, LOW); digitalWrite(IN4, HIGH);
}

void stop_motor() {
    setSpeed(0);
}