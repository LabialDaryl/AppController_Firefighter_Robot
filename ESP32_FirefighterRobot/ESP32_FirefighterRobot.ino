/*
 * ESP32 Firefighter Robot Firmware - Suppression Mechanism Version (with Debug)
 */

#include <WiFi.h>
#include <WiFiUdp.h>
#include <Wire.h>
#include <Adafruit_PWMServoDriver.h>

// --- Configuration ---
const char* ssid     = "Firefighter_Robot_AP";
const char* password = "password123";
const int   localPort = 80;

WiFiUDP udp;
IPAddress controllerIP;
bool controllerRegistered = false;
unsigned long lastPacketTime = 0;
const unsigned long SAFETY_TIMEOUT = 600;

// PCA9685 Setup
Adafruit_PWMServoDriver pwm = Adafruit_PWMServoDriver(0x40); // Standard Address
#define SERVOMIN  150
#define SERVOMAX  500
#define SERVO_FREQ 50
#define SUPPRESSION_CH 0

// --- Pin Definitions ---
const int MOTOR_IN1 = 12;
const int MOTOR_IN2 = 13;
const int MOTOR_IN3 = 14;
const int MOTOR_IN4 = 27;
const int MOTOR_ENA = 25;
const int MOTOR_ENB = 19;

const int PUMP_PIN  = 33;
const int LIGHT_PIN = 32;
const int HORN_PIN  = 23;

const int LEDC_FREQ = 15000;
const int LEDC_RES  = 8;
const int DRIVE_SPEED = 200;
const int TURN_SPEED  = 200;

int targetSpeedL = 0;
int targetSpeedR = 0;
char packetBuffer[255];

// --- Suppression State ---
bool pumpActive = false;
int sweepAngle = 90;
int sweepDirection = 2;
unsigned long lastSweepTime = 0;

void setup() {
  Serial.begin(115200);
  delay(2000);
  Serial.println("\n\n=== FIREFIGHTER ROBOT: DEBUG MODE ===");

  // I2C for Servo Driver
  Wire.begin(21, 22);
  Serial.println("[I2C] Initializing PCA9685...");
  if (pwm.begin()) {
    Serial.println("[I2C] PCA9685 Found!");
  } else {
    Serial.println("[I2C] PCA9685 NOT FOUND! Check wiring (21=SDA, 22=SCL)");
  }

  pwm.setOscillatorFrequency(27000000);
  pwm.setPWMFreq(SERVO_FREQ);
  setServoAngle(SUPPRESSION_CH, 90);

  pinMode(MOTOR_IN1, OUTPUT);
  pinMode(MOTOR_IN2, OUTPUT);
  pinMode(MOTOR_IN3, OUTPUT);
  pinMode(MOTOR_IN4, OUTPUT);
  ledcAttach(MOTOR_ENA, LEDC_FREQ, LEDC_RES);
  ledcAttach(MOTOR_ENB, LEDC_FREQ, LEDC_RES);

  pinMode(PUMP_PIN,  OUTPUT);
  pinMode(LIGHT_PIN, OUTPUT);
  pinMode(HORN_PIN,  OUTPUT);

  stopMotors();

  WiFi.softAP(ssid, password);
  udp.begin(localPort);
  Serial.println("[UDP] Service Started. Waiting for App...");
}

void loop() {
  int packetSize = udp.parsePacket();

  if (packetSize) {
    lastPacketTime = millis();
    int len = udp.read(packetBuffer, 255);
    if (len > 0) {
      packetBuffer[len] = 0;
      String cmd = String(packetBuffer);
      cmd.trim();
      processCommand(cmd);
    }
  }

  if (pumpActive) {
    if (millis() - lastSweepTime > 30) { // Slower sweep for testing
      lastSweepTime = millis();
      sweepAngle += sweepDirection;
      if (sweepAngle >= 150 || sweepAngle <= 30) sweepDirection *= -1;
      setServoAngle(SUPPRESSION_CH, sweepAngle);
    }
  }

  if (millis() - lastPacketTime > SAFETY_TIMEOUT && (targetSpeedL != 0 || targetSpeedR != 0)) {
    stopMotors();
  }

  ledcWrite(MOTOR_ENA, targetSpeedL);
  ledcWrite(MOTOR_ENB, targetSpeedR);
}

void setServoAngle(uint8_t n, double angle) {
  double pulse = map(angle, 0, 180, SERVOMIN, SERVOMAX);
  pwm.setPWM(n, 0, pulse);
}

void driveMotors(int l1, int l2, int r3, int r4, int speed) {
  digitalWrite(MOTOR_IN1, l1);
  digitalWrite(MOTOR_IN2, l2);
  digitalWrite(MOTOR_IN3, r3);
  digitalWrite(MOTOR_IN4, r4);
  targetSpeedL = speed;
  targetSpeedR = speed;
}

void stopMotors() {
  driveMotors(0, 0, 0, 0, 0);
}

void processCommand(String cmd) {
  if (cmd == "F")      driveMotors(HIGH, LOW, HIGH, LOW, DRIVE_SPEED);
  else if (cmd == "B") driveMotors(LOW, HIGH, LOW, HIGH, DRIVE_SPEED);
  else if (cmd == "L") driveMotors(LOW, HIGH, HIGH, LOW, TURN_SPEED);
  else if (cmd == "R") driveMotors(HIGH, LOW, LOW, HIGH, TURN_SPEED);
  else if (cmd == "S") stopMotors();
  else if (cmd == "PUMP_ON") {
    Serial.println("[CMD] PUMP ON - Activating Servo Sweep");
    digitalWrite(PUMP_PIN, HIGH);
    pumpActive = true;
  }
  else if (cmd == "PUMP_OFF") {
    Serial.println("[CMD] PUMP OFF - Stopping Servo");
    digitalWrite(PUMP_PIN, LOW);
    pumpActive = false;
    setServoAngle(SUPPRESSION_CH, 90);
  }
  else if (cmd == "LIGHT_ON")  digitalWrite(LIGHT_PIN, HIGH);
  else if (cmd == "LIGHT_OFF") digitalWrite(LIGHT_PIN, LOW);
  else if (cmd == "HORN_ON")   digitalWrite(HORN_PIN,  HIGH);
  else if (cmd == "HORN_OFF")  digitalWrite(HORN_PIN,  LOW);
}
