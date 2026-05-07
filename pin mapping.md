# Pin Mapping & System Interconnects

This document details the wiring and pin assignments for the Firefighter Robot system.

## 1. ESP32 Main Controller (Master)

### I2C Bus (Shared Communication Bus)
> **Note:** I2C is a bus architecture. All devices listed below connect to the **same two pins** (GPIO 21 & 22) and are distinguished by their unique addresses.

| Function | ESP32 Pin | Shared By | I2C Address |
|---|---|---|---|
| **SDA** | GPIO 21 | PCA9685, LCD, Arduino Uno | - |
| **SCL** | GPIO 22 | PCA9685, LCD, Arduino Uno | - |
| **PCA9685**| Shared | Servo Driver (Suppression) | `0x40` |
| **LCD 1602**| Shared | Status & Sensor Display | `0x27` |
| **Arduino** | Shared | Sensor Hub (Uno) | `0x08` |

### L298N Motor Driver (Locomotion)
| Function | ESP32 Pin | Description |
|---|---|---|
| MOTOR_ENA | GPIO 25 | Left Motor Speed (PWM) |
| MOTOR_ENB | GPIO 19 | Right Motor Speed (PWM) |
| MOTOR_IN1 | GPIO 12 | Left Motor Direction |
| MOTOR_IN2 | GPIO 13 | Left Motor Direction |
| MOTOR_IN3 | GPIO 14 | Right Motor Direction |
| MOTOR_IN4 | GPIO 27 | Right Motor Direction |

### Auxiliary Peripherals (Indicators & Output)
| Function | ESP32 Pin | Description |
|---|---|---|
| **LIGHT_PIN** | GPIO 32 | Ultra-Bright LEDs |
| **PUMP_PIN**  | GPIO 33 | Suppression Actuator Signal |
| **HORN_PIN** | GPIO 23 | Active Buzzer/Horn (Alarm Indicator) |

---

## 2. Arduino Uno Sensor Hub (I2C Slave: 0x08)

### I2C Connections (To ESP32)
| Function | Arduino Pin | Connect To |
|---|---|---|
| **SDA** | A4 | ESP32 GPIO 21 |
| **SCL** | A5 | ESP32 GPIO 22 |

### 5-Channel Flame Sensor Array (Front)
*Detects fire direction across a 120-180° arc. Pins A4/A5 are reserved for I2C.*
| Sensor Channel | Arduino Pin | Type | Description |
|---|---|---|---|
| **Flame 1** | D8 | Digital | Array: Far Left |
| **Flame 2** | A0 | Analog | Array: Mid-Left |
| **Flame 3** | A1 | Analog | Array: Center (Primary Tracking) |
| **Flame 4** | A2 | Analog | Array: Mid-Right |
| **Flame 5** | D9 | Digital | Array: Far Right |

### KY-026 Flame Sensor (Back)
| Component | Arduino Pin | Type | Description |
|---|---|---|---|
| **Back Flame** | D10 | Digital | **KY-026** Rear Fire Detection |

### Ultrasonic Sensors (HC-SR04)
| Location | Trig Pin | Echo Pin | Description |
|---|----------|----------|---|
| **Front** | D2       | D3       | Front Obstacle Avoidance |
| **Back**  | D4       | D5       | Rear Distance Check |

### Environmental & Safety Sensors
| Sensor Type | Arduino Pin | Type | Description |
|---|---|---|---|
| **Smoke/Gas** | A3 | Analog | MQ-2 Sensor |
| **KY-032 Left** | D6 | Digital | Obstacle Avoidance (Left) |
| **KY-032 Right**| D7 | Digital | Obstacle Avoidance (Right) |

---

## 3. Suppression System (PCA9685 @ 0x40)

| Channel | Component | Description |
|---|---|---|
| CH 0 | Sweeping Servo | MG996R: Horizontal nozzle movement |
| CH 1 | Extinguish Servo| MG996R: Trigger actuation for extinguisher |

---

## 4. Hardware Connectivity Notes
1. **Common Ground:** All modules (ESP32, Arduino, L298N, PCA9685) **MUST** share a common GND.
2. **Power Supply:** Motors and Servos require an external high-current power source (e.g., 7.4V Li-ion) and should not be powered directly from the logic pins.
3. **Logic Levels:** A logic level shifter is recommended for the I2C bus between the 3.3V ESP32 and 5V Arduino Uno.
