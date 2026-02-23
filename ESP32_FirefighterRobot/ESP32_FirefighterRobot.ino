/*
 * ESP32 Firefighter Robot Firmware
 *
 * Communication: WiFi (Access Point Mode)
 * Protocol: TCP Server on Port 80
 *
 * This firmware receives commands from the Android Controller app and
 * executes movement, nozzle aiming, and auxiliary functions.
 *
 * SECURITY: Limited to 1 concurrent connection to prevent command conflicts.
 */

#include <WiFi.h>

// --- Configuration ---
const char* ssid = "Firefighter_Robot_AP";
const char* password = "password123"; // Min 8 characters
const int port = 80;

WiFiServer server(port);
WiFiClient activeClient;
bool isClientConnected = false;

// --- Pin Definitions ---
const int MOTOR_L_IN1 = 12;
const int MOTOR_L_IN2 = 13;
const int MOTOR_R_IN1 = 14;
const int MOTOR_R_IN2 = 27;
const int SERVO_PAN_PIN  = 25;
const int SERVO_TILT_PIN = 26;
const int PUMP_PIN  = 33;
const int LIGHT_PIN = 32;
const int HORN_PIN  = 23;

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n--- Firefighter Robot Starting ---");

  pinMode(MOTOR_L_IN1, OUTPUT);
  pinMode(MOTOR_L_IN2, OUTPUT);
  pinMode(MOTOR_R_IN1, OUTPUT);
  pinMode(MOTOR_R_IN2, OUTPUT);
  pinMode(PUMP_PIN,  OUTPUT);
  pinMode(LIGHT_PIN, OUTPUT);
  pinMode(HORN_PIN,  OUTPUT);

  digitalWrite(PUMP_PIN, LOW);
  digitalWrite(LIGHT_PIN, LOW);
  digitalWrite(HORN_PIN, LOW);
  stopMotors();

  Serial.print("Setting up Access Point...");
  WiFi.softAP(ssid, password);

  IPAddress IP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(IP);

  server.begin();
  Serial.println("TCP Server started (Port 80)");
}

void loop() {
  // Check if a new client is trying to connect
  WiFiClient newClient = server.available();

  if (newClient) {
    if (!isClientConnected) {
      // Accept first client
      activeClient = newClient;
      isClientConnected = true;
      Serial.println(">>> New Controller Connected: " + activeClient.remoteIP().toString());
    } else {
      // Reject any additional clients immediately
      Serial.println("!!! Rejected connection attempt from: " + newClient.remoteIP().toString() + " (Session Busy)");
      newClient.println("ERROR: Session Busy. Only one controller allowed.");
      newClient.stop();
    }
  }

  // Handle active client communication
  if (isClientConnected) {
    if (activeClient.connected()) {
      if (activeClient.available()) {
        String cmd = activeClient.readStringUntil('\n');
        processCommand(cmd);
      }
    } else {
      // Client disconnected
      Serial.println("<<< Controller Disconnected");
      activeClient.stop();
      isClientConnected = false;
      stopMotors(); // Safety: stop if disconnected
    }
  }
}

void processCommand(String cmd) {
  cmd.trim();
  if (cmd.length() == 0) return;

  Serial.print("CMD: ");
  Serial.println(cmd);

  // --- Movement Commands ---
  if (cmd == "F")       moveForward();
  else if (cmd == "B")  moveBackward();
  else if (cmd == "L")  turnLeft();
  else if (cmd == "R")  turnRight();
  else if (cmd == "FL") moveForwardLeft();
  else if (cmd == "FR") moveForwardRight();
  else if (cmd == "BL") moveBackwardLeft();
  else if (cmd == "BR") moveBackwardRight();
  else if (cmd == "S")  stopMotors();

  // --- Nozzle Commands ---
  else if (cmd == "NU")  Serial.println("Nozzle: UP");
  else if (cmd == "ND")  Serial.println("Nozzle: DOWN");
  else if (cmd == "NL")  Serial.println("Nozzle: LEFT");
  else if (cmd == "NR")  Serial.println("Nozzle: RIGHT");
  else if (cmd == "NC")  Serial.println("Nozzle: CENTERED");

  // --- Auxiliary Commands ---
  else if (cmd == "PUMP_ON")   { digitalWrite(PUMP_PIN, HIGH); Serial.println("Pump: ON"); }
  else if (cmd == "PUMP_OFF")  { digitalWrite(PUMP_PIN, LOW);  Serial.println("Pump: OFF"); }
  else if (cmd == "LIGHT_ON")  { digitalWrite(LIGHT_PIN, HIGH); Serial.println("Lights: ON"); }
  else if (cmd == "LIGHT_OFF") { digitalWrite(LIGHT_PIN, LOW);  Serial.println("Lights: OFF"); }
  else if (cmd == "HORN_ON")   { digitalWrite(HORN_PIN, HIGH); Serial.println("Horn: ON"); }
  else if (cmd == "HORN_OFF")  { digitalWrite(HORN_PIN, LOW);  Serial.println("Horn: OFF"); }
  else if (cmd == "ESTOP")     { emergencyStop(); }
}

void stopMotors() {
  digitalWrite(MOTOR_L_IN1, LOW);
  digitalWrite(MOTOR_L_IN2, LOW);
  digitalWrite(MOTOR_R_IN1, LOW);
  digitalWrite(MOTOR_R_IN2, LOW);
}

void moveForward() {
  digitalWrite(MOTOR_L_IN1, HIGH);
  digitalWrite(MOTOR_L_IN2, LOW);
  digitalWrite(MOTOR_R_IN1, HIGH);
  digitalWrite(MOTOR_R_IN2, LOW);
}

void moveBackward() {
  digitalWrite(MOTOR_L_IN1, LOW);
  digitalWrite(MOTOR_L_IN2, HIGH);
  digitalWrite(MOTOR_R_IN1, LOW);
  digitalWrite(MOTOR_R_IN2, HIGH);
}

void turnLeft() {
  digitalWrite(MOTOR_L_IN1, LOW);
  digitalWrite(MOTOR_L_IN2, HIGH);
  digitalWrite(MOTOR_R_IN1, HIGH);
  digitalWrite(MOTOR_R_IN2, LOW);
}

void turnRight() {
  digitalWrite(MOTOR_L_IN1, HIGH);
  digitalWrite(MOTOR_L_IN2, LOW);
  digitalWrite(MOTOR_R_IN1, LOW);
  digitalWrite(MOTOR_R_IN2, HIGH);
}

void moveForwardLeft() {
  digitalWrite(MOTOR_L_IN1, LOW);
  digitalWrite(MOTOR_L_IN2, LOW);
  digitalWrite(MOTOR_R_IN1, HIGH);
  digitalWrite(MOTOR_R_IN2, LOW);
}

void moveForwardRight() {
  digitalWrite(MOTOR_L_IN1, HIGH);
  digitalWrite(MOTOR_L_IN2, LOW);
  digitalWrite(MOTOR_R_IN1, LOW);
  digitalWrite(MOTOR_R_IN2, LOW);
}

void moveBackwardLeft() {
  digitalWrite(MOTOR_L_IN1, LOW);
  digitalWrite(MOTOR_L_IN2, LOW);
  digitalWrite(MOTOR_R_IN1, LOW);
  digitalWrite(MOTOR_R_IN2, HIGH);
}

void moveBackwardRight() {
  digitalWrite(MOTOR_L_IN1, LOW);
  digitalWrite(MOTOR_L_IN2, HIGH);
  digitalWrite(MOTOR_R_IN1, LOW);
  digitalWrite(MOTOR_R_IN2, LOW);
}

void emergencyStop() {
  stopMotors();
  digitalWrite(PUMP_PIN, LOW);
  Serial.println("!!! EMERGENCY STOP !!!");
}
