//Binbot Drivers
//This code contains all the relevent IO assignments
//and basic functionality required for movement

#include <NewPing.h>
#include <NewTone.h>
#include <Wire.h>

//PIN NUMBERS
//Since we are emulating an Arduino Uno we must use the same numbering system as if we
//were plugging things in directly to an Uno. See IO Table for complete assignment of IO

//ANALOG IO ARDUINO PINS
int PIN_TEMP = 1;
int PIN_QRD1  = 2;
int PIN_QRD2 = 3;

//DIGITAL IO ARDUINO PINS
int PIN_ULTRA_FRONT = 2;
int PIN_ULTRA_RIGHT = 4;
int PIN_ULTRA_LEFT = 7;
int PIN_ULTRA_LEVEL = 13;
int PIN_ULTRA_VERTICAL = 12;
int PIN_RMFWD = 5;
int PIN_RMRVS = 6;
int PIN_LMFWD = 9;
int PIN_LMRVS = 10;
int PIN_SOUND = 8;
int PIN_SERVO = 3;
int PIN_STATUSLED = 11;

int fthresh = 40;
int lrthresh = 15;
int turnVal = 15;
double distanceVal;
int allowedMoves = 10;

int numMoves;
int moveFlag;

double wheelBotRatio = 3.149;
double secondDegreeRatio = 215.0;
double secondMeterRatio = 4.7;

double deltaDistance;
double deltaAngle;

double startDistance = 1;

double travelDistance;
double remainDistance; 

double avoidAngle;
double avoidDistance;

double newDistance;

#define SLAVE_ADDRESS 0x04
int piCommand;
int outValue;

int frontReadVal;
int rightReadVal;
int leftReadVal;
int levelReadVal;
int verticalReadVal;
int tempReadVal;
int qrd1ReadVal;
int qrd2ReadVal;

void setup() {
  //All the IO must be assigned to either an Input or Output mode
  //Ultrasonics are assigned with a special function and are called locally within each read function
  
  //ASSIGN ANALOG IO
  pinMode(PIN_TEMP,INPUT);
  pinMode(PIN_QRD1,INPUT);
  pinMode(PIN_QRD2,INPUT);

  //ASSIGN DIGITAL IO
  pinMode(PIN_RMFWD,OUTPUT);
  digitalWrite(PIN_RMFWD,0);
  
  pinMode(PIN_RMRVS,OUTPUT);
  digitalWrite(PIN_RMRVS,0);
  
  pinMode(PIN_LMFWD,OUTPUT);
  digitalWrite(PIN_LMFWD,0);
  
  pinMode(PIN_LMRVS,OUTPUT);
  digitalWrite(PIN_LMRVS,0);
  
  pinMode(PIN_SOUND,OUTPUT);
  pinMode(PIN_SERVO,OUTPUT);
  pinMode(PIN_STATUSLED,OUTPUT);
 
  Serial.begin(9600);

  Wire.begin(SLAVE_ADDRESS);
  Wire.onReceive(receiveData);
  Wire.onRequest(sendData);
}

void loop() {

    frontReadVal = ultraFrontRead();
    rightReadVal = ultraRightRead();
    leftReadVal = ultraLeftRead();
    levelReadVal = ultraLevelRead();
    verticalReadVal = ultraVerticalRead();
    tempReadVal = tempSensorRead();
    Serial.println(tempReadVal);

    //qrd1ReadVal = qrd1SensorRead();
    //qrd2ReadVal = qrd2SensorRead();
//
//  if(frontRead == 0 or frontRead > 400){
//    frontRead = 400;
//  }
//  if(rightRead == 0 or rightRead > 400){
//    rightRead = 400;
//  }
//  if(leftRead == 0 or leftRead > 400){
//    leftRead = 400;
//  }
//  printSensors(frontRead,rightRead,leftRead);
//  
//  if(numMoves==allowedMoves){
//    if((frontRead>=fthresh) & (rightRead>=lrthresh) & (leftRead>=lrthresh)){
//      numMoves = 0;
//    }
//    else{
//      rightRotateBot(turnVal);
//      NewTone(PIN_SOUND,400);    
//      Serial.println("Exiting looping state");
//    }
//  }
//  if(numMoves < allowedMoves){
//    if((frontRead>=fthresh) & (rightRead>=lrthresh) & (leftRead>=lrthresh)){
//      numMoves = 0;
//      statusLEDOn();
//      noNewTone(PIN_SOUND);
//      Serial.println("Moving forward");
//      remainDistance = remainDistance - distanceVal;
//      moveForwardDistance(distanceVal); 
//    }
//    else if(frontRead<=fthresh){
//      numMoves ++;
//      statusLEDOff();
//      NewTone(PIN_SOUND,262);
//      if(rightRead < leftRead){
//        leftRotateBot(turnVal);
//        Serial.println("Turning Left");
//      }
//      else if (leftRead < rightRead) {
//         rightRotateBot(turnVal);
//         Serial.println("Turning Right");
//      }
//    }
//    else if(frontRead>=fthresh){
//      numMoves ++;
//      statusLEDOff();
//      NewTone(PIN_SOUND,262);
//      if(rightRead<=lrthresh){
//        leftRotateBot(turnVal);
//        Serial.println("Turning Left");
//      }
//      else if (leftRead<=lrthresh) {
//         rightRotateBot(turnVal);
//         Serial.println("Turning Right");
//      }
//    }
//  }


}

void allStop(){
  digitalWrite(PIN_RMFWD,0);
  digitalWrite(PIN_RMRVS,0);
  digitalWrite(PIN_LMFWD,0);
  digitalWrite(PIN_LMRVS,0);
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


void moveReverse(){
  rightMotorReverse();
  leftMotorReverse();
}

void adjustAngleLeft(){
  rightMotorForward();
  leftMotorReverse();
}

void adjustAngleRight(){
  rightMotorReverse();
  leftMotorForward();
}

void statusLEDOn(){
  digitalWrite(PIN_STATUSLED,1);
}

void statusLEDOff(){
  digitalWrite(PIN_STATUSLED,0);
}

int tempSensorRead(){
  int tempAnalog = analogRead(PIN_TEMP);
  int temp = ((5*tempAnalog*100)/1024); 
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
  unsigned char val = IO_ULTRA_RIGHT.ping();
  val = val/US_ROUNDTRIP_CM;
  return val;
}

int ultraLeftRead(){
  NewPing IO_ULTRA_LEFT(PIN_ULTRA_LEFT,PIN_ULTRA_LEFT);
  delay(50);
  unsigned char val = IO_ULTRA_LEFT.ping();
  val = val/US_ROUNDTRIP_CM;
  return val;
}

int ultraLevelRead(){
  NewPing IO_ULTRA_LEVEL(PIN_ULTRA_LEVEL,PIN_ULTRA_LEVEL);
  delay(50);
  unsigned char val = IO_ULTRA_LEVEL.ping();
  val = val/US_ROUNDTRIP_CM;
  return val;
}

int ultraVerticalRead(){
  NewPing IO_ULTRA_VERTICAL(PIN_ULTRA_VERTICAL,PIN_ULTRA_VERTICAL);
  delay(50);
  unsigned char val = IO_ULTRA_VERTICAL.ping();
  val = val/US_ROUNDTRIP_CM;
  return val;
}


void moveForwardDistance(float val){
  double forwardBotDelay;
  forwardBotDelay = val*secondMeterRatio*1000;
  moveForward();
  delay(forwardBotDelay);
  //allStop();
}

void rightRotateWheels(int val){
  double turnWheelDelay;
  turnWheelDelay = val*(1000/secondDegreeRatio);
  //Experimentally derived, ideal value should be (1/240)s/deg
  adjustAngleRight();
  delay(turnWheelDelay);
  //allStop();
}

void rightRotateBot(int val){
  int turnBotDelay;
  turnBotDelay = val*wheelBotRatio*(1000/secondDegreeRatio);
  //Experimentally derived, ideal value should be (1/240)s/deg
  adjustAngleRight();
  delay(turnBotDelay);
  //allStop(); 
}

void leftRotateWheels(int val){
  int turnWheelDelay;
  turnWheelDelay = val*(1000/secondDegreeRatio);
  //Experimentally derived, ideal value should be (1/240)s/deg
  adjustAngleLeft();
  delay(turnWheelDelay);
  //allStop();
}

void leftRotateBot(int val){
  int turnBotDelay;
  turnBotDelay = val*wheelBotRatio*(1000/secondDegreeRatio);
  //Experimentally derived, ideal value should be (1/240)s/deg
  adjustAngleLeft();
  delay(turnBotDelay);
  //allStop(); 
}

void printSensors(int frontReadVal, int rightReadVal, int leftReadVal){
  Serial.print("Front sensor reading");
  Serial.print(frontReadVal);
  Serial.print("cm \t");
  Serial.print("Right sensor reading");
  Serial.print(rightReadVal);
  Serial.print("cm \t");
  Serial.print("Left sensor reading");
  Serial.print(leftReadVal);
  Serial.println("cm");
}

void receiveData(int byteCount){
  while(Wire.available()){
    piCommand = Wire.read();
    Serial.print("I have recieved the command #: ");
    Serial.println(piCommand);
    switch(piCommand){
      case 1:
        outValue = frontReadVal;
        break;
      case 2:
        outValue = rightReadVal;
        break;
      case 3:
        outValue = leftReadVal;
        break;
      case 4:
        outValue = levelReadVal;
        break;
      case 5:
        outValue = verticalReadVal;
        break;
      case 6:
        outValue = tempReadVal;
        break;
      case 7:
        outValue = qrd1ReadVal;
        break;
      case 8:
        outValue = qrd2ReadVal;
        break;
      case 9:
        statusLEDOn();
        break;
      case 10:
        statusLEDOff();
        break;
    }
  }  

}

void sendData(){
    switch(piCommand){
      case 1:
        Serial.print("Front Ultrasonic reading: ");
        break;
      case 2:
        Serial.print("Right Ultrasonic reading: ");
        break;
      case 3:
        Serial.print("Left Ultrasonic reading: ");
        break;
      case 4:
        Serial.print("Level Ultrasonic reading: ");
        break;
      case 5:
        Serial.print("Vertical Ultrasonic reading: ");
        break;
      case 6:
        Serial.print("Temperature reading: ");
        break;
      case 7:
        Serial.print("QRD1 reading: ");
        break;
      case 8:
        Serial.print("QRD2 reading: ");
        break;
      case 9:
        Serial.print("LED Turned On");
        break;
      case 10:
        Serial.print("LED Turned Off");
        break;
    }
  Serial.println(outValue);
  Wire.write(outValue);
}





