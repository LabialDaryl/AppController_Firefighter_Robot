/*
 * ESP32 Firefighter Robot Firmware - UDP Arrow Version (Speed Adjusted)
 *
 * Reduced overall speed and implemented slower turning for better control.
 */

#include <WiFi.h>
#include <WiFiUdp.h>

// --- Configuration ---
const char* ssid     = "Firefighter_Robot_AP";
const char* password = "password123";
const int   localPort = 80;

WiFiUDP udp;
IPAddress controllerIP;
bool controllerRegistered = false;
unsigned long lastPacketTime = 0;
const unsigned long SAFETY_TIMEOUT = 600;

// --- Pin Definitions (L298N) ---
const int MOTOR_IN1 = 12; // Left Forward
const int MOTOR_IN2 = 13; // Left Backward
const int MOTOR_IN3 = 14; // Right Forward
const int MOTOR_IN4 = 27; // Right Backward
const int MOTOR_ENA = 25; // Left PWM
const int MOTOR_ENB = 19; // Right PWM

const int PUMP_PIN  = 33;
const int LIGHT_PIN = 32;
const int HORN_PIN  = 23;

// --- PWM & Speed Settings ---
const int LEDC_FREQ = 15000;
const int LEDC_RES  = 8;

// Adjusted speeds for better control
const int DRIVE_SPEED = 200; // Reduced from 255 (Forward/Backward)
const int TURN_SPEED  = 200; // Slower speed for turning (Left/Right)

int targetSpeedL = 0;
int targetSpeedR = 0;
char packetBuffer[255];

void setup() {
  Serial.begin(115200);
  delay(2000);
  Serial.println("\n\n=== FIREFIGHTER ROBOT: SPEED ADJUSTED MODE ===");

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
  Serial.println("[UDP] Service Started");
}

void loop() {
  int packetSize = udp.parsePacket();

  if (packetSize) {
    lastPacketTime = millis();
    IPAddress remoteIP = udp.remoteIP();
    int len = udp.read(packetBuffer, 255);
    if (len > 0) {
      packetBuffer[len] = 0;
      String cmd = String(packetBuffer);
      cmd.trim();

      if (!controllerRegistered) {
        controllerIP = remoteIP;
        controllerRegistered = true;
      }

      if (remoteIP == controllerIP) {
        if (cmd == "PING") {
          udp.beginPacket(remoteIP, udp.remotePort());
          udp.write((const uint8_t*)"PONG", 4);
          udp.endPacket();
        } else {
          processCommand(cmd);
        }
      }
    }
  }

  // Safety Timeout
  if (millis() - lastPacketTime > SAFETY_TIMEOUT && (targetSpeedL != 0 || targetSpeedR != 0)) {
    stopMotors();
  }

  ledcWrite(MOTOR_ENA, targetSpeedL);
  ledcWrite(MOTOR_ENB, targetSpeedR);
}

void driveMotors(int l1, int l2, int r3, int r4, int speed) {
  digitalWrite(MOTOR_IN1, l1);
  digitalWrite(MOTOR_IN2, l2);
  digitalWrite(MOTOR_IN3, r3);
  digitalWrite(MOTOR_IN4, r4);
  targetSpeedL = speed;
  targetSpeedR = speed;

  Serial.printf("[MOT] L:%d%d R:%d%d | Spd:%d\n", l1, l2, r3, r4, speed);
}

void stopMotors() {
  driveMotors(0, 0, 0, 0, 0);
}

void processCommand(String cmd) {
  if (cmd == "F") {
    driveMotors(HIGH, LOW, HIGH, LOW, DRIVE_SPEED);
  }
  else if (cmd == "B") {
    driveMotors(LOW, HIGH, LOW, HIGH, DRIVE_SPEED);
  }
  else if (cmd == "L") {
    driveMotors(LOW, HIGH, HIGH, LOW, TURN_SPEED); // Slower turning
  }
  else if (cmd == "R") {
    driveMotors(HIGH, LOW, LOW, HIGH, TURN_SPEED); // Slower turning
  }
  else if (cmd == "S") {
    stopMotors();
  }
  else if (cmd == "PUMP_ON")   digitalWrite(PUMP_PIN,  HIGH);
  else if (cmd == "PUMP_OFF")  digitalWrite(PUMP_PIN,  LOW);
  else if (cmd == "LIGHT_ON")  digitalWrite(LIGHT_PIN, HIGH);
  else if (cmd == "LIGHT_OFF") digitalWrite(LIGHT_PIN, LOW);
  else if (cmd == "HORN_ON")   digitalWrite(HORN_PIN,  HIGH);
  else if (cmd == "HORN_OFF")  digitalWrite(HORN_PIN,  LOW);
}
