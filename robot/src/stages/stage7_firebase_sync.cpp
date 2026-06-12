/**
 * @file    stage7_firebase_sync.cpp
 * @brief   GIAI ĐOẠN 7 — Đồng Bộ Tọa Độ GPS lên Firebase Realtime Database
 * @project Robot Tự Hành | Mã Dự Án: 1682
 *
 * MỤC TIÊU:
 *   Đồng bộ vị trí của robot (Vĩ độ/Kinh độ) lên Firebase Realtime Database theo mạng di động GPRS.
 *   Vì Firebase bắt buộc kết nối bảo mật SSL (HTTPS), trong khi SIM900A không tự xử lý được TLS,
 *   ta sử dụng ESP_SSLClient để bọc ngoài TinyGsmClient.
 *   Đọc dữ liệu GPS từ HardwareSerial Serial2 và đẩy lên `/robot/location/lat` và `/robot/location/lng`.
 *   Nếu test trong nhà (không bắt được sóng GPS), tự động đẩy tọa độ giả lập (Đà Nẵng) để kiểm thử luồng.
 *
 * SƠ ĐỒ CHÂN (CONTEXT_ROBOT.md):
 *   SIM900A: TX -> RX_ESP = GPIO 34 (Input-Only, qua mạch phân áp)
 *            RX -> TX_ESP = GPIO 25
 *   GPS GT-U7: TX -> RX2 = GPIO 16
 *              RX -> TX2 = GPIO 17
 *
 * BUILD & NẠP MẠCH:
 *   pio run -e s7 -t upload
 *   pio device monitor          (115200 baud)
 */

#include <Arduino.h>
#include <HardwareSerial.h>
#include <SoftwareSerial.h>
#include <TinyGPS++.h>

// Định nghĩa modem SIM900A tương thích driver SIM800 trong TinyGSM
#define TINY_GSM_MODEM_SIM800
#include <TinyGsmClient.h>

#include <ESP_SSLClient.h>

// Định nghĩa các macro để bật tính năng Database và User Authentication trong FirebaseClient v2.x
#define ENABLE_DATABASE
#define ENABLE_USER_AUTH
#include <FirebaseClient.h>

// ─── CẤU HÌNH THÔNG TIN FIREBASE (PLACEHOLDER) ──────────────────────────────
#define API_KEY "/* PLACEHOLDER_API_KEY */"
#define DATABASE_URL "/* PLACEHOLDER_DATABASE_URL */"
#define USER_EMAIL "/* PLACEHOLDER_EMAIL */"
#define USER_PASSWORD "/* PLACEHOLDER_PASSWORD */"

// ─── THIẾT BỊ NGOẠI VI ──────────────────────────────────────────────────────
// Khởi tạo GPS
TinyGPSPlus gps;

// Khởi tạo SIM900A
#define PIN_SIM_TX 34
#define PIN_SIM_RX 25
SoftwareSerial SerialAT(PIN_SIM_TX, PIN_SIM_RX);

// APN Nhà mạng Mobifone
const char apn[]      = "m-wap";
const char gprsUser[] = "";
const char gprsPass[] = "";

// ─── KIẾN TRÚC MẠNG BỌC SSL CHO FIREBASE ─────────────────────────────────────
TinyGsm modem(SerialAT);
TinyGsmClient gsm_client(modem, 0); // Socket index 0
ESP_SSLClient ssl_client;

// Khởi tạo Firebase AsyncClient trực tiếp bọc ssl_client (FirebaseClient v2.x độc lập mạng)
using AsyncClient = AsyncClientClass;
AsyncClient aClient(ssl_client);

FirebaseApp app;
RealtimeDatabase Database;
UserAuth user_auth(API_KEY, USER_EMAIL, USER_PASSWORD);

void setup() {
    Serial.begin(115200);
    while (!Serial) {
        delay(10);
    }

    Serial.println("\n==================================================");
    Serial.println("KHIỂN THỬ GIAI ĐOẠN 7: FIREBASE SYNC OVER GPRS SSL");
    Serial.println("==================================================");

    // 1. Khởi tạo GPS GT-U7 trên Serial2
    Serial2.begin(9600, SERIAL_8N1, 16, 17);
    Serial.println("[OK] Đã khởi tạo Serial2 cho GPS (9600 bps).");

    // 2. Khởi tạo SIM900A
    SerialAT.begin(9600);
    Serial.println("Đang khởi động modem SIM900A... Vui lòng đợi 10 giây...");
    delay(10000); // Đợi modem khởi động

    if (!modem.restart()) {
        Serial.println("[LỖI] Không thể kết nối hoặc khởi động lại modem SIM900A.");
        while (1) delay(1000);
    }
    Serial.println("[OK] Kết nối SIM900A thành công.");

    // Kết nối GPRS
    Serial.print("Đang kết nối GPRS (APN: ");
    Serial.print(apn);
    Serial.println(")...");
    if (!modem.gprsConnect(apn, gprsUser, gprsPass)) {
        Serial.println("[LỖI] Kết nối GPRS thất bại!");
        while (1) delay(1000);
    }
    Serial.println("[OK] Kết nối GPRS Mobifone thành công.");

    // 3. Cấu hình cổng bọc SSL
    ssl_client.setClient(&gsm_client);
    ssl_client.setInsecure(); // Tắt kiểm tra chứng chỉ SSL để giảm tải bộ nhớ và đơn giản kiểm thử
    Serial.println("[OK] Bọc SSL qua ESP_SSLClient thành công.");

    // 4. Khởi tạo Firebase App & Realtime Database
    Serial.println("Đang khởi tạo kết nối Firebase...");
    initializeApp(aClient, app, getAuth(user_auth));
    app.getApp<RealtimeDatabase>(Database);
    Database.url(DATABASE_URL);
    Serial.println("[OK] Đã hoàn tất cấu hình Firebase.");
}

void loop() {
    // Luôn gọi app.loop() để duy trì phiên làm việc và làm tươi token của Firebase
    app.loop();

    // Đọc dữ liệu liên tục từ GPS
    while (Serial2.available() > 0) {
        gps.encode(Serial2.read());
    }

    static unsigned long last_sync_ms = 0;
    // Chu kỳ 2000ms thực hiện đẩy dữ liệu lên Firebase (non-blocking)
    if (millis() - last_sync_ms >= 2000) {
        last_sync_ms = millis();

        double lat = 0.0;
        double lng = 0.0;
        bool hasFix = false;

        if (gps.location.isValid()) {
            lat = gps.location.lat();
            lng = gps.location.lng();
            hasFix = true;
        }

        // Nếu test trong nhà hoặc chưa có sóng vệ tinh, sử dụng tọa độ giả lập (Cầu Rồng, Đà Nẵng)
        if (!hasFix) {
            lat = 16.061245; 
            lng = 108.227448;
            Serial.println("[GPS] Chưa fix được vị trí. Sử dụng tọa độ giả lập (Đà Nẵng).");
        } else {
            Serial.printf("[GPS] Đã fix vị trí thực tế: Lat = %f, Lng = %f\n", lat, lng);
        }

        Serial.println("[Firebase] Đang đẩy tọa độ lên database...");

        // Gửi dữ liệu không đồng bộ lên node giao kèo: /robot/location/lat và /robot/location/lng
        // Sử dụng await mode để hiển thị trạng thái kết quả gửi trực tiếp ra Serial.
        bool lat_ok = Database.set<double>(aClient, "/robot/location/lat", lat);
        bool lng_ok = Database.set<double>(aClient, "/robot/location/lng", lng);

        if (lat_ok && lng_ok) {
            Serial.printf("[Firebase] Gửi tọa độ thành công! Lat: %f, Lng: %f\n", lat, lng);
        } else {
            Serial.println("[Firebase] Gửi dữ liệu thất bại! Hãy kiểm tra kết nối mạng di động hoặc cấu hình Firebase.");
        }
    }
}
