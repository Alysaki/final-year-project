/**
 * @file    stage6_gprs_control.cpp
 * @brief   GIAI ĐOẠN 6 — Điều Khiển Động Cơ L298N qua GPRS HTTP (Cellular)
 * @project Robot Tự Hành | Mã Dự Án: 1682
 *
 * MỤC TIÊU:
 *   Khởi tạo SoftwareSerial (34, 25) giao tiếp với SIM900A ở tốc độ 9600 bps.
 *   Kết nối mạng di động GPRS mạng Mobifone (APN: "m-wap").
 *   Khởi tạo cấu hình các chân L298N (ENA, IN1, IN2, ENB, IN3, IN4).
 *   Mỗi 2000ms gửi một HTTP GET Request lên Mock Server công cộng (httpbin.org/anything?cmd=FORWARD).
 *   Nhận response, giải mã lệnh và điều khiển bánh xe tương ứng.
 *
 * SƠ ĐỒ CHÂN (Mục 2.4 & 2.5 — CONTEXT_ROBOT.md):
 *   SIM900A: TX -> RX_ESP = GPIO 34 (Input-Only, qua mạch phân áp)
 *            RX -> TX_ESP = GPIO 25 (Xuất 3.3V trực tiếp)
 *   L298N: ENA=32, IN1=33, IN2=26, ENB=19, IN3=18, IN4=23
 *
 * BUILD & NẠP MẠCH:
 *   pio run -e s6 -t upload
 *   pio device monitor          (115200 baud)
 */

#include <Arduino.h>
#include <SoftwareSerial.h>

// Định nghĩa modem SIM900A tương thích driver SIM800 trong TinyGSM
#define TINY_GSM_MODEM_SIM800
#include <TinyGsmClient.h>

// ─── CẤU HÌNH KẾT NỐI SIM900A ───────────────────────────────────────────────
#define PIN_SIM_TX 34 // TX của SIM900A nối với RX của ESP32 (GPIO 34)
#define PIN_SIM_RX 25 // RX của SIM900A nối với TX của ESP32 (GPIO 25)
SoftwareSerial SerialAT(PIN_SIM_TX, PIN_SIM_RX);

// Cấu hình mạng Mobifone APN
const char apn[]      = "m-wap";
const char gprsUser[] = "";
const char gprsPass[] = "";

TinyGsm modem(SerialAT);
TinyGsmClient gsm_client(modem);

// ─── SƠ ĐỒ CHÂN L298N (Mục 2.5 — CONTEXT_ROBOT.md) ─────────────────────────
#define PIN_ENA 32 
#define PIN_IN1 33 
#define PIN_IN2 26 
#define PIN_ENB 19 
#define PIN_IN3 18 
#define PIN_IN4 23 

#define PWM_FREQ 5000
#define PWM_RESOLUTION 8
#define PWM_CHANNEL_A 0
#define PWM_CHANNEL_B 1
#define MOTOR_SPEED 100 // Tốc độ chạy vi sai mặc định (0-255)

// ─── KHỐI ĐIỀU KHIỂN ĐỘNG CƠ L298N ────────────────────────────────────────────
void setMotorSpeed(uint8_t speedA, uint8_t speedB) {
    ledcWrite(PWM_CHANNEL_A, speedA);
    ledcWrite(PWM_CHANNEL_B, speedB);
}

void motorStop() {
    digitalWrite(PIN_IN1, LOW);
    digitalWrite(PIN_IN2, LOW);
    digitalWrite(PIN_IN3, LOW);
    digitalWrite(PIN_IN4, LOW);
    setMotorSpeed(0, 0);
    Serial.println("[MOTOR] || DỪNG");
}

void motorForward() {
    digitalWrite(PIN_IN1, HIGH);
    digitalWrite(PIN_IN2, LOW);
    digitalWrite(PIN_IN3, HIGH);
    digitalWrite(PIN_IN4, LOW);
    setMotorSpeed(MOTOR_SPEED, MOTOR_SPEED);
    Serial.println("[MOTOR] >> TIẾN");
}

void motorBackward() {
    digitalWrite(PIN_IN1, LOW);
    digitalWrite(PIN_IN2, HIGH);
    digitalWrite(PIN_IN3, LOW);
    digitalWrite(PIN_IN4, HIGH);
    setMotorSpeed(MOTOR_SPEED, MOTOR_SPEED);
    Serial.println("[MOTOR] << LÙI");
}

void motorLeft() {
    // Xoay trái tại chỗ
    digitalWrite(PIN_IN1, LOW);
    digitalWrite(PIN_IN2, HIGH);
    digitalWrite(PIN_IN3, HIGH);
    digitalWrite(PIN_IN4, LOW);
    setMotorSpeed(MOTOR_SPEED, MOTOR_SPEED);
    Serial.println("[MOTOR] <- TRÁI");
}

void motorRight() {
    // Xoay phải tại chỗ
    digitalWrite(PIN_IN1, HIGH);
    digitalWrite(PIN_IN2, LOW);
    digitalWrite(PIN_IN3, LOW);
    digitalWrite(PIN_IN4, HIGH);
    setMotorSpeed(MOTOR_SPEED, MOTOR_SPEED);
    Serial.println("[MOTOR] -> PHẢI");
}

void setup() {
    Serial.begin(115200);
    while (!Serial) {
        delay(10);
    }

    Serial.println("\n==================================================");
    Serial.println("KHIỂN THỬ GIAI ĐOẠN 6: GPRS HTTP CONTROL (SIM900A)");
    Serial.println("==================================================");

    // ── Khởi tạo chân L298N ──
    pinMode(PIN_IN1, OUTPUT);
    pinMode(PIN_IN2, OUTPUT);
    pinMode(PIN_IN3, OUTPUT);
    pinMode(PIN_IN4, OUTPUT);
    motorStop();

    // Cấu hình PWM cho chân ENA/ENB
    ledcSetup(PWM_CHANNEL_A, PWM_FREQ, PWM_RESOLUTION);
    ledcAttachPin(PIN_ENA, PWM_CHANNEL_A);
    ledcWrite(PWM_CHANNEL_A, 0);

    ledcSetup(PWM_CHANNEL_B, PWM_FREQ, PWM_RESOLUTION);
    ledcAttachPin(PIN_ENB, PWM_CHANNEL_B);
    ledcWrite(PWM_CHANNEL_B, 0);
    Serial.println("[OK] Đã cấu hình PWM cho động cơ L298N.");

    // ── Khởi tạo SIM900A ──
    SerialAT.begin(9600);
    Serial.println("Đang khởi động modem SIM900A... Vui lòng đợi 10 giây (ổn định nguồn)...");
    delay(10000); // Đợi SIM900A khởi tạo hoàn tất theo context khuyến nghị

    if (!modem.restart()) {
        Serial.println("[LỖI] Không thể kết nối hoặc khởi động lại modem SIM900A.");
        while (1) delay(1000);
    }
    Serial.println("[OK] Đã kết nối với SIM900A.");

    // Đăng ký GPRS Mobifone
    Serial.print("Đang kết nối GPRS (APN: ");
    Serial.print(apn);
    Serial.println(")...");
    
    if (!modem.gprsConnect(apn, gprsUser, gprsPass)) {
        Serial.println("[LỖI] Kết nối GPRS thất bại!");
        while (1) delay(1000);
    }
    Serial.println("[OK] Kết nối GPRS Mobifone thành công.");
}

void loop() {
    static unsigned long last_http_ms = 0;

    // Chu kỳ 2000ms gửi HTTP GET request (non-blocking)
    if (millis() - last_http_ms >= 2000) {
        last_http_ms = millis();

        // Sử dụng Mock Server công cộng (httpbin.org) để trả về lệnh giả lập.
        // Ở đây truyền tham số ?cmd=FORWARD để server trả về JSON chứa chữ FORWARD.
        const char server_host[] = "httpbin.org";
        const char server_path[] = "/anything?cmd=FORWARD";
        const int port = 80;

        Serial.printf("\n[HTTP] Đang kết nối tới %s...\n", server_host);

        if (gsm_client.connect(server_host, port)) {
            Serial.println("[HTTP] Đã kết nối. Đang gửi GET Request...");
            
            // Gửi HTTP GET Request thủ công qua TCP client
            gsm_client.print(String("GET ") + server_path + " HTTP/1.1\r\n" +
                             "Host: " + server_host + "\r\n" +
                             "Connection: close\r\n\r\n");

            // Đọc response
            unsigned long timeout = millis();
            String response = "";
            while (gsm_client.connected() && millis() - timeout < 5000) {
                while (gsm_client.available()) {
                    char c = gsm_client.read();
                    response += c;
                    timeout = millis();
                }
            }
            gsm_client.stop();

            Serial.println("[HTTP] Đã nhận được phản hồi từ server.");
            
            // Tìm và giải mã lệnh từ nội dung phản hồi
            if (response.indexOf("FORWARD") >= 0) {
                Serial.println("[HTTP] Giải mã lệnh nhận được: FORWARD");
                motorForward();
            } else if (response.indexOf("BACKWARD") >= 0) {
                Serial.println("[HTTP] Giải mã lệnh nhận được: BACKWARD");
                motorBackward();
            } else if (response.indexOf("LEFT") >= 0) {
                Serial.println("[HTTP] Giải mã lệnh nhận được: LEFT");
                motorLeft();
            } else if (response.indexOf("RIGHT") >= 0) {
                Serial.println("[HTTP] Giải mã lệnh nhận được: RIGHT");
                motorRight();
            } else if (response.indexOf("STOP") >= 0) {
                Serial.println("[HTTP] Giải mã lệnh nhận được: STOP");
                motorStop();
            } else {
                Serial.println("[HTTP] Không tìm thấy lệnh điều khiển hợp lệ trong response.");
            }
        } else {
            Serial.println("[HTTP] Kết nối tới Mock Server thất bại.");
        }
    }
}
