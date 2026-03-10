#include <Arduino.h>
#include <Wire.h>
#include <VL53L0X.h>
#include "distance.h"

#define XSHUT1 32
#define XSHUT2 33
#define XSHUT3 25
#define XSHUT4 26

VL53L0X sensor1;
VL53L0X sensor2;
VL53L0X sensor3;
VL53L0X sensor4;

void distanceInit()
{
    Wire.begin();

    pinMode(XSHUT1, OUTPUT);
    pinMode(XSHUT2, OUTPUT);
    pinMode(XSHUT3, OUTPUT);
    pinMode(XSHUT4, OUTPUT);

    digitalWrite(XSHUT1, LOW);
    digitalWrite(XSHUT2, LOW);
    digitalWrite(XSHUT3, LOW);
    digitalWrite(XSHUT4, LOW);

    delay(10);

    digitalWrite(XSHUT1, HIGH);
    delay(10);
    sensor1.init();
    sensor1.setAddress(0x30);

    digitalWrite(XSHUT2, HIGH);
    delay(10);
    sensor2.init();
    sensor2.setAddress(0x31);

    digitalWrite(XSHUT3, HIGH);
    delay(10);
    sensor3.init();
    sensor3.setAddress(0x32);

    digitalWrite(XSHUT4, HIGH);
    delay(10);
    sensor4.init();
    sensor4.setAddress(0x33);
}

int readFront()
{
    return sensor1.readRangeSingleMillimeters();
}

int readLeft()
{
    return sensor2.readRangeSingleMillimeters();
}

int readRight()
{
    return sensor3.readRangeSingleMillimeters();
}

int readBack()
{
    return sensor4.readRangeSingleMillimeters();
}