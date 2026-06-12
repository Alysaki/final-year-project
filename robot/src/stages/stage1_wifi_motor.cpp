/**
 * @file    stage1_wifi_motor.cpp
 * @brief   GIAI ĐOẠN 1 — Điều Khiển Động Cơ L298N qua Wi-Fi Web Server (Access
 Point)
 * @project Robot Tự Hành | Mã Dự Án: 1682
 *
 * MỤC TIÊU:
 *   ESP32 phát Wi-Fi Access Point (AP). Điện thoại kết nối vào AP và
 *   mở trình duyệt để điều khiển robot Tiến / Lùi / Trái / Phải / Dừng.
 *
 * SƠ ĐỒ CHÂN L298N (theo CONTEXT_ROBOT.md — Mục 2.5):
 *   Kênh A (Bánh TRÁI): ENA=32, IN1=33, IN2=26
 *   Kênh B (Bánh PHẢI): ENB=19, IN3=18, IN4=23
 *
 * GIẢI PHÁP CAPTIVE PORTAL (theo CONTEXT_ROBOT.md — Mục 7.4):
 *   Không dùng fetch() — Dùng new Image().src để gửi lệnh HTTP.
 *   Sự kiện ontouchstart trên nút bấm để phản hồi tức thì (0ms trễ).
 *
 * BUILD & NẠP MẠCH:
 *   pio run -e s1 -t upload
 *   pio device monitor          (115200 baud)

 */

#include <Arduino.h>
#include <WebServer.h>
#include <WiFi.h>

// ─── CẤU HÌNH WI-FI ACCESS POINT ────────────────────────────────────────────
const char *AP_SSID = "Robot-1682";    // Tên mạng Wi-Fi của robot
const char *AP_PASSWORD = "12345678"; // Mật khẩu (tối thiểu 8 ký tự)

// ─── SƠ ĐỒ CHÂN L298N (Mục 2.5 — CONTEXT_ROBOT.md) ─────────────────────────
// Kênh A — Bánh TRÁI (2 động cơ đấu song song)
#define PIN_ENA 32 // PWM — Điều chỉnh tốc độ bánh Trái
#define PIN_IN1 33 // Chiều quay bánh Trái
#define PIN_IN2 26 // Chiều quay bánh Trái

// Kênh B — Bánh PHẢI (2 động cơ đấu song song)
#define PIN_ENB 19 // PWM — Điều chỉnh tốc độ bánh Phải
#define PIN_IN3 18 // Chiều quay bánh Phải
#define PIN_IN4 23 // Chiều quay bánh Phải

// ─── CẤU HÌNH PWM (LEDC — ESP32 Arduino Framework) ──────────────────────────
#define PWM_FREQ 5000    // Tần số PWM: 5 kHz (phù hợp L298N)
#define PWM_RESOLUTION 8 // Độ phân giải: 8-bit (0–255)
#define PWM_CHANNEL_A 0  // Kênh LEDC cho ENA
#define PWM_CHANNEL_B 1  // Kênh LEDC cho ENB
#define MOTOR_SPEED 100  // Tốc độ mặc định (0–255), ~78%

// ─── WEB SERVER ──────────────────────────────────────────────────────────────
WebServer server(80);

// =============================================================================
// KHỐI ĐIỀU KHIỂN ĐỘNG CƠ
// Nguyên lý vi sai (Differential Drive):
//   Tiến  : Cả 2 kênh quay cùng chiều thuận
//   Lùi   : Cả 2 kênh quay cùng chiều ngược
//   Trái  : Bánh Phải tiến, bánh Trái lùi (hoặc dừng)
//   Phải  : Bánh Trái tiến, bánh Phải lùi (hoặc dừng)
//   Dừng  : Cắt PWM hoặc cắt tín hiệu IN để phanh động cơ
// =============================================================================

/**
 * @brief Đặt tốc độ PWM cho 2 kênh động cơ
 * @param speedA Tốc độ kênh A (0–255) — Bánh Trái
 * @param speedB Tốc độ kênh B (0–255) — Bánh Phải
 */
void setMotorSpeed(uint8_t speedA, uint8_t speedB) {
  ledcWrite(PWM_CHANNEL_A, speedA);
  ledcWrite(PWM_CHANNEL_B, speedB);
}

/**
 * @brief TIẾN — Cả 2 bánh quay chiều thuận
 */
void motorForward() {
  // Bánh Trái: IN1=HIGH, IN2=LOW → Quay thuận
  digitalWrite(PIN_IN1, HIGH);
  digitalWrite(PIN_IN2, LOW);
  // Bánh Phải: IN3=HIGH, IN4=LOW → Quay thuận
  digitalWrite(PIN_IN3, HIGH);
  digitalWrite(PIN_IN4, LOW);
  // Cấp PWM đầy đủ cho cả 2 kênh
  setMotorSpeed(MOTOR_SPEED, MOTOR_SPEED);
  Serial.println("[MOTOR] >> TIẾN");
}

/**
 * @brief LÙI — Cả 2 bánh quay chiều ngược
 */
void motorBackward() {
  // Bánh Trái: IN1=LOW, IN2=HIGH → Quay ngược
  digitalWrite(PIN_IN1, LOW);
  digitalWrite(PIN_IN2, HIGH);
  // Bánh Phải: IN3=LOW, IN4=HIGH → Quay ngược
  digitalWrite(PIN_IN3, LOW);
  digitalWrite(PIN_IN4, HIGH);
  setMotorSpeed(MOTOR_SPEED, MOTOR_SPEED);
  Serial.println("[MOTOR] << LÙI");
}

/**
 * @brief TRÁI — Bánh Phải tiến, bánh Trái lùi → Robot xoay trái tại chỗ
 */
void motorLeft() {
  // Bánh Trái: Quay ngược
  digitalWrite(PIN_IN1, LOW);
  digitalWrite(PIN_IN2, HIGH);
  // Bánh Phải: Quay thuận
  digitalWrite(PIN_IN3, HIGH);
  digitalWrite(PIN_IN4, LOW);
  setMotorSpeed(MOTOR_SPEED, MOTOR_SPEED);
  Serial.println("[MOTOR] <- TRÁI");
}

/**
 * @brief PHẢI — Bánh Trái tiến, bánh Phải lùi → Robot xoay phải tại chỗ
 */
void motorRight() {
  // Bánh Trái: Quay thuận
  digitalWrite(PIN_IN1, HIGH);
  digitalWrite(PIN_IN2, LOW);
  // Bánh Phải: Quay ngược
  digitalWrite(PIN_IN3, LOW);
  digitalWrite(PIN_IN4, HIGH);
  setMotorSpeed(MOTOR_SPEED, MOTOR_SPEED);
  Serial.println("[MOTOR] -> PHẢI");
}

/**
 * @brief DỪNG — Phanh động lực (Short Brake): IN1=IN2=LOW → L298N khoá bánh
 */
void motorStop() {
  // Cắt tín hiệu chiều quay của cả 2 kênh
  digitalWrite(PIN_IN1, LOW);
  digitalWrite(PIN_IN2, LOW);
  digitalWrite(PIN_IN3, LOW);
  digitalWrite(PIN_IN4, LOW);
  // Cắt PWM về 0
  setMotorSpeed(0, 0);
  Serial.println("[MOTOR] || DỪNG");
}

// =============================================================================
// GIAO DIỆN WEB (HTML + JavaScript)
// Giải pháp Captive Portal (Mục 7.4 — CONTEXT_ROBOT.md):
//   1. Dùng new Image().src thay cho fetch() để tránh bị OS chặn.
//   2. Sự kiện ontouchstart / ontouchend để phản hồi tức thì trên điện thoại.
//   3. Sự kiện onmousedown / onmouseup cho trình duyệt PC.
// =============================================================================
const char HTML_PAGE[] PROGMEM = R"rawhtml(
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
  <title>Robot 1682 — Điều Khiển</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }

    body {
      background: #0f0f1a;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      font-family: 'Segoe UI', Arial, sans-serif;
      color: #e0e0e0;
      user-select: none;
    }

    h1 {
      font-size: 1.4rem;
      letter-spacing: 2px;
      margin-bottom: 8px;
      color: #7ec8e3;
      text-transform: uppercase;
    }

    .subtitle {
      font-size: 0.78rem;
      color: #555;
      margin-bottom: 36px;
      letter-spacing: 1px;
    }

    /* ── Pad điều khiển ── */
    .pad {
      display: grid;
      grid-template-columns: repeat(3, 88px);
      grid-template-rows: repeat(3, 88px);
      gap: 10px;
    }

    .btn {
      border: none;
      border-radius: 14px;
      font-size: 2rem;
      cursor: pointer;
      transition: transform 0.08s, background 0.08s;
      background: #1e1e30;
      color: #7ec8e3;
      box-shadow: 0 4px 14px rgba(0,0,0,0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      touch-action: manipulation;
    }

    .btn:active, .btn.pressed {
      background: #2a6e9e;
      color: #ffffff;
      transform: scale(0.93);
    }

    /* Vị trí các nút trên lưới 3×3 */
    #btn-forward  { grid-column: 2; grid-row: 1; }
    #btn-left     { grid-column: 1; grid-row: 2; }
    #btn-stop     { grid-column: 2; grid-row: 2; background: #2a1a1a; color: #e07070; }
    #btn-right    { grid-column: 3; grid-row: 2; }
    #btn-backward { grid-column: 2; grid-row: 3; }

    #btn-stop:active, #btn-stop.pressed {
      background: #8b2020;
      color: #ffffff;
    }

    .status-bar {
      margin-top: 28px;
      font-size: 0.82rem;
      color: #444;
      height: 20px;
      letter-spacing: 0.5px;
    }

    .status-bar span { color: #7ec8e3; }
  </style>
</head>
<body>
  <h1>&#x1F916; Robot 1682</h1>
  <p class="subtitle">Wi-Fi Control — Stage 1</p>

  <div class="pad">
    <button id="btn-forward"  class="btn" title="Tiến">&#x2B06;</button>
    <button id="btn-left"     class="btn" title="Trái">&#x2B05;</button>
    <button id="btn-stop"     class="btn" title="Dừng">&#x23F9;</button>
    <button id="btn-right"    class="btn" title="Phải">&#x27A1;</button>
    <button id="btn-backward" class="btn" title="Lùi">&#x2B07;</button>
  </div>

  <p class="status-bar" id="status">Sẵn sàng &mdash; chạm để điều khiển</p>

  <script>
    /**
     * GIẢI PHÁP CAPTIVE PORTAL (Mục 7.4):
     *   Thay vì fetch() bị Captive Portal của iOS/Android chặn hoặc
     *   redirect sang trang xác thực mạng, ta dùng new Image().src.
     *   Trình duyệt sẽ tạo request GET đến URL chỉ định mà không cần
     *   xử lý response → Lệnh đến ESP32 ngay lập tức, không bị chặn.
     */
    function sendCmd(cmd) {
      // Tạo đối tượng Image tạm thời để gửi HTTP GET — tránh captive portal chặn
      new Image().src = '/' + cmd;
      document.getElementById('status').innerHTML =
        'Lệnh: <span>' + cmd.toUpperCase() + '</span>';
    }

    /**
     * Gắn sự kiện cho từng nút:
     *   ontouchstart / onmousedown : Gửi lệnh ngay khi chạm/nhấn (độ trễ 0ms)
     *   ontouchend  / onmouseup   : Gửi lệnh STOP khi nhả tay
     */
    const controls = [
      { id: 'btn-forward',  pressCmd: 'forward',  releaseCmd: 'stop' },
      { id: 'btn-backward', pressCmd: 'backward', releaseCmd: 'stop' },
      { id: 'btn-left',     pressCmd: 'left',     releaseCmd: 'stop' },
      { id: 'btn-right',    pressCmd: 'right',    releaseCmd: 'stop' },
      { id: 'btn-stop',     pressCmd: 'stop',     releaseCmd: null   },
    ];

    controls.forEach(function(ctrl) {
      var el = document.getElementById(ctrl.id);

      // ── SỰ KIỆN CHẠM (Mobile) ──────────────────────────────────────────
      el.ontouchstart = function(e) {
        e.preventDefault();   // Ngăn double-tap zoom và trễ 300ms của browser
        el.classList.add('pressed');
        sendCmd(ctrl.pressCmd);
      };

      el.ontouchend = function(e) {
        e.preventDefault();
        el.classList.remove('pressed');
        if (ctrl.releaseCmd) sendCmd(ctrl.releaseCmd);
      };

      // ── SỰ KIỆN NHẤP CHUỘT (PC / Desktop) ─────────────────────────────
      el.onmousedown = function() {
        el.classList.add('pressed');
        sendCmd(ctrl.pressCmd);
      };

      el.onmouseup = function() {
        el.classList.remove('pressed');
        if (ctrl.releaseCmd) sendCmd(ctrl.releaseCmd);
      };

      // Xử lý trường hợp chuột rời khỏi nút khi đang nhấn
      el.onmouseleave = function() {
        if (el.classList.contains('pressed')) {
          el.classList.remove('pressed');
          if (ctrl.releaseCmd) sendCmd(ctrl.releaseCmd);
        }
      };
    });
  </script>
</body>
</html>
)rawhtml";

// =============================================================================
// HANDLERS HTTP — Xử lý từng lệnh điều khiển từ Web UI
// =============================================================================
void handleRoot() { server.send(200, "text/html", HTML_PAGE); }
void handleForward() {
  motorForward();
  server.send(200, "text/plain", "OK");
}
void handleBackward() {
  motorBackward();
  server.send(200, "text/plain", "OK");
}
void handleLeft() {
  motorLeft();
  server.send(200, "text/plain", "OK");
}
void handleRight() {
  motorRight();
  server.send(200, "text/plain", "OK");
}
void handleStop() {
  motorStop();
  server.send(200, "text/plain", "OK");
}

// Xử lý URL không tồn tại — trả về trang điều khiển (tránh Captive Portal
// redirect)
void handleNotFound() {
  server.sendHeader("Location", "/", true);
  server.send(302, "text/plain", "");
}

// =============================================================================
// SETUP
// =============================================================================
void setup() {
  Serial.begin(115200);
  Serial.println("\n====================================");
  Serial.println("  ROBOT 1682 — GIAI DOAN 1 BOOT");
  Serial.println("====================================");

  // ── Khởi tạo chân chiều quay động cơ ─────────────────────────────────
  pinMode(PIN_IN1, OUTPUT);
  pinMode(PIN_IN2, OUTPUT);
  pinMode(PIN_IN3, OUTPUT);
  pinMode(PIN_IN4, OUTPUT);

  // Đảm bảo động cơ không chạy ngay khi khởi động
  digitalWrite(PIN_IN1, LOW);
  digitalWrite(PIN_IN2, LOW);
  digitalWrite(PIN_IN3, LOW);
  digitalWrite(PIN_IN4, LOW);

  // ── Cấu hình PWM (LEDC) cho chân ENA và ENB ──────────────────────────
  // Kênh A (ENA=32): Điều chỉnh tốc độ bánh Trái
  ledcSetup(PWM_CHANNEL_A, PWM_FREQ, PWM_RESOLUTION);
  ledcAttachPin(PIN_ENA, PWM_CHANNEL_A);
  ledcWrite(PWM_CHANNEL_A, 0); // Bắt đầu ở tốc độ 0

  // Kênh B (ENB=19): Điều chỉnh tốc độ bánh Phải
  ledcSetup(PWM_CHANNEL_B, PWM_FREQ, PWM_RESOLUTION);
  ledcAttachPin(PIN_ENB, PWM_CHANNEL_B);
  ledcWrite(PWM_CHANNEL_B, 0); // Bắt đầu ở tốc độ 0

  Serial.println(
      "[MOTOR] PWM khởi tạo OK — ENA:GPIO32 (CH0), ENB:GPIO19 (CH1)");

  // ── Khởi tạo Wi-Fi Access Point ───────────────────────────────────────
  WiFi.mode(WIFI_AP);
  WiFi.softAP(AP_SSID, AP_PASSWORD);

  IPAddress apIP = WiFi.softAPIP();
  Serial.printf("[WIFI]  AP SSID    : %s\n", AP_SSID);
  Serial.printf("[WIFI]  AP Password: %s\n", AP_PASSWORD);
  Serial.printf("[WIFI]  AP IP      : %s\n", apIP.toString().c_str());
  Serial.println("[WIFI]  Mở trình duyệt → http://192.168.4.1");

  // ── Đăng ký các route HTTP ────────────────────────────────────────────
  server.on("/", HTTP_GET, handleRoot);
  server.on("/forward", HTTP_GET, handleForward);
  server.on("/backward", HTTP_GET, handleBackward);
  server.on("/left", HTTP_GET, handleLeft);
  server.on("/right", HTTP_GET, handleRight);
  server.on("/stop", HTTP_GET, handleStop);
  server.onNotFound(handleNotFound);

  server.begin();
  Serial.println("[HTTP]  Web Server đang lắng nghe trên cổng 80");
  Serial.println("====================================\n");
}

// =============================================================================
// LOOP
// =============================================================================
void loop() {
  // Xử lý các request HTTP đến từ trình duyệt điện thoại
  server.handleClient();
}
