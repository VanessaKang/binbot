testmain: testmain.o .FORCE
	g++ -Wall -o testmain testmain.o -pthread -lwiringPi
testmain.o: testmain.cpp
	g++ -Wall -c testmain.cpp -pthread -lwiringPi
.FORCE: