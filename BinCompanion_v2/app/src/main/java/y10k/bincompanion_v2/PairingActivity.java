package y10k.bincompanion_v2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

/*
This activity is responsible for the following actions:
    - Allowing the user to pair with BinBot
       - Search for potential devices
       - Connect with device
       - Launch Bluetooth service linking BinBot with the app
    - Disconnecting the app from communicating with BinBot
    - TODO: Hide MAC_ADDRESS from the user (Create new set/array to hold addresses)
 */

public class PairingActivity extends AppCompatActivity {
    //VARIABLE DECLARATION
    private ArrayAdapter<String> mDiscoveredAdaptor;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean recieverFlag = false; //Checks to see if receiver was registered

    //Bind with Service
    private Messenger mMessenger = null;
    private boolean isBound = false;

    //Handles messages from Bluetooth Service
    class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                default:
                    super.handleMessage(msg);
            }//switch
        }//handleMessage
    }//ServiceHandler

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            mMessenger = new Messenger(service);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMessenger = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        //Bind to BluetoothService
        Intent bindingIntent = new Intent(this, BluetoothService.class);
        bindService(bindingIntent, mConnection, Context.BIND_AUTO_CREATE);

        //Assign Bluetooth Adapters
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ArrayAdapter<String> mPairedAdaptor = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mDiscoveredAdaptor = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        //Check if device supports Bluetooth
        if(mBluetoothAdapter == null){
            //Does not support bluetooth
            Toast.makeText(this, "Device does Not Support Bluetooth", Toast.LENGTH_LONG).show();
        } else {       //Bluetooth Supported
            //Check if bluetooth is on
            if(!mBluetoothAdapter.isEnabled()){
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, 1);
            }

            //Runtime Permissions Call to ensure BT can be used
            int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
            ActivityCompat.requestPermissions(this,  new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

            //Initializa GUI element
            ListView mPairedList = findViewById(R.id.pairedListView);
            mPairedList.setAdapter(mPairedAdaptor);
            mPairedList.setOnItemClickListener(mDeviceClickListener);

            ListView mDiscoveredList = findViewById(R.id.discoveredListView);
            mDiscoveredList.setAdapter(mDiscoveredAdaptor);
            mDiscoveredList.setOnItemClickListener(mDeviceClickListener);

            //Load Current Paired Devices into ListView
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if(pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices){
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();
                    mPairedAdaptor.add(deviceName + "\n" + deviceAddress);
                }
            }
        }//if
    }//onCreate

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(isBound){
            unbindService(mConnection);
            isBound = false;
        }

        if(recieverFlag) {
            unregisterReceiver(mReciever);
        }
    }
    //==============================================================================================
    public void discoverDevices (View view){
        Context context = getApplicationContext();
        Toast.makeText(context, "Scanning...", Toast.LENGTH_LONG).show();

        //Check if already Discovering
        if(!mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.startDiscovery();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            recieverFlag = true;
            registerReceiver(mReciever, filter);
        }
    }//discoverDevices
    //==============================================================================================
    private final BroadcastReceiver mReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //Device has been discovered
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                if(deviceName != null) {
                    mDiscoveredAdaptor.add(deviceName + "\n" + deviceAddress);
                    mDiscoveredAdaptor.notifyDataSetChanged();
                }
            }//if
        }//onReceive
    };

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            //Extract string of device address
            String deviceInfo = adapterView.getItemAtPosition(i).toString();
            String deviceAddress = deviceInfo.substring(deviceInfo.length() - 17);

            //Initiate Connection with Server
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);


            Message msg = Message.obtain(null, CONSTANTS.CONNECT, device);
            try {
                mMessenger.send(msg);
            } catch (RemoteException e){
                e.printStackTrace();
            }//try/catch
        }//onItemClick
    };
}//PairingActivity
