package y10k.bincompanion_mini;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Pairing extends AppCompatActivity {

    //GUI Variables
    private Button mHi;
    private Button mFU;

    private TextView mRead;
    private TextView mConnect;

    private ListView mList;
    private ListView mList1;

    private int flag = 1;

    //BLuetooth Variables
    BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBluetoothArrayAdapter;
    private ArrayAdapter<String> mPairedList;

    private Handler mHandler;
    private ConnectedThread mConnectedThread;
    private BluetoothSocket mBluetoothSocket = null;

    //Constants for identifying shared types for calling fnctions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    private static final String TAG = Pairing.class.getSimpleName();
    private static final String RASP_MAC = "B8:27:EB:30:F6:FD"; //raps pi MAC Address
    private static final UUID MY_UUID = UUID.fromString("18f86520-e2af-408d-8fe6-d7dc8336c13a") ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        //Set up Variables
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mPairedList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        mHi = (Button) findViewById(R.id.Hi);
        mFU = (Button) findViewById(R.id.FU);

        mRead = (TextView) findViewById(R.id.read);
        mConnect = (TextView) findViewById(R.id.connect);

        mList = (ListView) findViewById(R.id.list);
        mList.setAdapter(mBluetoothArrayAdapter);

        mList1 = (ListView) findViewById(R.id.list1);
        mList1.setAdapter(mPairedList);
        mList1.setOnItemClickListener(mDeviceClick);

        //Runtime Permissions Call
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,  new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        mHandler = new Handler(Looper.getMainLooper()){
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    mRead.setText(readMessage);
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        mConnect.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        mConnect.setText("Connection Failed");
                }
            }
        };

        if(mBluetoothAdapter == null) {
            //NDevice does not support Bluetooth
            Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            //Check if bluetooth is on
            if(!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            //Set-Up Buttons
            mHi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                   if (mConnectedThread != null){
                       mConnectedThread.write("1");
                   }
                }
            });

            mFU.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mConnectedThread != null){
                        mConnectedThread.write("2");
                    }
                }
            });

            //Begin Conection Command
            startConnect();
        }
    } //onCreate

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(flag == 0) {
            unregisterReceiver(mReceiver);
        }
    }

    private void startConnect() {

        mPairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : mPairedDevices){
           mPairedList.add(device.getAddress() + "\n");

           //Check if phone already paired
            if((device.getAddress()).equals(RASP_MAC)){
                flag = 1;
                connect(device);
            }
        }

        if(flag == 0) {
            mBluetoothAdapter.startDiscovery();

            // Register for broadcasts when a device is discovered.
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
        }

    } //startConnect

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBluetoothArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBluetoothArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private AdapterView.OnItemClickListener mDeviceClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            String info = ((TextView) view).getText().toString();
            final String address = info.substring(info.length() - 17);

            mConnect.setText("Connecting...");
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        }
    };

    private void connect(BluetoothDevice device){
        final BluetoothDevice d = device;
        new Thread()
        {
            public void run() {
                boolean fail = false;
                try {
                    mBluetoothSocket = createBluetoothSocket(d);
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
                if(fail == false) {
                    mConnectedThread = new ConnectedThread(mBluetoothSocket);
                    mConnectedThread.start();
                }
            }
        }.start();
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
            } catch (IOException e) { }

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
                    if(bytes != 0) {
                        buffer = new byte[1024];
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read

                        //Pass sting to handler to decode bytes
                        mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}//Pairing
