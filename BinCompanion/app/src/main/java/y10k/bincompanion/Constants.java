package y10k.bincompanion;

/**
 * Created by Nicholas on 2018-01-11.
 */

public interface Constants {
        int MESSAGE_READ = 1;
        int MESSAGE_WRITE = 2;

        int STATE_NONE = 3;
        int STATE_CONNECTING = 4;
        int STATE_CONNECTED = 5;

        String CALL_COMMAND = "CALL";
        String RETURN_COMMAND = "RETURN";
        String RESUME_COMMAND = "RESUME";
        String STOP_COMMAND = "STOP";
        String SHUTDOWN_COMMAND = "SHUTDOWN";
}
