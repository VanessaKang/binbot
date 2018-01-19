package y10k.bincompanion.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
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

import y10k.bincompanion.Constants;
import y10k.bincompanion.MainActivity;

import static android.content.ContentValues.TAG;

/*
    TODO: This service will create a new thread managing the conecction of the application to the
    Bluetooth server on the BinBot
        - Receive intent from Service start (DONE)
        - parse device name and addresss (DONE)
        - build thread using this information
        - handler to allow MainActivity to communicate
 */
public class BluetoothService extends Service {
    //Variable Deceleration
    private int mState = Constants.STATE_NONE;
    private IBinder mBinder = new LocalBinder();
    private  ConnectedThread mConnectedThread;
    private BluetoothSocket mBluetoothSocket = null;

    //Setup Handler
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                default:
                    break;
            }
        }
    };

    //Constant Deceleration
    private static final String TAG = BluetoothService.class.getSimpleName();

    private static final String RASP_MAC = "B8:27:EB:30:F6:FD"; //raps pi MAC Address
    private static final UUID MY_UUID = UUID.fromString("18f86520-e2af-408d-8fe6-d7dc8336c13a") ;

    public BluetoothService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"onCreate ");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public void onDestroy() {
        mConnectedThread.cancel();
        super.onDestroy();
    }

    //================================= METHODS ====================================================
    public int getState() {
        return mState;
    }

    public void write(String command){
        if(mState == Constants.STATE_CONNECTED) {
            mConnectedThread.write(command.getBytes());
        }
    }

    public void connect(final BluetoothDevice device){
        Toast.makeText(getApplicationContext(), "Connecting..", Toast.LENGTH_SHORT).show();
        mState = Constants.STATE_CONNECTING;

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

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, MY_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    } //BluetoothSocket
    //================================= CLASSES ====================================================
    public class LocalBinder extends Binder{
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread(BluetoothSocket socket){
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //Get BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
            mState = Constants.STATE_CONNECTED;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int numBytes;

            //Keep listening while connected
            while (mState == Constants.STATE_CONNECTED){
                try{
                    //Read from InputStream
                    numBytes = mInStream.read(buffer);

                    //Sned bytes to activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, numBytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Lost Connection");
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mSocket.close();
                mState = Constants.STATE_NONE;
            } catch (IOException e) { }
        }

        public void write(byte[] buffer) {
            try {
                mOutStream.write(buffer);
            } catch (IOException e) { }
        }
    }
}//BluetoothService
