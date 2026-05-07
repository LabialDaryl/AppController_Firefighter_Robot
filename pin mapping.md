# Pin Mapping & System Interconnects

This document details the wiring and pin assignments for the Firefighter Robot system.

## 1. ESP32 Main Controller (Master)

### I2C Bus (System-wide Communication)
| Function | ESP32 Pin | Connects To |
|---|---|---|
| SDA | GPIO 21 | PCA9685, LCD 1602, Arduino Uno |
| SCL | GPIO 22 | PCA9685, LCD 1602, Arduino Uno |

### L298N Motor Driver
| Function | ESP32 Pin | Description |
|---|---|---|
| MOTOR_ENA | GPIO 25 | Left Motor Speed (PWM) |
| MOTOR_ENB | GPIO 19 | Right Motor Speed (PWM) |
| MOTOR_IN1 | GPIO 12 | Left Motor Direction |
| MOTOR_IN2 | GPIO 13 | Left Motor Direction |
| MOTOR_IN3 | GPIO 14 | Right Motor Direction |
| MOTOR_IN4 | GPIO 27 | Right Motor Direction |

### Auxiliary Peripherals
| Function | ESP32 Pin | Description |
|---|---|---|
| LIGHT_PIN | GPIO 32 | High-intensity LEDs |
| PUMP_PIN | GPIO 33 | Extinguisher Trigger Mechanism |
| HORN_PIN | GPIO 23 | Active Buzzer/Horn |

---

## 2. PCA9685 Servo Driver (I2C: 0x40)

| Channel | Component | Description |
|---|---|---|
| CH 0 | Sweeping Servo | Horizontal yaw for nozzle coverage |
| CH 1 | Extinguish Servo | Actuates fire extinguisher trigger |

---

## 3. LCD 1602 Display (I2C: 0x27)
- **VCC:** 5V
- **GND:** GND
- **SDA:** ESP32 GPIO 21
- **SCL:** ESP32 GPIO 22

---

## 4. Arduino Uno Sensor Hub (I2C: 0x08)
The Arduino Uno acts as an I2C slave to the ESP32. It manages the following sensors:

### Sensor Connectivity (Arduino Uno Side)
| Sensor Type | Arduino Uno Pin (Suggested) | Description |
|---|---|---|
| **Flame Array** | A0 - A4 | 5-Channel Directional Array |
| **Back Flame** | A5 | KY-026 Rear Sensor |
| **Smoke/Gas** | A6 (or Digital) | MQ-2 Sensor |
| **Trig (Front)**| D2 | Ultrasonic Front Trigger |
| **Echo (Front)**| D3 | Ultrasonic Front Echo |
| **Trig (Back)** | D4 | Ultrasonic Back Trigger |
| **Echo (Back)** | D5 | Ultrasonic Back Echo |
| **IR Left** | D6 | Obstacle/Edge Sensor |
| **IR Right** | D7 | Obstacle/Edge Sensor |
| **SDA** | A4 | I2C Data to ESP32 |
| **SCL** | A5 | I2C Clock to ESP32 |

*Note: Ensure common ground (GND) is connected between the ESP32 and Arduino Uno.*
