/*
 * ESP32 Firefighter Robot Firmware
 * Fixed: Compatible with ESP32 Arduino Core v3.x+ (new LEDC API)
 */

#include <WiFi.h>

// --- Configuration ---
const char* ssid     = "Firefighter_Robot_AP";
const char* password = "password123";
const int   port     = 80;

WiFiServer server(port);
WiFiClient activeClient;
bool isClientConnected = false;

// --- Pin Definitions ---
const int MOTOR_IN1 = 12;
const int MOTOR_IN2 = 13;
const int MOTOR_IN3 = 14;
const int MOTOR_IN4 = 27;
const int MOTOR_ENA = 18;  // Left  PWM
const int MOTOR_ENB = 19;  // Right PWM

const int PUMP_PIN  = 33;
const int LIGHT_PIN = 32;
const int HORN_PIN  = 23;

// --- PWM Settings (new API — no channel needed) ---
const int LEDC_FREQ = 20000; // 20 kHz, above hearing range
const int LEDC_RES  = 8;     // 8-bit → 0–255

// --- Speed Settings ---
int targetSpeedL  = 0;
int targetSpeedR  = 0;
int currentSpeedL = 0;
int currentSpeedR = 0;
const int MAX_SPEED  = 220;
const int RAMP_STEP  = 15;
const int RAMP_DELAY = 8;

// -------------------------------------------------------
void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n--- Firefighter Robot Starting ---");

  // Direction pins
  pinMode(MOTOR_IN1, OUTPUT);
  pinMode(MOTOR_IN2, OUTPUT);
  pinMode(MOTOR_IN3, OUTPUT);
  pinMode(MOTOR_IN4, OUTPUT);

  // ✅ New LEDC API for Core v3.x — attach pin directly, no channel number
  ledcAttach(MOTOR_ENA, LEDC_FREQ, LEDC_RES);
  ledcAttach(MOTOR_ENB, LEDC_FREQ, LEDC_RES);

  pinMode(PUMP_PIN,  OUTPUT);
  pinMode(LIGHT_PIN, OUTPUT);
  pinMode(HORN_PIN,  OUTPUT);

  stopMotors();

  Serial.println("Configuring Access Point...");
  WiFi.softAP(ssid, password);
  Serial.print("AP IP address: ");
  Serial.println(WiFi.softAPIP());

  server.begin();
  Serial.println("TCP Server started on port 80");
  Serial.println("Waiting for controller...");
}

// -------------------------------------------------------
void loop() {
  WiFiClient newClient = server.available();
  if (newClient) {
    if (!isClientConnected) {
      activeClient = newClient;
      isClientConnected = true;
      Serial.print(">>> Connected: ");
      Serial.println(activeClient.remoteIP());
    } else {
      Serial.println("! Rejected extra connection");
      newClient.stop();
    }
  }

  if (isClientConnected) {
    if (activeClient.connected()) {
      if (activeClient.available()) {
        String cmd = activeClient.readStringUntil('\n');
        cmd.trim();
        if (cmd.length() > 0) {
          Serial.print("CMD: ["); Serial.print(cmd); Serial.println("]");
          processCommand(cmd);
        }
      }
      rampSpeeds();
    } else {
      Serial.println("<<< Disconnected");
      isClientConnected = false;
      stopMotors();
    }
  }
}

// -------------------------------------------------------
// ✅ New API: ledcWrite(pin, dutyCycle) — pin directly, no channel
void applyPWM(int speedL, int speedR) {
  ledcWrite(MOTOR_ENA, constrain(speedL, 0, 255));
  ledcWrite(MOTOR_ENB, constrain(speedR, 0, 255));
}

void rampSpeeds() {
  bool changed = false;

  if (currentSpeedL < targetSpeedL)      { currentSpeedL = min(currentSpeedL + RAMP_STEP, targetSpeedL); changed = true; }
  else if (currentSpeedL > targetSpeedL) { currentSpeedL = max(currentSpeedL - RAMP_STEP, targetSpeedL); changed = true; }

  if (currentSpeedR < targetSpeedR)      { currentSpeedR = min(currentSpeedR + RAMP_STEP, targetSpeedR); changed = true; }
  else if (currentSpeedR > targetSpeedR) { currentSpeedR = max(currentSpeedR - RAMP_STEP, targetSpeedR); changed = true; }

  if (changed) {
    applyPWM(currentSpeedL, currentSpeedR);
    delay(RAMP_DELAY);
  }
}

void setMotors(bool l_fwd, bool l_back, bool r_fwd, bool r_back, int spdL, int spdR) {
  digitalWrite(MOTOR_IN1, l_fwd  ? HIGH : LOW);
  digitalWrite(MOTOR_IN2, l_back ? HIGH : LOW);
  digitalWrite(MOTOR_IN3, r_fwd  ? HIGH : LOW);
  digitalWrite(MOTOR_IN4, r_back ? HIGH : LOW);
  targetSpeedL = spdL;
  targetSpeedR = spdR;
}

// -------------------------------------------------------
void stopMotors() {
  setMotors(0,0,0,0, 0, 0);
  currentSpeedL = 0;
  currentSpeedR = 0;
  applyPWM(0, 0);
  Serial.println("Motors: STOP");
}

void moveForward()       { setMotors(1,0,1,0, MAX_SPEED,   MAX_SPEED);   Serial.println("FWD"); }
void moveBackward()      { setMotors(0,1,0,1, MAX_SPEED,   MAX_SPEED);   Serial.println("BWD"); }
void turnLeft()          { setMotors(0,1,1,0, MAX_SPEED,   MAX_SPEED);   Serial.println("SPIN LEFT"); }
void turnRight()         { setMotors(1,0,0,1, MAX_SPEED,   MAX_SPEED);   Serial.println("SPIN RIGHT"); }
void moveForwardLeft()   { setMotors(1,0,1,0, MAX_SPEED/2, MAX_SPEED);   Serial.println("FWD-LEFT"); }
void moveForwardRight()  { setMotors(1,0,1,0, MAX_SPEED,   MAX_SPEED/2); Serial.println("FWD-RIGHT"); }
void moveBackwardLeft()  { setMotors(0,1,0,1, MAX_SPEED/2, MAX_SPEED);   Serial.println("BWD-LEFT"); }
void moveBackwardRight() { setMotors(0,1,0,1, MAX_SPEED,   MAX_SPEED/2); Serial.println("BWD-RIGHT"); }

// -------------------------------------------------------
void processCommand(String cmd) {
  if      (cmd == "F")  moveForward();
  else if (cmd == "B")  moveBackward();
  else if (cmd == "L")  turnLeft();
  else if (cmd == "R")  turnRight();
  else if (cmd == "FL") moveForwardLeft();
  else if (cmd == "FR") moveForwardRight();
  else if (cmd == "BL") moveBackwardLeft();
  else if (cmd == "BR") moveBackwardRight();
  else if (cmd == "S")  stopMotors();

  else if (cmd == "PUMP_ON")   { digitalWrite(PUMP_PIN,  HIGH); Serial.println("Pump: ON"); }
  else if (cmd == "PUMP_OFF")  { digitalWrite(PUMP_PIN,  LOW);  Serial.println("Pump: OFF"); }
  else if (cmd == "LIGHT_ON")  { digitalWrite(LIGHT_PIN, HIGH); Serial.println("Light: ON"); }
  else if (cmd == "LIGHT_OFF") { digitalWrite(LIGHT_PIN, LOW);  Serial.println("Light: OFF"); }
  else if (cmd == "HORN_ON")   { digitalWrite(HORN_PIN,  HIGH); Serial.println("Horn: ON"); }
  else if (cmd == "HORN_OFF")  { digitalWrite(HORN_PIN,  LOW);  Serial.println("Horn: OFF"); }

  else if (cmd == "ESTOP") {
    stopMotors();
    digitalWrite(PUMP_PIN, LOW);
    digitalWrite(HORN_PIN, LOW);
    Serial.println("!!! EMERGENCY STOP !!!");
  }

  else { Serial.print("Unknown CMD: "); Serial.println(cmd); }
}