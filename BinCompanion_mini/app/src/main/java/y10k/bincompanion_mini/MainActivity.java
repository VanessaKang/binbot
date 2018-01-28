package y10k.bincompanion_mini;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

public class MainActivity extends AppCompatActivity {
    //Variable Declaration
    TextView mConnection;
    TextView mState;
    TextView mFill;

    int mconnectionStatus = NOT_CONNECTED;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothSocket mSocket = null;

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
                    mConnection.setText("Disconnected");
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

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

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
            }
        }
    }//onCreate

    @Override
    protected void onDestroy() {
        super.onDestroy();

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
                    Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                }
                // Establish the Bluetooth socket connection.
                try {
                    mSocket.connect();
                } catch (IOException e) {
                    try {
                        fail = true;
                        mSocket.close();
                        mHandler.obtainMessage(CONNECTION_FAILED, -1, -1)
                                .sendToTarget();
                    } catch (IOException e2) {
                        //insert code to deal with this
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                }
                if(!fail) {
                    mConnectedThread = new ConnectedThread(mSocket);
                    mConnectedThread.start();

                    mHandler.obtainMessage(CONNECTION_MADE)
                            .sendToTarget();
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
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

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
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        buffer = new byte[1024];
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }

            mHandler.obtainMessage(DISCONNECTED);
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }//Cnnected Thread

    public void test(View v){
        if (mconnectionStatus == CONNECTED) {
            mConnectedThread.write("CALL");
        }
    }
}//MainActivity
