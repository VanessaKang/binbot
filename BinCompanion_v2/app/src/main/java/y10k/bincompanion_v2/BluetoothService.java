package y10k.bincompanion_v2;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/*
    This service will create a new thread managing the conecction of the application to the
    Bluetooth server on the BinBot
        - Receive intent from Service start (DONE)
        - parse device name and addresss (DONE)
        - build thread using this information (DONE)
        - handler to allow MainActivity to communicate
 */

public class BluetoothService extends Service {
    //VARIABLE DECLARATION
    private int mConnectionState = CONSTANTS.NOT_CONNECTED;
    private ConnectedThread mConnectedThread = null;
    private BluetoothSocket mBluetoothSocket = null;

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

    //Handles Incoming Messages from MainActivity
    private Messenger mMessenger = new Messenger(new ServerHandler());
    class ServerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case CONSTANTS.CALL:
                    if(mConnectionState == CONSTANTS.CONNECTED){
                        write("CALL");
                    }
                    break;
                case CONSTANTS.RESUME:
                    if(mConnectionState == CONSTANTS.CONNECTED){
                        write("RESUME");
                    }
                    break;
                case CONSTANTS.STOP:
                    if(mConnectionState == CONSTANTS.CONNECTED){
                        write("STOP");
                    }
                    break;
                case CONSTANTS.RETURN:
                    if(mConnectionState == CONSTANTS.CONNECTED){
                        write("RETURN");
                    }
                    break;
                case CONSTANTS.SHUTDOWN:
                    if(mConnectionState == CONSTANTS.CONNECTED){
                        write("SHUTDOWN");
                    }
                    break;
                case CONSTANTS.CONNECT:
                    if(mConnectionState == CONSTANTS.NOT_CONNECTED) {
                        BluetoothDevice device = (BluetoothDevice) msg.obj;
                        connect(device);
                    }
                    break;
                case CONSTANTS.GETSTATE:
                    //TODO
                    break;
                default:
                    super.handleMessage(msg);
            }//switch
        }//handleMessage
    }//ServerHandler
//==================================================================================================
    public BluetoothService() {} //Constructor

    @Override
    public IBinder onBind(Intent intent) {return mMessenger.getBinder();}//onBind

    @Override
    public boolean onUnbind(Intent intent) {return false;} //onUnbind

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if(mConnectedThread != null){
            mConnectedThread.cancel();
        }
        super.onDestroy();
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
                            Log.e(TAG, "Fail to Create");
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
                    break;
                }//try/catch
            }//while
        }//run

        private void cancel() {
            try {
                mSocket.close();
                mConnectionState = CONSTANTS.NOT_CONNECTED;
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
