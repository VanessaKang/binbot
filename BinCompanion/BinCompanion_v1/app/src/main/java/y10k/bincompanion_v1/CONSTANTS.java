package y10k.bincompanion_v1;

import java.util.UUID;

/**
 * Created by Nicholas on 2018-01-21.
 */

final class CONSTANTS {
    static final String TAG = BluetoothService.class.getSimpleName();
    static final String RASP_MAC = "B8:27:EB:30:F6:FD"; //raps pi MAC Address
    static final UUID MY_UUID = UUID.fromString("18f86520-e2af-408d-8fe6-d7dc8336c13a") ;

    //Handler Identifiers
    static final int MESSAGE_READ = 1;
    static final int MESSAGE_WRITE = 2;

    //Device Connection Status
    static final int CONNECTED = 10;
    static final int CONNECTING = 12;
    static final int NOT_CONNECTED = 11;

    static final int UPDATE = 13;

    static final String CONNECTED_STRING = "CONNECTED";
    static final String NOT_CONNECTED_STRING = "NOT CONNECTED";

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

    //Command Strings
    static final int CALL = 100;
    static final int RETURN = 101;
    static final int RESUME = 102;
    static final int STOP = 103;
    static final int SHUTDOWN = 104;
    static final int CONNECT = 105;

    static final int GETSTATE = 106;
    static final int PROVIDESTATE = 106;
}
