//Created by nchum on 2018-04-02.
package y10k.bincompanion_v4;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService extends Service {
    //CONSTANT DECLARATION
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //default pi UUID
    static final String TAG = "BluetoothService";

    //Handler Messages
    static final int MESSAGE_READ = 5;

    //Result Receiver Codes
    static final int STATE_CHANGE = 6;
    static final int UPDATE = 9;

    // Connection Status
    static final int STATE_NOT_CONNECTED = 10;
    static final int STATE_CONNECTING = 11; //to be used for client
    static final int STATE_CONNECTED = 13;
    static final int STATE_DISCONNECTED = 14;
    static final int STATE_FAILED = 15;

    //Server Constants
    static final int UPDATE_SIZE = 4;

    //VARIABLE DECLARATION
    private int mConnectionStatus = STATE_NOT_CONNECTED;
    private ResultReceiver mReceiver = null;
    private Bundle savedData = new Bundle();
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    //Used to bind this service with MainActivity
    private final IBinder mBinder = new LocalBinder();

    protected class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }//LocalBinder

    //Used to handle messages received by BluetoothServer
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MESSAGE_READ:
                    //Converts Received Byte Array into Integer Array
                    byte[] received_Status = (byte[]) msg.obj;

                    //Store status to send to MainActivity
                    savedData.putByteArray("status", received_Status);

                    //Send status to update on UI
                    mReceiver.send(UPDATE, savedData);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }//handleMessage
    };//mHandler

    // ===============================================================
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        mReceiver = intent.getParcelableExtra("receiver");
        return mBinder;
    }

    //=================================================================
    public void connect(BluetoothDevice device) {
        //Cancel any attempted connections
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        //Cancel any current connections
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //Signal Connection Disconnection
        mConnectionStatus = STATE_DISCONNECTED;
        savedData.putInt("state", STATE_DISCONNECTED);
        mReceiver.send(STATE_CHANGE, savedData);

        //Create Thread to execute Connection
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }//connect

    public void disconnect(){
        //Cancel any attempted connections
        if(mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        //Cancel any current connections
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //Signal Connection Disconnection
        mConnectionStatus = STATE_DISCONNECTED;
        savedData.putInt("state", STATE_DISCONNECTED);
        mReceiver.send(STATE_CHANGE, savedData);
    }//disconnect

    public void manageConnection (BluetoothSocket socket) {
        mConnectedThread = new ConnectedThread((socket));
        mConnectedThread.start();
        mConnectionStatus = STATE_CONNECTED;

        savedData.putInt("state", STATE_CONNECTED);
        mReceiver.send(STATE_CHANGE, savedData);
    }//manageSocket

    public void write(String command){
        //Convert string command into bytes to be sent to Raspberry Pi
        if(mConnectionStatus == STATE_CONNECTED) {
            mConnectedThread.write(command.getBytes());
        }
    }//write

    public void connection_failed(){
        mConnectionStatus = STATE_FAILED;
        savedData.putInt("state", STATE_FAILED);
        mReceiver.send(STATE_CHANGE, savedData);
    }//connection_failed
    // ==========================================================
    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device){
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                //tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Could not create socket", e);
            }
            mmSocket = tmp;

            //Change Connection State to Connecting
            mConnectionStatus = STATE_CONNECTING;
            savedData.putInt("state", STATE_CONNECTING);
            mReceiver.send(STATE_CHANGE, savedData);
        }//ConnectThread

        public void run(){
            mBluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                    connection_failed();
                } catch (IOException f) {
                    Log.e(TAG, "Could not close client socket", e);
                }
                return;
            }
            manageConnection(mmSocket);
        }//run

        public void cancel(){
            try{
                mmSocket.close();

                //Signal Connection Disconnection
                mConnectionStatus = STATE_DISCONNECTED;
                savedData.putInt("state", STATE_DISCONNECTED);
                mReceiver.send(STATE_CHANGE, savedData);
            } catch (IOException e){
                Log.e(TAG, "Could not close", e);
            }//try/catch
        }//cancel
    }//ConnectThread
    //Managed Connected Socket =====================================================================
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread (BluetoothSocket socket) {
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e){
                Log.e(TAG, "Error creating input stream",e);
            }

            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e){
                Log.e(TAG, "Error creating output stream",e);
            }

            //Change Connection State to Connected
            mConnectionStatus = STATE_CONNECTED;
            savedData.putInt("state", STATE_CONNECTED);
            mReceiver.send(STATE_CHANGE, savedData);

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }//ConnectedThread

        public void run() {
            byte[] mBuffer = new byte[UPDATE_SIZE];
            int numBytes;

            while(mConnectionStatus == STATE_CONNECTED){
                try {
                    Log.i(TAG, "Reading...");
                    numBytes = mmInStream.read(mBuffer);
                    mHandler.obtainMessage(MESSAGE_READ, numBytes, -1, mBuffer)
                            .sendToTarget();
                }catch (IOException e){
                    Log.e(TAG, "Failed to Read",e);
                    connection_failed(); //Signal Connection Disconnection
                    break;
                }//try/catch
            }//while
            disconnect();
        }//run

        public void write(byte[] bytes) {
            try{
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Failed to write", e);
            }//try/catch
        }//write

        public void cancel(){
            try {
                mSocket.close();

                //Signal Connection Disconnection
                mConnectionStatus = STATE_DISCONNECTED;
                savedData.putInt("state", STATE_DISCONNECTED);
                mReceiver.send(STATE_CHANGE, savedData);
            } catch (IOException e) {
                Log.e(TAG, "Failed to close socket", e);
            }//try/catch
        }//cancel
    }//ConnectedThread
}//BluetoothService

