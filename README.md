# 🤖 AGV Autonomous Delivery Robot — Hệ Thống Robot Giao Hàng Tự Hành Chặng Cuối

<p align="center">
  <img src="https://img.shields.io/badge/Python-3.11-3776AB?style=for-the-badge&logo=python&logoColor=white" alt="Python" />
  <img src="https://img.shields.io/badge/C++-FreeRTOS-00599C?style=for-the-badge&logo=cplusplus&logoColor=white" alt="C++ FreeRTOS" />
  <img src="https://img.shields.io/badge/Java-18-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/TypeScript-5.x-3178C6?style=for-the-badge&logo=typescript&logoColor=white" alt="TypeScript" />
  <img src="https://img.shields.io/badge/MQTT-TLS%208883-660066?style=for-the-badge&logo=mqtt&logoColor=white" alt="MQTT TLS" />
  <img src="https://img.shields.io/badge/BLE-GATT%20128--bit-0082FC?style=for-the-badge&logo=bluetooth&logoColor=white" alt="BLE GATT" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
</p>

<p align="center">
  <b>Hệ thống Robot tự hành giao hàng chặng cuối (Last-mile Delivery AGV) tích hợp kiến trúc IoT đa tầng phân tán.</b><br>
  <i>Tự động hóa toàn diện quy trình giao vận: Đặt đơn qua ứng dụng di động — Định vị vệ tinh dung hợp EKF — Bám quỹ đạo Pure Pursuit — Nhận hàng an toàn qua sóng Bluetooth Low Energy (BLE GATT).</i>
</p>

---

## 🌟 Tính Năng Nổi Bật

### 1. ⚡ Dẫn Đường Tự Động Vệ Tinh Dung Hợp Cảm Biến (EKF + Pure Pursuit)
- **Bộ lọc Kalman mở rộng (EKF 4 trạng thái):** Dung hợp liên tục $20\text{Hz}$ dữ liệu Dead Reckoning (Encoder quang học 2 bánh + Con quay hồi chuyển Gyro Z từ IMU GY-91) với tín hiệu GPS tần số $1\text{Hz}$, tự động ước lượng và triệt tiêu sai số trôi dạt ($\text{bias}_{gz}$).
- **Thuật toán bám quỹ đạo Pure Pursuit:** Khoảng cách nhìn trước $L_d = 1.0\text{m}$, làm mịn đường cong bằng OSRM GeoJSON với các điểm nút cách nhau $0.2\text{m}$. Tự động kích hoạt cơ chế xoay tại chỗ (Pivot Turn) khi góc lệch lớn ($>45^\circ$) và tự động giảm tốc khi ôm cua gắt.
- **Tiếp cận mục tiêu siêu mịn (Fine Positioning):** Trong bán kính $5\text{m}$ cuối, robot tự giảm tốc độ xuống $0.15\text{m/s}$ và cập bến chính xác trong phạm vi sai số $\le 1.5\text{m}$.

### 2. 🔐 Xác Thực Mở Cốp 2 Bước Qua Sóng BLE Cự Ly Gần (2-Step BLE GATT)
- **Bảo mật vật lý tuyệt đối:** Thùng hàng chỉ mở chốt khi người dùng đứng trực tiếp cạnh xe ($<10\text{m}$) và xác thực Token 128-bit qua sóng Bluetooth Low Energy với ESP32 GATT Server (`AGV_DELIVERY_01`).
- **Quy trình Handshake 2 bước:**
  - *Bước 1 (Xác thực cự ly):* Gửi lệnh `verify_token` kiểm tra tính hợp lệ trước khi cho phép tương tác.
  - *Bước 2 (Chấp hành cơ học):* Gửi lệnh `confirm_loaded` (Người gửi) hoặc `open_slot` (Người nhận) để kích hoạt Servo xoay $90^\circ$ giật chốt cốp.
- **Độc lập kết nối Internet:** Quá trình giao nhận diễn ra cục bộ trực tiếp giữa điện thoại và vi điều khiển ESP32, đảm bảo vẫn lấy được hàng ngay cả khi mất sóng 4G/Internet ngoài trời.

### 3. 🛡️ Cơ Chế An Toàn Kép & Phanh Khẩn Cấp Laser ToF (Hardware Override)
- **Phanh laser phản xạ cực nhanh ($<5\text{ms}$):** Cảm biến quang học Time-of-Flight VL53L0X quét khoảng cách chướng ngại vật chu kỳ $30\text{ms}$. Khi vật cản $< 300\text{mm}$, firmware ngắt trực tiếp xung PWM động cơ trên Core 1 của ESP32 mà không phụ thuộc vào hệ điều hành Linux của Raspberry Pi.
- **Cơ chế Hysteresis Latch chống mù khoảng cách:** Tự động bảo lưu cờ phanh khẩn cấp khi vật cản áp sát dưới ngưỡng tiêu cự ($<20\text{mm}$) và chỉ khôi phục hành trình khi khoảng cách an toàn đạt $>500\text{mm}$ liên tục.

### 4. 🛰️ Giám Sát Hạm Đội & Điều Phối Hàng Đợi (Web Fleet Management & FIFO Dispatcher)
- **Bản đồ giám sát trực quan (React 19 + Leaflet):** Marker AGV xoay góc Heading động theo thời gian thực nhờ GPU CSS Transform, triệt tiêu hiện tượng xoay ngược $350^\circ$ khi đi qua trục Bắc ($0^\circ$).
- **Bộ điều phối hàng đợi tự động (FIFO Queue Manager):** Quét đơn hàng `pending` mỗi $3\text{s}$, tích hợp Debounce Lock $10\text{s}$ chống bắn lặp lệnh và tự động đóng băng hàng đợi khi robot `OFFLINE`.
- **Hardware Test Lab:** Cho phép kỹ sư ghi đè tọa độ GPS giả lập (kiểm thử bám đường trong nhà), ép chuyển trạng thái máy FSM, kích hoạt còi/khóa cốp và stream Live Debug Logs trực tiếp từ xa.

### 5. 📱 Trải Nghiệm Ứng Dụng Di Động Toàn Diện (Android Native App)
- **Định tuyến giao thông thực tế:** Tích hợp MapLibre GL và máy chủ OSRM dẫn đường theo mạng lưới đường bộ thực tế, phân tách rõ ràng chặng lấy hàng (C→A) và chặng giao hàng (A→B).
- **Đồng bộ thời gian thực bền vững (Firebase-first):** Tự động khôi phục tuyến đường khi mở lại app, tra cứu tài khoản số điện thoại siêu tốc $O(1)$ qua node `/phoneIndex`, và giao dịch nguyên tử (Atomic Transaction) chống tranh chấp đặt trùng khay hàng.

---

## 🏛️ Bức Tranh Tổng Thể Kiến Trúc (System Architecture)

```
+───────────────────────────────────────────────────────────────────────────────────────────────────+
│                                     1. NGƯỜI DÙNG CUỐI (CLIENTS)                                  │
│  +───────────────────────────────────────────────+     +───────────────────────────────────────+  │
│  |       NGƯỜI GỬI (SENDER - Android App)        |     |       NGƯỜI NHẬN (RECEIVER - App)     |  │
│  |  - Chọn điểm Pickup/Dropoff, MapLibre & OSRM  |     |  - Nhận thông báo FCM khi xe đến      |  │
│  |  - Sinh Token 128-bit, đặt khay qua Tx RTDB   |     |  - Theo dõi tọa độ thời gian thực     |  │
│  |  - Quét BLE xác thực nạp hàng (confirm_loaded)|     |  - Quét BLE mở nắp lấy đồ (open_slot) |  │
│  +───────────────────────┬───────────────────────+     +───────────────────┬───────────────────+  │
+──────────────────────────┼─────────────────────────────────────────────────┼──────────────────────+
                           │ HTTPS / RTDB SDK                                │ HTTPS / RTDB SDK
                           ▼                                                 ▼
+───────────────────────────────────────────────────────────────────────────────────────────────────+
│                                2. NỀN TẢNG ĐÁM MÂY (CLOUD PLATFORM)                               │
│  +───────────────────────────────────────────────+     +───────────────────────────────────────+  │
│  |         FIREBASE REALTIME DATABASE            |     |        HIVEMQ CLOUD MQTT BROKER       |  │
│  |  - Đồng bộ đơn hàng: /tasks & /recipientTasks |     |  - Cổng MQTTS bảo mật: TLS 8883       |  │
│  |  - Quản lý khay hàng: /robotSlots             |     |  - Topics: agv/location (1Hz, QoS 0)  |  │
│  |  - Tra cứu số điện thoại O(1): /phoneIndex    |     |           agv/orders/pending (QoS 1)  |  │
│  |  - Cấu hình trạm sạc: /system/agv_home        |     |           agv/orders/status (QoS 1)   |  │
│  |                                               |     |           agv/commands (QoS 1)        |  │
│  +───────────────────────▲───────────────────────+     +───────────────────▲───────────────────+  │
+──────────────────────────┼─────────────────────────────────────────────────┼──────────────────────+
                           │ Firebase Admin SDK                              │ MQTT Client (TLS)
                           ▼                                                 ▼
+───────────────────────────────────────────────────────────────────────────────────────────────────+
│                             3. TRUNG TÂM ĐIỀU PHỐI (WEB ADMIN BACKEND & UI)                       │
│  +─────────────────────────────────────────────────────────────────────────────────────────────+  │
│  | BACKEND RELAY SERVER (Node.js / Express / Socket.io / MQTT)                                 |  │
│  | - Queue Manager (3s/lần): Quét đơn `pending` theo FIFO, đẩy sang MQTT khi xe IDLE           |  │
│  | - Watchdog: Tự chuyển trạng thái `OFFLINE` sau 5s mất tin telemetry                         |  │
│  | - Push Notification Engine: Kích hoạt thông báo FCM khi xe đổi trạng thái                   |  │
│  +───────────────────────────────────────────────┬─────────────────────────────────────────────+  │
│                                                  │ WebSocket Socket.io (Port 3001)                │
│  +───────────────────────────────────────────────▼─────────────────────────────────────────────+  │
│  | FRONTEND DASHBOARD (React 19 / TypeScript / Vite / TailwindCSS v4 / Leaflet)                |  │
│  | - Live Fleet Map: Marker AGV xoay góc Heading động, vẽ lộ trình thực tế OSRM                |  │
│  | - Hardware Test Lab: Override GPS, Force State Machine, IMU Rotation Test, BLE Test         |  │
│  | - Live Debug Terminal: Stream trực tiếp log phần cứng từ Pi và ESP32                        |  │
│  +─────────────────────────────────────────────────────────────────────────────────────────────+  │
+──────────────────────────────────────────────────┼────────────────────────────────────────────────+
                                                   │ MQTT over TLS (Port 8883)
                                                   ▼
+───────────────────────────────────────────────────────────────────────────────────────────────────+
│                               4. THIẾT BỊ BIÊN: ROBOT TỰ HÀNH (AGV)                               │
│  +─────────────────────────────────────────────────────────────────────────────────────────────+  │
│  | RASPBERRY PI 4 MASTER (High-Level Controller - Linux / Python 3)                            |  │
│  | - Finite State Machine (AgvStateMachine: 9 trạng thái tự hành, timeout 10 phút)             |  │
│  | - Sensor Fusion EKF 4 trạng thái: [x, y, θ, bias_gz] dung hợp GPS LC29H, Odometry, Gyro     |  │
│  | - Pure Pursuit Controller: Ld = 1.0m, nội suy làm mịn đường 0.2m, Pivot Turn > 45°          |  │
│  +───────────────────────────────────────────────┬─────────────────────────────────────────────+  │
│                                                  │ UART (115200 bps, Khung ASCII kèm CRC-8)       │
│  +───────────────────────────────────────────────▼─────────────────────────────────────────────+  │
│  | ESP32 SLAVE (Low-Level Real-Time Controller - FreeRTOS Dual Core C++)                       |  │
│  | - Core 0: Giao tiếp UART, nạp Telemetry 20Hz, BLE GATT Server (AGV_DELIVERY_01)            |  │
│  | - Core 1: Vòng lặp PID vận tốc 20Hz (Kp=3.5, Ki=0.8, Kd=0.2), ngắt Encoder IRAM_ATTR        |  │
│  | - Bảo vệ an toàn: Laser ToF VL53L0X phanh khẩn cấp độc lập khi vật cản < 300mm              |  │
│  | - Cơ cấu chấp hành: Servo SG90/MG996R giật chốt, công tắc hành trình giám sát nắp cốp      |  │
│  +─────────────────────────────────────────────────────────────────────────────────────────────+  │
+───────────────────────────────────────────────────────────────────────────────────────────────────+
```

---

## 📡 Bảng Hợp Đồng Giao Tiếp Cốt Lõi (Core Contracts)

### 1. MQTT Topic Registry (HiveMQ Broker - TLS 8883)

| Topic | Hướng truyền | QoS | Tần suất | Chức năng |
| :--- | :---: | :---: | :---: | :--- |
| `agv/location` | Pi 4 $\rightarrow$ Cloud | 0 | 1 Hz | Đẩy Telemetry tọa độ GPS, góc Heading, vận tốc thực và pin |
| `agv/orders/pending` | Backend $\rightarrow$ Pi 4 | 1 | Khi có đơn | Bắn đơn hàng mới cho robot khi trạng thái xe đang `IDLE` |
| `agv/orders/status` | Pi 4 $\rightarrow$ Backend | 1 | Khi đổi FSM | Báo cáo tiến trình di chuyển để Backend cập nhật DB & FCM |
| `agv/commands` | Web $\rightarrow$ Pi 4 | 1 | Theo lệnh | Lệnh can thiệp từ xa (`force_state`, `override_gps`, `ble_on`, `CANCEL_TASK`...) |
| `agv/debug/logs` | Pi 4 $\rightarrow$ Web | 0 | Realtime | Stream log nội bộ phần cứng lên Terminal Dashboard |

### 2. Bluetooth Low Energy (BLE GATT Contract)
* **Tên thiết bị quảng bá:** `AGV_DELIVERY_01`
* **Service UUID:** `4fafc201-1fb5-459e-8fcc-c5c9c331914b`
* **Characteristic UUID:** `beb5483e-36e1-4688-b7f5-ea07361b26a8` (Read/Write)
* **Cấu trúc JSON Trao Đổi:**
  ```json
  // Request từ Mobile App gửi xuống ESP32:
  { "action": "verify_token" | "confirm_loaded" | "open_slot", "token": "HEX_128_BIT_TOKEN", "slotId": "slot1" }

  // Response từ ESP32 trả ngược lại App:
  { "ok": true, "action": "confirm_loaded", "slotId": "slot1", "error": "" }
  ```

### 3. Khung Truyền UART Kèm CRC-8 (Pi 4 $\leftrightarrow$ ESP32)
* **Baudrate:** 115200 bps (8-N-1) | **Đa thức kiểm tra:** CRC-8 `0x07` ($X^8 + X^2 + X^1 + 1$).
* **Định dạng:** `<PAYLOAD>*<CRC8_HEX>\n` (Ví dụ: `SPEED:15:15*3A\n`, `BLE:ON:token123*7F\n`).

---

## ⚡ Hiệu Năng & Chỉ Số Đo Lường Thực Tế (Production Benchmarks)

*Các chỉ số đo lường thực tế trên hệ thống phần cứng và môi trường mạng thực nghiệm:*

| Hạng mục kỹ thuật | Kết quả đo được | Đánh giá & Tiêu chuẩn |
| :--- | :---: | :--- |
| **Tần số vòng lặp PID Động cơ (ESP32)** | **20 Hz (50ms)** | Cân bằng vận tốc 2 bánh qua mạch cầu H L298N |
| **Thời gian phản xạ phanh khẩn cấp Laser ToF** | **< 5 ms** | Ngắt cứng trên Core 1, tự dừng trước vật cản $30\text{cm}$ |
| **Độ trễ truyền nhận UART kèm CRC-8** | **< 2 ms** | Tỷ lệ rớt gói $< 0.01\%$ nhờ mạch dập xung tụ gốm 104 |
| **Tần số cập nhật vị trí EKF (Dead Reckoning)** | **20 Hz** | Duy trì mượt mà cả khi tín hiệu GPS bị che khuất |
| **Bán kính cập bến chính xác (Docking Accuracy)** | **≤ 1.5 m** | Tiếp cận tinh bằng la bàn + Odometry bánh xe |
| **Thời gian nạp dữ liệu Firebase RTDB** | **< 120 ms** | Đồng bộ đám mây tức thời trên mạng di động 4G |
| **Thời gian xác thực mở cốp qua sóng BLE** | **< 0.8 s** | Quét và kết nối trực tiếp cục bộ không phụ thuộc Internet |

---

## 📁 Cấu Trúc Cây Thư Mục (Project Directory Tree)

```
final-year-project/
├── AutoDeliveryApp/               # PHÂN HỆ MOBILE ANDROID NATIVE
│   ├── app/src/main/              # Mã nguồn Java, Layout XML, Gradle Kotlin DSL
│   └── docs app/                  # Tài liệu kiến trúc, BLE contract, lỗi đã gặp của App
│
├── web-admin/                     # PHÂN HỆ QUẢN TRỊ & ĐIỀU PHỐI ĐÁM MÂY
│   ├── backend/                   # Node.js, Express, Socket.io, MQTT Client, Firebase Admin
│   ├── frontend/                  # React 19, TypeScript, Vite, TailwindCSS v4, Leaflet Map
│   └── web docs/                  # Tài liệu triển khai, nhật ký tiến hóa, lỗi đã gặp của Web
│
├── robot/                         # PHÂN HỆ ĐIỀU KHIỂN ROBOT TỰ HÀNH
│   ├── pi_master/                 # Raspberry Pi 4 (Python 3): FSM, EKF, Pure Pursuit, MQTT
│   ├── arduino_slave/             # ESP32 (C++/FreeRTOS): Motor PID, BLE, ToF, Servo, Encoder
│   └── robot docs/                # Sơ đồ phần cứng, hướng dẫn nối dây, lỗi đã gặp của Robot
│
├── docs tổng/                     # TÀI LIỆU HẠT NHÂN & QUY TẮC TOÀN HỆ THỐNG
│   ├── rule.md                    # Quy tắc vận hành, thứ tự nạp ngữ cảnh, bảo mật Git
│   ├── phương án triển khai.md    # Rich Picture, bản thiết kế kỹ thuật, hợp đồng dữ liệu
│   ├── phương án update.md        # Cân nhắc kỹ thuật, các giải pháp đã chọn vs đã loại bỏ
│   ├── các lỗi đã gặp và cách xử lý.md # Sổ tay 30 sự cố thực tế & giải pháp kiểm chứng
│   └── ProjectLog.md              # Tiến trình lịch sử commit và ma trận hiện trạng dự án
│
└── README.md                      # Trang giới thiệu và hướng dẫn tổng quan của dự án
```

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

* **Robot Master (Pi 4):** Python 3.11, Extended Kalman Filter (EKF), Pure Pursuit Controller, Paho MQTTv5, PySerial, OSRM Routing Client.
* **Robot Slave (ESP32):** C/C++, FreeRTOS Dual-Core, PlatformIO, PID Controller, BLE GATT Server, Wire (I2C), ESP32 Encoder Interrupts.
* **Web Admin Backend:** Node.js (v20+), Express.js, Socket.IO, MQTT.js, Firebase Admin SDK (FCM & Realtime DB).
* **Web Admin Frontend:** React 19, TypeScript, Vite 6, Tailwind CSS v4, Leaflet Map, Lucide React Icons.
* **Mobile Client:** Android Native Java (Java 18), Gradle Kotlin DSL, MapLibre GL Android SDK 11.0, Firebase Authentication & Realtime Database, BLE GATT Client.

---

## 🚀 Hướng Dẫn Cài Đặt & Khởi Chạy (Quick Start)

### 1. Phân hệ Web Admin (Backend & Frontend)
```bash
# Khởi động Backend Relay Server (Port 3001)
cd web-admin/backend
npm install
# Cấu hình .env và thêm serviceAccountKey.json vào web-admin/backend/
npm start

# Khởi động Frontend Dashboard (Port 3000)
cd ../frontend
npm install
npm run dev
```

### 2. Phân hệ Robot Tự Hành
```bash
# Nạp Firmware ESP32 Slave:
# Mở thư mục robot/arduino_slave/ bằng VS Code PlatformIO -> Nhấn Upload.

# Chạy Điều khiển Trung tâm trên Raspberry Pi 4:
cd robot/pi_master
pip install -r requirements.txt
# Cấu hình biến môi trường trong file .env
bash run_robot.sh
```

### 3. Phân hệ Mobile Android App
- Mở thư mục `AutoDeliveryApp/` bằng **Android Studio**.
- Tạo file `local.properties` tại thư mục gốc của app và điền:
  ```properties
  Firebase_API_Key=YOUR_FIREBASE_WEB_API_KEY
  MAPTILER_API_KEY=YOUR_MAPTILER_KEY
  DB_URL=https://YOUR_PROJECT_ID.asia-southeast1.firebasedatabase.app
  ```
- Build và chạy ứng dụng trên thiết bị Android thật (hỗ trợ Bluetooth LE).

---

## 📅 Lịch Sử Tiến Hóa Kiến Trúc (Changelog)

### 🚀 Thế Hệ 3: Master-Slave Đa Tầng & Cầu Nối Đám Mây (Hiện tại - v3.0)
- **Tách đôi phần cứng:** Phân tách hoàn toàn Raspberry Pi 4 (Master tính toán nặng EKF/Pure Pursuit) và ESP32 (Slave thời gian thực PID/BLE/ToF).
- **Web Admin độc lập:** Xây dựng Node.js Relay Server điều phối hàng đợi FIFO tự động, loại bỏ hoàn toàn các server nhúng trên phần cứng xe.
- **Bảo mật BLE GATT:** Nâng cấp cơ chế xác thực Token 128-bit cự ly gần 2 bước qua chip ESP32.
- **Chuẩn hóa khung truyền:** Khung ASCII UART bảo vệ mã lỗi CRC-8 đa thức `0x07`.

### ⚡ Thế Hệ 2: Flask Web Controller & Kiểm Thử Phần Cứng (v2.0)
- Đưa Raspberry Pi 4 vào làm bộ điều khiển trung tâm, phục vụ giao diện Web Flask cục bộ (Port 5000) qua mạng LAN.
- Tích hợp kiểm thử cảm biến ToF, GPS GT-U7 và IMU GY-91.
- *Giới hạn:* Chỉ hoạt động trong phòng lab qua IP nội bộ, chưa liên kết được người dùng Internet.

### 🧪 Thế Hệ 1: Web Server Nhúng & Đơn ESP32 (v1.0)
- Chạy Web Server nhúng trực tiếp trên Flash của chip ESP32, kết nối Firebase qua modem GPRS SIM900A 2G.
- *Bài học:* ESP32 bị treo CPU do quá tải mạng SSL, SIM900A sụt áp dòng đỉnh 2A và sóng 2G bị cắt sóng.

---

## 📚 Hướng Dẫn Nạp Ngữ Cảnh & Tài Liệu Kỹ Thuật

Dự án áp dụng quy chuẩn tài liệu 2 tầng nghiêm ngặt được quy định tại [docs tổng/rule.md](docs%20tổng/rule.md):

```
[BẮT ĐẦU DỰ ÁN] ──► ĐỌC "docs tổng/" (Rich Picture & Master Rules)
                      ├── rule.md: Quy tắc phát triển, bảo mật secrets và quy trình tài liệu
                      ├── phương án triển khai.md: Bức tranh tổng thể và hợp đồng dữ liệu toàn hệ thống
                      ├── phương án update.md: Nhật ký quyết định kiến trúc (ADR)
                      ├── các lỗi đã gặp và cách xử lý.md: Sổ tay 30 sự cố thực tế
                      └── ProjectLog.md: Tiến trình lịch sử commit và ma trận sẵn sàng
                            │
                            ▼
              CHUYỂN TIẾP VÀO PHÂN HỆ CỤ THỂ
                      ├── Mobile App: AutoDeliveryApp/docs app/
                      ├── Web Admin:  web-admin/web docs/
                      └── Robot:      robot/robot docs/
```

---

## 📄 Giấy Phép & Tuyên Bố Bản Quyền

* Dự án được phát triển phục vụ đồ án tốt nghiệp kỹ sư và được phân phối theo giấy phép mã nguồn mở **MIT License**.
* Bản quyền thuộc về nhóm tác giả dự án AGV Autonomous Delivery Robot System © 2026.
