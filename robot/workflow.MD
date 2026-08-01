# Workflow of the AGV Delivery Robot

This document details the complete workflow of the autonomous delivery robot (AGV) based on the **ESP32 + Contactless BLE App + ToF Laser Sensor** architecture.
Operation model: Peer-to-Peer delivery between 2 users (A sends to B) via a locker system with separate compartments (Slots) on the Robot.

---

## 1. Step-by-step Sequence (Text)

### Step 1: Sender (A) Places Order & Calls Robot
1. **Initialize order:** User A uses the App to create a new order, selecting a compartment (Slot), A's location (Sender Location), and B's location (Receiver Location). The App automatically generates a random, secure 128-bit Hex `Token` for this order. The App pushes the order status to **Firebase Realtime Database** with `status = "PENDING"` and sends it via the MQTT protocol to call the vehicle.
2. **Robot moves to A:** The Pi 4, currently in the IDLE state, intercepts the order via MQTT. It saves the `Token` into memory, calculates the path, and drives to A's location. State is `NAVIGATING_TO_A`.
3. **Arrival (ARRIVED_A):** The vehicle reaches User A's location and stops.

### Step 2: Open Locker via App (2-Step BLE Verification) & Load Cargo
1. **Turn on BLE & Notify:** When the vehicle arrives (ARRIVED_A), the Pi 4 automatically turns on the BLE signal on the ESP32 and loads the `Token` into it. The App displays a Push Notification inviting User A to come out and open the locker.
2. **Verification Step 1 (Verify):** User A opens the App and taps the "Scan and authenticate AGV" button. The App connects via BLE and transmits the JSON packet `{"action":"verify_token", "token":"..."}`. The ESP32 checks if the Token matches and returns `{"ok":true}`.
3. **Verification Step 2 (Open):** Upon receiving the correct response, the App displays the "Open compartment" button. User A presses this button, and the App proceeds to send the command `{"action":"open_slot", "token":"..."}`. Only then does the ESP32 physically release the compartment latch and report back to the Pi 4.
4. **Load Cargo:** User A places the items into the locker, then closes the door. The ESP32 detects this via the Limit Switch and notifies the Pi 4. The Pi 4 updates the status to `LOADED`.
*(Timeout Note: If User A does not load the cargo within 10 minutes, the vehicle will auto-cancel the order and return to the HOME coordinates).*

### Step 3: Move to Receiver (B)
1. **Turn off BLE:** For security and power saving, the BLE signal is completely turned off while the vehicle is in motion.
2. **Pathfinding:** The Pi 4 uses User B's coordinates (`recv_lat`, `recv_lon`) to find a direct route to the delivery point.
3. **Movement:** The Pi 4 runs Pure Pursuit and sends `SPEED` commands to the ESP32. Updates `status = "NAVIGATING_TO_B"`.
4. **Obstacle Avoidance (ToF I2C):** The ESP32 continuously scans the VL53L0X laser. If an obstacle is under 30cm, it automatically brakes hard and alerts the Pi 4.

### Step 4: Unlock & Retrieve Cargo (2-Step BLE Verification)
1. **Arrival:** The Pi 4 confirms arrival at coordinate B, sends a stop command, turns BLE back on, loads the `Token`, and updates Firebase `status = "ARRIVED_B"`.
2. **Notify Receiver:** User B's App receives a push notification.
3. **Verification Step 1 (Verify):** User B opens the App and taps "Tap to scan and authenticate AGV". The App connects via BLE and sends the JSON command `"verify_token"`. The ESP32 checks for Token matches and responds with `{"ok":true}`.
4. **Verification Step 2 (Open):** The App displays "Order from: [Sender A's Name]" and the "Open compartment" button. User B presses the button, and the App sends JSON `"open_slot"`. The cargo locker safely pops open.

### Step 5: Order Completion / Timeout Handling
- **Success Scenario:**
  1. **Retrieve Cargo:** Receiver B takes the items. The Limit Switch is released. The ESP32 can additionally use a ToF/Loadcell to confirm the compartment is truly empty.
  2. **Completion:** The Pi 4 updates `status = "DONE"`.
  3. **Return to Station:** The vehicle automatically disables BLE, calculates the route, and drives back to the `HOME` coordinates (State `IDLE`).
- **Timeout Scenario (10 Minutes at B):**
  1. If no one retrieves the goods after 10 minutes, the vehicle updates its status to `TIMEOUT_RETURNING`.
  2. The system sends a notification to Sender (A) that B did not receive the cargo.
  3. BLE is turned off, and the AGV returns to Sender A's location.
  4. Upon arrival (`RETURNED_TO_A`), a notification is sent to A to come out and retrieve the cargo via the 2-step BLE flow on the App.
- **Locked at Home Scenario (LOCKED_AT_HOME):**
  1. If during the return trip User A still does not retrieve the cargo after 10 minutes, the vehicle drives straight back to the Station (HOME).
  2. The vehicle keeps the BLE signal permanently on in the `LOCKED_AT_HOME` state. User A can go to the Station and scan BLE to unlock and retrieve the cargo at any time.

---

## 2. Detailed Process Flowchart (ASCII Flowchart)

### Stages 1 & 2: Call Vehicle to Sender (A) & Open Locker to Load Cargo

```text
[ SENDER A'S APP ]
         | (Create Order: Generate 128-bit Token)
         v
[ FIREBASE DB / MQTT ] (status: PENDING)
         |
         v
 +-------------------+
 | Pi 4 (Master)     | -> [ Calculate route to User A ]
 +-------------------+ -> [ Vehicle drives to A (NAVIGATING_TO_A) ]
         |
         v
    [ ARRIVED (ARRIVED_A) ]
         | -> Turn on BLE & load Token
         v
 [ User A's App scans & taps Verify ]
         |
         v
 [ App sends JSON "verify_token" ] -> ESP32 checks -> [ Returns "ok":true ]
         |
         v
 [ App shows "Open compartment" button ]
         |
         v
 [ App sends JSON "open_slot" ] -> ESP32 triggers Relay to Open Locker
         |
    (User A loads cargo & Closes locker)
         |
         v
 [ ESP32 reads Limit Switch ] --- UART ---> [ Pi 4 records cargo loaded ]
```

### Stage 3: Move to Receiver (B)

```text
      +-------------------+
      | Pi 4 (Master)     |
      | Calculate route to B|
      +---------+---------+
                |
                v
      +-------------------+
      | Pi 4 follows path |
      | Sends SPEED command|
      +---------+---------+
                | (UART)
                v
      +-------------------+
      | ESP32 (Slave)     |
      | Reads ToF sensor  |
      +---------+---------+
                |
        [ Distance? ]
       /             \
  ( < 30cm )      ( > 30cm )
      /               \
+-----------+    +---------------+
| Auto      |    | Run PID Motor |
| STOP &    |    | Vehicle proceeds|
| Warn flag |    |               |
+-----------+    +---------------+
```

### Stage 4: At Delivery Point & 2-Step BLE Verification Flow (User B)

```text
[ AGV ARRIVES AT DELIVERY POINT B & STOPS ]
               |
               v
  +--------------------------+
  | Pi 4: Updates Firebase   |
  | Turns on BLE & Loads Token|
  +------------+-------------+
               |
               v
  +--------------------------+
  | User B gets Push Notif   |
  | Opens App, taps Verify   |
  +------------+-------------+
               |
               v
  +--------------------------+
  | App sends JSON:          |
  | {"action":"verify_token"}|
  +------------+-------------+
               |
               v
  +--------------------------+
  | ESP32 verifies Token     |
  +------------+-------------+
         |            |
   (FAIL)|            | (PASS)
         v            v
 [ Returns "ok":false]+--------------------------+
                      | ESP32 returns "ok":true  |
                      | DOES NOT open door       |
                      +------------+-------------+
                                   |
                                   v
                      +--------------------------+
                      | App shows Sender A's name|
                      | Shows "Open compartment" |
                      | User B taps Open button  |
                      +------------+-------------+
                                   |
                                   v
                      +--------------------------+
                      | App sends JSON:          |
                      | {"action":"open_slot"}   |
                      +------------+-------------+
                                   |
                                   v
                      +--------------------------+
                      | ESP32 triggers Relay     |
                      | Locker pops open auto!   |
                      +--------------------------+
```
