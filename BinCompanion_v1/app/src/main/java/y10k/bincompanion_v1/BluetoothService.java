package y10k.bincompanion_v1;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class BluetoothService extends Service {
    //Variable Declaration
    private int mConnectionState = CONSTANTS.NOT_CONNECTED;
    private ConnectedThread mConnectedThread = null;
    private BluetoothSocket mBluetoothSocket = null;
    private Bundle savedData;

    //Handles Incoming bytes from Bluetooth Server
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if(msg.what == CONSTANTS.MESSAGE_READ){
                //switch ((int) msg.obj){
                //Decode Recieved Bytes
                //Perform Operations Based on Received Bytes
                Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                //}
            }
        }//handle message
    };

    //Used to send messages to MainActivity
    private ResultReceiver mReceiver = null;

    //Create binder to bind activites to the service
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        mReceiver = intent.getParcelableExtra("receiver");
        return mBinder;
    }//onBind

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }//onUnbind

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mConnectedThread != null){
            mConnectedThread.cancel();
        }
    }

    //==============================================================================================
    public void write(String command){
        if(mConnectionState == CONSTANTS.CONNECTED) {
            mConnectedThread.write(command.getBytes());
        }
    } //write

    public void connect(final BluetoothDevice device){
        Toast.makeText(getApplicationContext(), "Connecting..", Toast.LENGTH_SHORT).show();
        mConnectionState = CONSTANTS.CONNECTING;

        //Spawn Thread to run Server Connection
        if(mConnectedThread == null) {
            new Thread() {
                public void run() {
                    boolean fail = false;
                    try {
                        mBluetoothSocket = createBluetoothSocket(device);
                        Log.i(TAG, "Created");
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBluetoothSocket.connect();
                        Log.i(TAG, "Connected");
                    } catch (IOException e) {
                        try {
                            fail = true;
                            Log.e(TAG, "Fail to Create");
                            mBluetoothSocket.close();
                            stopSelf();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (!fail) {
                        savedData.putInt("state", CONSTANTS.CONNECTED);
                        mReceiver.send(2, savedData);
                        mConnectedThread = new ConnectedThread(mBluetoothSocket);
                        mConnectedThread.start();
                        Log.i(TAG, "Started");
                    }
                }
            }.start();
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device,CONSTANTS.MY_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(CONSTANTS.MY_UUID);
    } //BluetoothSocket

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        private ConnectedThread(BluetoothSocket socket){
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //Get BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Failed to Create Socket");
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
            mConnectionState = CONSTANTS.CONNECTED;
        }//constructor

        public void run() {
            byte[] buffer = new byte[1024];
            int numBytes;

            //Keep listening while connected
            while (mConnectionState == CONSTANTS.CONNECTED){
                try{
                    //Read from InputStream
                    numBytes = mInStream.read(buffer);

                    //Handle Read Bytes
                    mHandler.obtainMessage(CONSTANTS.MESSAGE_READ, numBytes,-1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.i(TAG, "Closed");
                    break;
                }//try/catch
            }//while
        }//run

        private void cancel() {
            try {
                mSocket.close();
                mConnectionState = CONSTANTS.NOT_CONNECTED;
                stopSelf();
            } catch (IOException e) {
                Log.e(TAG, "Failed to Close");
            }
        }

        private void write(byte[] buffer) {
            try {
                mOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Failed to Write");
            }//try/catch
        }//write
    }//connectedThread
}//BluetoothService
