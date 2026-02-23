# 🚒 Firefighter Robot Controller  v2.0

Android remote-control app for an ESP32-powered tank-chassis firefighter robot.
Uses **Bluetooth Low Energy (BLE)** for real-time command delivery.

---

## 📱 Screen Layout

```
┌──────────────────────────────────────────────────────────────┐
│ 🚒 FIREFIGHTER ROBOT          🟢 Connected        [⚙️]       │
├─────────────────┬──────────────────────┬─────────────────────┤
│                 │                      │                     │
│   MOVEMENT      │     CONTROLS         │   NOZZLE AIM        │
│                 │                      │                     │
│    ╭─────╮      │  ┌──────────────┐    │    ╭─────╮          │
│    │ [◉] │      │  │💧 SUPPRESS   │    │    │ [◉] │          │
│    ╰─────╯      │  │   FIRE       │    │    ╰─────╯          │
│                 │  ├──────────────┤    │                     │
│                 │  │💡 LIGHTS     │    │                     │
│ Fwd·Back        │  ├──────────────┤    │  Pan·Tilt           │
│ Left·Right      │  │📢 HORN(HOLD) │    │  Nozzle Dir         │
│                 │  ├──────────────┤    │                     │
│                 │  │⛔ E-STOP     │    │                     │
└─────────────────┴──────────────────────┴─────────────────────┘
```

---

## 🎮 Controls

| Control | Action |
|---------|--------|
| Left joystick | Tank movement (8 directions + stop) |
| Right joystick | Nozzle pan / tilt (8 directions) |
| SUPPRESS FIRE | Toggle water pump ON / OFF |
| LIGHTS | Toggle lights ON / OFF |
| HORN | Momentary press-and-hold |
| E-STOP | Instant stop — all motors + peripherals |
| ⚙️ Settings | Scan & save the ESP32 BLE device |

---

## 📡 BLE Command Reference

### Movement
| Command | Direction |
|---------|-----------|
| `F` | Forward |
| `B` | Backward |
| `L` | Pivot Left |
| `R` | Pivot Right |
| `FL` | Forward-Left diagonal |
| `FR` | Forward-Right diagonal |
| `BL` | Backward-Left diagonal |
| `BR` | Backward-Right diagonal |
| `S` | Stop |

### Nozzle
| Command | Action |
|---------|--------|
| `NL` | Pan Left |
| `NR` | Pan Right |
| `NU` | Tilt Up |
| `ND` | Tilt Down |
| `NUL/NUR/NDL/NDR` | Diagonal aim |
| `NC` | No change (centre position) |

### Peripherals
| Command | Action |
|---------|--------|
| `PUMP_ON / PUMP_OFF` | Water pump |
| `LIGHT_ON / LIGHT_OFF` | Lights |
| `HORN_ON / HORN_OFF` | Horn / buzzer |
| `ESTOP` | Emergency stop all |

---

## 🛠 Quick Setup

### 1 — Android App
1. Open folder in **Android Studio**
2. Sync Gradle (auto)
3. Connect phone via USB → Run ▶

### 2 — ESP32 Firmware
1. Install library: **ESP32Servo** (Arduino Library Manager)
2. Open `ESP32_FirefighterRobot.ino`
3. Set board: `ESP32 Dev Module`
4. Upload

### 3 — First Connection
1. Open app → tap ⚙️ Settings
2. Scan → select **FirefighterRobot**
3. Save → return to main screen
4. Status turns 🟢 Connected

---

## 🔌 Wiring

### Motor Driver (L298N)
```
ESP32 Pin 25  →  IN1  (Left FWD)
ESP32 Pin 26  →  IN2  (Left REV)
ESP32 Pin 27  →  IN3  (Right FWD)
ESP32 Pin 14  →  IN4  (Right REV)
Battery 12V   →  VIN
Common GND    →  GND
```

### Servos
```
ESP32 Pin 18  →  Pan servo  signal
ESP32 Pin 19  →  Tilt servo signal
5V supply     →  Servo VCC (use external if possible)
Common GND    →  Servo GND
```

### Peripherals
```
ESP32 Pin 32  →  Water pump relay/MOSFET
ESP32 Pin 33  →  Lights relay
ESP32 Pin 13  →  Horn / buzzer
```

---

## 📁 Project Structure

```
FirefighterRobotController/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/firefighter/robotcontroller/
│       │   ├── MainActivity.kt        ← dual joystick + buttons
│       │   ├── SettingsActivity.kt    ← BLE device picker
│       │   ├── JoystickView.kt        ← custom joystick widget
│       │   ├── BLEManager.kt          ← scan / connect / send
│       │   └── BLEDeviceAdapter.kt    ← device list adapter
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_settings.xml
│           │   └── item_ble_device.xml
│           └── values/
│               ├── strings.xml
│               ├── colors.xml
│               └── themes.xml
├── build.gradle
├── settings.gradle
├── gradle.properties
└── ESP32_FirefighterRobot.ino   ← upload this to ESP32
```

---

## ⚙️ Customisation

### Change BLE UUIDs
Edit `BLEManager.kt` **and** `ESP32_FirefighterRobot.ino` — both files must use identical UUIDs.

### Adjust motor speed
In the `.ino` file:
```cpp
const int MOTOR_SPEED_FULL = 255;   // 0-255
```

### Adjust servo sensitivity
In the `.ino` file:
```cpp
const int SERVO_STEP = 6;   // degrees per joystick nudge
```

### Add a button
1. Add `MaterialButton` inside `controlPanel` in `activity_main.xml`
2. Bind it in `MainActivity.kt → bindViews()`
3. Wire a command in `initButtons()`
4. Handle the command in the `.ino` `handleCommand()` function

---

## ✅ Requirements

- Android 8.0+ (API 26), Bluetooth LE capable
- ESP32 DevKit (any 38-pin variant)
- Android Studio Hedgehog or newer
- Arduino IDE 2.x with ESP32 board support
