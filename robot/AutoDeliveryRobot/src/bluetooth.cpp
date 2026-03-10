#include <Arduino.h>
#include "BluetoothSerial.h"
#include "bluetooth.h"

BluetoothSerial SerialBT;

void bluetoothInit()
{
    SerialBT.begin("ESP32_ROBOT");
}

char readBluetooth()
{
    if (SerialBT.available())
    {
        return SerialBT.read();
    }

    return 0;
}