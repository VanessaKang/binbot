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
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netdb.h>
#include <ifaddrs.h>

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
void allStop();


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

void showIP();
void printHardwareValues();

//DIGITAL IO Pi GPIO pins
#define PIN_TEMP 5
#define PIN_RMFWD 20
#define PIN_RMRVS 21
#define PIN_LMFWD 26
#define PIN_LMRVS 4
#define PIN_SERVO 17

// Define Constants used in code **** Needs to be edited
//BIN SENSOR CONSTANTS
#define BINFULLDIST 5.0
#define BINEMPTYDIST 60.0

//State Constants
#define ERRORSTATE 0
#define TRAVELSTATE 1
#define COLLECTIONSTATE 2
#define DISPOSALSTATE 3

//USER COMMAND
#define NO_COMMAND 0
#define SHUT_DOWN 1
#define STOP 2
#define MOVE_TO_COLLECTIONS 3
#define MOVE_TO_DISPOSAL 4

//i2c global variables
int file_i2c;
int length;
unsigned char buffer[60] = {0};
unsigned char ultraVal;


//declare global variables------------------------------------------------------------
int ei_state = 0;
double ed_fillLevel;
double vd_battVoltage;
int ei_error = 0;
//These variables cannot be defined as string, and i am not quite sure how we are using it
//string es_statusString;
//string es_commandString;
bool eb_destReached = 0;
bool eb_nextDest = 0;
int ei_prevState;
double md_botLocation;
double td_temp;
double cmd_objDist;
double ed_appRssi;
double ed_beaconRssi;
double cd_sensorLeft;
double cd_sensorFront;
double cd_sensorRight;
double cd_sensorFill;
double cd_sensorVertical;

// New proposed variables from Component document
int ei_userCommand;
char eC_appCmdRecv;
char eC_appStatusSend;


//New proposed variables of time
int si_fsmTime;
int si_diagTime;
int si_serverTime;
int si_dataTime;
int si_clockTime;

//Host name IP
char host[NI_MAXHOST];

int runTime = 50;

//Declare time variable for timing purposes
time_t clock_time;
const clock_t begin_time = clock(); //used to calculate total running time
// (float( clock() - begin_time ) /CLOCKS_PER_SEC) <-- Gives total running time

//***************** Main Function ****************//

int main(){
//Setup hardware functionality
setupPins();
setupi2c();
showIP();

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
				//thread so we just need to check the global variable indicating errors (ei_error?)

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
	//std::cout << "Please enter the Value of es_commandString: ";
	//std::cin >> es_commandString;

	//Here we need to check all of our diagnostics and make changes to pertinant
	//diagnostic variables so the Comm/FSM functions can make the correct decisions to
	//deal with the error

}

void *Data(void *ptr){
	printf("Data Thread is running\n");
 	while(1){
 		//Turn LED ON
    	writeData(9);
    	delay(100);

    	writeData(1);
    	delay(100);
    	cd_sensorFront = readData();

    	writeData(2);
    	delay(100);
    	cd_sensorRight = readData();

    	writeData(3);
    	delay(100);
    	cd_sensorLeft = readData();

    	writeData(4);
    	delay(100);
    	cd_sensorFill = readData();

    	writeData(5);
    	delay(100);
    	cd_sensorVertical = readData();


    	writeData(6);
    	delay(100);
    	td_temp = readData();

    	//Turn LED Off
    	writeData(10);
    	delay(100);

    	//printHardwareValues();


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
		printf("Failed to open the i2c bus\n");
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
		//printf("Failed to read from the i2c bus.\n");
	}
	unsigned char val = buffer[0];
	return val;
}

void writeData(int val){
	buffer[0] = val;
	length = 1;
	if (write(file_i2c, buffer, length) != length){
		/* ERROR HANDLING: i2c transaction failed */
		//printf("Failed to write to the i2c bus.\n");
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

void rightMotorStop(){
	digitalWrite(PIN_RMFWD,LOW);
	digitalWrite(PIN_RMRVS,LOW);
}

void leftMotorStop(){
	digitalWrite(PIN_LMFWD,LOW);
	digitalWrite(PIN_LMRVS,LOW);
}

void moveForward(){
	rightMotorForward();
	leftMotorForward();
}

void allStop(){
	rightMotorStop();
	leftMotorStop();
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
    while ((cd_sensorFill >= BINEMPTYDIST) && (ei_userCommand == NO_COMMAND)){
        // Making it a super super
        if (ei_error != 0)
        {
            ei_prevState = ei_state;
            ei_state = ERRORSTATE;
            break;
        }
    }
    if(ei_userCommand != NO_COMMAND){
        //switch cases to adjust state based on user command
        switch(ei_userCommand){
            case SHUT_DOWN:
                //do shutdown stuff
                logFunc();
                system("sudo shutdown -h now");
                break;
            case STOP;
                //do stop stuff
                break;
            case MOVE_TO_COLLECTIONS:
                //do move to collections stuff
                break;
            case MOVE_TO_DISPOSAL:
                //do move to disposal stuff
                break;
            break; //break out of while loop after changing state based on user command
        }
    }
    else if (cd_sensorFill <= BINFULLDIST)
    {
        ei_prevState = ei_state;
        eb_nextDest = 1; //next destination is disposal
        ei_state = TRAVELSTATE;
        break;
    }
}

void disposal(){
	while( (cd_sensorFill < BINFULLDIST) && (ei_userCommand == NO_COMMAND) ){
		//Stay still, wait for garbage to be disposed
		//send message to app, error state will bring back to disposal state
		if(ei_error != 0){
			ei_prevState = ei_state;
			ei_state = ERRORSTATE; //set state to 0 for error state due to error
            break;
		}
	}

	if(ei_userCommand != NO_COMMAND){
		//switch cases to adjust state based on user command
		switch(ei_userCommand){
			case SHUT_DOWN:
				//do shutdown stuff
				logFunc();
				system("sudo shutdown -h now");
				break;
			case STOP;
				//do stop stuff
				break;
			case MOVE_TO_COLLECTIONS:
				//do move to collections stuff
				break;
			case MOVE_TO_DISPOSAL:
				//do move to disposal stuff
				break;
			break; //break out of while loop after changing state based on user command
		}
	}
	else{
        ei_prevState = ei_state;
		ei_state = TRAVELSTATE; //travel mode
		eb_nextDest = 0; //next destination is collection zone
	}
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
    //LAN9512 has operating range of 0 celsius to 70 celsius
    // 250mV at 25 celsius and 20mV/(degree celsius)
	if td_temp >=

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


//Miscellaneous functions, can be moved wherever

void showIP()
{
    struct ifaddrs *ifaddr, *ifa;
    int s;

    if (getifaddrs(&ifaddr) == -1)
    {
        perror("getifaddrs");
        exit(EXIT_FAILURE);
    }


    for (ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next)
    {
        if (ifa->ifa_addr == NULL)
            continue;

        s=getnameinfo(ifa->ifa_addr,sizeof(struct sockaddr_in),host, NI_MAXHOST, NULL, 0, NI_NUMERICHOST);

        if( /*(strcmp(ifa->ifa_name,"wlan0")==0)&&( */ ifa->ifa_addr->sa_family==AF_INET) // )
        {
            if (s != 0)
            {
                printf("getnameinfo() failed: %s\n", gai_strerror(s));
                exit(EXIT_FAILURE);
            }
            printf("Interface : <%s>\n",ifa->ifa_name );
            printf("Address : <%s>\n", host);
        }
    }
}

void printHardwareValues(){
	printf("Front sensor value: ");
    printf("%d\n",cd_sensorFront);
    printf("Right sensor value: ");
    printf("%d\n",cd_sensorRight);
    printf("Left sensor value: ");
    printf("%d\n",cd_sensorLeft);
    printf("Fill sensor value: ");
    printf("%d\n",cd_sensorFill);
    printf("Vertical sensor value: ");
    printf("%d\n",cd_sensorVertical);
    printf("Temperature value: ");
    printf("%d\n",td_temp);
    printf("\n");
}
