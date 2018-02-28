#include <stdlib.h> 
#include <stdio.h>
#include <unistd.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h> 
#include <bluetooth/rfcomm.h> 

#include <pthread.h> 
#include <time.h> 

int main(){ 
	//Variable Decleration 
	struct sockaddr_rc loc_addr = { 0 }, rem_addr = { 0 }; 
	char buf[1024] = { 0 }; 
	int sock, client, server, readbytes, writebytes; 
	socklen_t opt = sizeof(rem_addr); 
        
	char address[18] = "B8:27:EB:98:DA:8B"; //Address of the pi NOTE: Must change for each spereate pi used  
	
	//allocate socket
	sock = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM); 

	//bind socket to port of BluetoothAdapter 
	loc_addr.rc_family = AF_BLUETOOTH;
	str2ba(address, &loc_addr.rc_bdaddr);  
	loc_addr.rc_channel = (uint8_t) 1;
	
	bind(sock, (struct sockaddr *) &loc_addr, sizeof(loc_addr)); 

	//put socket into listening mode 
	printf("Listening...\n"); 
	listen(sock, 1); 

	//Accept a connection 
	client = accept(sock, (struct sockaddr *) &rem_addr, &opt); 

	ba2str(&rem_addr.rc_bdaddr, buf); 
	fprintf(stderr, "accepted connection from %s\n", buf); 
	memset(buf, 0 , sizeof(buf)); 

	//Manage the connection 
	while(true){
		//read data from client
		readbytes = read(client, buf, sizeof(buf)); 

		if(readbytes > 0) {
			printf("recieved %s\n", buf); 
			memset(buf, 0 , sizeof(buf));
		}//if

		 // send a message
		if (status == 0) {
			status = write(s, "hello!", 6);
		}
	}

	//close connection 
	close(client);
	close(sock);
} 
