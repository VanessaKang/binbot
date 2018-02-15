//Created by Nicholas Chumney
package y10k.bincompanion_v3;

import java.util.UUID;

public final class Constants {
    static final String SERVICE_NAME = "BinBrothers";
    static final UUID MY_UUID = UUID.fromString("1111-1111-1111-1111");

    static final int STATE_NOT_CONNECTED = 0;
    static final int STATE_CONNECTING = 1; //to be used for client
    static final int STATE_LISTEN = 2;  //to be used for server
    static final int STATE_CONNECTED = 3;
    static final int STATE_DISCONNECTED = 4;

    static final int MESSAGE_READ = 5;

    static final int STATE_CHANGE = 6;
}


