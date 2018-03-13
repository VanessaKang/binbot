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
#include <chrono>
#include <iomanip>
#include <fstream>
#include <vector>

//NicksServer includes------------------------------------------------------------
#include <sys/socket.h>
#include <bluetooth/bluetooth.h> 
#include <bluetooth/rfcomm.h> 
#include <string.h>

//CONSTANT DECLARTION 
//TODO change macros to const 
#define STATE_NOCONNECTION 0 
#define STATE_CONNECTED 1 


//CONSTANTS DECLARATION 
//TODO change macros to const 
#define MODE 0 
#define FILL 1
#define BATT 2 
#define SIG  3
#define UPDATE_SIZE 4

#define FILL_FULL 0 
#define FILL_PARTIAL 1 
#define FILL_NEAR_EMPTY 2 
#define FILL_EMPTY 3 

//GLOBAL VARIABLE DECLARATION 
int connectionStatus = STATE_NOCONNECTION; 

struct sockaddr_rc loc_addr = { 0 }, rem_addr = { 0 };
char buf[1024] = { 0 };
int sock, client; 
socklen_t opt = sizeof(rem_addr);

char address[18] = "B8:27:EB:08:F9:52"; //Address of the pi NOTE: Must change for each spereate pi used  

pthread_t readThread, writeThread; 
clock_t t, new_t; 


//FUNCTION DECLARATION 
void setupSocket(); 
void listen(); 
void spawn();
void *writeToApp(void *ptr); 
void *readFromApp(void *ptr); 

//-------------------------------------------------------------------------


//declare primary functions
//hello
void *FSM(void* ptr);
void *bluetoothServer(void* ptr);
void *errorDiag(void* ptr);
void *Data(void* ptr);
void errorState();
void travel();
void collection();
void disposal();
void pathFinding();
//void obstacleAvoidance();
void endFunc();
void logFunc();

//declare hardware functions
void setupPins();
void setupi2c();
unsigned char readData();
void writeData(int val);



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
void dataCollection();
double timeFromStart(auto y);

//DIGITAL IO Pi GPIO pins
#define PIN_TEMP 5
#define PIN_RMFWD 20
#define PIN_RMRVS 21
#define PIN_LMFWD 26
#define PIN_LMRVS 4
#define PIN_SERVO 17

// Define Constants used in code **** Needs to be edited
//BIN SENSOR CONSTANTS
#define BINFULLDIST 5
#define BINEMPTYDIST 25
#define FTHRESH 40
#define LRTHRESH 25
#define ALLOWEDMOVES 10
#define ATDESTRSSI -44


//State Constants
#define ERRORSTATE 0
#define TRAVELSTATE 1
#define COLLECTIONSTATE 2
#define DISPOSALSTATE 3

//nextDest Constants
#define COLLECTION 0
#define DISPOSAL 1

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
#define I2CDELAY 75


//declare global variables--------
int ei_state= TRAVELSTATE;
double ed_fillLevel;
double vd_battVoltage;
int ei_error=0;
//These variables cannot be defined as string, and i am not quite sure how we are using it
//string es_statusString;
//string es_commandString;
bool eb_destReached;
bool eb_nextDest= COLLECTION;
bool eb_lineFollow = 0;
bool eb_binFilled = 0;
bool eb_binEmptied = 0;
int ei_prevState = 0;
double md_botLocation;
int ti_temp;
double cmd_objDist;
double ed_appRssi;
double ed_beaconRssi = -70;
int ci_sensorLeft;
int ci_sensorFront;
int ci_sensorRight;
int ci_sensorFill = 15;
int ci_sensorVertical;

// New proposed variables from Component document
int ei_userCommand = NO_COMMAND;
char eC_appCmdRecv;
char eC_appStatusSend;


//New proposed variables of time
int si_fsmTime;
int si_errorDiagTime;
int si_serverTime;
int si_dataTime;
int si_clockTime;

//Host name IP
char host[NI_MAXHOST];


//Variables for Collison Avoidance
int numMoves;
int obstacleCount=1;

//Declare time variable for timing purposes
double runTime = 45000.0; //run time in milliseconds
auto start = std::chrono::system_clock::now();


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

int iret1FSM, iret2bluetoothServer, iret3errorDiag, iret4Data;

//Initialize global variables for start of FSM, Diagnostics and Communications (maybe read from a log to get last values)

//create independent threads to run each function
//iret1FSM = pthread_create( &thread1, NULL, FSM, NULL);
iret2bluetoothServer = pthread_create( &thread2, NULL, bluetoothServer, NULL);
//iret3errorDiag = pthread_create( &thread3, NULL, errorDiag, NULL);
//iret4Data = pthread_create( &thread4, NULL, Data, NULL);


//wait for each thread to finish before completing program
//pthread_join( thread1, NULL);
pthread_join( thread2, NULL);
//pthread_join( thread3, NULL);
//pthread_join( thread4, NULL);

//print to console to confirm program has finished
std::cout << "\n";
std::cout << "Program Ended";
std::cout << "\n";


}

//*********************** Primary Functions ********************************//


void *FSM(void *ptr){
    delay(1000);
    printf("FSM Thread Running \n");
    while(1){   //Run Various states until commanded to break

        if(timeFromStart(start) > runTime ){
            printf("errorDiag has exited \n");
            break;
        }

        switch (ei_state){
            case 0:
                if (ei_prevState != 0){
                    printf("Error State = %i \n ", ei_state);
                //std::cout << float( clock() ) /CLOCKS_PER_SEC;
                //Check for an error here and take remedial actions?, Diagnostics are run by another
                //thread so we just need to check the global variable indicating errors (ei_error?)
                }
                errorState();
                ei_prevState = 0;
            break;
            case 1: //Travel State
                if(ei_prevState != 1){
		    
                }
		printf("entering travel \n");
		travel();
                
                ei_prevState = 1;
            break;
            case 2: //Collection State
                if(ei_prevState != 2){
                    printf("Collection State = %i \n", ei_state);
                }
                collection();
                ei_prevState = 2;
            break;
            case 3: // Disposal State
                if(ei_prevState){
                    printf("Disposal State = %i \n", ei_state);                    
                }
                disposal();
                ei_prevState = 3;
            break;
        }
    }
}


void *bluetoothServer(void *ptr){
    printf("bluetoothServer Thread Running \n");
    while(1){
        if(timeFromStart(start) > runTime){
            printf("bluetoothServer has exited \n");
            break;
        }
    }
}

void *errorDiag(void *ptr){
    printf("errorDiag Thread Running \n");
    while(1){
        overheatDiag();
        batteryLowDiag();
        fallOverDiag();
        immobileDiag();
        noBinDiag();
        ultraSensDiag();
        motorDiag();
        connectionDiag();
        if(timeFromStart(start) > runTime){
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
    while(1){
    	if(eb_lineFollow == TRUE && ei_state == TRAVELSTATE){
                //printf("linefollowing \n");
        		writeData(19);
    	}
    	else if(eb_lineFollow == FALSE && ei_state == TRAVELSTATE){
            //printf("stopping \n");
    		writeData(20);
    	}
        if(ei_state == COLLECTIONSTATE || ei_state == DISPOSALSTATE){
            writeData(4);
            delay(I2CDELAY);
            //ci_sensorFill = readData();
            if(ci_sensorFill==0 or ci_sensorFill>255){
                ci_sensorFill = 255;
            }
            printf("SensorFill: %i \n",ci_sensorFill);
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


void errorState(){
    printf("Error State\n");
}

void travel(){
    printf("in the travel state \n");
    eb_lineFollow = TRUE;
    while (ei_userCommand == NO_COMMAND && ei_error == 0)
    {	
	std::string str1 (" 0");
	std::string ID("66666666-6666-6666-6666-666666666666");
	usleep(500000);
	std::ifstream beaconFile;
	beaconFile.open("beaconvalues.txt");
	if(!beaconFile){
		std::cout << "Unable to open beaconFile";
		exit(1);
	}
	
	std::vector<std::string> values;
	const int MAXSIZE = 100;
	char thisVal[MAXSIZE];

	while(beaconFile.getline(thisVal,MAXSIZE,',')){
		values.push_back(thisVal);
	}
	beaconFile.close();
    //std::cout << values.size() << "\n";
    if(values.size() == 0){     //protects against seg faults by trying to access
        break;                  //an empty array
    }

	
        for(int i = 0; i<values.size();i++){
           std::cout << values[i] << "\n";
        }
	
	if(values[1].compare(ID) == 0){
		int major = stoi(values[2]);
		if(major == 0 && eb_nextDest == COLLECTION){
			int collectRSSI = stoi(values[3]);
			std::cout << collectRSSI << "\n";
            printf("Going to Collection beacon \n");
			
			if(collectRSSI > ATDESTRSSI){
				
				ei_state = COLLECTIONSTATE;
				eb_lineFollow = FALSE;
				printf("Arrived at Collection Zone \n");
				break;
			}
		}
		else if(major == 1 && eb_nextDest == DISPOSAL){
			int disposalRSSI = stoi(values[3]);
			std::cout << disposalRSSI << "\n";
            printf("Going to Disposal beacon \n");

			if(disposalRSSI > ATDESTRSSI){
				
				ei_state = DISPOSALSTATE;
				eb_lineFollow = 0;
				printf("Arrived at Disposal Zone \n");
				break;
			}
		}	
	}
    }
    /*if (ei_error != 0)
    {
        ei_prevState = TRAVELSTATE;
        ei_state = ERRORSTATE;
        return;
    }
    if (ei_userCommand != NO_COMMAND)
    {
            //switch cases to adjust state based on user command
        switch(ei_userCommand){
            case SHUT_DOWN:
                //do shutdown stuff
                logFunc();
                //system("sudo shutdown -h now");
                break;
            case STOP:
                //do stop stuff
                break;
            case MOVE_TO_COLLECTIONS:
                //do move to collections stuff
                break;
            case MOVE_TO_DISPOSAL:
                //do move to disposal stuff
                break;
    return; //break out of function after receiving user command
        }
    }*/
}


void collection(){
    while ((eb_binFilled == FALSE) && (ei_userCommand == NO_COMMAND)){
        //printf("Collecting Mode \n");
        //Can replace if statement with function
        //that implements more accurate bin full function
        if(ci_sensorFill <= BINFULLDIST){
            eb_binFilled = TRUE;
        }
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
            case STOP:
                //do stop stuff
                break;
            case MOVE_TO_COLLECTIONS:
                //do move to collections stuff
                break;
            case MOVE_TO_DISPOSAL:
                //do move to disposal stuff
                break;
    return; //break out of function after receiving user command
        }
    }
    else
    {
        printf("in collection");
        while(1){

        }
        ei_prevState = ei_state;
        eb_nextDest = DISPOSAL; //next destination is disposal
        ei_state = TRAVELSTATE;
    return;
    }
}

void disposal(){
    while( (eb_binEmptied == FALSE) && (ei_userCommand == NO_COMMAND) ){
        //Stay still, wait for garbage to be disposed
        //send message to app, error state will bring back to disposal state
        if(ci_sensorFill < BINEMPTYDIST){
            eb_binEmptied= TRUE;
        }
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
            case STOP:
                //do stop stuff
                break;
            case MOVE_TO_COLLECTIONS:
                //do move to collections stuff
                break;
            case MOVE_TO_DISPOSAL:
                //do move to disposal stuff
                break;
        return; //break out of function after receiving user command
        }
    }
    else{
        ei_prevState = ei_state;
        ei_state = TRAVELSTATE; //travel mode
        eb_nextDest = 0; //next destination is collection zone
        return;
    }
}

void endFunc(){
    printf("FSM has exited \n");
    std::cout << timeFromStart(start); //print out time spent running program
}

void pathFinding(){
}
/*
void obstacleAvoidance(){
    if(numMoves>=ALLOWEDMOVES){
        if((ci_sensorFront>=FTHRESH) && (ci_sensorRight>=LRTHRESH) && (ci_sensorLeft>=LRTHRESH)){
            numMoves = 0;
        }
        else{
            numMoves++;
            adjustAngleNegative();
            //printf("Turning Right\n");
        }
    }
    if(numMoves < ALLOWEDMOVES){
        if((ci_sensorFront>=FTHRESH) && (ci_sensorRight>=LRTHRESH) && (ci_sensorLeft>=LRTHRESH)){
            numMoves = 0;
            moveForward(); 
            //printf("Moving Forward\n");
        }
        else if(ci_sensorFront<=FTHRESH){
            numMoves ++;
            if(ci_sensorRight < ci_sensorLeft){
                adjustAnglePositive();
                //printf("Turning Left\n");
            }
            else if (ci_sensorLeft < ci_sensorRight) {
                adjustAngleNegative();
                //printf("Turning Right\n");
            }
        }
        else if(ci_sensorFront>=FTHRESH){
            numMoves ++;
            if(ci_sensorRight<=LRTHRESH){
                adjustAnglePositive();
                //printf("Turning Left\n");
            }
            else if (ci_sensorLeft<=LRTHRESH) {
                adjustAngleNegative();
                //printf("Turning Right\n");
            }
        }
    }
    //printf("Nummoves: \t");
    //printf("%i\n",numMoves);
}
*/

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
    // 750mV at 25 celsius and 10mV/(degree celsius)
    if (ti_temp >= 35)
    {
        printf("Overheated\n");
        ei_error = 1;
        return;
    }

    ei_error = 0;
    return;

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

void showIP(){
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

void dataCollection(){
    printf("Data Thread is running\n");
    while(1){
    //Turn LED ON
        writeData(9);
        //delay(I2CDELAY);
        delay(I2CDELAY);

        writeData(1);
        delay(I2CDELAY);
        ci_sensorFront = readData();
        if(ci_sensorFront==0 or ci_sensorFront>255){
            ci_sensorFront = 255;
        }

        writeData(2);
        delay(I2CDELAY);
        ci_sensorRight = readData();
        if(ci_sensorRight==0 or ci_sensorRight>255){
            ci_sensorRight = 255;
        } 

        writeData(3);
        delay(I2CDELAY);
        ci_sensorLeft = readData();
        if(ci_sensorLeft==0 or ci_sensorLeft>255){
            ci_sensorLeft = 255;
        } 

        writeData(4);
        delay(I2CDELAY);
        ci_sensorFill = readData();
        if(ci_sensorFill==0 or ci_sensorFill>255){
            ci_sensorFill = 255;
        } 

        // writeData(5);
        // delay(I2CDELAY);
        // ci_sensorVertical = readData();
        // if(ci_sensorVertical==0 or ci_sensorVertical>255){
        //  ci_sensorVertical = 255;
        // } 

        writeData(6);
        delay(I2CDELAY);
        ti_temp = readData();

        //Turn LED Off
        writeData(10);
        delay(I2CDELAY);
        //printHardwareValues();
        //obstacleAvoidance();
        if(timeFromStart(start) > runTime){
            printf("Data has exited \n");
            break;
        }
    }
}

void printHardwareValues(){
    printf("Front sensor value: ");
    printf("%i\n",ci_sensorFront);
    printf("Right sensor value: ");
    printf("%i\n",ci_sensorRight);
    printf("Left sensor value: ");
    printf("%i\n",ci_sensorLeft);
    printf("Fill sensor value: ");
    printf("%i\n",ci_sensorFill);
    //printf("Vertical sensor value: ");
    //printf("%i\n",ci_sensorVertical);
    printf("Temperature value: ");
    printf("%i\n",ti_temp);
    printf("\n");
}

double timeFromStart(auto y){

    auto end = std::chrono::system_clock::now();
    double z = std::chrono::duration_cast<std::chrono::milliseconds>(end-y).count();


    return z;
}