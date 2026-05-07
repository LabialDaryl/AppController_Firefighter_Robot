# ESP32 Firefighter Robot Implementation Plan

This document outlines the logic for the ESP32 firmware to handle both **Manual** and **Autonomous** modes, integrating the Arduino Uno as a sensor hub.

## 1. Global Variables & Configuration
- **Network:** WiFi SSID/Pass, UDP Port.
- **State:** `currentMode` (MANUAL / AUTO), `isSuppressing` (Boolean).
- **Sensors Data (from Arduino Uno via I2C):**
    - `flameFront[5]` (5-channel array)
    - `flameBack` (KY-026)
    - `smokeValue`
    - `distanceFront`, `distanceBack`
    - `irLeft`, `irRight`
- **Servo Positions:** `nozzleYaw`, `nozzlePitch`.

## 2. Setup Function
1. Initialize Serial for debugging.
2. Initialize **I2C** (Master for LCD and Arduino Uno).
3. Initialize **PCA9685** (Servo Driver) and set initial nozzle position.
4. Initialize **L298N** motor pins and PWM channels.
5. Initialize WiFi as Access Point (AP).
6. Start UDP Service.
7. Initialize LCD and display "System Ready".

## 3. Main Loop
1. **Handle UDP Packets:**
    - If command received, parse it.
    - If `MODE_AUTO` or `MODE_MANUAL` received, switch state.
    - If in Manual Mode, execute movement/auxiliary commands.
2. **Read Sensors:**
    - Request sensor data block from Arduino Uno via I2C.
3. **Execution Logic:**
    - `if (currentMode == MANUAL)` -> Execute **Manual Controller Logic**.
    - `else` -> Execute **Autonomous Logic (State Machine)**.
4. **Safety Check:** 
    - If UDP heartbeat lost in Manual mode, stop motors.
5. **Update LCD:** 
    - Show Mode, Connection Status, and Fire Detection status.

## 4. Manual Mode Logic
- **Movement:** `F`, `B`, `L`, `R`, `S` control the L298N.
- **Nozzle:** `NU`, `ND` increment/decrement PCA9685 servo angles.
- **Auxiliary:** Toggle Pump, Lights, or Horn.

## 5. Autonomous Mode Logic (State Machine)
### State: SEARCHING
- Robot rotates slowly (360 degrees).
- Check `flameFront` and `flameBack`.
- If flame detected: Transition to **APPROACHING**.

### State: APPROACHING
- Use `flameFront[5]` to determine direction (Left, Center, Right).
- **Obstacle Avoidance:** 
    - If `distanceFront` < 20cm or `irLeft/Right` triggered, pause or maneuver.
- If flame signal is very strong (close proximity): Transition to **SUPPRESSING**.

### State: SUPPRESSING
- **Verify:** Read `smokeValue`. If smoke > threshold, proceed (prevent false alarms from sunlight).
- **Aim:** Adjust PCA9685 servos based on which of the 5 front sensors is strongest.
- **Extinguish:** 
    - Activate Pump.
    - Sweep nozzle slightly (left/right) for better coverage.
    - If flame signal disappears: Stop Pump, Transition to **SEARCHING**.

## 6. Helper Functions
- `sendToUno(command)`: Send specific triggers to Arduino Uno (e.g., buzzer/LED patterns).
- `drive(speedL, speedR)`: Direct control of motor PWM.
- `updateRGB(color)`: Change LED patterns (e.g., Red/Blue for active fire).
