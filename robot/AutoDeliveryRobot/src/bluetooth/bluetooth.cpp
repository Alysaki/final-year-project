#include "bluetooth.h"

BluetoothSerial SerialBT;

void bt_init() {
    SerialBT.begin("ESP32_ROBOT");
}

String bt_read() {
    if (SerialBT.available()) {
        return SerialBT.readStringUntil('\n');
    }
    return "";
}