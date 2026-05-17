# Pin Mapping & System Interconnects

This document details the wiring and pin assignments for the Firefighter Robot system.

## 1. ESP32 Main Controller (Main Brain)


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

---

## 3. Suppression System (PCA9685 @ 0x40)

| Channel | Component | Description |
|---|---|---|
| CH 0 | Sweeping Servo | MG996R: Horizontal nozzle movement |
| CH 1 | Extinguish Servo| MG996R: Trigger actuation for extinguisher |

### PCA9685 (16-Channel) Wiring & Power

| Signal | Connection | Notes |
|---|---|---|
| SDA | ESP32 GPIO 21 | I2C data line (shared bus). Use level shifter or ensure PCA9685 logic VCC = 3.3V to avoid 5V on bus. |
| SCL | ESP32 GPIO 22 | I2C clock line (shared bus). Use level shifter or ensure PCA9685 logic VCC = 3.3V. |
| VCC (logic) | 3.3V (preferred) or through level shifter | Some PCA9685 modules have a separate logic `VCC` and servo `V+`. If module only supports 5V VCC, place a bidirectional I2C level shifter between ESP32 and PCA9685. |
| V+ (servo power) | External 5–6V supply (e.g., battery) | Power for servos only. Must share common ground with the ESP32 and Uno. Add decoupling capacitor (e.g., 1000 µF) near servo power input. |
| GND | Common ground (ESP32, Uno, servo supply) | Required. |
| OE / RESET (if present) | Tie OE low (GND) to enable outputs; RESET to 3.3V or leave pulled high per module docs | Module variants differ; follow your breakout documentation. |

Notes:
- Address: PCA9685 default I2C address is `0x40` (configurable by solder jumpers on some boards).
- Avoid tying SDA/SCL to 5V pull-ups when the ESP32 is on the bus. Use a proper level shifter or 3.3V pull-ups.
- Servos draw significant current; do not power servos from the ESP32 or Uno 5V pins.

---

## 4. Sensors (new assignments)

These sensors are mapped to unique ESP32 pins. Analog sensors are placed on ADC1 channels (GPIO32-39) where possible for stable readings while avoiding ADC2 (shared with Wi‑Fi).

| Sensor / Channel | ESP32 Pin | Interface | Notes |
|---|---:|---|---|
| 5-Channel Flame Sensor Array (Front) — OUT1 (module: 7-pin: VCC, GND, OUT1..OUT5) | GPIO4 | Digital IN | Signal pin OUT1 from front array. VCC and GND wired to 3.3V/ GND as module requires; do not tie to ESP32 GPIOs. |
| 5-Channel Flame Sensor Array (Front) — OUT2 (module: 7-pin: VCC, GND, OUT1..OUT5) | GPIO16 | Digital IN | Signal pin OUT2 from front array. Use pull-down or external pull resistors if needed. |
| 5-Channel Flame Sensor Array (Front) — OUT3 (module: 7-pin: VCC, GND, OUT1..OUT5) | GPIO17 | Digital IN | Signal pin OUT3. Avoid pins 6-11 (flash) and boot-straps (0,2,15) to prevent conflicts. |
| 5-Channel Flame Sensor Array (Front) — OUT4 (module: 7-pin: VCC, GND, OUT1..OUT5) | GPIO18 | Digital IN | Signal pin OUT4. |
| 5-Channel Flame Sensor Array (Front) — OUT5 (module: 7-pin: VCC, GND, OUT1..OUT5) | GPIO5 | Digital IN | Signal pin OUT5. |
| KY-026 Flame Sensor (Rear) | GPIO39 | Analog IN (ADC1_CH3) | Use ADC1 input-only pin for stable analog flame level reading. Pin is input-only (no pull-ups). |
| MQ-2 Smoke/Gas Sensor | GPIO36 | Analog IN (ADC1_CH0) | Place on ADC1 channel. Provide load resistor as per MQ-2 module docs and warm-up time for readings. |
| KY-032 Obstacle IR Sensor (Left) | GPIO34 | Digital IN (input-only) | Use input-only pins for IR detection; add small hardware hysteresis if noisy. |
| KY-032 Obstacle IR Sensor (Right) | GPIO35 | Digital IN (input-only) | |

Notes:
- Each sensor channel is assigned to a single GPIO to avoid bus/contention.
- Analog sensors use ADC1 pins (GPIO32-39) to avoid ADC2 Wi‑Fi conflicts.
- For stable analog readings add a small RC filter (10k + 0.1uF) if noisy.

## 5. Indicators and Outputs (added passive buzzer + LED notes)

| Function | ESP32 Pin | Interface | Notes |
|---|---:|---|---|
| Ultra-Bright LEDs (siren/indicator) | GPIO32 | PWM / Digital OUT | Existing `LIGHT_PIN` retained on GPIO32. Drive LEDs via MOSFET/LED driver; do NOT draw high current from ESP32 pin. Use PWM for dimming/strobe. |
| Passive Buzzer (siren) | GPIO26 | PWM (LEDC) | Use an N‑MOSFET or driver transistor and PWM/LEDC to generate tones. Keep one component per pin; do not share with motor drivers. |

Hardware notes:
- Use transistors or MOSFET drivers for both the Ultra-Bright LED strip and passive buzzer to keep MCU pins safe.
- Keep all grounds common (ESP32, sensors, PCA9685, power supplies).
- Avoid using pins 6–11 (flash), and be cautious with boot strapping pins `GPIO0`, `GPIO2`, and `GPIO15`.

