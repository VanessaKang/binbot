//Binbot Drivers
//This code contains all the relevent IO assignments
//and basic functionality required for movement

#include <NewPing.h>
//PIN NUMBERS
//Since we are emulating an Arduino Uno we must use the same numbering system as if we
//were plugging things in directly to an Uno. See IO Table for complete assignment of IO

//ANALOG IO ARDUINO PINS
#define PIN_TEMP 1
#define PIN_QRD1  2
#define PIN_QRD2 3

//DIGITAL IO ARDUINO PINS
#define PIN_ULTRA_FRONT 2
#define PIN_ULTRA_RIGHT 4
#define PIN_ULTRA_LEFT 7
#define PIN_ULTRA_REAR 12
#define PIN_ULTRA_LEVEL 13
#define PIN_RMFWD 5
#define PIN_RMRVS 6
#define PIN_LMFWD 10
#define PIN_LMRVS 9
#define PIN_SOUND 8
#define PIN_SERVO 3
#define PIN_STATUSLED 11
void setup() {
  //All the IO must be assigned to either an Input or Output mode
  //Ultrasonics are assigned with a special function and are called locally within each read function
  
  //ASSIGN ANALOG IO
  pinMode(PIN_TEMP,INPUT);
  pinMode(PIN_QRD1,INPUT);
  pinMode(PIN_QRD2,INPUT);

  //ASSIGN DIGITAL IO
  pinMode(PIN_RMFWD,OUTPUT);
  pinMode(PIN_RMRVS,OUTPUT);
  pinMode(PIN_LMFWD,OUTPUT);
  pinMode(PIN_LMRVS,OUTPUT);
  pinMode(PIN_SOUND,OUTPUT);
  pinMode(PIN_SERVO,OUTPUT);
  pinMode(PIN_STATUSLED,OUTPUT);
}

void loop() { 
  statusLEDOn();
  delay(500);
  statusLEDOff();
  delay(500);
}

void rightMotorForward(){
  digitalWrite(PIN_RMFWD,1);
  digitalWrite(PIN_RMRVS,0);
}

void rightMotorReverse(){
  digitalWrite(PIN_RMRVS,1);
  digitalWrite(PIN_RMFWD,0);
}

void leftMotorForward(){
  digitalWrite(PIN_LMFWD,1);
  digitalWrite(PIN_LMRVS,0);
}

void leftMotorReverse(){
  digitalWrite(PIN_LMRVS,1);
  digitalWrite(PIN_LMFWD,0);
}

void moveForward(){
  rightMotorForward();
  leftMotorForward();
}

void adjustAnglePositive(){
  rightMotorForward();
  leftMotorReverse();
}

void adjustAngleNegative(){
  rightMotorReverse();
  leftMotorForward();
}

void statusLEDOn(){
  digitalWrite(PIN_STATUSLED,1);
}

void statusLEDOff(){
  digitalWrite(PIN_STATUSLED,0);
}

int tempRead(){
  int temp = analogRead(PIN_TEMP);
  //Do some math
  return temp;
}

int ultraFrontRead(){
  NewPing IO_ULTRA_FRONT(PIN_ULTRA_FRONT,PIN_ULTRA_FRONT);
  delay(50);
  int val = IO_ULTRA_FRONT.ping();
  val = val/US_ROUNDTRIP_CM;
  return val;
}

int ultraRightRead(){
  NewPing IO_ULTRA_RIGHT(PIN_ULTRA_RIGHT,PIN_ULTRA_RIGHT);
  delay(50);
  int val = IO_ULTRA_RIGHT.ping();
  val = val/US_ROUNDTRIP_CM;
  return val;
}

int ultraLeftRead(){
  NewPing IO_ULTRA_LEFT(PIN_ULTRA_LEFT,PIN_ULTRA_LEFT);
  delay(50);
  int val = IO_ULTRA_LEFT.ping();
  val = val/US_ROUNDTRIP_CM;
  return val;
}

int ultraLevelRead(){
  NewPing IO_ULTRA_LEVEL(PIN_ULTRA_LEVEL,PIN_ULTRA_LEVEL);
  delay(50);
  int val = IO_ULTRA_LEVEL.ping();
  val = val/US_ROUNDTRIP_CM;
  return val;
}

int ultraRearRead(){
  NewPing IO_ULTRA_REAR(PIN_ULTRA_REAR,PIN_ULTRA_REAR);
  delay(50);
  int val = IO_ULTRA_REAR.ping();
  val = val/US_ROUNDTRIP_CM;
  return val;
}

