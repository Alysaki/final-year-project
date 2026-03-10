#include <Arduino.h>

#include "motor.h"
#include "bluetooth.h"
#include "distance.h"

#define BUZZER 25
#define OBSTACLE_DISTANCE 300   // 30 cm

void setup()
{
    Serial.begin(115200);

    motorInit();
    bluetoothInit();
    distanceInit();

    pinMode(BUZZER, OUTPUT);

    Serial.println("Robot Ready");
}

void loop()
{
    // đọc lệnh bluetooth
    char cmd = readBluetooth();

    if (cmd == 'F')
        forward();

    else if (cmd == 'B')
        back();

    else if (cmd == 'L')
        left();

    else if (cmd == 'R')
        right();

    else if (cmd == 'S')
        stopMotor();


    // đọc khoảng cách từ 4 cảm biến
    int front = readFront();
    int leftD = readLeft();
    int rightD = readRight();
    int backD = readBack();


    // hiển thị debug
    Serial.print("Front: ");
    Serial.print(front);

    Serial.print("  Left: ");
    Serial.print(leftD);

    Serial.print("  Right: ");
    Serial.print(rightD);

    Serial.print("  Back: ");
    Serial.println(backD);


    // kiểm tra vật cản
    bool obstacle =
        (front > 0 && front < OBSTACLE_DISTANCE) ||
        (leftD > 0 && leftD < OBSTACLE_DISTANCE) ||
        (rightD > 0 && rightD < OBSTACLE_DISTANCE) ||
        (backD > 0 && backD < OBSTACLE_DISTANCE);


    if (obstacle)
        digitalWrite(BUZZER, HIGH);
    else
        digitalWrite(BUZZER, LOW);


    delay(60);
}