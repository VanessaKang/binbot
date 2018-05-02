package y10k.bincompanion_mini;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    //Variable Declaration
    TextView mConnection;
    TextView mState;
    TextView mFill;
    TextView mRSSI;

    int mconnectionStatus = NOT_CONNECTED;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothSocket mSocket = null;
    BluetoothGatt bluetoothGatt = null;

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case CONNECTION_MADE:


                    mconnectionStatus = CONNECTED;
                    mConnection.setText(R.string.connected);
                    break;
                case CONNECTION_FAILED:
                    mconnectionStatus = NOT_CONNECTED;
                    mConnection.setText(R.string.failed);
                    break;
                case MESSAGE_READ:
                    mState.setText("Hello World");
                    break;
                case DISCONNECTED:
                    mconnectionStatus = NOT_CONNECTED;
                    mConnection.setText(R.string.disconnected);
            }
        }
    };

    ConnectedThread mConnectedThread = null;

    //Constant Declaration
    final String TAG = "BINCOMPANION";
    final String RASP = "B8:27:EB:08:F9:52";
    final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    final static int CONNECTION_MADE = 3;
    final static int CONNECTION_FAILED = 4;
    final static int MESSAGE_READ = 2;
    final static int REQUEST_ENABLE_BT = 1;

    final static int DISCONNECTED = 7;
    final static int NOT_CONNECTED = 4;
    final static int CONNECTED = 5;
    final static int CONNECTING = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConnection = findViewById(R.id.connection_content);
        mState = findViewById(R.id.state_content);
        mFill = findViewById(R.id.fill_content);
        mRSSI = findViewById(R.id.rssi_content);

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        if(mBluetoothAdapter == null){
            //Device does not support Bluetooth
            mConnection.setText(R.string.no_bluetooth);
        } else {
            //Check if blueooth is on
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                mConnection.setText(R.string.no_connection);
                mState.setText(R.string.no_connection);
                mFill.setText(R.string.no_connection);
                mRSSI.setText(R.string.no_connection);
            }
        }
    }//onCreate

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
        if(mConnectedThread != null){
            mConnectedThread.cancel();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();  //Use AppCompactActivity's Method to get handle on menu inflator
        inflater.inflate(R.menu.pair, menu); //Use the inflater's inflate method to inflate our menu layout to this menu
        return true; // Retrun true to show mun in toolbar
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_pair) {
            mConnection.setText(R.string.connecting);

            try {
                wait(2000);
            } catch (Exception e){}

            connect();
        }
        return super.onOptionsItemSelected(item);
    }

    public void connect(){
        mconnectionStatus = CONNECTING;

        // Spawn a new thread to avoid blocking the GUI one
        new Thread()
        {
            public void run() {
                boolean fail = false;
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(RASP);

                try {
                    mSocket = createBluetoothSocket(device);
                } catch (IOException e) {
                    fail = true;
                    mHandler.obtainMessage(CONNECTION_FAILED).sendToTarget();
                }
                // Establish the Bluetooth socket connection.
                try {
                    mSocket.connect();
                } catch (IOException e) {
                    try {
                        fail = true;
                        mSocket.close();
                        mHandler.obtainMessage(CONNECTION_FAILED).sendToTarget();
                    } catch (IOException e2) {
                        //insert code to deal with this
                        mHandler.obtainMessage(CONNECTION_FAILED).sendToTarget();;
                    }
                }
                if(!fail) {
                    mConnectedThread = new ConnectedThread(mSocket);
                    mConnectedThread.start();
                    mHandler.obtainMessage(CONNECTION_MADE).sendToTarget();
                }
            }
        }.start();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, myUUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(myUUID);
    }

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
        }//constructor

        public void run() {
            byte[] buffer = new byte[1024];
            int numBytes;

            //Keep listening while connected
            while (mconnectionStatus == CONNECTED){
                try{
                    //Read from InputStream
                    numBytes = mInStream.read(buffer);

                    //Handle Read Bytes
                    mHandler.obtainMessage(MESSAGE_READ, numBytes,-1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.i(TAG, "Closed");
                    mHandler.obtainMessage(DISCONNECTED);
                    break;
                }//try/catch
            }//while
        }//run

        private void cancel() {
            try {
                mSocket.close();
                mHandler.obtainMessage(NOT_CONNECTED);
            } catch (IOException e) {
                Log.e(TAG, "Failed to Close");
            }
        }

        private void write(String s) {
            try {
                byte[] buffer = s.getBytes();
                mOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Failed to Write");
            }//try/catch
        }//write
    }//Connected Thread

    public  void call_command (View v){
        if(mconnectionStatus == CONNECTED) {
            mConnectedThread.write("call");
        }
    }

    public void return_command (View v){
        if(mconnectionStatus == CONNECTED) {
            mRSSI.setText(R.string.getting_rssi);
            mConnectedThread.write("Starting Discovery");

            if(mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
                mBluetoothAdapter.startDiscovery();
            } else {
                mBluetoothAdapter.startDiscovery();
            }
        }
    }

    public  void resume_command  (View v){
        if(mconnectionStatus == CONNECTED) {
            mConnectedThread.write("resume");

        }
    }

    public  void stop_command (View v){
        if(mconnectionStatus == CONNECTED) {
            mConnectedThread.write("stop");
        }
    }

    public  void shutdown_command(View v){
        if(mconnectionStatus == CONNECTED) {
            mConnectedThread.write("shutdown");
        }
    }

    final BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceAddress = device.getAddress();

            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                if(deviceAddress.equals(RASP)) {
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    String s = Integer.toString(rssi);
                    mConnectedThread.write(s);
                    mRSSI.setText(s);

                    mConnectedThread.write("Stop Discovery");
                    mBluetoothAdapter.cancelDiscovery();


                }
            }
        }
    };
}//MainActivity
