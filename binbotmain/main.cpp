#include <stdio.h>
#include <iostream>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <pthread.h>
#include <time.h>
#include <math.h>
#include <wiringPi.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/i2c-dev.h>

//declare primary functions
void *FSM(void* ptr);
void *Comms(void* ptr);
void *Diag(void* ptr);
void *Data(void* ptr);
void errorState();
void travel();
void collection();
void disposal();
void endFunc();
void logFunc();

//declare hardware functions
void setupPins();
void setupi2c();
unsigned char readData();
void writeData(int val);
void rightMotorForward();
void rightMotorReverse();
void leftMotorForward();
void leftMotorReverse();
void moveForward();
void adjustAnglePositive();
void adjustAngleNegative();


//declare sensor/actuator functions
void ultrasonicPing();
void binLevelDetect();

//declare diagnostic functions
void overheatDiag();
void batteryLowDiag();
void fallOverDiag();
void immobileDiag();
void noBinDiag();
void ultraSensDiag();
void motorDiag();
void connectionDiag();

//DIGITAL IO Pi GPIO pins
#define PIN_TEMP 5
#define PIN_RMFWD 20
#define PIN_RMRVS 21
#define PIN_LMFWD 26
#define PIN_LMRVS 4
#define PIN_SERVO 17

//i2c global variables
int file_i2c;
int length;
unsigned char buffer[60] = {0};
unsigned char ultraVal;


//declare global variables
int ei_state = 0;
float ef_fillLevel;
int vi_battVolt;
bool eb_error = 0;
int ei_statusString;
int ei_commandString;
bool eb_destReached = 0;
bool eb_dest = 0;
int ei_prevState;
int ei_botLocation;
int ei_temp;
int mi_objDist;
float ef_rssi;
float ef_beacon;
int ei_sensorLeft;
int ei_sensorFront;
int ei_sensorRight;
int ei_sensorFill;
int ei_sensorVertical;

float runTime = 2000.0;

//Declare time variable for timing purposes
time_t clock_time;
const clock_t begin_time = clock(); //used to calculate total running time
// (float( clock() - begin_time ) /CLOCKS_PER_SEC) <-- Gives total running time

//***************** Main Function ****************//

int main(){
//Setup hardware functionality
setupPins();
setupi2c();

//declare local variables
int placeholder;

//Set up threads
pthread_t thread1, thread2, thread3, thread4;

int iret1FSM, iret2Comms, iret3Diag, iret4Data;

//Initialize global variables for start of FSM, Diagnostics and Communications (maybe read from a log to get last values)

//create independent threads to run each function
iret1FSM = pthread_create( &thread1, NULL, FSM, NULL);
iret2Comms = pthread_create( &thread2, NULL, Comms, NULL);
iret3Diag = pthread_create( &thread3, NULL, Diag, NULL);
iret4Data = pthread_create( &thread4, NULL, Data, NULL);

//wait for each thread to finish before completing program
pthread_join( thread1, NULL);
pthread_join( thread2, NULL);
pthread_join( thread3, NULL);
pthread_join( thread4, NULL);

//print to console to confirm program has finished
std::cout << "\n";
std::cout << "Program Ended";
std::cout << "\n";

return 0;

}

//*********************** Primary Functions ********************************//


void *FSM(void *ptr){
	printf("FSM Thread Running \n");
	while(1){	//Run Various states until commanded to break
		float time = ( float( clock() ) /CLOCKS_PER_SEC );

		if((float( clock() - begin_time ) /CLOCKS_PER_SEC) > runTime){
			printf("Diag has exited \n");
			break;
		}
		switch (ei_state){
			case 0:
			if ( fmod(time,1) == 0){
				printf("Error State = %i \n ", ei_state);
				//std::cout << float( clock() ) /CLOCKS_PER_SEC;
				//Check for an error here and take remedial actions?, Diagnostics are run by another
				//thread so we just need to check the global variable indicating errors (eb_error?)

				ei_prevState = 0;
			}
			break;
			case 1: //Travel State
				printf("Travel State = %i \n", ei_state);
				//Run Travel Code here

				ei_prevState = 1;
			break;
			case 2: //Collection State
				printf("Collection State = %i \n", ei_state);
				//Run Collection Code here

				ei_prevState = 2;
			break;
			case 3: // Disposal State
				printf("Disposal State = %i \n", ei_state);
				//Run Disposal Code here

				ei_prevState = 3;
			break;
		}
	}
}


void *Comms(void *ptr){
	printf("Comms Thread Running \n");
	while(1){
		if((float( clock() - begin_time ) /CLOCKS_PER_SEC) > runTime){
			printf("Comms has exited \n");
			break;
		}
	}
}

void *Diag(void *ptr){
	printf("Diag Thread Running \n");
	while(1){
		overheatDiag();
		batteryLowDiag();
		fallOverDiag();
		immobileDiag();
		noBinDiag();
		ultraSensDiag();
		motorDiag();
		connectionDiag();
		if((float( clock() - begin_time ) /CLOCKS_PER_SEC) > runTime){
			printf("Diag has exited \n");
			break;
		}
	}
	//std::cout << "Please enter the Value of ei_commandString: ";
	//std::cin >> ei_commandString;

	//Here we need to check all of our diagnostics and make changes to pertinant
	//diagnostic variables so the Comm/FSM functions can make the correct decisions to
	//deal with the error

}

void *Data(void *ptr){
 	while(1){
 		//Turn LED ON
    	writeData(9);
    	delay(100);

    	writeData(1);
    	delay(100);
    	ei_sensorFront = readData();
    	printf("Front sensor value: ");
    	printf("%d\n",ei_sensorFront);

    	writeData(2);
    	delay(100);
    	ei_sensorRight = readData();
    	printf("Right sensor value: ");
    	printf("%d\n",ei_sensorRight);

    	writeData(3);
    	delay(100);
    	ei_sensorLeft = readData();
    	printf("Left sensor value: ");
    	printf("%d\n",ei_sensorLeft);

    	writeData(4);
    	delay(100);
    	ei_sensorFill = readData();
    	printf("Fill sensor value: ");
    	printf("%d\n",ei_sensorFill);

    	writeData(5);
    	delay(100);
    	ei_sensorVertical = readData();
    	printf("Vertical sensor value: ");
    	printf("%d\n",ei_sensorVertical);

    	writeData(6);
    	delay(100);
    	ei_temp = readData();
    	printf("Temperature value: ");
    	printf("%d\n",ei_temp);

    	printf("\n");

    	writeData(10);
    	delay(100);


    	if((float( clock() - begin_time ) /CLOCKS_PER_SEC) > runTime){
			printf("Data has exited \n");
			break;
		}
  	}
}

//*********************** Supporting Functions ********************************//

//Need to add all the functions pertaining to moving the robot
//Perhaps have functions that support object avoidance and regular navigation for the travel state

//Also should have a function for initializing global variables reading from a log file/text file to assist with reboots after an error
//and so that the robot has a memory. Probably would be useful.

void setupPins(){
  wiringPiSetupGpio();
  //ASSIGN DIGITAL IO


  pinMode(PIN_RMFWD,OUTPUT);
  pinMode(PIN_RMRVS,OUTPUT);
  pinMode(PIN_LMFWD,OUTPUT);
  pinMode(PIN_LMRVS,OUTPUT);
  pinMode(PIN_SERVO,OUTPUT);

  pinMode(PIN_TEMP,INPUT);
  pullUpDnControl(PIN_TEMP,PUD_DOWN);
}

void setupi2c(){
	//----- OPEN THE I2C BUS -----
	char *filename = (char*)"/dev/i2c-1";
	if ((file_i2c = open(filename, O_RDWR)) < 0)
	{
		//ERROR HANDLING: you can check errno to see what went wrong
		printf("Failed to open the i2c bus");
		return;
	}

	int addr = 0x04;          //<<<<<The I2C address of the slave
	if (ioctl(file_i2c, I2C_SLAVE, addr) < 0)
	{
		printf("Failed to acquire bus access and/or talk to slave.\n");
		//ERROR HANDLING; you can check errno to see what went wrong
		return;
	}
}

unsigned char readData(){
	length = 1;
	if (read(file_i2c, buffer, length) != length){
		//ERROR HANDLING: i2c transaction failed
		printf("Failed to read from the i2c bus.\n");
	}
	unsigned char val = buffer[0];
	return val;
}

void writeData(int val){
	buffer[0] = val;
	length = 1;
	if (write(file_i2c, buffer, length) != length){
		/* ERROR HANDLING: i2c transaction failed */
		printf("Failed to write to the i2c bus.\n");
	}
}

void rightMotorForward(){
  digitalWrite(PIN_RMFWD,HIGH);
  digitalWrite(PIN_RMRVS,LOW);
}

void rightMotorReverse(){
  digitalWrite(PIN_RMRVS,HIGH);
  digitalWrite(PIN_RMFWD,LOW);
}

void leftMotorForward(){
  digitalWrite(PIN_LMFWD,HIGH);
  digitalWrite(PIN_LMRVS,LOW);
}

void leftMotorReverse(){
  digitalWrite(PIN_LMRVS,HIGH);
  digitalWrite(PIN_LMFWD,LOW);
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

void errorState(){

}

void travel(){

}

void collection(){

}

void disposal(){

}

void endFunc(){
	printf("FSM has exited \n");
	std::cout << float( clock() ) /CLOCKS_PER_SEC; //print out time spent running program
}

void logFunc(){
	//put code here to write variables to a log
}

void binLevelDetect(){
	//detects fullness of bin from waste
}

//********************* Sensor/Actuator Functions *****************************//


//*********************** Diagnostic Functions ********************************//
void overheatDiag(){

}
void batteryLowDiag(){

}
void fallOverDiag(){

}
void immobileDiag(){

}
void noBinDiag(){

}
void ultraSensDiag(){

}
void motorDiag(){

}
void connectionDiag(){

}
