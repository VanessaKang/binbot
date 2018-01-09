package y10k.bincompanion.sync;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import java.util.UUID;

import y10k.bincompanion.MainActivity;
import y10k.bincompanion.PairingActivity;

/*
  - Attempts to connect to bluetooth
  - If it can, will create a thread managing the connection
 */

public class BluetoothService extends Service {
    //Variable Decleration
    private BluetoothAdapter mBluetoothAdapter;
    private final IBinder mBinder = new LocalBinder(); //Binder given to  a client

    //Constants
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String RASP_MAC = "B8:27:EB:30:F6:FD"; //raps pi MAC Address
    private static final UUID MY_UUID = UUID.fromString("18f86520-e2af-408d-8fe6-d7dc8336c13a") ;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTENING = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3 ;

    public BluetoothService() {}

    @Override
    public void onCreate() {
        Log.d("BluetoothService", "Service Started");
        super.onCreate();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //////////////////////////////////////////////////////////////
    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }
}
