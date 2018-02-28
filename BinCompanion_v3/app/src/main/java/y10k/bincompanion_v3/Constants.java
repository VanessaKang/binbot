//Created by Nicholas Chumney
package y10k.bincompanion_v3;

import java.util.UUID;

public final class Constants {
    static final String SERVICE_NAME = "BinBrothers";
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //default pi UUID

    //Handler Messages
    static final int MESSAGE_READ = 5;

    //Result Receiver Codes
    static final int STATE_CHANGE = 6;
    static final int OBTAINED_ADDRESS = 7;
    static final int ERROR_OCCURRED = 8;

    // Connection Status
    static final int STATE_NOT_CONNECTED = 10;
    static final int STATE_CONNECTING = 11; //to be used for client
    static final int STATE_LISTEN = 12;  //to be used for server
    static final int STATE_CONNECTED = 13;
    static final int STATE_DISCONNECTED = 14;
    static final int STATE_FAILED = 15;

    //State Identifiers
    static final int COLLECTION = 20;
    static final int DISPOSAL = 21;
    static final int TRAVEL = 22;
    static final int ERROR = 23;

    //Fill Level Identifiers
    static final int FILL_FULL = 30;
    static final int FILL_NEARFULL = 31;
    static final int FILL_PARTIAL = 32;
    static final int FILL_EMPTY = 33;

    //Battery Level Identifiers
    static final int BATTERY_HIGH = 40;
    static final int BATTERY_MEDIUM = 41;
    static final int BATTERY_LOW = 42;

    //Signal Strength Identifiers
    static final int SIGNAL_STRONG = 50;
    static final int SIGNAL_OKAY = 51;
    static final int SIGNAL_WEAK = 52;

    // Command Strings
    static final String CALL = "call";
    static final String RETURN = "return";
    static final String STOP = "stop";
    static final String RESUME = "resume";
    static final String SHUTDOWN = "shutdown";
}


