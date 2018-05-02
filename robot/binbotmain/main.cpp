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
#include <mutex>
//hello
std::mutex mtx;
//declare primary functions
void *FSM(void* ptr);
void *bluetoothServer(void* ptr);
void *errorDiag(void* ptr);
void *Data(void* ptr);
void errorState();
void travel();
void collection();
void disposal();
void pathFinding();
void obstacleAvoidance();
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
void dataCollection();
double timeFromStart(auto y);

//DIGITAL IO Pi GPIO pins
#define PIN_TEMP 5
#define PIN_RMFWD 28
#define PIN_RMRVS 29
#define PIN_LMFWD 25
#define PIN_LMRVS 7
#define PIN_SERVO 17

// Define Constants used in code **** Needs to be edited
//BIN SENSOR CONSTANTS
#define BINFULLDIST 5
#define BINEMPTYDIST 30
#define FTHRESH 60
#define LRTHRESH 40
#define ALLOWEDMOVES 10
#define ATDESTRSSI -40
#define MAXTEMP 40


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
int fillSensorCount;
bool travelLock;
int currentMove=0;
//1=forward
//2=right
//3=left

#define I2CDELAY 75


//declare global variables------------------------------------------------------------
int ei_state=1;
double ed_fillLevel;
double vd_battVoltage;
int ei_error=0;
//These variables cannot be defined as string, and i am not quite sure how we are using it
//string es_statusString;
//string es_commandString;
bool eb_destReached;
bool eb_nextDest;
int ei_prevState;
double md_botLocation;
int ti_temp=25;
double cmd_objDist;
double ed_appRssi;
double ed_beaconRssi = -70;
int ci_sensorLeft;
int ci_sensorFront;
int ci_sensorRight;
int ci_sensorFill;
int ci_sensorVertical;

// New proposed variables from Component document
int ei_userCommand;
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
double runTime = 4500000.0; //run time in milliseconds
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
// iret1FSM = pthread_create( &thread1, NULL, FSM, NULL);
// iret2bluetoothServer = pthread_create( &thread2, NULL, bluetoothServer, NULL);
// iret3errorDiag = pthread_create( &thread3, NULL, errorDiag, NULL);
iret4Data = pthread_create( &thread4, NULL, Data, NULL);


//wait for each thread to finish before completing program
// pthread_join( thread1, NULL);
// pthread_join( thread2, NULL);
// pthread_join( thread3, NULL);
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
                    printf("Travel State = %i \n", ei_state);
                }
               // travel();
                obstacleAvoidance();
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
        //overheatDiag();
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
    printf("data Thread running \n");
    //while(1){
   	// obstacleAvoidance();
		writeData(18);
        //delay(100);
    //}
}

//*********************** Supporting Functions ********************************//

//Need to add all the functions pertaining to moving the robot
//Perhaps have functions that support object avoidance and regular navigation for the travel state

//Also should have a function for initializing global variables reading from a log file/text file to assist with reboots after an error
//and so that the robot has a memory. Probably would be useful.

void setupPins(){
  wiringPiSetup();
  //ASSIGN DIGITAL IO
  pinMode(PIN_RMFWD,OUTPUT);
  pinMode(PIN_RMRVS,OUTPUT);
  pinMode(PIN_LMFWD,OUTPUT);
  pinMode(PIN_LMRVS,OUTPUT);
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
    //mtx.lock();
    length = 1;
    if (read(file_i2c, buffer, length) != length){
        //ERROR HANDLING: i2c transaction failed
        //printf("Failed to read from the i2c bus.\n");
    }
    unsigned char val = buffer[0];
    //mtx.unlock();
    return val;
}

void writeData(int val){
    //mtx.lock();
    buffer[0] = val;
    length = 1;
    if (write(file_i2c, buffer, length) != length){
        /* ERROR HANDLING: i2c transaction failed */
        //printf("Failed to write to the i2c bus.\n");
    }
    //mtx.unlock();
}

void rightMotorForward(){
    delay(I2CDELAY);
    writeData(11);
    //digitalWrite(PIN_RMFWD,HIGH);
    //digitalWrite(PIN_RMRVS,LOW);
}

void rightMotorReverse(){
    delay(I2CDELAY);
    writeData(12);
    delay(I2CDELAY);
    //digitalWrite(PIN_RMRVS,HIGH);
    //digitalWrite(PIN_RMFWD,LOW);
}

void leftMotorForward(){
    delay(I2CDELAY);
    writeData(14);
    delay(I2CDELAY);
    //digitalWrite(PIN_LMFWD,HIGH);
    //digitalWrite(PIN_LMRVS,LOW);
}

void leftMotorReverse(){
    delay(I2CDELAY);
    writeData(15);
    delay(I2CDELAY);
    //digitalWrite(PIN_LMRVS,HIGH);
    //digitalWrite(PIN_LMFWD,LOW);
}

void rightMotorStop(){
    delay(I2CDELAY);
    writeData(13);
    delay(I2CDELAY);
    //digitalWrite(PIN_RMRVS,HIGH);
    //digitalWrite(PIN_RMFWD,LOW);
}

void leftMotorStop(){
    delay(I2CDELAY);
    writeData(16);
    delay(I2CDELAY);
    //digitalWrite(PIN_LMFWD,LOW);
    //digitalWrite(PIN_LMRVS,LOW);
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
    printf("Error State\n");
}
//test

void travel(){
    while (ei_userCommand == NO_COMMAND && ei_error == 0)
    {

        if ((ci_sensorFront <= FTHRESH ) || (ci_sensorLeft <= LRTHRESH) || (ci_sensorRight <= LRTHRESH))
        {
            if(obstacleCount==0){
                printf("Obstacle Avoidance\n");
                printHardwareValues();
            }
            obstacleCount = 1;
            obstacleAvoidance();
        }
        else if (ed_beaconRssi >= ATDESTRSSI)
        {
            if (eb_nextDest == 0)
            {
                ei_state == COLLECTIONSTATE;
                break;
            }
            if (eb_nextDest == 1)
            {
                ei_state = DISPOSALSTATE;
                break;
            }
        }
        else
        {
            if(obstacleCount==1){
                printf("Path Finding\n");
            }
            obstacleCount = 0;
            pathFinding();
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
    }
}


void collection(){
    while ((ci_sensorFill >= BINEMPTYDIST) && (ei_userCommand == NO_COMMAND)){
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
        ei_prevState = ei_state;
        eb_nextDest = 1; //next destination is disposal
        ei_state = TRAVELSTATE;
    return;
    }
}

void disposal(){
    while( (ci_sensorFill < BINFULLDIST) && (ei_userCommand == NO_COMMAND) ){
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

void obstacleAvoidance(){
    // if(numMoves>=ALLOWEDMOVES){
    //     if((ci_sensorFront>=FTHRESH) && (ci_sensorRight>=LRTHRESH) && (ci_sensorLeft>=LRTHRESH)){
    //         numMoves = 0;
    //     }
    //     else{
    //         numMoves++;
    //         if(currentMove!=2){
    //             adjustAngleNegative();    
    //         }
    //         printf("Turning Right\n");
    //         currentMove = 2;
    //     }
    // }
    // if(numMoves < ALLOWEDMOVES){
    //     if((ci_sensorFront>=FTHRESH) && (ci_sensorRight>=LRTHRESH) && (ci_sensorLeft>=LRTHRESH)){
    //         numMoves = 0;
    //         if(currentMove!=1){
    //             moveForward();   
    //         }
    //         printf("Moving Forward\n");
    //         currentMove = 1;
    //     }
    //     else if(ci_sensorFront<=FTHRESH){
    //         numMoves ++;
    //         if(ci_sensorRight < ci_sensorLeft){
    //             if(currentMove!=3){
    //                 adjustAnglePositive();                    
    //             }
    //             printf("Turning Left\n");
    //             currentMove = 3;
    //         }
    //         else if (ci_sensorLeft < ci_sensorRight) {
    //             if(currentMove!=2){
    //                 adjustAngleNegative();                    


    //             }

    //             printf("Turning Right\n");
    //             currentMove = 2;
    //         }
    //     }
    //     else if(ci_sensorFront>=FTHRESH){
    //         numMoves ++;
    //         if(ci_sensorRight<=LRTHRESH){
    //             if(currentMove!=3){
    //                 adjustAnglePositive();                    
    //             }
    //             printf("Turning Left\n");
    //             currentMove = 3;
    //         }
    //         else if (ci_sensorLeft<=LRTHRESH) {
    //             if(currentMove!=2){
    //                 adjustAngleNegative();    
    //             }
    //             printf("Turning Right\n");
    //             currentMove = 2;
    //         }
    //     }
    // }
    // printf("Nummoves: \t");
    // printf("%i\n",numMoves);
    //writeData(18);
    //delay(I2CDELAY);
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
    // 750mV at 25 celsius and 10mV/(degree celsius)
    if (ti_temp >= MAXTEMP)
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

    // while(1){

    //     delay(I2CDELAY);
    //     writeData(1);
    //     delay(I2CDELAY);
    //     ci_sensorFront = readData();

    //     if(ci_sensorFront==0 or ci_sensorFront>255){
    //        ci_sensorFront = 255;
    //     }
    //     delay(I2CDELAY);

    //     writeData(2);
    //     delay(I2CDELAY);
    //     ci_sensorRight = readData();
    //     if(ci_sensorRight==0 or ci_sensorRight>255){
    //        ci_sensorRight = 255;
    //     }
    //     delay(I2CDELAY); 

    //     writeData(3);
    //     delay(I2CDELAY);
    //     ci_sensorLeft = readData();
    //     if(ci_sensorLeft==0 or ci_sensorLeft>255){
    //        ci_sensorLeft = 255;
    //     }
    //     delay(I2CDELAY);         

    //     writeData(4);
    //     delay(I2CDELAY);
    //     ci_sensorFill = readData();
    //     if(ci_sensorFill==0 or ci_sensorFill>255){
    //        ci_sensorFill = 255;
    //     }
    //     delay(I2CDELAY); 

    //     // writeData(5);
    //     // delay(I2CDELAY);
    //     // ci_sensorVertical = readData();
    //     // if(ci_sensorVertical==0 or ci_sensorVertical>255){
    //     // ci_sensorVertical = 255;
    //     // }
    //     // delay(I2CDELAY); 

    //     // writeData(6);
    //     // delay(I2CDELAY);
    //     // ti_temp = readData();
    //     // delay(I2CDELAY);

    //     printHardwareValues();
    //     if(ci_sensorFill<=28){
    //         fillSensorCount += 1;
    //     }
    //     else if(travelLock!=1){
    //         fillSensorCount = 0;
    //     }
        
    //     if(fillSensorCount>=5){
    //         travelLock = 1;
    //        obstacleAvoidance();
    //     }
    //     if(timeFromStart(start) > runTime){
    //         printf("Data has exited \n");
    //         break;
    //     }
    //   }

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
    printf("Right Motor Forward Value: ");
    printf("%i\n",digitalRead(PIN_RMFWD));
    printf("Right Motor Reverse Value: ");
    printf("%i\n",digitalRead(PIN_RMRVS));
    printf("Left Motor Forward Value: ");
    printf("%i\n",digitalRead(PIN_LMFWD));
    printf("Left Motor Reverse Value: ");
    printf("%i\n",digitalRead(PIN_LMRVS));
    printf("\n");
}

double timeFromStart(auto y){

    auto end = std::chrono::system_clock::now();
    double z = std::chrono::duration_cast<std::chrono::milliseconds>(end-y).count();


    return z;
}
