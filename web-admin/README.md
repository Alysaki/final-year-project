# AGV Fleet Management & Telemetry Web Admin System

[![Node.js](https://img.shields.io/badge/Node.js-v18%2B-green.svg)](https://nodejs.org/)
[![React](https://img.shields.io/badge/React-19.0-blue.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-6.2-purple.svg)](https://vitejs.dev/)
[![MQTT](https://img.shields.io/badge/Protocol-MQTT%20v5-orange.svg)](https://mqtt.org/)
[![Socket.IO](https://img.shields.io/badge/Realtime-Socket.IO-black.svg)](https://socket.io/)

> **AGV Autonomous Delivery Robot Fleet Supervision & Operations Web Admin System**  
> A real-time management module for map telemetry visualization, hardware testing, and mobile order dispatch integration.

---

## 📋 Table of Contents
1. [System Overview](#-system-overview)
2. [Key Features](#-key-features)
3. [Technical Architecture & Data Flow](#-technical-architecture--data-flow)
4. [Technologies Used](#-technologies-used)
5. [Directory Structure](#-directory-structure)
6. [Environment Configuration](#-environment-configuration)
7. [System Startup](#-system-startup)
8. [APIs & MQTT Topics](#-apis--mqtt-topics)

---

## 🛠 System Overview

The **Web Admin** module acts as the "Command Center" for the entire Autonomous AGV Delivery Robot system. The system bridges events between the AGV Robot (Raspberry Pi/ESP32), the customer's Mobile Application (Android App), and the Fleet Operator.

The system provides capabilities for precise real-time location tracking, actual route calculation, remote State Machine overrides, hardware communication testing (BLE token, IMU heading, GPS simulation), and automatic order dispatching from the Firebase queue.

---

## ⚡ Key Features

* 🗺️ **Live Telemetry & Tracking**: Real-time monitoring of GPS location, `heading`, `speed`, and `battery` capacity of each AGV.
* 🛣️ **OSRM Route Interpolation**: Automatically calls the OSRM API to calculate and visualize the actual travel route from Robot $\rightarrow$ Pickup Point $\rightarrow$ Delivery Point.
* 🧪 **Hardware Test Lab**:
  * **State Machine Override**: Intervenes and forces the AGV's working state (`IDLE`, `GOING_TO_SENDER`, `WAITING_SENDER`, etc.).
  * **BLE Token Testing**: Simulates broadcasting Bluetooth Low Energy Tokens to test compartment unlocking from the Mobile App.
  * **GPS Manual Override**: Overrides GPS coordinates to test navigation algorithms (Pure Pursuit / EKF) in an indoor environment.
  * **IMU Heading Rotation Test**: Sends commands to rotate the AGV in place to a fixed target angle to test the IMU sensor.
* 📦 **Firebase Order Dispatcher**: Automatically reads pending orders (`tasks`) from Firebase, assigns them to idle AGVs, and dispatches commands via MQTT.
* 🔔 **FCM Push Notification**: Automatically sends push notifications to the sender's and receiver's mobile apps when the robot updates the delivery progress.
* 📟 **Live Debug Terminal**: Streams technical logs directly from the Raspberry Pi / ESP32 to the Web Admin interface.

---

## 📐 Technical Architecture & Data Flow

The system follows an **Event-Driven Microservices / Real-time Bridge** architecture:

```
                                  +------------------------------------+
                                  |   Mobile App / Firebase Database   |
                                  +-----------------+------------------+
                                                    | (Firebase Admin SDK)
                                                    v
+------------------+    Socket.IO    +--------------+--+   MQTT (MQTTS/TLS)   +--------------------+
|  React Web App   | <-------------> |  Node Backend   | <------------------> | AGV Robot System   |
|  (Port 3000)     |   (port 3001)   |  (server.js)    |   (agv/* topics)     | (Raspberry Pi /    |
+------------------+                 +-----------------+                      |  ESP32 Controller) |
                                                                              +--------------------+
```

### Order Dispatch Workflow:
1. Customer creates an order on the Mobile App $\rightarrow$ Saved to Firebase Realtime Database (`tasks/{uid}/{taskId}`).
2. Backend Server runs a **Queue Manager loop (every 3 seconds)** scanning for `status === 'pending'` orders.
3. Backend packages the order info and pushes it to the AGV via the MQTT topic `agv/orders/pending`.
4. AGV receives the order, transitions its state, and updates back via the `agv/orders/status` topic.
5. Backend acknowledges the new state $\rightarrow$ Updates Firebase, sends Push Notifications via FCM, and alerts the Web Admin via Socket.IO.

---

## 🧰 Technologies Used

### Backend (`/backend`)
* **Node.js**: Backend runtime environment (ES Modules).
* **Express.js**: HTTP Server for managing APIs.
* **Socket.IO**: WebSocket Server pushing real-time telemetry data to the Web Admin.
* **MQTT.js**: MQTT Client connecting to the Broker via MQTTS protocol (TLS encrypted, Port 8883).
* **Firebase Admin SDK**: Manages Realtime Database and sends Cloud Messaging (FCM) push notifications.

### Frontend (`/frontend`)
* **React 19 & TypeScript**: Type-safe component-driven UI development framework.
* **Vite**: Ultra-fast build tool & Development server.
* **Tailwind CSS v4**: Modern, responsive dark-mode UI styling.
* **Leaflet.js & Maptiler API**: Field map visualization and interactive markers.
* **Lucide React**: Modern UI icon set.

---

## 📁 Directory Structure

```text
web-admin/
├── start_web.bat                  # Windows script to launch Backend & Frontend in parallel
├── README.md                      # Overall system documentation
├── MANUAL_GUIDE.md                # Detailed manual startup & installation guide
├── backend/
│   ├── package.json               # Backend dependencies
│   ├── server.js                  # Core server: MQTT <-> Socket.IO <-> Firebase Bridge
│   └── serviceAccountKey.json     # Firebase Secret Credentials Key
└── frontend/
    ├── package.json               # Frontend dependencies
    ├── vite.config.ts             # Vite dev server configuration (Port 3000)
    ├── index.html                 # Single Page Application HTML root
    └── src/
        ├── App.tsx                # Dashboard layout & Socket.IO client state
        ├── types.ts               # Definitions for AGV, Order, Log, Maintenance
        └── components/
            ├── MapContainer.tsx   # Leaflet Map Engine, Marker rotation, OSRM routing
            ├── TestingView.tsx    # Test Lab: Override State, BLE test, GPS Override, IMU Spin
            ├── Header.tsx         # Quick statistics bar & control settings
            ├── SidebarLeft.tsx    # Left menu & Pending Orders accordion
            ├── SidebarRight.tsx   # AGV detailed telemetry panel
            ├── DeployModal.tsx    # Modal to initialize new virtual AGVs
            └── MaintenanceView.tsx # Equipment maintenance ticket management
```

---

## ⚙️ Environment Configuration

The Backend system requires an environment configuration file located at the AGV Robot system's common path:
`../../robot/pi_master/.env`

Required environment variables include:
```env
MQTT_BROKER=your_mqtt_broker_domain.com
MQTT_PORT=8883
MQTT_USERNAME=your_mqtt_username
MQTT_PASSWORD=your_mqtt_password
```

And the Firebase Admin SDK authentication file located at:
`backend/serviceAccountKey.json`

---

## 🚀 System Startup

### Quick Start (Windows)
Run the automated script file:
```cmd
start_web.bat
```

### Manual Execution
For details, refer to the guide file: [MANUAL_GUIDE.md](file:///e:/final-year-project/web-admin/MANUAL_GUIDE.md)

1. **Launch Backend**:
   ```bash
   cd backend
   npm install
   node server.js
   ```
2. **Launch Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
3. Access the application at: `http://localhost:3000`

---

## 📡 Key APIs & MQTT Topics

| MQTT Topic | Direction | Function |
| :--- | :--- | :--- |
| `agv/location` | Sub | Receives GPS, Battery, Heading, Speed Telemetry from Robot |
| `agv/orders/status` | Sub | Updates order State Machine progress from Robot |
| `agv/debug/logs` | Sub | Streams technical logs from ESP32/Raspberry Pi |
| `agv/commands` | Pub | Sends hardware intervention commands from Web Admin to Robot |
| `agv/orders/pending` | Pub | Dispatches new Firebase orders to the Robot |

---
*System developed by IoT & Robotics Engineers Team - Final Year Project: AGV Auto-Delivery Robot.*
