//RFCOMM Bluetooth Server allowing comunication between BinBot and BinCompanion
#include <stdio.h>
#include <unistd.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>

#include <pthread.h>
#include <time.h> 

//Constant Declaration 
#define STATE_NOCONNECTION 0 
#define STATE_CONNECTED 1 

//Variable Declaration
int connectionStatus = STATE_NOCONNECTION; 
clock_t t;

//Function Declaration 
void sendStatusToBinCompanion(); 
void sendFeedbackToBinCompanion(); 
void handleRecvMsg();
int awaitConnection(int socket, sockaddr_rc rem_addr, socklen_t opt); 
void closeConnection(int client, int socket); 
void* readOnThread(void* ptr); 

//Main =======================================================================================
int main(int argc, char **argv) {
	//Variable Declaration 
	struct sockaddr_rc loc_addr = { 0 }, rem_addr = { 0 }; 
	socklen_t opt = sizeof(rem_addr); 
	char address[18] = "40:D3:AE:B0:E7:37";
	int sock, client, numbytes; 

	pthread_t readThread; 

	clock_t t, new_t; 
	t = clock()/CLOCKS_PER_SEC; 

	//Allocate Socket
	sock = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);

	// bind socket to port 1 of the first available local bluetooth adapter
	loc_addr.rc_family = AF_BLUETOOTH;
	//loc_addr.rc_bdaddr = *BDADDR_ANY;
	str2ba( address,&loc_addr.rc_bdaddr);
	loc_addr.rc_channel = (uint8_t)1;
	bind(sock, (struct sockaddr *)&loc_addr, sizeof(loc_addr));

	//Wait to connect to a device  
	client = awaitConnection(sock, rem_addr, opt);

	//Main Loop 
	while (true) {
		//Spawn thread to read data off of, use main thread to write to 
		int result = pthread_create(&readThread, NULL, readOnThread, (void*) &client); 
		if (result != 0) {
			printf("Thread Creation Failed\n"); 
		}


		new_t = clock() / CLOCKS_PER_SEC; 

		if (new_t - t > 2) {
			//Exceute System Clock
			//Write to BinCompanion every time period 
			sendStatusToBinCompanion();
		}
	}
	
	closeConnection(client, sock); 
	return 0;
}//main 

//Functions ==================================================================================
//This function sends out current status variables periodically 
void sendStatusToBinCompanion() {
	printf("Writing..");
}

//THis function sends back information in if behviour is being excuted back to user 
void sendFeedbackToBinCompanion() {

}

//This function decodes the msg recieved on the function 
void handleRecvMsg() {

}

//This function sets the socket to wait for the application to try and connect 
int awaitConnection(int socket, sockaddr_rc rem_addr, socklen_t opt) {
	char buf[1024] = { 0 };

	// put socket into listening mode
	listen(socket, 1);

	// accept one connection
	int client = accept(socket, (struct sockaddr*) &rem_addr, &opt);

	ba2str(&rem_addr.rc_bdaddr, buf);
	fprintf(stderr, "accepted connection from %s\n", buf);
	memset(buf, 0, sizeof(buf));
}

//This function terminates all running threads and closes the socket 
void closeConnection(int client, int socket) {
	//Terminate Threads 

	// close connection
	close(client);
	close(socket);
	return; 

}

//This code excutes reading from a socket on a seperate thread 
void* readOnThread(void* ptr) {
	char buf[1024] = { 0 };
	int client = *((int*) ptr); 

	// read data from the client
	int bytes_read = read(client, buf, sizeof(buf));
	if (bytes_read > 0) {
		printf("received [%s]\n", buf);
	}


}