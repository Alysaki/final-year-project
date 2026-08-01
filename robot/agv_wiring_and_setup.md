# Architecture & Operation Flow of the AGV Delivery System

This document is the overall blueprint for the autonomous delivery AGV project, including the optimal hardware diagram, positioning algorithm flow, and details of the 5 operational stages from receiving an order to returning to the charging station.

---

## 1. System Architecture & Data Processing

### 1.1. Raspberry Pi 4 (High-Level Controller - Central Brain)
- **Role:** Manages Internet connection (MQTT), computes algorithms, and plans trajectories.
- **Sensor Fusion:** Receives 3 continuous data streams:
  1.  **Heading:** From the Compass/IMU sensor (MPU6050/9250) transmitted by the ESP32.
  2.  **Odometry:** From the pulse counts (ticks) of the 2 Encoders transmitted by the ESP32.
  3.  **Absolute Coordinates (GPS):** From the satellite positioning module.
  $\rightarrow$ The Pi 4 runs a filter (Kalman Filter / Complementary) to eliminate GPS noise and calculate the most accurate real position $(x, y, \theta)$ of the vehicle.
- **Path Planning & Following:** Uses **Spline** interpolation to smooth the map route into tiny Waypoints. Then it runs the **Pure Pursuit** algorithm to calculate the individual speeds for the left/right wheels, helping the vehicle navigate corners smoothly.

### 1.2. ESP32 (Low-Level Controller - Real-time Processing)
- **Role:** Executes commands from the Pi 4, controls the Motors via a velocity PID loop, reads emergency sensors (ToF), and manages the Contactless door security system (BLE).
- **Fast Reflex (Real-time):** Makes emergency braking decisions independent of the Pi 4 when detecting an obstacle at a dangerous distance.
- **BLE JSON Processing:** Receives the command stream from the Android App in JSON format (e.g., `{"action":"verify_token", "token":"..."}`). Parses JSON using the `ArduinoJson` library to route processing: Proximity verification or Physical unlock (Open Slot).

---

## 2. Hardware Wiring Guide

> [!IMPORTANT]
> The entire system (Pi 4, ESP32, Power Module, Sensor Modules) must share a common **GND (Ground)** to unify the logic voltage.

### 2.1. Power Module & Motor Connection (ESP32 $\rightarrow$ L298N / BTS7960)
- 12V/24V power supply to the H-bridge module.
- PWM and direction pins from the H-bridge are connected to the PWM output GPIOs of the ESP32.
- **2-wheel Encoders:** Connected to 4 GPIO pins supporting External Interrupts on the ESP32 (e.g., `34, 35` and `36, 39`) for high-speed pulse counting.

### 2.2. I2C Sensor Connection (SDA/SCL)
ESP32 uses the I2C standard (SDA: `GPIO 21`, SCL: `GPIO 22`) to simultaneously read multiple devices on the same bus:
- **Angle Sensor (IMU - MPU6050 or 9-axis MPU9250):** Reads yaw data. *Note: A thorough calibration function must be written at startup to eliminate drift.*
- **Front ToF Sensor:** Scans the distance in front of the vehicle (e.g., VL53L0X) for obstacle avoidance.
- **Compartment ToF Sensor:** Placed at the bottom or on the lid of the cargo compartment to check if it is truly empty.

### 2.3. Smart Door System (Electric Lock & Magnetic Sensor)
- **Solenoid Lock (Relay):** Signal pin connected to ESP32 `GPIO` $\rightarrow$ Relay Module $\rightarrow$ 12V Solenoid Lock.
- **Limit Switch / Magnetic Door Sensor:** `NO/NC` pin connected to ESP32 `GPIO` (declared as `INPUT_PULLUP`), the other pin to `GND`. Used to report whether the door is open or closed.

### 2.4. Serial Connection (UART)
- **USB UART 1:** Pi 4 $\leftrightarrow$ ESP32. Used to transmit/receive structured commands like speed, MPU data, Encoder ticks, ToF, and door status.
- **USB UART 2:** Pi 4 $\leftrightarrow$ GPS Module. Reads standard NMEA sentences (GPGGA, GPRMC).

---

## 3. Operational Details across 5 Stages

### STAGE 1: Receive Order & Move to Sender
**System State:** `GOING_TO_SENDER`

1.  **Receive Command:** The Android App generates an independent 128-bit Token for the order, then sends a `START_TRIP` MQTT command containing the Sender's coordinates, `Slot_ID`, and `Token` to the Server/Pi 4.
2.  **Offline Storage:** The Pi 4 stores the `Token` in local RAM (ready for the ESP32 to authenticate and open the door even in basements/without network).
3.  **Path Smoothing:** The Pi 4 uses Spline interpolation to turn the map route into a smooth sequence of Waypoints.
4.  **Path Following (Pure Pursuit):**
    *   Pi 4 fuses position data (GPS + MPU + Encoder) $\rightarrow$ Finds the "Look-ahead point" $\rightarrow$ Calculates the required speeds for the left wheel ($V_L$) and right wheel ($V_R$).
    *   Sends UART commands to the ESP32 continuously every 100ms: `SPEED:v_left:v_right`.
5.  **ESP32 Execution:**
    *   Converts `v_left`, `v_right` into PWM running through the PID loop.
    *   Periodically every 50ms, sends UART to Pi 4: `ODO:ticks_L:ticks_R` along with `MPU` angle data.
    *   Continuously scans the front ToF every 30ms.

### STAGE 2: Arrive at Sender & Load Cargo
**System State:** `WAITING_SENDER`

1.  **Stop Vehicle:** Pi 4 sends the stop command `SPEED:0:0`. ESP32 brakes immediately.
2.  **Start BLE Security:** Pi 4 sends UART `BLE:ON:Token`. ESP32 creates a GATT Server, broadcasts Bluetooth named `AGV_DELIVERY_01`, and assigns `Local_Token = Token`.
3.  **Notify Server:** Pi 4 sends an MQTT message with status `ARRIVED_PICKUP` for the App to show action buttons.
4.  **2-Step BLE Verification:**
    *   **Step 1 (Verify):** Sender taps the button on the App $\rightarrow$ App connects via BLE and sends JSON `{"action":"verify_token", "token":"..."}`. ESP32 checks against `Local_Token` $\rightarrow$ Returns `{"ok":true}` without releasing the latch.
    *   **Step 2 (Open):** App displays the Receiver's Name and an "Open Compartment" button. Sender taps the button $\rightarrow$ App sends JSON `{"action":"open_slot", "token":"..."}`. ESP32 triggers the Relay to release the latch $\rightarrow$ Reports to Pi 4: `DOOR:OPENED`.
5.  **Close Door:** Sender loads the cargo and closes the door. ESP32 disables the Relay to safely lock the latch $\rightarrow$ Reports to Pi 4: `DOOR:LOCKED`.
6.  **Departure:** Pi 4 receives `DOOR:LOCKED`, updates Firebase, and begins the journey.

### STAGE 3: Move to Receiver
**System State:** `GOING_TO_RECEIVER`

-   The path-following mechanism is identical to Stage 1. BLE signal is turned off to save power.

> [!WARNING]
> **SPECIAL SCENARIO: Real-time ToF Obstacle Handling (Applies to Stages 1, 3, 5)**
> 1. **Detection:** ESP32 ToF reads distance `< 30cm` $\rightarrow$ immediately cuts PWM to 0, sends emergency UART: `WARN:OBSTACLE`.
> 2. **Pi 4 Freeze:** Upon receiving the warning, Pi 4 *Pauses* the Pure Pursuit calculation loop and reports MQTT `{"status":"BLOCKED"}`.
> 3. **Clearance:** When the obstacle moves away (`> 50cm`), ESP32 sends UART: `WARN:CLEAR`. Pi 4 resumes the path-following algorithm.

### STAGE 4: Arrive at Receiver & Deliver Cargo
**System State:** `WAITING_RECEIVER`

1.  The stopping and `Token` loading process is similar to Stage 2.
2.  The Receiver gets a Push Notification, opens the App $\rightarrow$ App requests a tap for Proximity Check verification.
3.  **2-Step Verification** proceeds similarly: App sends `verify_token` $\rightarrow$ Receives correct response $\rightarrow$ Shows Sender's name and Unlock button $\rightarrow$ User taps button $\rightarrow$ App sends `open_slot` to retrieve the cargo.
4.  **Compartment Safety Check:**
    *   After closing the door, ESP32 locks the latch.
    *   Pi 4 sends a check command: `BOX:CHECK`.
    *   ESP32 reads the internal compartment sensor $\rightarrow$ Sends UART: `BOX:EMPTY` (if the cargo was taken).
5.  Pi 4 sends MQTT reporting order completion.

### STAGE 5: Return to Charging Station
**System State:** `RETURNING` $\rightarrow$ `IDLE`

1.  Pi 4 runs Pure Pursuit to drive the vehicle back to the Home coordinates.
2.  Upon arrival, Pi 4 sends UART `BLE:OFF` to save battery.
3.  Updates the vehicle status to `IDLE` on Firebase and activates auto-charging mode.
