testmain: testmain.o .FORCE
	g++ -Wall -o testmain testmain.o -pthread -lwiringPi -lbluetooth
testmain.o: testmain.cpp
	g++ -Wall -c testmain.cpp -pthread -lwiringPi -lbluetooth
.FORCE:
