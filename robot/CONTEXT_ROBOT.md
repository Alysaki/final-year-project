# CONTEXT ROBOT — AUTO DELIVERY SYSTEM
> **Đồ án Tốt nghiệp cuối kỳ — Mã dự án: 1682**
> Nền tảng: ESP32 DevKit 38-Pin | Kết nối: Firebase Realtime Database | App: `com.example.autodeliveryapp`
> Phiên bản tài liệu: **v2.2** | Cập nhật lần cuối: 2026-06-12

---

## 1. TỔNG QUAN & LINH KIỆN CỐT LÕI

### 1.1 Vi điều khiển chủ
| Thành phần | Thông số |
|---|---|
| **Module** | ESP32 DevKit 38-Pin (CP2102 USB-to-UART) |
| **Shield mở rộng** | Shield ra chân S/V/G (Signal / Voltage / Ground) cho tất cả ngoại vi |
| **Framework** | Arduino (PlatformIO) |

### 1.2 Truyền thông di động
| Thành phần | Thông số |
|---|---|
| **Module chính** | SIM900A (Băng tần **2G** — Nhà mạng **Mobifone** làm môi trường kết nối thử nghiệm) |
| **Dự phòng** | SIM7600 (4G LTE) — Sẵn sàng cấu hình tương thích lệnh AT nếu hạ tầng mạng 2G tại **Đà Nẵng** bị gián đoạn ngầm |
| **Thư viện** | `TinyGSM` v0.11.7 — Giao tiếp qua `SoftwareSerial` |

### 1.3 Định vị vị trí (GPS)
| Thành phần | Thông số |
|---|---|
| **Module** | GPS GT-U7 |
| **Chuẩn dữ liệu** | NMEA 0183 (câu GPGGA, GPRMC) |
| **Thư viện** | `TinyGPS++` v1.0.3 |
| **Cổng kết nối** | `HardwareSerial2` (RX2/TX2 của ESP32) |

### 1.4 Hệ thống cảm biến
| Cảm biến | Số lượng | Chức năng | Giao tiếp |
|---|---|---|---|
| **IMU GY-91** (MPU9250 + BMP280) | 1x | Đọc góc nghiêng Yaw / Pitch / Roll, tính toán hướng la bàn | I2C |
| **Laser VL53L0X** | 4x | Phát hiện vật cản 4 hướng: Trước / Sau / Trái / Phải | I2C (địa chỉ đổi động qua XSHUT) |

### 1.5 Hệ thống động cơ
| Thành phần | Thông số |
|---|---|
| **Mạch cầu H** | L298N (2 kênh: Kênh A điều khiển bánh Trái, Kênh B điều khiển bánh Phải) |
| **Động cơ** | 4x Động cơ giảm tốc DC màu vàng (bánh TT) |
| **Cấu hình đấu dây** | 2 bánh bên **Trái** đấu **song song** vào Kênh A — 2 bánh bên **Phải** đấu **song song** vào Kênh B |
| **Phương thức điều khiển** | Vi sai (Differential Drive) — Điều hướng bằng cách điều chỉnh tốc độ 2 phía độc lập |

### 1.6 Hệ thống nguồn cấp
```
Pin 3S Li-Po (11.1V)
│
├─── Nhánh ĐỘNG CƠ: Từ Pin thẳng → L298N (12V IN) — Không qua Buck
│
├─── Nhánh SIM900A: Pin → Buck LM2596 #1 (5.0V / 2A) → Tụ 680uF (lọc xung nhiễu sụt áp) → SIM900A
│
└─── Nhánh ESP32 + GPS: Pin → Buck LM2596 #2 (5.0V) → ESP32 (qua chân 5V/VIN) & GPS GT-U7
```

### 1.7 Bảo vệ mức logic (Level Shifter thụ động)
> **Vấn đề**: Chân TX của SIM900A xuất mức logic **5V**, trong khi ESP32 chỉ chịu được **3.3V**.

**Giải pháp: Mạch phân áp điện trở**
```
TX_SIM (5V) ──[390Ω]──┬── P34 (ESP32 Input-Only, ~3.33V)
                       │
                    [390Ω] ← Điện trở song song = 195Ω
                    [390Ω] ←
                       │
                      GND
```
- **Trở kháng xuống đất**: 2 điện trở 390Ω song song = **195Ω**
- **Điện áp tại P34**: `5V × 195/(390+195) ≈ 3.33V` ✅ An toàn
- **Lưu ý**: P34 là chân **Input-Only** của ESP32 — Chỉ đọc dữ liệu từ SIM, không ghi ngược lại.

---

## 2. KIẾN TRÚC SƠ ĐỒ CHÂN (PIN MAPPING)

### 2.1 Bus I2C Phần cứng (Dùng chung cho GY-91 & 4x VL53L0X)
| Tín hiệu | Chân ESP32 |
|---|---|
| **SDA** | GPIO **5** |
| **SCL** | GPIO **15** |

### 2.2 Chân kích hoạt XSHUT — VL53L0X (Gán địa chỉ I2C động)
| Vị trí cảm biến | Chân XSHUT | Địa chỉ I2C sau khi đổi |
|---|---|---|
| **Trước** (Front) | GPIO **4** | `0x30` |
| **Sau** (Back) | GPIO **13** | `0x31` |
| **Trái** (Left) | GPIO **14** | `0x32` |
| **Phải** (Right) | GPIO **27** | `0x33` |

> 💡 **Quy trình khởi tạo**: Kéo tất cả XSHUT xuống LOW → Bật từng chân lên HIGH → Gọi `setAddress()` trước khi bật chân tiếp theo.

### 2.3 Cổng GPS (HardwareSerial 2)
| Tín hiệu | Chân ESP32 |
|---|---|
| TX (GPS) → **RX2** | GPIO **16** |
| RX (GPS) → **TX2** | GPIO **17** |
| Baud rate | 9600 bps |

### 2.4 Cổng SIM900A (SoftwareSerial)
| Tín hiệu | Chân ESP32 | Ghi chú |
|---|---|---|
| TX (SIM) → **RX_ESP** | GPIO **34** | Input-Only, qua mạch phân áp 3.33V |
| RX (SIM) → **TX_ESP** | GPIO **25** | Xuất 3.3V logic, SIM900A nhận OK |
| Baud rate | 9600 bps | |

### 2.5 Điều khiển L298N — Động cơ
| Kênh | Tín hiệu | Chân ESP32 | Ghi chú |
|---|---|---|---|
| **Kênh A (Trái)** | ENA | GPIO **32** | PWM — Điều chỉnh tốc độ bánh Trái |
| **Kênh A (Trái)** | IN1 | GPIO **33** | Chiều quay |
| **Kênh A (Trái)** | IN2 | GPIO **26** | Chiều quay |
| **Kênh B (Phải)** | ENB | GPIO **19** | PWM — Điều chỉnh tốc độ bánh Phải |
| **Kênh B (Phải)** | IN3 | GPIO **18** | Chiều quay |
| **Kênh B (Phải)** | IN4 | GPIO **23** | Chiều quay |

---

## 3. BẢN GIAO KÈO DỮ LIỆU — FIREBASE REALTIME DATABASE SCHEMA

> **Ràng buộc cứng**: Cấu trúc node dưới đây phải khớp **100%** với định dạng mà Android App (`com.example.autodeliveryapp`) đang lắng nghe và ghi dữ liệu. Không được tự ý thay đổi tên trường.

### 3.1 Sơ đồ cấu trúc tổng thể
```
Firebase Realtime Database Root
│
├── /users/
│   └── {userId}/
│       └── fcmToken: String          ← Token để Robot gửi push notification
│
├── /tasks/
│   └── {userId}/
│       └── {orderId}/               ← Định danh dạng "RBT-XXXX"
│           ├── orderId: String
│           ├── status: String       ← "pending" | "delivering" | "completed" | "cancelled"
│           ├── distance: Double     ← Khoảng cách km
│           └── price: Long          ← Giá tiền VND
│
└── /robot/
    └── location/
        ├── lat: Double              ← Vĩ độ hiện tại (Robot cập nhật từ GPS)
        └── lng: Double              ← Kinh độ hiện tại (Robot cập nhật từ GPS)
```

### 3.2 Luồng dữ liệu (Data Flow)

```
Android App ──write──► /tasks/{userId}/{orderId}/status = "delivering"
                                │
                                ▼
                        ESP32 (listener) đọc FCM Token tại /users/{userId}/fcmToken
                                │
                                ▼
                        ESP32 gọi FCM HTTP API v1 → Push notification đến điện thoại
                                │
                                ▼
                        ESP32 đọc tọa độ dropoff → tự hành đến đích
                                │
                                ▼
                        ESP32 ──write──► /robot/location/ (lat, lng) liên tục
                                │
                                ▼
                        Android App đọc /robot/location → vẽ marker di chuyển real-time
```

---

## 4. LỘ TRÌNH 9 GIAI ĐOẠN PHÁT TRIỂN & QUY TRÌNH KIỂM THỬ

### Giai đoạn 1 — Điều khiển động cơ qua Wi-Fi cục bộ
- **Mục tiêu**: ESP32 phát Wi-Fi Access Point, giao diện web đơn giản điều khiển tiến/lùi/trái/phải.
- **Kiểm thử**: Kết nối điện thoại vào AP của ESP32, xác nhận 4 bánh phản hồi đúng chiều quay.
- **File**: `src/stages/stage1_wifi_motor.cpp`

### Giai đoạn 2 — Đọc góc nghiêng từ IMU GY-91
- **Mục tiêu**: Khởi tạo MPU9250 qua I2C, đọc và in liên tục Yaw/Pitch/Roll ra Serial Monitor.
- **Kiểm thử**: Nghiêng robot 45° và xác nhận góc thay đổi chính xác tương ứng.
- **File**: `src/stages/stage2_imu.cpp`

### Giai đoạn 3 — Khởi tạo 4x VL53L0X đa địa chỉ
- **Mục tiêu**: Tuần tự kéo XSHUT từng sensor, đổi địa chỉ I2C động (0x30–0x33), đọc khoảng cách 4 hướng.
- **Kiểm thử**: Đưa tay lại gần từng cảm biến, xác nhận đúng sensor phản hồi thay đổi khoảng cách.
- **File**: `src/stages/stage3_vl53l0x.cpp`

### Giai đoạn 4 — Lấy tọa độ GPS ngoài trời
- **Mục tiêu**: Khởi tạo HardwareSerial2 tại 9600 bps, parse câu NMEA qua TinyGPS++, in lat/lng ra Serial.
- **Kiểm thử**: Mang module ra sân trống, xác nhận tọa độ GPS khớp với Google Maps ±5m.
- **File**: `src/stages/stage4_gps.cpp`

### Giai đoạn 5 — Khởi tạo kết nối mạng SIM900A
- **Mục tiêu**: Gửi tập lệnh AT qua SoftwareSerial, xác nhận SIM bắt sóng, đăng ký GPRS thành công.
- **Kiểm thử**: Gọi `AT+CSQ` xem chất lượng tín hiệu, gọi `AT+CGATT?` xác nhận đã attach GPRS.
- **File**: `src/stages/stage5_sim900a.cpp`

### Giai đoạn 6 — Điều khiển robot từ xa qua mạng Cellular (GPRS)
- **Mục tiêu**: Cắt Wi-Fi, ESP32 dùng TinyGSM nhận lệnh HTTP từ server công cộng để điều khiển động cơ.
- **Kiểm thử**: Tắt Wi-Fi máy tính, gửi lệnh HTTP qua 4G điện thoại → Robot phản hồi đúng chiều.
- **File**: `src/stages/stage6_gprs_control.cpp`

### Giai đoạn 7 — Đồng bộ hóa GPS & Nhận lệnh qua Firebase (TinyGSM + Firebase-ESP-Client)
- **Mục tiêu**: ESP32 dùng TinyGSM làm modem TCP, kết nối Firebase qua GPRS, ghi lat/lng liên tục, đọc lệnh điều hướng.
- **Kiểm thử**: Mở Firebase Console xem node `/robot/location/` cập nhật tọa độ theo thời gian thực.
- **File**: `src/stages/stage7_firebase_sync.cpp`

### Giai đoạn 8 — Nhận nhiệm vụ & Gửi Push Notification
- **Mục tiêu**: Khi `/tasks/{userId}/{orderId}/status` đổi sang `"delivering"`, Robot đọc FCM Token, gọi FCM HTTP API v1, gửi thông báo đẩy "Robot đã nhận đơn và bắt đầu di chuyển" đến điện thoại khách hàng.
- **Kiểm thử**: Đổi trạng thái đơn hàng trên Firebase Console → Điện thoại nhận được push notification ngay lập tức.
- **File**: `src/stages/stage8_fcm_notify.cpp`

### Giai đoạn 9 — Tự hành hoàn toàn (Autonomous Delivery)
- **Mục tiêu**:
  1. Robot đọc tọa độ đích (dropoff) từ Firebase.
  2. Tính hướng la bàn cần đi bằng công thức `atan2` (Bearing) từ GPS hiện tại → GPS đích.
  3. IMU GY-91 cung cấp góc Yaw hiện tại → So sánh với hướng đích → Điều chỉnh tốc độ 2 bên bánh để rẽ về đúng hướng (PID heading control).
  4. So sánh GPS hiện tại với GPS đích, khi khoảng cách < 1m thì dừng.
  5. 4x VL53L0X: Nếu bất kỳ sensor nào phát hiện vật cản < **20cm**, Robot tự động dừng hoặc rẽ tránh.
- **Kiểm thử**: Thả Robot chạy tự động trên sân trường Đà Nẵng, đặt chướng ngại vật ngẫu nhiên trước mặt → Xác nhận tự hành an toàn và đến đích thành công.
- **File**: `src/main.cpp` (tích hợp toàn bộ)

---

## 5. TRẠNG THÁI TIẾN ĐỘ THỰC TẾ

> Cập nhật lần cuối: **2026-06-12**

| Giai đoạn | Tên | Trạng thái | Ngày hoàn thành |
|:---:|---|:---:|:---:|
| **GĐ 1** | Điều khiển động cơ qua Wi-Fi | 🟢 Hoàn thành 100% | 2026-06-12 |
| **GĐ 2** | Đọc IMU GY-91 (MPU9250) | 🔴 Tạm dừng / Blocked | — |
| **GĐ 3** | 4x VL53L0X đa địa chỉ I2C | ⚪ Chưa bắt đầu | — |
| **GĐ 4** | GPS GT-U7 ngoài trời | ⚪ Chưa bắt đầu | — |
| **GĐ 5** | Kết nối SIM900A GPRS | ⚪ Chưa bắt đầu | — |
| **GĐ 6** | Điều khiển qua GPRS HTTP | ⚪ Chưa bắt đầu | — |
| **GĐ 7** | Firebase Sync qua GPRS | ⚪ Chưa bắt đầu | — |
| **GĐ 8** | FCM Push Notification | ⚪ Chưa bắt đầu | — |
| **GĐ 9** | Tự hành hoàn toàn (main.cpp) | ⚪ Chưa bắt đầu | — |

---

### 5.1 Biên bản kiểm thử — Giai đoạn 1 ✅

**Trạng thái**: 🟢 **Hoàn thành 100%** — Kiểm thử thực tế thành công

**Thông số firmware đã nạp** (đọc từ `src/stages/stage1_wifi_motor.cpp`):

| Tham số | Giá trị thực tế trong code |
|---|---|
| Wi-Fi SSID | `"Robot-1682"` |
| Wi-Fi Password | `"12345678"` |
| AP IP mặc định | `192.168.4.1` |
| Tốc độ động cơ (MOTOR_SPEED) | `100 / 255` (~39% — đã tinh chỉnh khi test thực tế) |
| PWM Frequency | `5000 Hz` |
| PWM Resolution | `8-bit (0–255)` |
| LEDC Channel A (ENA/GPIO32) | Channel `0` |
| LEDC Channel B (ENB/GPIO19) | Channel `1` |

**Sơ đồ chân L298N đã xác nhận hoạt động**:
```
Kênh A (Bánh TRÁI): ENA=GPIO32 | IN1=GPIO33 | IN2=GPIO26
Kênh B (Bánh PHẢI): ENB=GPIO19 | IN3=GPIO18 | IN4=GPIO23
```

**Giải pháp Captive Portal đã tích hợp và xác nhận**:
- ✅ Dùng `new Image().src = '/' + cmd` thay cho `fetch()` → Bypass hoàn toàn Captive Portal chặn request của iOS/Android
- ✅ Sự kiện `ontouchstart` + `e.preventDefault()` → Phản hồi tức thì **0ms** (loại bỏ trễ 300ms mặc định của mobile browser)
- ✅ `ontouchend` tự động gửi lệnh `/stop` khi nhả tay → An toàn, không trôi xe
- ✅ `onmouseleave` xử lý trường hợp chuột PC trượt ra ngoài nút khi đang nhấn

**Kết quả kiểm thử thực tế**:
- Điện thoại kết nối SSID `Robot-1682` → Mở `http://192.168.4.1` thành công
- Các nút Tiến / Lùi / Trái / Phải / Dừng phản hồi đúng chiều quay 4 bánh
- Không bị Captive Portal chặn request trên cả Android lẫn iOS

---

### 5.2 Biên bản kiểm thử — Giai đoạn 2 🔴

**Trạng thái**: 🔴 **Tạm dừng / Blocked — Lỗi IC phần cứng**

**Vấn đề**: Module GY-91 bị lỗi phần cứng, bus I2C kẹt ở mức LOW. Xem chi tiết nhật ký lỗi tại **Mục 7.4** (`[GĐ2-HW-002]` và `[GĐ2-HW-003]`).

**Hướng xử lý**: Đặt module GY-91 thay thế. Khi có module mới, áp dụng ngay thuật toán `i2c_bus_recovery()` đã ghi trong nhật ký để phòng ngừa tình trạng bus kẹt tái phát.

---

## 6. CẤU TRÚC THƯ MỤC DỰ ÁN

> [!NOTE]
> Cây thư mục dưới đây phản ánh **trạng thái thực tế** trên đĩa tính đến 2026-06-12.
> Các mục đánh dấu `[chưa tạo]` là kế hoạch kiến trúc, chưa có file vật lý.

```
robot/
├── platformio.ini              ← ✅ Cấu hình PlatformIO & multi-environment (9 env)
├── CONTEXT_ROBOT.md            ← File này — Tài liệu kiến trúc hệ thống (v2.1)
│
├── src/
│   ├── main.cpp                ← File TÍCH HỢP DUY NHẤT (Giai đoạn 9)
│   │                              ⚠️ CHỈ chứa code hoàn chỉnh, KHÔNG dùng để test
│   └── stages/                 ← Thư mục kiểm thử độc lập từng giai đoạn
│       ├── stage1_wifi_motor.cpp  ← ✅ Đã tạo — Wi-Fi AP + L298N + Captive Portal fix
│       ├── stage2_imu.cpp         ← ✅ Đã tạo — IMU GY-91 Test & Hiệu chuẩn
│       ├── stage3_vl53l0x.cpp     ← ✅ Đã tạo — 4x VL53L0X đa địa chỉ Test
│       ├── stage4_gps.cpp         ← ✅ Đã tạo — GPS GT-U7 Test
│       ├── stage5_sim900a.cpp     ← ⚪ Được bỏ qua (Skip)
│       ├── stage6_gprs_control.cpp  ← ✅ Đã tạo — GPRS HTTP Control Test
│       ├── stage7_firebase_sync.cpp ← ✅ Đã tạo — Firebase Sync GPRS SSL Test
│       └── stage8_fcm_notify.cpp   ← ⚪ Được bỏ qua (Skip)
│
├── include/                    ← [chưa tạo] — Sẽ tạo từ Giai đoạn 5 trở đi
│   ├── pin_config.h            ← Định nghĩa tất cả chân GPIO
│   ├── firebase_config.h       ← API Key, Database URL, FCM credentials
│   └── robot_config.h          ← Hằng số vật lý (ngưỡng VL53L0X, PID gains...)
│
└── lib/                        ← [chưa tạo] — Thư viện nội bộ nếu cần
```

---

## 7. GHI CHÚ QUAN TRỌNG CHO PHÁT TRIỂN

---

### 7.1 Quy tắc kiến trúc bắt buộc — Phân tách file kiểm thử

> [!IMPORTANT]
> **`src/main.cpp` được bảo vệ độc quyền cho Giai đoạn 9.**
> Tệp này chỉ được phép chứa code tích hợp hoàn chỉnh của robot tự hành.
> **Tuyệt đối cấm** viết code kiểm thử của bất kỳ giai đoạn nào (1–8) vào `main.cpp`.
>
> **Lý do kỹ thuật**: PlatformIO biên dịch toàn bộ file `.cpp` trong `src/`. Nếu có 2 file
> cùng định nghĩa hàm `setup()` và `loop()`, trình linker sẽ báo lỗi:
> `multiple definition of 'setup'` và từ chối tạo firmware.
>
> **Giải pháp đã triển khai**: Mỗi giai đoạn kiểm thử được cô lập trong
> `src/stages/stageN_xxx.cpp` và build bằng environment riêng trong `platformio.ini`
> (xem bảng lệnh bên dưới).

---

### 7.2 Hệ thống Multi-Environment — Bảng lệnh vận hành

Dự án đã cấu hình **9 environment độc lập** trong `platformio.ini`. Mỗi environment
chỉ biên dịch đúng 1 file `.cpp` của giai đoạn tương ứng.

| Environment | Lệnh Build | Lệnh Nạp mạch | Giai đoạn |
|:---:|---|---|---|
| `main` | `pio run -e main` | `pio run -e main -t upload` | Giai đoạn 9 — Firmware tích hợp hoàn chỉnh |
| `s1` | `pio run -e s1` | `pio run -e s1 -t upload` | Giai đoạn 1 — Điều khiển L298N ✅ |
| `s2` | `pio run -e s2` | `pio run -e s2 -t upload` | Giai đoạn 2 — IMU GY-91 🔴 |
| `s3` | `pio run -e s3` | `pio run -e s3 -t upload` | Giai đoạn 3 — VL53L0X laser TOF |
| `s4` | `pio run -e s4` | `pio run -e s4 -t upload` | Giai đoạn 4 — GPS GT-U7 |
| `s5` | `pio run -e s5` | `pio run -e s5 -t upload` | Giai đoạn 5 — SIM900A GPRS |
| `s6` | `pio run -e s6` | `pio run -e s6 -t upload` | Giai đoạn 6 — GPRS HTTP Control |
| `s7` | `pio run -e s7` | `pio run -e s7 -t upload` | Giai đoạn 7 — Firebase Sync |
| `s8` | `pio run -e s8` | `pio run -e s8 -t upload` | Giai đoạn 8 — FCM Notification |

**Ví dụ quy trình nạp và debug Giai đoạn 1:**
```powershell
# Bước 1: Build và nạp firmware kiểm thử Wi-Fi Motor
pio run -e s1 -t upload

# Bước 2: Mở Serial Monitor (115200 baud) để xem log boot
pio device monitor

# Bước 3: Sau khi kiểm thử xong, quay lại build firmware chính
pio run -e main -t upload
```

> [!TIP]
> Có thể chạy `pio run` (không chỉ định `-e`) để build **tất cả** environments cùng lúc
> và kiểm tra toàn bộ codebase không có lỗi biên dịch.

---

### 7.3 Lưu ý phần cứng

> [!WARNING]
> **GPIO34 của ESP32 là Input-Only** — Không kết nối GPIO34 với bất kỳ nguồn đầu ra nào. Chỉ dùng để nhận tín hiệu từ SIM900A sau mạch phân áp.

> [!NOTE]
> **Tương thích thư viện Firebase với TinyGSM**: Cần định nghĩa `#define TINY_GSM_MODEM_SIM800` trước khi include TinyGSM để khai báo đúng modem. SIM900A tương thích hoàn toàn với driver `SIM800`. Macro này đã được thêm sẵn vào `build_flags` của các environment `s5`, `s6`, `s7`, `s8` trong `platformio.ini`.

> [!NOTE]
> **Địa chỉ I2C VL53L0X**: Địa chỉ mặc định xuất xưởng là `0x29`. Nếu không đổi địa chỉ qua XSHUT, các sensor sẽ xung đột nhau trên cùng bus I2C. Bắt buộc phải thực hiện quy trình khởi tạo tuần tự ở Giai đoạn 3.

> [!TIP]
> **SIM900A bắt sóng chậm**: Sau khi cấp nguồn, chờ tối thiểu **10 giây** trước khi gửi lệnh AT. Có thể kiểm tra đèn trạng thái NET LED: Nhấp chậm 1s/lần = đã bắt sóng.

> [!NOTE]
> **Nạp firmware vào ESP32 (board CP2102)**: Board này không có mạch tự động reset DTR/RTS đáng tin cậy. Quy trình nạp thủ công: **(1)** Giữ nút `BOOT` → **(2)** Nhấn + nhả `EN/RST` → **(3)** Nhả `BOOT` → **(4)** Chạy lệnh upload → **(5)** Sau khi upload xong, nhấn `EN/RST` để khởi động firmware.

---

### 7.4 NHẬT KÝ KHỬ LỖI — BÀI HỌC THỰC CHIẾN

> [!CAUTION]
> **KHÔNG XÓA MỤC NÀY.** Đây là nhật ký lỗi lịch sử của dự án. Các bản ghi dưới đây
> chứa tri thức quan trọng để phòng ngừa lỗi tái phát ở Giai đoạn 3 (VL53L0X — cùng bus I2C).

---

#### [GĐ1-SW-001] Lỗi Captive Portal chặn HTTP Request từ điện thoại
- **Triệu chứng**: Điện thoại kết nối vào AP của ESP32, nhấn nút trên web nhưng robot không phản hồi. Trình duyệt bị redirect sang trang xác thực mạng của hệ điều hành.
- **Nguyên nhân**: iOS và Android tự động phát hiện mạng không có Internet. Khi đó, OS intercept mọi HTTP request (kể cả `fetch()`) và chuyển hướng sang Captive Portal page để yêu cầu xác thực.
- **Giải pháp đã áp dụng**: Thay `fetch('/forward')` bằng `new Image().src = '/forward'`. Trình duyệt xử lý request tải ảnh như một side-effect, không bị OS intercept. Request đến ESP32 thành công.
- **Giải pháp bổ sung**: Dùng `ontouchstart` thay cho `onclick` để loại bỏ trễ 300ms của mobile browser khi xử lý touch event.
- **Trạng thái**: ✅ Đã khắc phục hoàn toàn — Áp dụng vĩnh viễn cho mọi giai đoạn có Web UI.

---

#### [GĐ2-HW-002] Lỗi Kẹt Bus I2C — GY-91 (MPU9250) Kéo SDA/SCL Xuống LOW
- **Triệu chứng**: Sau khi khởi tạo, `Wire.begin(5, 15)` trả về thành công nhưng `mpu.setup(0x68)` luôn trả về `false`. Đo oscilloscope: SDA (GPIO5) và SCL (GPIO15) bị kéo cứng xuống 0V, không có xung clock.
- **Nguyên nhân**: IC MPU9250 trên module GY-91 bị lỗi phần cứng — chân SDA của IC bị short nội bộ xuống GND, kéo toàn bộ bus I2C xuống LOW và "đóng băng" tất cả thiết bị trên cùng bus.
- **Hệ quả nghiêm trọng**: Vì bus I2C dùng chung (SDA=GPIO5, SCL=GPIO15), lỗi này sẽ ảnh hưởng đến **cả 4 sensor VL53L0X** ở Giai đoạn 3 nếu module GY-91 lỗi vẫn được cắm vào mạch.
- **Trạng thái**: 🔴 Chưa khắc phục — Đang chờ module GY-91 thay thế.

---

#### [GĐ2-HW-003] Thuật Toán Khôi Phục Bus I2C — `i2c_bus_recovery()`
- **Mục đích**: Phòng ngừa tình trạng bus I2C kẹt tái phát ở Giai đoạn 3 (VL53L0X). Áp dụng trước khi gọi `Wire.begin()` trong mọi file stage có dùng I2C.
- **Nguyên lý**: Nếu slave bị treo giữa chừng trong một giao dịch I2C, nó có thể đang giữ SDA ở mức LOW. Master cần tạo 9 xung clock thủ công bằng GPIO để "giải phóng" slave và đưa bus về trạng thái IDLE.
- **Đoạn code cần tích hợp**:

```cpp
/**
 * @brief Khôi phục bus I2C bị kẹt bằng cách phát 9 xung clock thủ công.
 * Gọi hàm này TRƯỚC Wire.begin() trong setup() của mọi stage có I2C.
 * Tham chiếu: CONTEXT_ROBOT.md — Mục 7.4 [GĐ2-HW-003]
 */
void i2c_bus_recovery() {
    pinMode(5,  OUTPUT); // SDA — GPIO5
    pinMode(15, OUTPUT); // SCL — GPIO15

    // Đưa SDA lên HIGH để kiểm tra bus có bị kẹt không
    digitalWrite(5, HIGH);
    delayMicroseconds(5);

    // Nếu SDA vẫn bị kéo xuống LOW bởi slave, phát 9 xung clock để giải phóng
    for (int i = 0; i < 9; i++) {
        digitalWrite(15, LOW);
        delayMicroseconds(5);
        digitalWrite(15, HIGH);
        delayMicroseconds(5);
    }

    // Gửi điều kiện STOP: SDA từ LOW → HIGH khi SCL = HIGH
    digitalWrite(5,  LOW);  delayMicroseconds(5);
    digitalWrite(15, HIGH); delayMicroseconds(5);
    digitalWrite(5,  HIGH); delayMicroseconds(5);

    // Trả quyền điều khiển chân về cho thư viện Wire
    pinMode(5,  INPUT_PULLUP);
    pinMode(15, INPUT_PULLUP);
    delay(10);

    Serial.println("[I2C] Bus recovery hoàn tất.");
}
```

- **Cách dùng trong `setup()`**:
```cpp
void setup() {
    Serial.begin(115200);
    i2c_bus_recovery();          // ← Gọi TRƯỚC Wire.begin()
    Wire.begin(5, 15);           // SDA=GPIO5, SCL=GPIO15
    // ... khởi tạo sensor ...
}
```
- **Trạng thái**: 📋 Đã thiết kế — Sẵn sàng tích hợp vào `stage3_vl53l0x.cpp`.
