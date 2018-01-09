package y10k.bincompanion.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.UUID;

/*
    TODO: This sevice will create a new thread managin the conecction of the application to the Bluetooth
    server on the BinBot

        - Receive intent from Service start (DONE)
        - parse device name and addresss (DONE)
        - build thread using this information
        - handler to allow MainActivity to communicate
 */
public class BluetoothService extends Service {
    //Variable Deceleration
    private BluetoothDevice device;
    private BluetoothSocket mBluetoothSocket = null;

    private Handler mHandler; //TODO: Setup handler to receive/send messages
    private final IBinder mBinder = new LocalBinder();

    public ConnectedThread mConnectedThread;

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
        //Get device from sent intent
        device = (BluetoothDevice) intent.getExtras().get("device");

        if(device != null){
            connectToDevice(device);
        }//if

        stopSelf();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO: Setup Binder if needed
        return null;
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
    private void connectToDevice(final BluetoothDevice device){
        Context context = getApplicationContext();
        Toast.makeText(context, device.getAddress() , Toast.LENGTH_SHORT).show();

        //Spawn Thread to run Server Connection
        if(mConnectedThread == null) {
            new Thread() {
                public void run() {
                    boolean fail = false;
                    try {
                        mBluetoothSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBluetoothSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBluetoothSocket.close();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (!fail) {
                        mConnectedThread = new ConnectedThread(mBluetoothSocket);
                        mConnectedThread.start();
                    }
                }
            }.start();
        }
    }

    //================================= CLASSES + OBJECTS ==========================================
    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }//LocalBInder

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, MY_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    } //BluetoothSocket

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //Default Constructor
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "Unable to get streams");
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }//constructor

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        buffer = new byte[1024];
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }

        //TODO: Reads from BT Server
        public void read() {}

        //Write to BT Server
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
    }
}
