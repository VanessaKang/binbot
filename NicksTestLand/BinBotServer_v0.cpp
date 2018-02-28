#include <stdlib.h> 
#include <stdio.h>
#include <unistd.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h> 
#include <bluetooth/rfcomm.h> 

#include <pthread.h> 
#include <time.h> 

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

pthread_t readThread, writeThread; 

clock_t t, new_t; 

//FUNCTION DECLARATION 
void setupSocket(); 
void listen(); 
void spawn(): 
void writeToApp(); 
void readFromApp(); 
void close(); 

//MAIN 
int main() {
	setupSocket(); 
	
	while (true) {
		listen(); 
		spawn(); 

		//TODO While loop used to manage threads for lost connections 
		while (connectionStatus == STATE_CONNECTED) {
			if (client < 0) {
				connectionStatus = STATE_NOCONNECTION; 
			}
		} 

		close();  
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
	printf("Listening...\n");
	listen(sock, 1);

	//Accept a connection 
	client = accept(sock, (struct sockaddr *) &rem_addr, &opt);
	connectionStatus = STATE_CONNECTED; 
}//listen 

//Spawn Threads to handle connection read and write 
void spawn() {
	//TODO Create Thread for reading
	int read_result = pthread(&readThread, NULL, readFromApp, NULL); 
	if (read_result != 0) {
		printf("Read Thread Creation Failed \n"); 
	}

	//TODO Create thread for writing 
	int write_result = pthread(&writeThread, NULL, writeToApp, NULL); 
	if (write_result != 0) {
		printf("Write Thread Creation Failed \n");
	}
}//spawn 

//TODO Handles periodic messaging to App and error messaging 
void writeToApp(){
	//Initialize timer,t for first braodcast 
	t = clock() / CLOCKS_PER_SEC;

	while (true) {
		//Set timer, new_t to compare timer, twith 
		new_t = clock() / CLOCKS_PER_SEC; 

		//Braodcast a message every # seconds 
		if (new_t - t > 5) {
			//TODO Write to BinCompanion every time period status of relevent variables 
			int bytes_wrote = write(client, "Hello", 5); 
			if (bytes_wrote > 0) {
				printf("wrote successfully\n"); 
			}
		
			//TODO Reset Timer, t 
			t = clock() / CLOCKS_PER_SEC;
		}//if 
	}//while 
}//wirteToApp 

//TODO Handles reading commands from the app 
void readFromApp(){
	// read data from the client
	while (true) {
		int bytes_read = read(client, buf, sizeof(buf));
		if (bytes_read > 0) {
			printf("received [%s]\n", buf);
		}
	}
}//readFromApp 

//Close the connection and cancel threads 
void close(){ 
	//TODO close Threads 
	pthread_exit(void *readThread);
	pthread_exit(void *writeThread); 

	//close connection 
	close(client);
	close(sock);
}


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