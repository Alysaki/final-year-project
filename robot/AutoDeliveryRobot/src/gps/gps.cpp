#include "config.h"
#include <HardwareSerial.h>
#include <TinyGPS++.h>
#include "gps.h"


HardwareSerial gpsSerial(2);
TinyGPSPlus gps;

void gps_init(){
    gpsSerial.begin(9600, SERIAL_8N1, GPS_RX, GPS_TX);
}

void gps_read(){
    while(gpsSerial.available()){
        gps.encode(gpsSerial.read());
    }

    if(gps.location.isUpdated()){
        Serial.print("Lat: ");
        Serial.println(gps.location.lat());
    }
}