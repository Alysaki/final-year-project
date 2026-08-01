# Autonomous Delivery Robot (AGV Delivery Robot)

This project is the complete software system for an Autonomous Delivery Robot (AGV), comprising two main microcontrollers communicating via the UART standard:
1. **Master (Raspberry Pi 4 - Python):** Handles RTK GPS navigation, Google Maps, Firebase, and State Machine orchestration.
2. **Slave (ESP32 - C++):** Responsible for motor speed control with Encoder pulse counting, automatic obstacle avoidance via ToF, toggling Bluetooth (BLE) signals, and controlling the locker release relay.

*(Note: The system has been upgraded from a multi-compartment hub model to a **Peer-to-Peer (Direct person-to-person delivery)** model with a single cargo compartment, optimizing the authentication process via a mobile app (1-tap BLE)).*

---

## 1. Hardware Architecture

### 🧠 Raspberry Pi 4 (Master)
- **OS:** Linux (Raspberry Pi OS) running Python 3.
- **USB Connections:**
  - `ttyUSB0`: Reads NMEA data (`$GNGGA`) from the Quectel LC29H RTK GPS module at 9600 baudrate.
  - `ttyACM0`: UART communication with the ESP32 at 115200 baudrate.
- **Main Task:** The State Machine that manages the entire lifecycle of an order.
- **Highlight Features:**
  - **Smart Navigation:** Uses the Google Maps Directions API to find the shortest route to the pickup point (A) and delivery point (B).
  - **Power Management:** Contextually commands the ESP32 to turn the BLE signal on/off to save battery and enhance security.
  - **Timeout & Stuck Handling:** Automatically returns to return the cargo if there is no receiver after 10 minutes. Automatically locks the vehicle (`LOCKED_AT_HOME`) if the sender also fails to retrieve the goods.

### ⚙️ ESP32 (Slave)
- **Main Task:** Counts Encoder pulses (Dead reckoning), controls the L298N (Tank Drive), brakes automatically upon encountering obstacles (ToF VL53L0X), broadcasts a BLE Server waiting for customer connections, reads the limit switch, and controls the locker release relay.
- **BLE Feature:** Built-in on the ESP32 SoC. Customers use the App to scan for signals and press a single button to open the locker (Unified BLE Unlock).

---

## 2. UART Communication Protocol

Both sides communicate via UART with fixed-size packets, utilizing the **CRC8** algorithm for integrity checks and noise elimination.

### Master (Pi) ➡️ Slave (ESP32) [Size: 5 Bytes]
Structure: `[0xAA] [Move_Cmd] [Solenoid_Cmd] [CRC8] [0xBB]`
- `0xAA`: Header.
- `Move_Cmd`: Navigation command (`0x00` = Stop, `0x01` = Forward, `0x02` = Backward, `0x03` = Turn Left, `0x04` = Turn Right).
- `Solenoid_Cmd`: Hardware command (`0x00` = Relay Off, `0x01` = Open Locker 1, `0x10` = Turn On BLE, `0x11` = Turn Off BLE).
- `CRC8`: Error checking code.
- `0xBB`: Footer.

### Slave (ESP32) ➡️ Master (Pi) [Size: 7 Bytes]
Structure: `[0xAA] [Key_High] [Key_Low] [Limit_States] [Sensor_Flags] [CRC8] [0xBB]`
- `0xAA`: Header.
- `Key_High` & `Key_Low`: Combined into a 16-bit integer storing the PIN code the user just entered from the App via BLE. If no new code is available, sends `0xFFFF`.
- `Limit_States`: Flag byte (Only Bit 0 is used for Locker 1). `1` = Door closed/Cargo present, `0` = Empty/Door open.
- `Sensor_Flags`: Sensor flags. Bit 0 = `1` if currently blocked by an obstacle in front according to the ToF sensor.
- `CRC8`: Error checking code.
- `0xBB`: Footer.

---

## 3. Workflow

The system operates in a closed lifecycle:
1. **Idle (IDLE):** The vehicle is parked at the Station (HOME), BLE is off. Waits for Firebase to have an order with status `"PENDING"`.
2. **Pickup at A:** The Pi calculates the route and drives to the Sender's coordinates. Upon arrival, it turns on the BLE signal. The Sender uses the App to press "Open Locker", loads the cargo, and closes it.
3. **Delivery at B:** Turns off the BLE signal, drives to the Receiver's coordinates. Upon arrival, turns on BLE. The Receiver uses the App to press "Open Locker" to retrieve the cargo.
4. **Return Home (HOME):** After completing the delivery, the vehicle turns off BLE and drives back to HOME, switching to `IDLE`.
5. **Troubleshooting Mechanism (10-Minute Timeout):**
   - If B does not receive the goods: The vehicle automatically returns the cargo back to A.
   - If A (or B) calls the vehicle but does not interact: Cancels the order, returns HOME autonomously.
   - If the vehicle brings the cargo back to HOME: It is locked in the `LOCKED_AT_HOME` state and leaves BLE permanently on, waiting for User A to come to the Station to retrieve their belongings.
