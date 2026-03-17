#ifndef BLUETOOTH_H
#define BLUETOOTH_H

#include <BluetoothSerial.h>

extern BluetoothSerial SerialBT;

void bt_init();
String bt_read();

#endif