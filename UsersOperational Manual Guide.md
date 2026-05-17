# 🚒 Firefighter Robot: Operational Manual Guide (Professional Edition)

This guide provides instructions on how to operate the Firefighter Robot system, featuring a professional Hierarchical Finite State Machine (HFSM) and multi-sensor fusion architecture.

---

## 1. System Overview
The Firefighter Robot utilizes a high-performance robotics architecture:
- **Main Brain (ESP32):** Runs the HFSM, handles WiFi/UDP communication, and executes autonomous navigation.
- **Intelligent Sensor Hub (Arduino Uno):** Pre-processes telemetry using Exponential Moving Average (EMA) filters for 5-channel flame array, sonar, smoke, and temperature.
- **Communication:** High-speed UART telemetry link between controllers.
- **Android App:** Real-time manual interface and telemetry monitor.

---

## 2. Hardware Startup
1. **Power On:** Turn on the robot's main power switch.
2. **Initialization:** 
   - Onboard LCD displays "HFSM v4.0 READY".
   - The robot self-calibrates sensors and parks the extinguisher servos at 90°.
   - System defaults to **IDLE / MANUAL MODE**.
3. **WiFi Connection:** 
   - Network: `Firefighter_Robot_AP`
   - Password: `password123`

---

## 3. Remote Controller Setup
1. **Join Network:** Connect your Android device to the robot's WiFi.
2. **Launch App:** Open the **Firefighter Robot Controller** app.
3. **Configure:** In **Settings**, verify IP `192.168.4.1` and Port `80`.
4. **Link Status:** App status bar should show **CONNECTED** (Green).

---

## 4. Manual Control Mode
### Locomotion (Left/Right Panels)
- **Drive (Left):** Forward/Backward with sub-10ms latency.
- **Steer (Right):** Pivot Left/Right to aim the chassis.
- *Motors stop instantly upon touch release for precision.*

### Fire Suppression (Center Panel)
- **🧯 EXTINGUISH:** Toggles the fire extinguisher actuator. The nozzle performs an adaptive horizontal sweep to maximize suppression coverage.
- **🚨 SIRENS:** Activates the audible alarm horn.
- **⛔ EMERGENCY STOP (E-STOP):** High-priority lockout. Cuts power to motors/actuators and sounds a continuous alarm.

---

## 5. Autonomous Autopilot (HFSM)
Tap the **MODE** button to engage the professional autonomous firefighter logic.

### State-Based Intelligence:
1. **SEARCHING:** Executes an intelligent sector-based scan and expand-radius pattern.
2. **TARGET LOCK:** Once a potential fire is detected, the robot stops to verify coordinates.
3. **SAFE APPROACH:** Navigates toward the fire using **Proportional Steering (Kp)** and **Adaptive Speed** (slows down as it gets closer).
4. **OBSTACLE AVOIDANCE:** Dynamically maneuvers around walls or objects while maintaining fire target awareness.
5. **FIRE VALIDATION:** Verifies the heat source using the **FireScore Fusion** (Flame + Smoke + Temperature).
6. **FIRE LOCATION:** Records the exact coordinates of the fire.
7. **EXTINGUISHING:** Stops movement and deploys the extinguisher with an intensity-biased sweep.
8. **FIRE CONFIRMATION:** Monitors the area for 3 seconds after fire suppression to ensure zero reignition.
9. **RECOVERY MODE:** If the robot detects a stall (immobilized), it automatically reverses and rotates to find a new path.

---

## 6. FireScore Fusion System
The robot evaluates confidence before acting:
- **Score > 80:** Confirmed Fire (Immediate Pursuit).
- **Score 60–80:** Investigation (Target Lock).
- **Score < 40:** Ignore (Sunlight/Noise filtering).

---

## 7. Safety & Troubleshooting
- **Watchdog:** If communication with the app is lost for >1s, the robot enters **SAFE MODE** (Emergency Stop).
- **Stall Recovery:** If the robot hits an unmapped obstacle, it will attempt a **RECOVERY_MODE** reverse-pivot maneuver.
- **E-STOP Recovery:** Switch to **MANUAL MODE** to resume normal operation.
- **Offline Fix:** Ensure your phone isn't switching to mobile data because the Robot WiFi has no internet.

---
**System Version:** 4.0 (HFSM Architecture)  
**Suppression:** Fire Extinguisher (Servo Actuated)
**Safety Protocol:** Professional Robotics Watchdog & Persistence Verification
