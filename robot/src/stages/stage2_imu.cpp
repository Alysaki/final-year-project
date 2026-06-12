/**
 * @file    stage2_imu.cpp
 * @brief   GIAI ĐOẠN 2 — Đọc Góc Nghiêng Từ IMU GY-91 (MPU9250)
 * @project Robot Tự Hành | Mã Dự Án: 1682
 *
 * MỤC TIÊU:
 *   Khởi tạo MPU9250 trên bus I2C sạch (SDA=GPIO5, SCL=GPIO15).
 *   Thực hiện thuật toán xả bus để tránh kẹt I2C.
 *   Hiệu chuẩn Accel/Gyro (giữ yên xe) và Magnetometer (xoay hình số 8).
 *   In các góc Yaw | Pitch | Roll ra Serial Monitor mỗi 100ms.
 *
 * SƠ ĐỒ CHÂN I2C (Mục 2.1 — CONTEXT_ROBOT.md):
 *   SDA = GPIO 5
 *   SCL = GPIO 15
 *
 * BUILD & NẠP MẠCH:
 *   pio run -e s2 -t upload
 *   pio device monitor          (115200 baud)
 */

#include <Arduino.h>
#include <Wire.h>
#include <MPU9250.h>

// Định nghĩa chân I2C
#define I2C_SDA 5
#define I2C_SCL 15

// Khởi tạo đối tượng MPU9250
MPU9250 mpu;

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
        delay(10); // Đợi Serial kết nối (dành cho một số kit test)
    }
    
    Serial.println("\n==================================================");
    Serial.println("KHIỂN THỬ GIAI ĐOẠN 2: ĐỌC IMU GY-91 (MPU9250)");
    Serial.println("==================================================");

    // Bước 1: Xả bus I2C phòng tránh kẹt phần cứng
    i2c_bus_recovery();

    // Bước 2: Khởi tạo I2C sạch (SDA=5, SCL=15)
    Wire.begin(I2C_SDA, I2C_SCL);
    Wire.setClock(100000); // Standard Mode: 100 kHz
    delay(200); // Delay ổn định mạch điện

    // Bước 3: Kiểm tra kết nối với cảm biến
    if (!mpu.setup(0x68)) {
        while (1) {
            Serial.println("[LỖI] Không tìm thấy hoặc kết nối thất bại tới MPU9250 (Địa chỉ 0x68).");
            delay(1000);
        }
    }
    Serial.println("[OK] Kết nối MPU9250 thành công.");

    // Bước 4: Hiệu chuẩn Accel & Gyro
    Serial.println("\n[HIỆU CHUẨN ACCEL/GYRO] Vui lòng đặt yên xe trên mặt phẳng cố định!");
    for (int i = 5; i > 0; i--) {
        Serial.printf("Bắt đầu sau: %d...\n", i);
        delay(1000);
    }
    Serial.println("Đang hiệu chuẩn Accel/Gyro... Vui lòng giữ yên tuyệt đối...");
    mpu.calibrateAccelGyro();
    Serial.println("[OK] Hiệu chuẩn Accel/Gyro hoàn tất.");

    // Bước 5: Hiệu chuẩn Magnetometer (La bàn số)
    Serial.println("\n[HIỆU CHUẨN MAGNETOMETER] Vui lòng nhấc xe lên và xoay hình số 8 liên tục!");
    for (int i = 3; i > 0; i--) {
        Serial.printf("Bắt đầu sau: %d...\n", i);
        delay(1000);
    }
    Serial.println("Đang hiệu chuẩn Magnetometer... Hãy xoay xe hình số 8 liên tục...");
    mpu.calibrateMag();
    Serial.println("[OK] Hiệu chuẩn Magnetometer hoàn tất.");

    Serial.println("\n-> Bắt đầu đọc góc Yaw | Pitch | Roll.");
}

void loop() {
    static unsigned long last_print_ms = 0;
    
    // Đọc cập nhật dữ liệu từ cảm biến liên tục
    mpu.update();

    // In góc ra Serial Monitor định kỳ 100ms
    if (millis() - last_print_ms >= 100) {
        last_print_ms = millis();
        
        float yaw = mpu.getYaw();
        float pitch = mpu.getPitch();
        float roll = mpu.getRoll();

        Serial.printf("Yaw: %6.2f | Pitch: %6.2f | Roll: %6.2f\n", yaw, pitch, roll);
    }
}
