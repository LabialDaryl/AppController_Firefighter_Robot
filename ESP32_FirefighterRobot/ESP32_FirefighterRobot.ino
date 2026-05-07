/*
 * =========================================================
 *  ESP32 Firefighter Robot Firmware
 *  - Motor control via L298N
 *  - Dual MG996R servo suppression via PCA9685
 *  - I2C Integration: Arduino Uno (Sensor Hub) & LCD 1602
 *  - UDP over WiFi AP (Manual & Auto Modes)
 * =========================================================
 */

#include <Adafruit_PWMServoDriver.h>
#include <WiFi.h>
#include <WiFiUdp.h>
#include <Wire.h>
#include <hd44780.h>
#include <hd44780ioClass/hd44780_I2Cexp.h>

// =========================================================
//  Configurations & Constants
// =========================================================
const char *ssid = "Firefighter_Robot_AP";
const char *password = "password123";
const int localPort = 80;

#define UNO_I2C_ADDR 0x08
#define LCD_I2C_ADDR 0x27

// PCA9685 Settings
#define SERVO_FREQ 50
#define SERVOMIN 150
#define SERVOMAX 500
#define SWEEP_SERVO_CH 0
#define EXTINGUISH_SERVO_CH 1

// L298N Pins
const int MOTOR_IN1 = 12, MOTOR_IN2 = 13;
const int MOTOR_IN3 = 14, MOTOR_IN4 = 27;
const int MOTOR_ENA = 25, MOTOR_ENB = 19;
const int LEDC_FREQ = 15000, LEDC_RES = 8;
const int DRIVE_SPEED = 200, TURN_SPEED = 200;

// Motor Calibration Bias (Adjust these to balance speeds)
// If Right is faster, decrease R_MOTOR_BIAS (e.g., to 0.95 or 0.90)
const float L_MOTOR_BIAS = 1.00;
const float R_MOTOR_BIAS = 0.97; // Reduced to 0.94 to balance faster right motor

// Peripherals
const int PUMP_PIN = 33, LIGHT_PIN = 32, HORN_PIN = 23;

// =========================================================
//  Global State & Data Structures
// =========================================================
enum RobotMode { MANUAL, AUTO };
RobotMode currentMode = MANUAL;

struct __attribute__((packed)) SensorData {
  uint16_t flameFront[5];
  uint16_t flameBack;
  uint16_t smokeValue;
  float distanceFront;
  float distanceBack;
  bool irLeft;
  bool irRight;
} sensors;

WiFiUDP udp;
IPAddress controllerIP;
bool controllerRegistered = false;
unsigned long lastPacketTime = 0;
const unsigned long SAFETY_TIMEOUT = 800; // Reduced for faster safety stop
char packetBuffer[255];

Adafruit_PWMServoDriver pwm = Adafruit_PWMServoDriver(0x40);
hd44780_I2Cexp lcd(LCD_I2C_ADDR);

int targetSpeedL = 0, targetSpeedR = 0;
bool extinguishActive = false;

// Servo States
int sweepAngle = 90, sweepDir = 2;
int extAngle = 90, extDir = -1;
unsigned long lastSweepTime = 0;
const int SWEEP_MIN = 60, SWEEP_MAX = 120;
const unsigned long SWEEP_INTERVAL = 30;
const int EXT_DEPLOY_ANGLE = 0, EXT_STOW_ANGLE = 90;

// =========================================================
//  PROTOTYPES
// =========================================================
void setServoAngle(uint8_t ch, int angle);
void driveMotors(int l1, int l2, int r3, int r4, int speed);
void stopMotors();
void processCommand(String cmd);
void readSensors();
void updateLCD();
void handleAutonomousLogic();

// =========================================================
//  SETUP
// =========================================================
void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n\n[SYS] --- FIREFIGHTER ROBOT STARTING ---");

  // I2C Init
  Wire.begin(21, 22);
  Wire.setClock(400000);
  Serial.println("[I2C] Bus initialized.");

  // LCD Init
  int status = lcd.begin(16, 2);
  if (!status) {
    lcd.backlight();
    lcd.print("System Booting...");
  }

  // PCA9685 Init
  pwm.begin();
  pwm.setOscillatorFrequency(27000000);
  pwm.setPWMFreq(SERVO_FREQ);
  setServoAngle(SWEEP_SERVO_CH, 90);
  setServoAngle(EXTINGUISH_SERVO_CH, EXT_STOW_ANGLE);

  // Motor Pins
  pinMode(MOTOR_IN1, OUTPUT); pinMode(MOTOR_IN2, OUTPUT);
  pinMode(MOTOR_IN3, OUTPUT); pinMode(MOTOR_IN4, OUTPUT);
  ledcAttach(MOTOR_ENA, LEDC_FREQ, LEDC_RES);
  ledcAttach(MOTOR_ENB, LEDC_FREQ, LEDC_RES);
  stopMotors();

  // Peripherals
  pinMode(PUMP_PIN, OUTPUT); pinMode(LIGHT_PIN, OUTPUT); pinMode(HORN_PIN, OUTPUT);
  digitalWrite(PUMP_PIN, LOW); digitalWrite(LIGHT_PIN, LOW); digitalWrite(HORN_PIN, LOW);

  // WiFi AP
  WiFi.softAP(ssid, password);
  udp.begin(localPort);
  Serial.print("[WiFi] IP: "); Serial.println(WiFi.softAPIP());

  lcd.clear();
  lcd.print("Ready: MANUAL");
  lastPacketTime = millis();
}

// =========================================================
//  LOOP
// =========================================================
void loop() {
  // 1. UDP Handling - Low Latency processing
  while (int packetSize = udp.parsePacket()) {
    int len = udp.read(packetBuffer, 254);
    if (len > 0) {
      packetBuffer[len] = 0;
      String cmd = String(packetBuffer);
      cmd.trim();

      // Handle PING separately - Don't let it reset safety timer for driving
      if (cmd == "PING") {
        udp.beginPacket(udp.remoteIP(), udp.remotePort());
        udp.print("PONG");
        udp.endPacket();
      } else {
        // Only non-PING commands reset the safety timeout
        lastPacketTime = millis();

        if (!controllerRegistered) {
          controllerIP = udp.remoteIP();
          controllerRegistered = true;
          Serial.print("[UDP] Controller registered: "); Serial.println(controllerIP);
        }
        processCommand(cmd);
      }
    }
  }

  // 2. Read Sensors (Non-blocking check)
  static unsigned long lastSensorRead = 0;
  if (millis() - lastSensorRead > 50) { // Read every 50ms to keep loop fast
    readSensors();
    lastSensorRead = millis();
  }

  // 3. Logic Execution
  if (currentMode == AUTO) {
    handleAutonomousLogic();
  } else {
    // Manual Safety Timeout: If no DRIVE command for > SAFETY_TIMEOUT, stop.
    if ((millis() - lastPacketTime > SAFETY_TIMEOUT) && (targetSpeedL != 0 || targetSpeedR != 0)) {
      Serial.println("[SAFE] Drive command lost. Stopping motors.");
      stopMotors();
    }
  }

  // 4. Servo Animations
  if (extinguishActive) {
    if (millis() - lastSweepTime > SWEEP_INTERVAL) {
      lastSweepTime = millis();
      sweepAngle += sweepDir;
      if (sweepAngle >= SWEEP_MAX || sweepAngle <= SWEEP_MIN) sweepDir *= -1;
      setServoAngle(SWEEP_SERVO_CH, sweepAngle);
    }
  }

  // 5. Apply PWM
  ledcWrite(MOTOR_ENA, targetSpeedL);
  ledcWrite(MOTOR_ENB, targetSpeedR);

  // 6. Periodic UI Updates
  static unsigned long lastUI = 0;
  if (millis() - lastUI > 1000) {
    lastUI = millis();
    updateLCD();
  }
}

void readSensors() {
  Wire.requestFrom(UNO_I2C_ADDR, sizeof(sensors));
  if (Wire.available() == sizeof(sensors)) {
    uint8_t *ptr = (uint8_t *)&sensors;
    for (size_t i = 0; i < sizeof(sensors); i++) {
      ptr[i] = Wire.read();
    }
  }
}

void processCommand(String cmd) {
  if (cmd == "MODE_AUTO") {
    currentMode = AUTO;
    stopMotors();
  } else if (cmd == "MODE_MANUAL") {
    currentMode = MANUAL;
    stopMotors();
  }

  if (currentMode == MANUAL) {
    if (cmd == "F") driveMotors(HIGH, LOW, HIGH, LOW, DRIVE_SPEED);
    else if (cmd == "B") driveMotors(LOW, HIGH, LOW, HIGH, DRIVE_SPEED);
    else if (cmd == "L") driveMotors(LOW, HIGH, HIGH, LOW, TURN_SPEED);
    else if (cmd == "R") driveMotors(HIGH, LOW, LOW, HIGH, TURN_SPEED);
    else if (cmd == "S") stopMotors();
    else if (cmd == "EXT_ON") {
      extinguishActive = true; 
      digitalWrite(PUMP_PIN, HIGH); 
      setServoAngle(EXTINGUISH_SERVO_CH, EXT_DEPLOY_ANGLE);
    }
    else if (cmd == "EXT_OFF") { 
      extinguishActive = false; 
      digitalWrite(PUMP_PIN, LOW); 
      setServoAngle(EXTINGUISH_SERVO_CH, EXT_STOW_ANGLE);
      setServoAngle(SWEEP_SERVO_CH, 90);
    }
    else if (cmd == "SIREN_ON") digitalWrite(HORN_PIN, HIGH);
    else if (cmd == "SIREN_OFF") digitalWrite(HORN_PIN, LOW);
    else if (cmd == "STANDBY" || cmd == "ESTOP") {
      stopMotors();
      digitalWrite(PUMP_PIN, LOW);
      digitalWrite(HORN_PIN, LOW);
      extinguishActive = false;
      setServoAngle(EXTINGUISH_SERVO_CH, EXT_STOW_ANGLE);
      setServoAngle(SWEEP_SERVO_CH, 90);
    }
  }
}

void handleAutonomousLogic() {
  if (sensors.distanceFront < 20.0 && sensors.distanceFront > 0) {
    if (targetSpeedL != 0) stopMotors();
  }
}

void updateLCD() {
  lcd.setCursor(0, 0);
  lcd.print(currentMode == AUTO ? "MOD: AUTO      " : "MOD: MANUAL    ");
  lcd.setCursor(0, 1);
  char buf[16];
  snprintf(buf, 16, "D:%dcm F:%d   ", (int)sensors.distanceFront, sensors.flameFront[2]);
  lcd.print(buf);
}

void setServoAngle(uint8_t ch, int angle) {
  int pulse = map(constrain(angle, 0, 180), 0, 180, SERVOMIN, SERVOMAX);
  pwm.setPWM(ch, 0, pulse);
}

void driveMotors(int l1, int l2, int r3, int r4, int speed) {
  digitalWrite(MOTOR_IN1, l1); digitalWrite(MOTOR_IN2, l2);
  digitalWrite(MOTOR_IN3, r3); digitalWrite(MOTOR_IN4, r4);

  // Apply calibration bias to ensure balanced movement
  targetSpeedL = (int)(speed * L_MOTOR_BIAS);
  targetSpeedR = (int)(speed * R_MOTOR_BIAS);

  // Constrain to PWM limits
  targetSpeedL = constrain(targetSpeedL, 0, 255);
  targetSpeedR = constrain(targetSpeedR, 0, 255);
}

void stopMotors() {
  driveMotors(0, 0, 0, 0, 0);
}
