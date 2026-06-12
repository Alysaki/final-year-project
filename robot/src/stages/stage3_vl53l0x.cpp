/**
 * @file    stage3_vl53l0x.cpp
 * @brief   GIAI ĐOẠN 3 — Khởi Tạo 4 Cảm Biến Laser VL53L0X Đa Địa Chỉ I2C
 * @project Robot Tự Hành | Mã Dự Án: 1682
 *
 * MỤC TIÊU:
 *   Đọc khoảng cách vật cản ở 4 hướng: Trước, Sau, Trái, Phải dùng VL53L0X.
 *   Vì các cảm biến có cùng địa chỉ I2C mặc định (0x29), ta sử dụng chân XSHUT
 *   để bật từng cảm biến lên và đổi địa chỉ I2C động (0x30 -> 0x33) tuần tự.
 *   Áp dụng hàm i2c_bus_recovery() để khôi phục bus I2C trước khi khởi tạo.
 *   In khoảng cách đo được ra Serial Monitor mỗi 100ms.
 *
 * CHÂN ĐIỀU KHIỂN XSHUT (Mục 2.2 — CONTEXT_ROBOT.md):
 *   Trước (Front) : GPIO 4   -> Địa chỉ mới: 0x30
 *   Sau (Back)    : GPIO 13  -> Địa chỉ mới: 0x31
 *   Trái (Left)   : GPIO 14  -> Địa chỉ mới: 0x32
 *   Phải (Right)  : GPIO 27  -> Địa chỉ mới: 0x33
 *
 * CHÂN GIAO TIẾP I2C (Mục 2.1 — CONTEXT_ROBOT.md):
 *   SDA = GPIO 5
 *   SCL = GPIO 15
 *
 * BUILD & NẠP MẠCH:
 *   pio run -e s3 -t upload
 *   pio device monitor          (115200 baud)
 */

#include <Arduino.h>
#include <Wire.h>
#include <VL53L0X.h>

// Định nghĩa các chân I2C
#define I2C_SDA 5
#define I2C_SCL 15

// Định nghĩa chân XSHUT điều khiển nguồn cảm biến
#define PIN_XSHUT_FRONT 4
#define PIN_XSHUT_BACK  13
#define PIN_XSHUT_LEFT  14
#define PIN_XSHUT_RIGHT 27

// Khởi tạo các đối tượng cảm biến
VL53L0X sensorFront;
VL53L0X sensorBack;
VL53L0X sensorLeft;
VL53L0X sensorRight;

/**
 * @brief Khôi phục bus I2C bị kẹt bằng cách phát 9 xung clock thủ công.
 * Gọi hàm này TRƯỚC Wire.begin() trong setup() để giải phóng slave bị kẹt.
 * Tham chiếu: CONTEXT_ROBOT.md — Mục 7.4 [GĐ2-HW-003]
 */
void i2c_bus_recovery() {
    pinMode(I2C_SDA, OUTPUT);
    pinMode(I2C_SCL, OUTPUT);

    // Đưa SDA lên HIGH để kiểm tra bus có bị kẹt không
    digitalWrite(I2C_SDA, HIGH);
    delayMicroseconds(5);

    // Nếu SDA vẫn bị kéo xuống LOW bởi slave, phát 9 xung clock để giải phóng
    for (int i = 0; i < 9; i++) {
        digitalWrite(I2C_SCL, LOW);
        delayMicroseconds(5);
        digitalWrite(I2C_SCL, HIGH);
        delayMicroseconds(5);
    }

    // Gửi điều kiện STOP: SDA từ LOW → HIGH khi SCL = HIGH
    digitalWrite(I2C_SDA, LOW);  delayMicroseconds(5);
    digitalWrite(I2C_SCL, HIGH); delayMicroseconds(5);
    digitalWrite(I2C_SDA, HIGH); delayMicroseconds(5);

    // Trả quyền điều khiển chân về cho thư viện Wire
    pinMode(I2C_SDA, INPUT_PULLUP);
    pinMode(I2C_SCL, INPUT_PULLUP);
    delay(10);

    Serial.println("[I2C] Bus recovery hoàn tất.");
}

void setup() {
    Serial.begin(115200);
    while (!Serial) {
        delay(10); // Đợi Serial kết nối
    }

    Serial.println("\n==================================================");
    Serial.println("KHIỂN THỬ GIAI ĐOẠN 3: KHỞI TẠO 4X VL53L0X");
    Serial.println("==================================================");

    // Bước 1: Khởi tạo các chân điều khiển XSHUT làm OUTPUT và kéo xuống LOW
    // Việc này sẽ tắt toàn bộ 4 cảm biến để chúng không phản hồi trên địa chỉ 0x29 mặc định.
    pinMode(PIN_XSHUT_FRONT, OUTPUT);
    pinMode(PIN_XSHUT_BACK,  OUTPUT);
    pinMode(PIN_XSHUT_LEFT,  OUTPUT);
    pinMode(PIN_XSHUT_RIGHT, OUTPUT);

    digitalWrite(PIN_XSHUT_FRONT, LOW);
    digitalWrite(PIN_XSHUT_BACK,  LOW);
    digitalWrite(PIN_XSHUT_LEFT,  LOW);
    digitalWrite(PIN_XSHUT_RIGHT, LOW);
    delay(10); // Đợi 10ms tắt hoàn toàn cảm biến

    // Bước 2: Thực hiện xả bus I2C
    i2c_bus_recovery();

    // Bước 3: Khởi tạo giao tiếp I2C sạch (SDA=5, SCL=15)
    Wire.begin(I2C_SDA, I2C_SCL);
    Wire.setClock(100000); // 100 kHz
    delay(100);

    // Bước 4: Khởi tạo và gán địa chỉ động tuần tự cho từng cảm biến

    // --- Cảm biến 1: TRƯỚC (Front) ---
    Serial.println("Đang khởi tạo cảm biến TRƯỚC (Front)...");
    digitalWrite(PIN_XSHUT_FRONT, HIGH); // Bật cảm biến Front lên
    delay(10); // Chờ 10ms ổn định
    if (!sensorFront.init()) {
        Serial.println("[LỖI] Khởi tạo cảm biến TRƯỚC thất bại!");
        while (1) delay(10);
    }
    sensorFront.setAddress(0x30); // Đổi địa chỉ mặc định từ 0x29 sang 0x30
    sensorFront.setTimeout(500);
    Serial.println("[OK] Cảm biến TRƯỚC khởi tạo thành công tại địa chỉ 0x30.");

    // --- Cảm biến 2: SAU (Back) ---
    Serial.println("Đang khởi tạo cảm biến SAU (Back)...");
    digitalWrite(PIN_XSHUT_BACK, HIGH); // Bật cảm biến Back lên
    delay(10);
    if (!sensorBack.init()) {
        Serial.println("[LỖI] Khởi tạo cảm biến SAU thất bại!");
        while (1) delay(10);
    }
    sensorBack.setAddress(0x31); // Đổi địa chỉ mặc định từ 0x29 sang 0x31
    sensorBack.setTimeout(500);
    Serial.println("[OK] Cảm biến SAU khởi tạo thành công tại địa chỉ 0x31.");

    // --- Cảm biến 3: TRÁI (Left) ---
    Serial.println("Đang khởi tạo cảm biến TRÁI (Left)...");
    digitalWrite(PIN_XSHUT_LEFT, HIGH); // Bật cảm biến Left lên
    delay(10);
    if (!sensorLeft.init()) {
        Serial.println("[LỖI] Khởi tạo cảm biến TRÁI thất bại!");
        while (1) delay(10);
    }
    sensorLeft.setAddress(0x32); // Đổi địa chỉ mặc định từ 0x29 sang 0x32
    sensorLeft.setTimeout(500);
    Serial.println("[OK] Cảm biến TRÁI khởi tạo thành công tại địa chỉ 0x32.");

    // --- Cảm biến 4: PHẢI (Right) ---
    Serial.println("Đang khởi tạo cảm biến PHẢI (Right)...");
    digitalWrite(PIN_XSHUT_RIGHT, HIGH); // Bật cảm biến Right lên
    delay(10);
    if (!sensorRight.init()) {
        Serial.println("[LỖI] Khởi tạo cảm biến PHẢI thất bại!");
        while (1) delay(10);
    }
    sensorRight.setAddress(0x33); // Đổi địa chỉ mặc định từ 0x29 sang 0x33
    sensorRight.setTimeout(500);
    Serial.println("[OK] Cảm biến PHẢI khởi tạo thành công tại địa chỉ 0x33.");

    Serial.println("\n-> Tất cả cảm biến khởi tạo hoàn tất. Bắt đầu đo khoảng cách.");
}

void loop() {
    static unsigned long last_read_ms = 0;

    // Đọc và in dữ liệu mỗi 100ms (non-blocking)
    if (millis() - last_read_ms >= 100) {
        last_read_ms = millis();

        // Đọc khoảng cách từ các cảm biến (đơn vị mm)
        uint16_t distFront = sensorFront.readRangeSingleMillimeters();
        uint16_t distBack  = sensorBack.readRangeSingleMillimeters();
        uint16_t distLeft  = sensorLeft.readRangeSingleMillimeters();
        uint16_t distRight = sensorRight.readRangeSingleMillimeters();

        // Kiểm tra lỗi timeout của từng cảm biến
        bool errFront = sensorFront.timeoutOccurred();
        bool errBack  = sensorBack.timeoutOccurred();
        bool errLeft  = sensorLeft.timeoutOccurred();
        bool errRight = sensorRight.timeoutOccurred();

        // In kết quả
        Serial.print("F: ");
        if (errFront) Serial.print("TIMEOUT");
        else Serial.print(distFront);
        Serial.print(" mm | ");

        Serial.print("B: ");
        if (errBack) Serial.print("TIMEOUT");
        else Serial.print(distBack);
        Serial.print(" mm | ");

        Serial.print("L: ");
        if (errLeft) Serial.print("TIMEOUT");
        else Serial.print(distLeft);
        Serial.print(" mm | ");

        Serial.print("R: ");
        if (errRight) Serial.print("TIMEOUT");
        else Serial.print(distRight);
        Serial.println(" mm");
    }
}
