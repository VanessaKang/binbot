package y10k.bincompanion;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

import y10k.bincompanion.service.BluetoothService;

/*
This activity is responsible for the following actions:
    - TODO: allowing the user to pair with BinBot
       - Search for potential devices
       - Connect with device
       - Launch Bluetooth service linking BinBot with the app
    - TODO: Disconnecting the app from communicating with BinBot
    - TODO: Hide MAC_ADDRESS from the user (Create new set/array to hold addresses)
 */

public class PairingActivity extends AppCompatActivity {
    //Variable Deceleration
    private Button mScanButton;
    private ListView mPairedList, mDiscoveredList;

    private ArrayAdapter<String> mPairedAdaptor, mDiscoveredAdaptor;

    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;

    private boolean recieverFlag = false; //Checks to see if receiver was registered

    //Constant Deceleration
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names

    //TODO: program state variable to keep track of state of connection
    private final static int NO_CONNECTION = 0;
    private final static int CONNECTED = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        //Assign Bluetooth Adapters
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mPairedAdaptor = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mDiscoveredAdaptor = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        //Check if device supports Bluetooth
        if(mBluetoothAdapter == null){
            //Does not support bluetooth
            Toast.makeText(this, "Device does Not Support Bluetooth", Toast.LENGTH_LONG).show();
        } else {       //Bluetooth Supported
            //Setup Activity
            setupActivity();
        }//if
    }//onCreate

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(recieverFlag) {
            unregisterReceiver(mReciever);
        }
    }

    //============================= METTHODS ========================================================
    private void setupActivity () {
        //Check if bluetooth is on
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        }

        //Runtime Permissions Call to ensure BT can be used
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,  new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        //Initializa GUI element
        mScanButton = findViewById(R.id.scanButton);

        mPairedList = findViewById(R.id.pairedListView);
        mPairedList.setAdapter(mPairedAdaptor);
        mPairedList.setOnItemClickListener(mDeviceClickListener);

        mDiscoveredList = findViewById(R.id.discoveredListView);
        mDiscoveredList.setAdapter(mDiscoveredAdaptor);
        mDiscoveredList.setOnItemClickListener(mDeviceClickListener);

        //Load cCUrrent Paired Devices into ListView
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices){
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                mPairedAdaptor.add(deviceName + "\n" + deviceAddress);
            }
        }

        mScanButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Context context = getApplicationContext();
                //TODO: Implement better way view connection status
                Toast.makeText(context, "Scanning...", Toast.LENGTH_LONG).show();
                discoverDevices(v);
            }
        });
    } //setupActivity

    private void discoverDevices (View view){
        //Check if already Discovering
        if(!mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.startDiscovery();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            recieverFlag = true;
            registerReceiver(mReciever, filter);
        }
    }//discoverDevices

    //======================= CLASSES + OBJECTS ====================================================
    private final BroadcastReceiver mReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //Device has been discovered
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                mDiscoveredAdaptor.add(deviceName + "\n" + deviceAddress);
                mDiscoveredAdaptor.notifyDataSetChanged();
            }//if
        }//onReceive
    };

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            //TODO: Highlight Connected Device
            view.setBackgroundColor(getResources().getColor(R.color.connectedColour));

            //Extract string of device address
            String deviceInfo = adapterView.getItemAtPosition(i).toString();
            String deviceAddress = deviceInfo.substring(deviceInfo.length() - 17);

            //Start Thread pairing to BinBot
            Context context = getApplicationContext();
            Intent startBluetoothServiceIntent = new Intent(context, BluetoothService.class);

            //TODO: Pass device address to the service
            startBluetoothServiceIntent.putExtra("device",
                    mBluetoothAdapter.getRemoteDevice(deviceAddress));
            startService(startBluetoothServiceIntent);
        }//onItemClick
    };
}
