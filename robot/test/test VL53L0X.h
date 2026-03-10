//Adafruit VL53L0X
//VCC → 3.3V
//GND → GND
//SDA → GPIO21
//SCL → GPIO22



#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_VL53L0X.h>

Adafruit_VL53L0X lox = Adafruit_VL53L0X();

void setup() {
  Serial.begin(115200);
  Wire.begin();

  if (!lox.begin()) {
    Serial.println("VL53L0X not found");
    while(1);
  }

  Serial.println("VL53L0X ready");
}

void loop() {

  VL53L0X_RangingMeasurementData_t measure;

  lox.rangingTest(&measure, false);

  if (measure.RangeStatus != 4) {
      Serial.print("Distance: ");
      Serial.print(measure.RangeMilliMeter);
      Serial.println(" mm");
  } 
  else {
      Serial.println("Out of range");
  }

  delay(200);
}