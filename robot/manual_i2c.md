# HƯỚNG DẪN ĐẤU NỐI & KIỂM THỬ BUS I2C (MÃ DỰ ÁN: 1682)

Tài liệu này hướng dẫn chi tiết cách đấu nối song song cụm 5 cảm biến (1x IMU GY-91 + 4x Laser VL53L0X) vào bus I2C sạch (SDA=5, SCL=15) trên Shield SVG của ESP32 DevKit, cùng các lệnh nạp mạch kiểm thử độc lập cho Giai đoạn 2 (`s2`) và Giai đoạn 3 (`s3`).

---

## 1. SƠ ĐỒ ĐẤU NỐI VẬT LÝ TRÊN SHIELD SVG

### 1.1 Nguyên lý kết nối song song Bus I2C
Tất cả 5 cảm biến được đấu nối song song chung đường dữ liệu (SDA) và đường xung nhịp (SCL) vào cọc ra chân GPIO của Shield SVG:

```
                  ┌─────────────── ESP32 SHIELD SVG ───────────────┐
                  │   [VCC 3.3V]      [GND]      [GPIO 5]  [GPIO 15]   │
                  └───────┬─────────────┬────────────┬─────────┬───────┘
                          │             │            │         │
       ┌──────────────────┼─────────────┼────────────┼─────────┼──────────────────┐
       │ (Song song VCC)  │ (Song song) │            │ (SDA)   │ (SCL)            │
       ▼                  ▼             ▼            ▼         ▼                  ▼
┌──────────────┐    ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐
│  IMU GY-91   │    │ VL53L0X_F │ │ VL53L0X_B │ │ VL53L0X_L │ │ VL53L0X_R │
│  (MPU9250)   │    │  (Trước)  │ │   (Sau)   │ │  (Trái)   │ │  (Phải)   │
└──────────────┘    └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
                          │             │             │             │
                          │ (XSHUT)     │ (XSHUT)     │ (XSHUT)     │ (XSHUT)
                          ▼             ▼             ▼             ▼
                      [GPIO 4]      [GPIO 13]     [GPIO 14]     [GPIO 27]
```

### 1.2 Bảng chi tiết đấu nối các chân

| Thiết bị | Chân cảm biến | Chân trên Shield SVG | Ghi chú |
| :--- | :--- | :--- | :--- |
| **TẤT CẢ 5 CẢM BIẾN** | **VCC** / **VIN** | **3.3V** (hoặc 5V tùy loại module hỗ trợ) | Cấp nguồn song song |
| | **GND** | **GND** | Nối đất chung |
| | **SDA** | **GPIO 5** (Cọc P5) | Đường truyền dữ liệu I2C |
| | **SCL** | **GPIO 15** (Cọc P15) | Đường phát xung nhịp I2C |
| **IMU GY-91** | *Không có* | *Không nối chân XSHUT hay AD0* | Địa chỉ mặc định cố định `0x68` |
| **VL53L0X Trước (Front)** | **XSHUT** | **GPIO 4** (Cọc P4) | Điều khiển bật/tắt để gán địa chỉ `0x30` |
| **VL53L0X Sau (Back)** | **XSHUT** | **GPIO 13** (Cọc P13) | Điều khiển bật/tắt để gán địa chỉ `0x31` |
| **VL53L0X Trái (Left)** | **XSHUT** | **GPIO 14** (Cọc P14) | Điều khiển bật/tắt để gán địa chỉ `0x32` |
| **VL53L0X Phải (Right)** | **XSHUT** | **GPIO 27** (Cọc P27) | Điều khiển bật/tắt để gán địa chỉ `0x33` |

> [!WARNING]
> - Cảm biến Laser VL53L0X **bắt buộc phải nối chân XSHUT** để đổi địa chỉ động. Nếu không nối XSHUT, cả 4 cảm biến sẽ bị trùng địa chỉ mặc định `0x29` và gây xung đột bus I2C khiến hệ thống tê liệt.
> - Hãy kiểm tra các mối hàn và dây nối kỹ lưỡng để tránh sụt áp hoặc nhiễu đường truyền tín hiệu I2C.

---

## 2. HƯỚNG DẪN BIÊN DỊCH & NẠP FIRMWARE (PLATFORMIO CLI)

Để biên dịch và nạp firmware kiểm thử độc lập cho từng giai đoạn, mở Terminal trong VSCode và chạy các dòng lệnh sau:

### 2.1 Kiểm thử Giai đoạn 2: IMU GY-91 (`stage2_imu.cpp`)

1. **Biên dịch và nạp chương trình**:
   ```powershell
   pio run -e s2 -t upload
   ```
2. **Mở Serial Monitor để theo dõi**:
   ```powershell
   pio device monitor
   ```
3. **Quy trình hiệu chuẩn trên màn hình**:
   - Khi khởi động, chương trình đếm ngược 5 giây. Đặt robot **nằm im hoàn toàn** trên mặt phẳng phẳng lặng để hiệu chuẩn Accel/Gyro.
   - Tiếp tục đếm ngược 3 giây. Nhấc robot lên và **xoay liên tục hình số 8** trên không trung để hiệu chuẩn Magnetometer (la bàn).
   - Sau khi hoàn thành, màn hình sẽ in liên tục góc `Yaw | Pitch | Roll` mỗi 100ms.

### 2.2 Kiểm thử Giai đoạn 3: 4x Laser VL53L0X (`stage3_vl53l0x.cpp`)

1. **Biên dịch và nạp chương trình**:
   ```powershell
   pio run -e s3 -t upload
   ```
2. **Mở Serial Monitor để theo dõi**:
   ```powershell
   pio device monitor
   ```
3. **Quy trình kiểm thử**:
   - Chương trình sẽ thực hiện tắt toàn bộ cảm biến, sau đó bật lần lượt từng chiếc một và đổi địa chỉ I2C động từ `0x29` sang `0x30` -> `0x33`.
   - Kết quả đo khoảng cách của 4 cảm biến Trước (F), Sau (B), Trái (L), Phải (R) sẽ được in trực quan dưới dạng `mm`.
   - Bạn hãy đưa tay lại gần từng cảm biến để kiểm tra xem giá trị tương ứng có giảm đi hay không.
