#include <Arduino.h>

#include "bluetooth/bluetooth.h"
#include "motor/motor.h"
#include "sensor/vl53.h"
#include "gps/gps.h"

void setup() {
    Serial.begin(115200);

    motor_init();
    bt_init();
    vl53_init();
    gps_init();

    Serial.println("SYSTEM READY");
}

void loop() {
    String cmd = bt_read();

    if(cmd == "F") move_forward();
    else if(cmd == "B") move_backward();
    else if(cmd == "L") turn_left();
    else if(cmd == "R") turn_right();
    else if(cmd == "S") stop_motor();

    vl53_read();
    gps_read();
}