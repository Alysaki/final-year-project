/**
 * @file    stage4_gps.cpp
 * @brief   GIAI ĐOẠN 4 — Kiểm Thử Định Vị Vệ Tinh GPS GT-U7
 * @project Robot Tự Hành | Mã Dự Án: 1682
 *
 * MỤC TIÊU:
 *   Khởi tạo HardwareSerial Serial2 để giao tiếp với GPS GT-U7 qua chân RX2=16, TX2=17 ở tốc độ 9600 bps.
 *   Giải mã các bản tin NMEA qua thư viện TinyGPS++.
 *   In Lat/Lng ra Serial Monitor chính xác 6 chữ số thập phân khi đã fix được vị trí.
 *   In thông báo chờ tìm vệ tinh nếu chưa fix được vị trí.
 *
 * SƠ ĐỒ CHÂN GPS (Mục 2.3 — CONTEXT_ROBOT.md):
 *   TX (GPS) -> RX2 (ESP32) = GPIO 16
 *   RX (GPS) -> TX2 (ESP32) = GPIO 17
 *   Baud rate: 9600 bps
 *
 * BUILD & NẠP MẠCH:
 *   pio run -e s4 -t upload
 *   pio device monitor          (115200 baud)
 */

#include <Arduino.h>
#include <HardwareSerial.h>
#include <TinyGPS++.h>

// Khởi tạo đối tượng GPS
TinyGPSPlus gps;

void setup() {
    // Serial giao tiếp với máy tính (cho mục đích debug)
    Serial.begin(115200);
    while (!Serial) {
        delay(10);
    }

    Serial.println("\n==================================================");
    Serial.println("KHIỂN THỬ GIAI ĐOẠN 4: ĐỌC TỌA ĐỘ GPS GT-U7");
    Serial.println("==================================================");

    // Khởi tạo HardwareSerial 2: RX2=GPIO 16, TX2=GPIO 17
    Serial2.begin(9600, SERIAL_8N1, 16, 17);
    Serial.println("[OK] Đã khởi tạo Serial2 kết nối với GPS GT-U7 (9600 bps).");
    Serial.println("-> Đang chờ giải mã dữ liệu NMEA... (Cần kiểm thử ngoài trời hoặc gần cửa sổ)");
}

void loop() {
    // Đọc dữ liệu liên tục từ module GPS đưa vào bộ giải mã TinyGPS++
    while (Serial2.available() > 0) {
        gps.encode(Serial2.read());
    }

    // Thực hiện in thông tin định kỳ mỗi 1000ms (non-blocking)
    static unsigned long last_print_ms = 0;
    if (millis() - last_print_ms >= 1000) {
        last_print_ms = millis();

        // Kiểm tra xem vị trí đã được cập nhật/xác định chưa
        if (gps.location.isValid() && gps.location.isUpdated()) {
            double lat = gps.location.lat();
            double lng = gps.location.lng();
            
            // In tọa độ chính xác 6 chữ số thập phân
            Serial.print("[GPS] Tọa độ hiện tại: Lat = ");
            Serial.print(lat, 6);
            Serial.print(" | Lng = ");
            Serial.println(lng, 6);
            
            // In thêm số lượng vệ tinh kết nối (nếu có)
            if (gps.satellites.isValid()) {
                Serial.printf("      Số lượng vệ tinh: %d\n", gps.satellites.value());
            }
        } else {
            // Chưa fix được vị trí hoặc chưa có đủ số lượng vệ tinh
            Serial.println("[GPS] Chưa fix được vị trí GPS (Đang tìm vệ tinh...)");
            
            // Cảnh báo thêm nếu không nhận được dữ liệu thô (có thể do dây nối sai chân RX/TX)
            if (millis() > 5000 && gps.charsProcessed() < 10) {
                Serial.println("      [CẢNH BÁO] Không nhận thấy dữ liệu Serial gửi về từ GPS. Hãy kiểm tra lại dây nối RX2/TX2!");
            }
        }
    }
}
