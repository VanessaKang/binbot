#include <stdio.h>
#include <unistd.h>
#include <sys/socket.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h> 

using namespace std; 

int main() {
	struct sockaddr_rc addr = { 0 };
	int sock, status; 

	char dest[18] = "40:D3:AE:B0:E7:37"; 

	//Allocate Socket 
	sock = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM); 

	//Set connection parameters
	addr.rc_family = AF_BLUETOOTH; 
	addr.rc_channel = (uint8_t) 1; 
	str2ba(dest, &addr.rc_bdaddr);

	//connect to server
	printf("Looking for connection...\n"); 
	status = connect(sock, (struct sockaddr *) &addr, sizeof(addr)); 

	//Send a Message
	if(status == 0) { 
		printf("Connected\n"); 
		status = write(sock, "hello!", 6); 
	} 

	if(status < 0){
       		perror("ERROR");
	} 	

	close(sock); 

	return 0; 	
}//main
