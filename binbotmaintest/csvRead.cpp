#include <iostream>
#include <iomanip>
#include <fstream>
#include <vector>
#include <unistd.h>

//using namespace std;

int main() {
    int count=0;
    std::ifstream inFile;
	printf("Working666\n");
    while(1){

    inFile.open("beaconvalues.txt");

    if (!inFile) {
        std::cout << "Unable to open file";
        exit(1); // terminate with error
    }

    std::vector<std::string> values;
    const int MAXSIZE = 100;
    char thisVal[MAXSIZE];
    
    while (inFile.getline(thisVal,MAXSIZE,',')) {
	values.push_back(thisVal);
    }
    
    inFile.close();


    printf("print each vector \n");
    int i = 0;
    for(i; i<values.size();i++){
        std::cout << values[i] << "\n";
    }
    
    printf("vector size: %i \n", values.size());
    usleep(2000000);
    }
    return 0;
}