#include <stdlib.h> 
#include <stdio.h>
#include <unistd.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h> 
#include <bluetooth/rfcomm.h> 
#include <string.h>
#include <pthread.h> 
#include <time.h> 

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

#define ERRORSTATE 0 
#define TRAVELSTATE 1
#define COLLECTIONSTATE 2
#define DISPOSALSTATE 3

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

//FOR TESTING//////////////////
int ei_state = 1; 
int ed_fillLevel = 1; 
///////////////////////////////

//FUNCTION DECLARATION 
void setupSocket(); 
void listen(); 
void spawn();
void *writeToApp(void *ptr); 
void *readFromApp(void *ptr); 

//MAIN 
int main() {
	setupSocket(); 

	while (true) {
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

 //TODO Handles periodic messaging to App and error messaging
void *writeToApp(void *ptr){
	//CONSTANTS DECLARATION 
	//TODO change macros to const 
	#define MODE 0 
	#define FILL 1
	#define BATT 2 
	#define SIG  3

	#define UPDATE_SIZE 4

	#define ERRORSTATE 0 
	#define TRAVELSTATE 1
	#define COLLECTIONSTATE 2
	#define DISPOSALSTATE 3

	#define FILL_FULL 0; 
	#define FILL_PARTIAL 1; 
	#define FILL_NEAR_EMPTY 2; 
	#define FILL_EMPTY 3;

	const char ID[4] = { '0','1','2','3' }; 

	//Initialize timer,t for first broadcast 
	t = clock() / CLOCKS_PER_SEC;

	while (connectionStatus == STATE_CONNECTED) {
		//Set timer, new_t to compare timer, twith 
		new_t = clock() / CLOCKS_PER_SEC; 

		//Broadcast a message every # seconds 
		if (new_t - t > 5) {

			//Create update code to pass to the App 
			char[] updateMsg = new char[UPDATE_SIZE]; 

			switch(ei_state){ 
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
			} 
			
			switch(ed_fillLevel){
				case FILL_FULL: 
					updateMsg[FILL] = ID[FILL_FULL];
					break; 
				case FILL_PARTIAL: 
					updateMsg[FILL] = ID[FILL_PARTIAL];
					break; 
				case FILL_NEAR_EMPTY: 
					updateMsg[FILL] = ID[FILL_NEAR_EMPTY];
					break; 
				case FILL_EMPTY: 
					updateMsg[FILL] = ID[FILL_EMPTY];
					break; 
			} 
			
			//TODO Write to BinCompanion every time period status of relevent variables 

			//TESTING
			printf("WRITE: %s\n", updateMsg[0]); 
			printf("WRITE: %s\n", updateMsg[1]);
			//////////

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

//TODO Handles reading commands from the app 
void *readFromApp(void *ptr){
	// read data from the client
	while (connectionStatus == STATE_CONNECTED) {
		int bytes_read = read(client, buf, sizeof(buf));
		if (bytes_read > 0) {
			printf("READ: received %s\n", buf);

			//TODO:Compare buf to strings to perfrom actions 
			if (strcmp(buf, "call") == 0) {}
			if (strcmp(buf, "return") == 0) {}
			if (strcmp(buf, "resume") == 0) {}
			if (strcmp(buf, "stop") == 0) {}
			if (strcmp(buf, "shutdown") == 0) {}

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
