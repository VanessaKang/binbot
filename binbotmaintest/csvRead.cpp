
#include <iostream>
#include <iomanip>
#include <fstream>
#include <vector>

//using namespace std;

int main() {
    int count=0;

    std::vector<int> ei_state1;
    std::vector<int> ei_userCommand1;
    std::vector<int> ei_error1;

    std::ifstream inFile;
    
    inFile.open("CSV.csv");

    if (!inFile) {
        std::cout << "Unable to open file";
        exit(1); // terminate with error
    }

    std::vector<std::string> values;
    const int MAXSIZE = 100;
    char thisVal[MAXSIZE];
    
    while (inFile.getline(thisVal,MAXSIZE,',')) {
        std::stoi(thisVal);
        std::cout << thisVal;
        printf("\n");
        ei_state1.push_back(std::stoi(thisVal));

    }
    
    inFile.close();

    //std::cout << ei_state[0];

    printf("print each vector \n");
    //int i= ei_state.front();
    int i = 0;
    for(i; i<ei_state1.size();i++){
        std::cout << ei_state1[i] << "\n";
    }
    
    printf("vector size: %i \n", ei_state1.size());
    return 0;
}