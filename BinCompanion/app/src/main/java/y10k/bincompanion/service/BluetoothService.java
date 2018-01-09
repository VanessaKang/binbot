package y10k.bincompanion.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Telephony;

import java.util.UUID;

/*
    TODO: This sevice will create a new thread managin the conecction of the application to the Bluetooth
    server on the BinBot

        - Receive intent from Service start
        - parse device name and addresss
        - build thread using this information
        - handler to allow MainActivity to communicate
 */

public class BluetoothService extends Service {
    //Variable Deceleration
    private BluetoothDevice device;
    private final IBinder mBinder = new LocalBinder();

    //Constant Deceleration
    private static final String TAG = BluetoothService.class.getSimpleName();
    private static final String RASP_MAC = "B8:27:EB:30:F6:FD"; //raps pi MAC Address
    private static final UUID MY_UUID = UUID.fromString("18f86520-e2af-408d-8fe6-d7dc8336c13a") ;

    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status


    public BluetoothService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO: Get device from sent intent
        device = (BluetoothDevice) intent.getExtras().get("device");
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //================================= METHODS ====================================================

    //================================= CLASSES + OBJECTS ==========================================
    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }
    private class ConnectedThreaad extends Thread {

    }
}
