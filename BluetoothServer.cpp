//RFCOMM Bluetooth Server allowing comunication between BinBot and BinCompanion
#include <stdio.h>
#include <unistd.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>

#include <pthread.h>
#include <mutex.h>

#include <time.h> 

//Constant Declaration 
#define STATE_NOCONNECTION 0 
#define STATE_CONNECTED 1 

//Variable Declaration
int connectionStatus = STATE_NOCONNECTION; 

clock_t t; 

//Main =======================================================================================
int main(int argc, char **argv) {
	//Variable Declaration 
	struct sockaddr_rc loc_addr = { 0 }, rem_addr = { 0 }; 
	socklen_t opt = sizeof(rem_addr); 

	char buf[1024] = { 0 }; 
	int s, client, numbytes; 

	pthread_t readThread; 

	clock_t t, new_t; 
	t = clock()/CLOCKS_PER_SEC; 

	//Allocate Socket
	s = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);

	// bind socket to port 1 of the first available local bluetooth adapter
	loc_addr.rc_family = AF_BLUETOOTH;
	loc_addr.rc_bdaddr = *BDADDR_ANY;
	loc_addr.rc_channel = (uint8_t)1;
	bind(s, (struct sockaddr *)&loc_addr, sizeof(loc_addr));

	//Wait to connect to a device  
	awaitConnection();

	//Main Loop 
	while (true) {
		//Spawn thread to read data off of, use main thread to write to 
		int result = pthread_create(readThread, NULL, readOnThread(), NULL); 
		if (result != 0) {
			fprint(stderr, "Thread Creation Failed\n"); 
		}


		new_t = clock() / CLOCKS_PER_SEC; 

		if (new_t - t > 2) {
			//Exceute System Clock
			//Write to BinCompanion every time period 
			sendStatusToBinCompanion();
		}
	}
	
	closeConnection(); 
	return 0;
}//main 

//Functions ==================================================================================
//This function sends out current status variables periodically 
void sendStatusToBinCompanion() {
	cout << "Writing.."
}

//THis function sends back information in if behviour is being excuted back to user 
void sendFeedbackToBinCompanion() {

}

//This function decodes the msg recieved on the function 
void handleRecvMsg() {

}

//This function sets the socket to wait for the application to try and connect 
void awaitConnection(int client, int socket) {


	// put socket into listening mode
	listen(s, 1);

	// accept one connection
	client = accept(s, (struct sockaddr *)&rem_addr, &opt);

	ba2str(&rem_addr.rc_bdaddr, buf);
	fprintf(stderr, "accepted connection from %s\n", buf);
	memset(buf, 0, sizeof(buf));
}

//This function terminates all running threads and closes the socket 
void closeConnection(int client, int socket) {
	//Terminate Threads 

	// close connection
	close(client);
	close(s);

}

//This code excutes reading from a socket on a seperate thread 
void* readOnThread() {
	// read data from the client
	bytes_read = read(client, buf, sizeof(buf));
	if (bytes_read > 0) {
		printf("received [%s]\n", buf);
	}
}