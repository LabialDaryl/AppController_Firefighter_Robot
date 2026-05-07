# Hardware Components List

This document lists the hardware components used in the Firefighter Robot project, organized by their role in the system.

## 1. Control & Processing
- **ESP32 Dev Module:** Main controller handling WiFi/UDP communication, manual/auto logic, and high-level coordination.
- **Arduino Uno:** Peripheral sensor hub. Offloads sensor reading and processing from the ESP32. Communicates via I2C (Address: `0x08`).

## 2. Locomotion & Drive
- **L298N Dual H-Bridge Motor Driver:** Controls the two main DC drive motors.
- **2x DC Gear Motors:** High-torque motors for robot movement.

## 3. Suppression System
- **PCA9685 16-Channel 12-bit PWM/Servo Driver:** Controls the suppression servos via I2C (Address: `0x40`).
- **2x MG996R High Torque Servos:** 
    - **Sweeping Servo:** Controls the horizontal sweeping mechanism of the extinguisher nozzle.
    - **Extinguishing Servo:** Mechanically actuates the trigger of the 500ml portable fire extinguisher to release the suppression agent.
- **500ml Portable Fire Extinguisher:** The primary suppression agent, replacing the water pump system.

## 4. Sensing (Managed by Arduino Uno)
- **5-Channel Flame Sensor Array:** Mounted on the front for directional fire detection.
- **KY-026 Flame Sensor:** Mounted on the back for rear fire detection.
- **MQ-2 Smoke/Gas Sensor:** For fire verification and safety.
- **Ultrasonic Sensors (HC-SR04):** For front and back distance measurement/obstacle avoidance.
- **IR Obstacle Sensors:** For left/right edge and obstacle detection.

## 5. Feedback & Indicators
- **LCD 1602 with I2C Adapter:** Displays system status, mode (Manual/Auto), and sensor data (Address: `0x27`).
- **Active Buzzer/Horn:** Audible alerts for fire detection or system status.
- **Ultra-Bright LEDs:** For lighting and status indication.

## 6. Power System
- **Power Source:** Typically a Li-ion battery pack (7.4V or 11.1V).
- **Voltage Regulators:** Buck converters to provide stable 5V for the microcontrollers/sensors and high current for servos.
