#include <iostream>
#include <iomanip>
#include <fstream>
#include <vector>
#include <unistd.h>

//using namespace std;

int main() {
    int count=0;

    std::vector<int> ei_state1;
    std::vector<char*> beaconVal;
    std::vector<int> ei_userCommand1;
    std::vector<int> ei_error1;

    std::ifstream inFile;
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
        //std::stoi(thisVal);
        //std::cout << thisVal;
        //printf("\n");
        //ei_state1.push_back(std::stoi(thisVal));
	//end of vector
	values.push_back(thisVal);
    }
    
    inFile.close();

    //std::cout << ei_state[0];

    printf("print each vector \n");
    //int i= ei_state.front();
    int i = 0;
    for(i; i<values.size();i++){
        std::cout << values[i] << "\n";
    }
    /*for(i; i<beaconVal.size();i++){
        std::cout << beaconVal[i] << "\n";
    }*/
    
    printf("vector size: %i \n", values.size());
    usleep(2000000);
    }
    return 0;
}