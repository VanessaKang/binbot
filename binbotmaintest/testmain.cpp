#include <stdlib.h> 
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
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>

// BINBOT SERVER
//CONSTANT DECLARTION 
#define STATE_NOCONNECTION 0 
#define STATE_CONNECTED 1 

//GLOBAL VARIABLE DECLARATION 
int connectionStatus = STATE_NOCONNECTION; 

struct sockaddr_rc loc_addr = { 0 }, rem_addr = { 0 };
char buf[1024] = { 0 };
int sock, client; 
int channel = 1; 
socklen_t opt = sizeof(rem_addr);

char address[18] = "B8:27:EB:30:19:A2"; //Address of the pi NOTE: Must change for each spereate pi used  

// B8:27:EB:30:19:A2 matts  
// B8:27:EB:98:DA:8B nicks
// B8:27:EB:08:F9:52 vanessas 

pthread_t readThread, writeThread; 
clock_t t, new_t; 
//

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
#define ATDESTRSSI -45


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
int ei_state= DISPOSALSTATE;
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
bool eb_motorStop = 0;
bool eb_binFullCheck = false;      //used for the averaging of fill sensor (collection())
bool eb_binEmptyCheck = false;      //used for the averaging of fill sensor (disposal())
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
iret1FSM = pthread_create( &thread1, NULL, FSM, NULL);
iret2bluetoothServer = pthread_create( &thread2, NULL, bluetoothServer, NULL);
//iret3errorDiag = pthread_create( &thread3, NULL, errorDiag, NULL);
iret4Data = pthread_create( &thread4, NULL, Data, NULL);


//wait for each thread to finish before completing program
pthread_join( thread1, NULL);
pthread_join( thread2, NULL);
//pthread_join( thread3, NULL);
pthread_join( thread4, NULL);

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

        /*if(timeFromStart(start) > runTime ){
            printf("errorDiag has exited \n");
            break;
        }*/

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
    setupSocket();
    while(1){
        /*if(timeFromStart(start) > runTime){
            printf("bluetoothServer has exited \n");
            break;
        } */

        listen();
        spawn(); 

        clock_t begin, end; //FOR TESTING
        begin = clock()/CLOCKS_PER_SEC;  //FOR TESTING 

        //While loop used to manage threads for lost connections 
        while (connectionStatus == STATE_CONNECTED) {
            ///////FOR TESTING//////////////////////////////////////
            end = clock()/CLOCKS_PER_SEC; 
            if(end - begin > 5){
                printf("MAIN: Looping\n"); 
                //printf("%i \n", ei_userCommand);
                begin = clock()/CLOCKS_PER_SEC; 
            } 
            /////////////////////////////////////////////////////////////////////
        }//while(connectionStatus) 

        //Handles status when connection is lost 
        printf("MAIN: Connection Lost\n"); 
        
        //Ensure Threads have closed  
        pthread_join(readThread, NULL); 
        pthread_join(writeThread, NULL);

        //close client connection 
        close(client);
    }//while 
}

void *errorDiag(void *ptr){
    printf("errorDiag Thread Running \n");
    while(TRUE){
        overheatDiag();
        batteryLowDiag();
        fallOverDiag();
        immobileDiag();
        noBinDiag();
        ultraSensDiag();
        motorDiag();
        connectionDiag();
        /*if(timeFromStart(start) > runTime){
            printf("Diag has exited \n");
            break;
        }*/
    }
    //std::cout << "Please enter the Value of es_commandString: ";
    //std::cin >> es_commandString;

    //Here we need to check all of our diagnostics and make changes to pertinant
    //diagnostic variables so the Comm/FSM functions can make the correct decisions to
    //deal with the error

}

void *Data(void *ptr){
    while(1){
        usleep(250000);
        if(eb_lineFollow == TRUE && ei_state == TRAVELSTATE && eb_motorStop == FALSE){
                //printf("linefollowing \n");
                writeData(19);
        }
        else if(eb_lineFollow == FALSE || eb_motorStop == TRUE){
            //printf("stopping \n");
            writeData(20);
        }
        if(ei_state == COLLECTIONSTATE || ei_state == DISPOSALSTATE){
            printf("reading sensor values \n");
            writeData(4);
            delay(I2CDELAY);
            ci_sensorFill = readData();
            if(ci_sensorFill==0 or ci_sensorFill>255){
                ci_sensorFill = 255;
            }
            //printf("SensorFill: %i \n",ci_sensorFill);
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
    //printf("%i \n",val);
    if (write(file_i2c, buffer, length) != length){
        /* ERROR HANDLING: i2c transaction failed */
        //printf("Failed to write to the i2c bus.\n");
    }
}


void errorState(){
    printf("Error State\n");
    if(ei_error != 0){
        printf("do the stuff");

    }
    else{
        ei_state=ei_prevState;
    }
}

void travel(){
    printf("in the travel state \n");
    eb_lineFollow = TRUE;
    int collectRSSI [10] = {-70,-70,-70,-70,-70,-70,-70,-70,-70,-70};
    
    int disposalRSSI [10] = {-70,-70,-70,-70,-70,-70,-70,-70,-70,-70};
    while (ei_userCommand == NO_COMMAND && ei_error == 0)
    {   
    std::string str1 (" 0");
    std::string ID("66666666-6666-6666-6666-666666666666");
    usleep(100000);
    std::ifstream beaconFile;
  std::vector<std::string> values;
  const int MAXSIZE = 100;
  char thisVal[MAXSIZE];
    if (eb_nextDest == COLLECTION){
     std::cout << "Update with CollectionRssi" << "\n";
            beaconFile.open("beaconvaluesCol.txt");
        if(!beaconFile){
            std::cout << "Unable to open beaconFile";
            exit(1);
        }
        
        while(beaconFile.getline(thisVal,MAXSIZE,',')){
            values.push_back(thisVal);
        }
        beaconFile.close();
        //std::cout << values.size() << "\n";
    }else if(eb_nextDest == DISPOSAL){
     std::cout << "Update with DisposalRssi" << "\n";
            beaconFile.open("beaconvaluesDis.txt");
        if(!beaconFile){
            std::cout << "Unable to open beaconFile";
            exit(1);
        }

        while(beaconFile.getline(thisVal,MAXSIZE,',')){
            values.push_back(thisVal);
        }
        beaconFile.close();
        //std::cout << values.size() << "\n";
    }
    if(values.size() == 0){     //protects against seg faults by trying to access
        break;                  //an empty array
    }

    
    for(int i = 0; i<values.size();i++){
        std::cout << values[i] << "\n";
    }
    
    if(values[1].compare(ID) == 0){
        int major = stoi(values[2]);
        if(major == 0 && eb_nextDest == COLLECTION){
            for(int i =9; i>=1;i--){
      collectRSSI[i] = collectRSSI[i-1];
      }
      collectRSSI[0] = stoi(values[3]);
            std::cout << "array" << collectRSSI[0] << " " << collectRSSI[1] << " " << collectRSSI[2] << " " << collectRSSI[3] << " " << collectRSSI[4] << " " << collectRSSI[5] << " " << collectRSSI[6] << " " << collectRSSI[7] << " " << collectRSSI[8] << " " << collectRSSI[9] << "\n";
      //printf("Going to Collection beacon \n");
            int sum = 0;
      for (int i = 0; i<10 ; i++){
        sum = sum+collectRSSI[i];
      }
      int av = sum/10;
      std::cout << "Average" << av << "\n";
            if(av > ATDESTRSSI){
                ei_prevState = TRAVELSTATE;
                ei_state = COLLECTIONSTATE;
                eb_lineFollow = FALSE;
                printf("Arrived at Collection Zone \n");
                break;
            }
        }
        else if(major == 1 && eb_nextDest == DISPOSAL){
            //int disposalRSSI = stoi(values[3]);
            //std::cout << disposalRSSI << "\n";
      printf("Going to Disposal beacon \n");
      for(int i =9; i>=1;i--){
      disposalRSSI[i] = disposalRSSI[i-1];
      }
      disposalRSSI[0] = stoi(values[3]);
        std::cout << "array" << disposalRSSI[0] << " " << disposalRSSI[1] << " " << disposalRSSI[2] << " " << disposalRSSI[3] << " " << disposalRSSI[4] << " " << disposalRSSI[5] << " " << disposalRSSI[6] << " " << disposalRSSI[7] << " " << disposalRSSI[8] << " " << disposalRSSI[9] << "\n";

        int sum = 0;
      for (int i = 0; i<10 ; i++){
        sum = sum+disposalRSSI[i];
      }
      int av = sum/10;
      std::cout << "Average" << av << "\n";
            if(av > ATDESTRSSI){
                ei_prevState = TRAVELSTATE;
                ei_state = DISPOSALSTATE;
                eb_lineFollow = 0;
                printf("Arrived at Disposal Zone \n");
                break;
            }
        }   
    }
    }
    if (ei_error != 0)
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
                system("sudo shutdown -h now");
                break;
            case STOP:
                //do stop stuff
                if(eb_motorStop == TRUE){
                    eb_motorStop = FALSE;
                }
                else if(eb_motorStop == FALSE){
                    eb_motorStop = TRUE;
                }
                ei_userCommand = NO_COMMAND;
                break;
            case MOVE_TO_COLLECTIONS:

                eb_nextDest = COLLECTION;
                ei_prevState = TRAVELSTATE;
                ei_state = TRAVELSTATE;
                eb_lineFollow = FALSE;
                ei_userCommand = NO_COMMAND;
                break;
            case MOVE_TO_DISPOSAL:

                eb_nextDest = DISPOSAL;
                ei_prevState = TRAVELSTATE;
                ei_state = TRAVELSTATE;
                eb_lineFollow = FALSE;
                ei_userCommand = NO_COMMAND;
                break;
    return; //break out of function after receiving user command
        }
    }
}


void collection(){
    printf("Collection state reached \n");
    int fillLevel[3] = {25,25,25};
    int avFill;
    int sum;
    int fillCheckStart;
    while ((eb_binFilled == FALSE) && (ei_userCommand == NO_COMMAND)){
        //usleep(500000);
        //printf("Collecting Mode \n");
        //Can replace if statement with function
        //that implements more accurate bin full function
        
        for(int i =2; i>=1;i--){
          fillLevel[i] = fillLevel[i-1];
        }
        fillLevel[0] = ci_sensorFill;
        sum = 0;
        for (int i = 0; i<3 ; i++){
          sum = sum+fillLevel[i];
        }
        avFill = sum/3;
        //printf("level val %i %\n", ci_sensorFill);
        if(avFill <= BINFULLDIST){
            //printf("Check if bin has been filled \n");
            if (eb_binFullCheck == FALSE){
              fillCheckStart = timeFromStart(start);
              eb_binFullCheck = TRUE;
            }
            //printf("timeFromStart(start): %i ", timeFromStart(start));
            //printf("fillCheckStart: %i \n", fillCheckStart);
            if((timeFromStart(start) - fillCheckStart) >= 2000 && (avFill <= BINFULLDIST) && (eb_binFullCheck = TRUE)){
              printf("Bin full and successfully waited 2sec ");
              eb_binFullCheck = FALSE;
              eb_binFilled = TRUE;
            }else if ((timeFromStart(start) - fillCheckStart) >= 2000 && (avFill >= BINFULLDIST) && (eb_binFullCheck = TRUE)){
              printf("Bin not full and successfully waited 2sec, reset collection");
              eb_binFullCheck = FALSE;
              eb_binFilled = FALSE;
            } 
        }  
        if (ei_error != 0)
        {
            ei_prevState = ei_state;
            ei_state = ERRORSTATE;
            eb_binFilled = FALSE; //reset flag after exiting loop
            break;
        }
    }
    eb_binFilled = FALSE; //reset flag after exiting loop
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
                if(eb_motorStop == TRUE){
                    eb_motorStop = FALSE;
                }
                else if(eb_motorStop == FALSE){
                    eb_motorStop = TRUE;
                }
                ei_userCommand = NO_COMMAND;
                break;
            case MOVE_TO_COLLECTIONS:
                eb_nextDest = COLLECTION;
                ei_prevState = TRAVELSTATE;
                ei_state = TRAVELSTATE;
                eb_lineFollow = FALSE;
                ei_userCommand = NO_COMMAND;
                break;
            case MOVE_TO_DISPOSAL:
                eb_nextDest = DISPOSAL;
                ei_prevState = TRAVELSTATE;
                ei_state = TRAVELSTATE;
                eb_lineFollow = FALSE;
                ei_userCommand = NO_COMMAND;
                break;
    return; //break out of function after receiving user command
        }
    }
    else
    {
        ei_prevState = ei_state;
        eb_nextDest = DISPOSAL; //next destination is disposal
        ei_state = TRAVELSTATE;
    return;
    }
}

void disposal(){
    printf("Disposal state reached \n");
    int fillLevel[3] = {3,3,3};
    int avFill;
    int sum;
    int fillCheckStart;

    if (connectionStatus == STATE_CONNECTED) {
        while (ei_userCommand == NO_COMMAND) {
            if(ei_error != 0){
                ei_prevState = ei_state;
                ei_state = ERRORSTATE; //set state to 0 for error state due to error
                eb_binEmptied = FALSE; //reset flag after exiting loop
                break;
            }
        }
    }
    else {
        while( (eb_binEmptied == FALSE) && (ei_userCommand == NO_COMMAND) ){
            //Stay still, wait for garbage to be disposed
            //send message to app, error state will bring back to disposal state
            for(int i =2; i>=1;i--){
              fillLevel[i] = fillLevel[i-1];
            }
            fillLevel[0] = ci_sensorFill;
            sum = 0;
            for (int i = 0; i<3 ; i++){
              sum = sum+fillLevel[i];
            }
            avFill = sum/3;
            
            if(avFill >= BINEMPTYDIST){
                if (eb_binEmptyCheck == FALSE){
                  fillCheckStart = timeFromStart(start);
                  eb_binEmptyCheck = TRUE;
                }
                //printf("timeFromStart(start): %i ", timeFromStart(start));
                //printf("fillCheckStart: %i \n", fillCheckStart);
                if((timeFromStart(start) - fillCheckStart) >= 2000 && (avFill >= BINFULLDIST) && (eb_binEmptyCheck = TRUE)){
                  printf("Bin full and successfully waited 2sec ");
                  eb_binEmptyCheck = FALSE;
                  eb_binEmptied = TRUE;
                }else if ((timeFromStart(start) - fillCheckStart) >= 2000 && (avFill <= BINFULLDIST) && (eb_binEmptyCheck = TRUE)){
                  printf("Bin not full and successfully waited 2sec, reset collection");
                  eb_binEmptyCheck = FALSE;
                  eb_binEmptied = FALSE;
                } 
            }
            if(ei_error != 0){
                ei_prevState = ei_state;
                ei_state = ERRORSTATE; //set state to 0 for error state due to error
                eb_binEmptied = FALSE; //reset flag after exiting loop
                break;
            }
        }
    }
    eb_binEmptied = FALSE; //reset flag after exiting loop

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
                if(eb_motorStop == TRUE){
                    eb_motorStop = FALSE;
                }
                else if(eb_motorStop == FALSE){
                    eb_motorStop = TRUE;
                }
                ei_userCommand = NO_COMMAND;
                break;
            case MOVE_TO_COLLECTIONS:
                eb_nextDest = COLLECTION;
                ei_prevState = TRAVELSTATE;
                ei_state = TRAVELSTATE;
                eb_lineFollow = FALSE;
                ei_userCommand = NO_COMMAND;
                break;
            case MOVE_TO_DISPOSAL:
                eb_nextDest = DISPOSAL;
                ei_prevState = TRAVELSTATE;
                ei_state = TRAVELSTATE;
                eb_lineFollow = FALSE;
                ei_userCommand = NO_COMMAND;
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
 if(ci_sensorFill >= 32 && ei_error==FALSE){
        ei_error = TRUE;
    }
    if(ci_sensorFill < 31 && ei_error==TRUE){
        ei_error ==FALSE;
    }
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

/*void dataCollection(){
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
}*/

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


//************************ BlueTooth Server Functions *****************//

//Setup the socket on start 
void setupSocket() {
    //allocate socket
    sock = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
	if (sock < 0) {
		perror("MAIN: Socket Error");
	}

    //bind socket to port of BluetoothAdapter 
    loc_addr.rc_family = AF_BLUETOOTH;
    str2ba(address, &loc_addr.rc_bdaddr);
	loc_addr.rc_channel = (uint8_t) channel;

    bind(sock, (struct sockaddr *) &loc_addr, sizeof(loc_addr));
}

//set socket to listen for connection requests 
int listen() {
	bool hasAccepted = false;

	//put socket into listening mode (blocking call) 
	printf("MAIN: Listening...\n");
	listen(sock, 1);

	//Accept a connection 
	while (!hasAccepted) {
		client = accept(sock, (struct sockaddr *) &rem_addr, &opt);
		if (client < 0) {
			perror("MAIN: failed to accept connection");
			channel++; 
			loc_addr.rc_channel = (uint8_t) channel;
		}
		else {
			hasAccepted = true;
		}
	}

	//Print connection success 
	ba2str(&rem_addr.rc_bdaddr, buf);
	printf("MAIN: accepted connection from %s\n", buf);

	//clears byte array 
	memset(buf, 0, sizeof(buf));

	//Alter connection status to display succcess 
	connectionStatus = STATE_CONNECTED;
}//listen 


//Spawn Threads to handle connection read and write 
void spawn() {
    //Create Thread for reading
    int read_result = pthread_create(&readThread, NULL, readFromApp, NULL); 

    if (read_result != 0) {
        printf("MAIN: Read Thread Creation Failed \n"); 
    }

    //Create thread for writing 
    int write_result = pthread_create(&writeThread, NULL, writeToApp, NULL); 

    if (write_result != 0) {
        printf("MAIN: Write Thread Creation Failed \n");
    }
}//spawn 

 //TODO Handles periodic messaging to App and error messaging
void *writeToApp(void *ptr){
    //CONSTANTS DECLARATION 
    #define MODE 0 
    #define FILL 1
    #define BATT 2 
    #define SIG  3

    #define UPDATE_SIZE 4

    #define FILL_FULL 0
    #define FILL_PARTIAL 1
    #define FILL_NEAR_EMPTY 2
    #define FILL_EMPTY 3

    const char ID[4] = { '0','1','2','3' };

    //Initialize timer,t for first broadcast 
    t = clock() / CLOCKS_PER_SEC;

    while (connectionStatus == STATE_CONNECTED) {
        //Set timer, new_t to compare timer, twith 
        new_t = clock() / CLOCKS_PER_SEC;

        //Broadcast a message every # seconds 
        if (new_t - t > 5) {

            //Create update code to pass to the App 
            char updateMsg[UPDATE_SIZE] = { '0','0','0','0' };

            switch (ei_state) {
            case ERRORSTATE:
                updateMsg[MODE] = ID[ERRORSTATE];
                break;
            case TRAVELSTATE:
                updateMsg[MODE] = ID[TRAVELSTATE];
                break;
            case COLLECTIONSTATE:
                updateMsg[MODE] = ID[COLLECTIONSTATE];
                break;
            case DISPOSALSTATE:
                updateMsg[MODE] = ID[DISPOSALSTATE];
                break;
            }//switch(ei_state) 

            if (eb_nextDest == COLLECTION) {
                updateMsg[DESTINATION] = ID[COLLECTION];
            } 
            else if (eb_nextDest == DISPOSAL) {
                updateMsg[DESTINATION] = ID[DISPOSAL];
            }//if eb_extDest

            if (avFill < 10) {
                updateMsg[FILL] = ID[FILL_FULL];
            }
            else if (avFill < 17 && avFill >= 10) {
                updateMsg[FILL] = ID[FILL_PARTIAL];
            }
            else if (avFill < 24 && avFill >= 17) {
                updateMsg[FILL] = ID[FILL_NEAR_EMPTY];
            }
            else {
                updateMsg[FILL] = ID[FILL_EMPTY];
            }//if ed_filllevel

             //Write to BinCompanion every time period status of relevent variables 
            int bytes_wrote = write(client, updateMsg, UPDATE_SIZE);
            if (bytes_wrote >= 0) {
                printf("WRITE: wrote successfully\n");
            }
            else {
                printf("WRITE: unable to write\n");

                //Unable to send likely to problem with socket 
                connectionStatus = STATE_NOCONNECTION;
            }
            //Reset Timer, t 
            t = clock() / CLOCKS_PER_SEC;
        }//if 
    }//while 
}//writeToApp 

//Handles reading commands from the app 
void *readFromApp(void *ptr){
    // read data from the client
    while (connectionStatus == STATE_CONNECTED) {
        int bytes_read = read(client, buf, sizeof(buf));
        if (bytes_read > 0) {
            printf("READ: received %s\n", buf);

            //TODO:Compare buf to strings to perfrom actions 
            if (strcmp(buf, "call") == 0) {ei_userCommand = MOVE_TO_DISPOSAL;}
            if (strcmp(buf, "return") == 0) { ei_userCommand = MOVE_TO_COLLECTIONS;}
            if (strcmp(buf, "resume") == 0) {ei_userCommand = STOP;}
            if (strcmp(buf, "stop") == 0) { ei_userCommand = STOP;}
            if (strcmp(buf, "shutdown") == 0) { ei_userCommand = SHUT_DOWN;}

            if(strcmp(buf, "disconnect") == 0){
                connectionStatus = STATE_NOCONNECTION;
            }
    
            //clears byte array 
            memset(buf, 0, sizeof(buf));  
        } else {
            printf("READ: failed to read\n"); 
            connectionStatus = STATE_NOCONNECTION; 
        } 
    }//while
}//readFromApp 
//PSUEDOCODE
/* 
1. On start, set up socket and enter listen state
2. On accept, manange two threads
    - readThread: listens to recieve commands from app and reacts to one of five 
    scenarios
    - writeThread: writes to app perodically to inform of current status of BinBot; also
    needs to write to app in case an error occurs (must inform type of error)
3. on cancel, close socket and set to listen for new connection again 
*/
