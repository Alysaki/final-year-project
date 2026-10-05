# AGV Autonomous Delivery Robot System (Hệ Thống Robot Giao Hàng Tự Hành)

> **Đồ án tốt nghiệp**: Xây dựng hệ sinh thái Robot tự hành giao hàng chặng cuối (Last-mile Delivery AGV) tích hợp kiến trúc Đa tầng IoT (Multi-tier IoT Architecture), dẫn đường vệ tinh kết hợp cảm biến (GPS/EKF/Pure Pursuit), điều khiển phân tầng Master-Slave, giám sát hạm đội thời gian thực (Web Fleet Management) và xác thực mở cốp cự ly gần qua Bluetooth Low Energy (BLE GATT).

---

## 1. Bức Tranh Tổng Thể Kiến Trúc (System Architecture)

Hệ thống hoạt động theo mô hình phân tán 4 thực thể liên kết chặt chẽ qua Đám mây (Cloud Broker & Realtime Database) và Giao tiếp Cục bộ (UART nội bộ & BLE):

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
│  |  - Danh bạ & OTP: /phoneIndex & /users        |     |           agv/orders/pending (QoS 1)  |  │
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

## 2. Các Tính Năng Cốt Lõi (Key Features)

1. **Đặt đơn & Dẫn đường Đa phương thức (Mobile Ordering & OSRM Routing):**
   - Khách hàng tạo đơn trên Android App, chọn điểm đi/đến qua bản đồ MapLibre GL.
   - Tuyến đường di chuyển được tính toán thực tế theo mạng lưới giao thông qua OSRM API.
2. **Định vị & Tự hành Bám Quỹ đạo (EKF Fusion & Pure Pursuit):**
   - Dung hợp cảm biến bằng bộ lọc Kalman mở rộng (EKF 4 trạng thái: $x, y, \theta, \text{bias}_{gz}$) kết hợp GPS RTK LC29H và Odometry/Gyro tần số $20\text{Hz}$.
   - Bộ điều khiển Pure Pursuit bám đường chính xác, hỗ trợ xoay tại chỗ (Pivot Turn) tại góc cua gắt và cơ chế tiếp cận tinh (Fine Positioning $< 1.5\text{m}$).
3. **Xác thực An Toàn Mở Cốp Qua BLE Cự Ly Gần (2-Step BLE GATT Handshake):**
   - Mở nắp khay chứa hàng bằng Token 128-bit xác thực trực tiếp qua sóng Bluetooth Low Energy (ESP32 GATT Server).
   - Ngăn chặn triệt để hành vi can thiệp hoặc mở trộm từ xa khi robot đang trên đường chạy.
4. **Phanh Khẩn Cấp Độc Lập Bằng Laser ToF (Hardware Safety Override):**
   - Cảm biến Time-of-Flight VL53L0X quét khoảng cách chu kỳ $30\text{ms}$.
   - Tự động ngắt xung PWM động cơ ngay trên Core 1 của ESP32 khi vật cản $< 300\text{mm}$, độ trễ phản xạ $< 5\text{ms}$.
5. **Giám Sát & Điều Phối Hạm Đội (Web Fleet Management & Dispatcher):**
   - Giao diện Web Admin thời gian thực (React 19 + Leaflet) hiển thị vị trí và góc hướng xe (Heading CSS rotation).
   - Hàng đợi FIFO tự động điều phối đơn hàng, đi kèm Test Lab cho phép mô phỏng tọa độ GPS, ép trạng thái FSM và test phần cứng từ xa.

---

## 3. Cấu Trúc Cây Thư Mục (Project Directory Tree)

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

## 4. Công Nghệ Sử Dụng (Tech Stack)

| Phân hệ | Thành phần | Công nghệ / Thư viện chính |
| :--- | :--- | :--- |
| **Robot Master** | Single-board Computer | Raspberry Pi 4 (Raspberry Pi OS 64-bit), Python 3.11 |
| | Thuật toán & Giao thức | Extended Kalman Filter (EKF), Pure Pursuit, Paho MQTT, PySerial |
| **Robot Slave** | Vi điều khiển thời gian thực | ESP32 DevKit V1, FreeRTOS Dual Core, PlatformIO |
| | Ngoại vi & Cảm biến | Laser ToF VL53L0X, IMU 9 trục GY-91, Cầu H L298N, Servo SG90, BLE GATT |
| **Đám mây** | Message Broker & Database | HiveMQ Cloud (MQTTS TLS 8883), Firebase Realtime Database, FCM |
| **Web Backend** | Máy chủ điều phối trung gian | Node.js (v20+), Express.js, Socket.IO, Firebase Admin SDK, MQTT.js |
| **Web Frontend**| Giao diện giám sát & Test Lab | React 19, TypeScript, Vite 6, Tailwind CSS v4, Leaflet, Lucide Icons |
| **Mobile App** | Ứng dụng người dùng Android | Android Native Java (Java 18), Gradle Kotlin DSL, MapLibre GL 11.0, OSRM |

---

## 5. Hướng Dẫn Cài Đặt & Khởi Chạy (Quick Start)

### 5.1 Phân hệ Web Admin (Backend & Frontend)

1. **Khởi động Backend:**
   ```bash
   cd web-admin/backend
   npm install
   # Cấu hình .env (MQTT_HOST, MQTT_USERNAME, MQTT_PASSWORD, PORT=3001)
   # Đặt file serviceAccountKey.json vào web-admin/backend/
   npm start
   ```
2. **Khởi động Frontend Dashboard:**
   ```bash
   cd web-admin/frontend
   npm install
   npm run dev
   # Truy cập giao diện tại: http://localhost:3000
   ```

### 5.2 Phân hệ Robot

1. **Nạp Firmware ESP32 Slave:**
   - Mở thư mục `robot/arduino_slave/` trên VS Code với PlatformIO extension.
   - Kết nối ESP32 qua cổng USB và nhấn **Upload**.
2. **Chạy Điều khiển Master trên Raspberry Pi 4:**
   ```bash
   cd robot/pi_master
   pip install -r requirements.txt
   # Cấu hình file .env chứa thông tin HiveMQ Broker và tọa độ trạm HOME
   bash run_robot.sh
   ```

### 5.3 Phân hệ Android Mobile App

- Mở thư mục `AutoDeliveryApp/` bằng **Android Studio**.
- Tạo file `local.properties` tại thư mục gốc của app và bổ sung các biến:
  ```properties
  Firebase_API_Key=YOUR_FIREBASE_WEB_API_KEY
  MAPTILER_API_KEY=YOUR_MAPTILER_KEY
  DB_URL=https://YOUR_PROJECT_ID.asia-southeast1.firebasedatabase.app
  ```
- Build và chạy trên thiết bị Android thật (yêu cầu Android 8.0 trở lên, hỗ trợ BLE).

---

## 6. Hướng Dẫn Đọc Tài Liệu Dự Án (Documentation Guide)

Mọi kỹ sư phát triển và trợ lý AI khi tiếp cận dự án cần tuân thủ **Quy tắc nạp ngữ cảnh 2 tầng** được quy định tại [docs tổng/rule.md](docs%20tổng/rule.md):

1. **Tầng 1 (Toàn cảnh hệ thống tại `docs tổng/`):**
   - [rule.md](docs%20tổng/rule.md): Quy tắc bảo mật, cấm rò rỉ secrets, chiến lược phân nhánh và cơ chế tài liệu.
   - [phương án triển khai.md](docs%20tổng/phương%20án%20triển%20khai.md): Bức tranh tổng thể (Rich Picture), quy trình 5 bước và hợp đồng giao tiếp (MQTT, BLE, UART CRC-8, Firebase).
   - [phương án update.md](docs%20tổng/phương%20án%20update.md): Hồ sơ quyết định kiến trúc (ADR) và các phương án đã loại bỏ.
   - [các lỗi đã gặp và cách xử lý.md](docs%20tổng/các%20lỗi%20đã%20gặp%20và%20cách%20xử%20lý.md): Sổ tay 30 sự cố thực tế kèm giải pháp triệt để.
   - [ProjectLog.md](docs%20tổng/ProjectLog.md): Nhật ký tiến trình, mốc lịch sử commit và ma trận sẵn sàng.
2. **Tầng 2 (Tài liệu chuyên sâu phân hệ):**
   - Android App: [AutoDeliveryApp/docs app/](AutoDeliveryApp/docs%20app/)
   - Web Admin: [web-admin/web docs/](web-admin/web%20docs/)
   - Robot Nhúng: [robot/robot docs/](robot/robot%20docs/)
