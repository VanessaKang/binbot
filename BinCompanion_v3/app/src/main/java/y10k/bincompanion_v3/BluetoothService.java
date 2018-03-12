//Created by Nicholas Chumney
package y10k.bincompanion_v3;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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
import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class BluetoothService extends Service {
    //CONSTANTS DECLARATION
    static final String SERVICE_NAME = "BinBrothers";
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //default pi UUID

    //Handler Messages
    static final int MESSAGE_READ = 5;

    //Result Receiver Codes
    static final int STATE_CHANGE = 6;
    static final int OBTAINED_ADDRESS = 7;
    static final int ERROR_OCCURRED = 8;
    static final int UPDATE = 9;

    // Connection Status
    static final int STATE_NOT_CONNECTED = 10;
    static final int STATE_CONNECTING = 11; //to be used for client
    static final int STATE_LISTEN = 12;  //to be used for server
    static final int STATE_CONNECTED = 13;
    static final int STATE_DISCONNECTED = 14;
    static final int STATE_FAILED = 15;

    //VARIABLE DECLARATION


    private int mConnectionStatus = STATE_NOT_CONNECTED;

    private ResultReceiver mReceiver = null;
    private Bundle savedData;
    private BluetoothAdapter mBluetoothAdapter = null;

    private ConnectThread mConnectThread;
    private AcceptThread mAcceptThread;
    private ConnectedThread mConnectedThread;

    private static final String TAG = "BluetoothService";

    private final IBinder mBinder = new LocalBinder();
    protected class LocalBinder extends Binder{
        BluetoothService getService() {return BluetoothService.this;}
    }//LocalBinder

    //To handle messages received by the client
    private Handler mHandler = new Handler(getMainLooper()){
        @Override
        public void handleMessage(Message msg) {             //TODO
            switch(msg.what) {
                case MESSAGE_READ:
                    String readMessage = null;

                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    savedData.putString("msg", readMessage);
                    mReceiver.send(UPDATE, savedData);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }//handleMessage
    };//Handler
    // =============================================================================================
    public BluetoothService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        savedData = new Bundle();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public IBinder onBind(Intent intent) {
        mReceiver = intent.getParcelableExtra("receiver"); //to communicate with MainActivity
        return mBinder;
    }//onBind

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }//onUnbind
    // =============================================================================================
    public void listen() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //SIgnal Connection Disconnection
        mConnectionStatus = STATE_DISCONNECTED;
        savedData.putInt("state", STATE_DISCONNECTED);
        mReceiver.send(STATE_CHANGE, savedData);

        //Create thread te manage listening for devices
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }//listen

    public void connect(BluetoothDevice device){
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

        //Create Thread to execute Connection
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }//connect

    public void disconnect(){
        //Cancel listening threads
        if(mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

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
    }

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
    }//connection failed
   // ==============================================================================================
    //Server Side Implementation
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        SERVICE_NAME,
                        MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket Listen Failed", e);
            }

            mmServerSocket = tmp;

            //Change Connection State to listening
            mConnectionStatus = STATE_LISTEN;
            savedData.putInt("state", STATE_LISTEN);
            mReceiver.send(STATE_CHANGE, savedData);
        }//AcceptThread

        public  void run() {
            BluetoothSocket socket = null;
            while(true) {
                try{
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Accept Failed", e);
                    connection_failed();
                    break;
                }

                if(socket != null) {
                    manageConnection(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e){
                        Log.e(TAG, "Socket Close Failed", e);
                    }
                    break;
                } //if
            }//while
        }//run

        public void cancel(){
            try {
                mmServerSocket.close();

                //Signal Connection Disconnection
                mConnectionStatus = STATE_DISCONNECTED;
                savedData.putInt("state", STATE_DISCONNECTED);
                mReceiver.send(STATE_CHANGE, savedData);
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }//cancel
    }//AcceptThread
    //Client Side Implementation ===================================================================
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
            byte[] mBuffer = new byte[1024];
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
