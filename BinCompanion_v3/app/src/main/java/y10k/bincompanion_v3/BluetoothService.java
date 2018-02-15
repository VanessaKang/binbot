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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothService extends Service {
    //Variable Declaration =========================================================================
    private int mConnectionStatus = Constants.STATE_NOT_CONNECTED;

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

    //To hadle messages recieved by the client
    private static final Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            //TODO
            switch(msg.what) {
                case Constants.MESSAGE_READ:
                    break;
                default:
                    super.handleMessage(msg);
            }//swtich
        }
    };
    // =============================================================================================
    public BluetoothService() {
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
    public void listen(){}
    public void  connect(){}

    public void manageConnection (BluetoothSocket socket) {
        mConnectedThread = new ConnectedThread((socket));
        mConnectionStatus = Constants.STATE_CONNECTED;

        savedData.putInt("state", Constants.STATE_CONNECTED);
        mReceiver.send(Constants.STATE_CHANGE, savedData);
   }//manageSocket

    public void write(String command){
        if(mConnectionStatus == Constants.STATE_CONNECTED) {
            mConnectedThread.write(command.getBytes());
        }
    }
   // ==============================================================================================
    //Server Side Implementation
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(Constants.SERVICE_NAME,
                        Constants.MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket Listen Failed", e);
            }
            mmServerSocket = tmp;
        }//AcceptThread

        public  void run() {
            BluetoothSocket socket = null;
            while(true) {
                try{
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Accept Failed", e);
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
                tmp = device.createRfcommSocketToServiceRecord(Constants.MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Could not create socket", e);
            }
            mmSocket = tmp;
        }//ConnectThread

        public void run(){
            mBluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
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
        private byte[] mBuffer;

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

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }//ConnectedThread

        public void run() {
            mBuffer = new byte[1024];
            int numBytes;

            while(true){
                try {
                    numBytes = mmInStream.read(mBuffer);
                    Message readMsg = mHandler.obtainMessage(Constants.MESSAGE_READ, numBytes, -1, mBuffer);
                    readMsg.sendToTarget();
                }catch (IOException e){
                    Log.e(TAG, "Failed to Read",e);
                    break;
                }//try/catch
            }//while

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
            } catch (IOException e) {
                Log.e(TAG, "Failed to close socket", e);
            }//try/catch
        }//cancel
    }//ConnectedThread
}//BluetoothService
