#include <stdlib.h> 
#include <stdio.h>
#include <unistd.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h> 
#include <bluetooth/rfcomm.h> 
#include <string.h>
#include <pthread.h> 
#include <time.h> 

////////////// NATIVE TO MAIN //////////////
//BIN SENSOR CONSTANTS
#define BINFULLDIST 5
#define BINEMPTYDIST 25

//nextDest Constants
#define COLLECTION 0
#define DISPOSAL 1

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

int ei_state;
int avFill;
int ei_userCommand;

bool eb_nextDest;
////////////////////////////////////////////
//CONSTANT DECLARTION 
#define STATE_NOCONNECTION 0 
#define STATE_CONNECTED 1 

//GLOBAL VARIABLE DECLARATION 
int connectionStatus = STATE_NOCONNECTION;

struct sockaddr_rc loc_addr = { 0 }, rem_addr = { 0 };
char buf[1024] = { 0 };
int sock, client; 
socklen_t opt = sizeof(rem_addr);

char address[18] = "B8:27:EB:98:DA:8B"; //Address of the pi NOTE: Must change for each spereate pi used
										// B8:27:EB:30:19:A2 matts | B8:27:EB:98:DA:8B nick | B8:27:EB:08:F9:52 vanessas 
pthread_t readThread, writeThread; 
clock_t t, new_t; 

//FUNCTION DECLARATION 
void setupSocket(); 
void listen(); 
void spawn();
void *writeToApp(void *ptr); 
void *readFromApp(void *ptr); 

//MAIN 
int main() {
	setupSocket(); 

	while (1) {
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
}//main 

//Setup the socket on start 
void setupSocket() {
	//Ensure Serial port is registered 
	system("sudo sdptool add SP");

	//allocate socket
	sock = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);

	//bind socket to port of BluetoothAdapter 
	loc_addr.rc_family = AF_BLUETOOTH;
	str2ba(address, &loc_addr.rc_bdaddr);
	loc_addr.rc_channel = (uint8_t)1;

	bind(sock, (struct sockaddr *) &loc_addr, sizeof(loc_addr));
}

//set socket to listen for connection requests 
void listen() {
	//put socket into listening mode (blocking call) 
	printf("MAIN: Listening...\n");
	listen(sock, 1);

	//Accept a connection 
	client = accept(sock, (struct sockaddr *) &rem_addr, &opt);

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

 //Handles periodic messaging to App and error messaging
void *writeToApp(void *ptr){
	//CONSTANTS DECLARATION 
	#define STATE 0 
    #define DESTINATION 1
	#define ERRORCODE 2
	#define FILL 3

	#define UPDATE_SIZE 4

	#define FILL_FULL 0
	#define FILL_PARTIAL 1
	#define FILL_NEAR_EMPTY 2
	#define FILL_EMPTY 3

	//Initialize timer,t for first broadcast 
	t = clock() / CLOCKS_PER_SEC;

	while (connectionStatus == STATE_CONNECTED) {
		//Set timer, new_t to compare timer, twith 
		new_t = clock() / CLOCKS_PER_SEC; 

		//Broadcast a message every # seconds 
		if (new_t - t > 5) {

			//Create update code to pass to the App 
			char updateMsg[UPDATE_SIZE] = { 0 };

			char ID[4] = { '0', '1', '2', '3' };

			//TODO
			/* 
			Use ed_nextDest to provide feedback for destination of travel
			Inform user if we are stopped/resume
			*/
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

			if (eb_nextDest = COLLECTION) {
				updateMsg[DESTINATION] = ID[COLLECTION]; 
			else if (eb_nextDest = DISPOSAL){ }
				updateMsg[DESTINATION] = ID[DISPOSAL]; 
			}

			if (avFill < 10) {
				updateMsg[FILL] = ID[FILL_FULL];
			} else if (avFill < 17 && avFill >= 10) {
				updateMsg[FILL] = ID[FILL_PARTIAL];
			} else if (avFill < 24 && avFill >= 17) {
				updateMsg[FILL] = ID[FILL_NEAR_EMPTY];
			} else { 
				updateMsg[FILL] = ID[FILL_EMPTY];
			}//if ed_filllevel
			
			//Write to BinCompanion every time period status of relevent variables 
			int bytes_wrote = write(client, updateMsg, UPDATE_SIZE); 
			if (bytes_wrote >= 0) {
				printf("WRITE: wrote successfully\n"); 
			} else { 
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

			//       Compare buf to strings to perfrom actions 
			if (strcmp(buf, "call") == 0) { ei_userCommand = MOVE_TO_DISPOSAL; }
			if (strcmp(buf, "return") == 0) { ei_userCommand = MOVE_TO_COLLECTIONS; }
			if (strcmp(buf, "resume") == 0) { ei_userCommand = STOP; }
			if (strcmp(buf, "stop") == 0) { ei_userCommand = STOP; }
			if (strcmp(buf, "shutdown") == 0) { ei_userCommand = SHUT_DOWN; }

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
