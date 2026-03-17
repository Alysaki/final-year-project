#include <Wire.h>
#include <Adafruit_VL53L0X.h>
#include "vl53.h"
#include "config.h"

Adafruit_VL53L0X sensor[4];

int xshutPins[4] = {XSHUT_1, XSHUT_2, XSHUT_3, XSHUT_4};

void vl53_init() {
    Wire.begin(SDA_PIN, SCL_PIN);

    for(int i=0;i<4;i++){
        pinMode(xshutPins[i], OUTPUT);
        digitalWrite(xshutPins[i], LOW);
    }

    delay(100);

    for(int i=0;i<4;i++){
        digitalWrite(xshutPins[i], HIGH);
        delay(50);
        sensor[i].begin(0x30 + i);
    }
}

void vl53_read(){
    for(int i=0;i<4;i++){
        VL53L0X_RangingMeasurementData_t measure;
        sensor[i].rangingTest(&measure, false);

        if(measure.RangeStatus != 4){
            Serial.print("S"); Serial.print(i);
            Serial.print(": ");
            Serial.print(measure.RangeMilliMeter);
            Serial.print(" ");
        }
    }
    Serial.println();
}