package y10k.bincompanion_v1;

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
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    //Variable Declaration
    private int mStatusConnected = CONSTANTS.NOT_CONNECTED;
    private boolean recieverFlag = false;
    private BluetoothAdapter mBluetoothAdapter;

    //Used to receive status updates from BluetoothService
    final ResultReceiver mReceiver = new resultReceiver(null);
    public class resultReceiver extends ResultReceiver {
        private resultReceiver(Handler handler) {
            super(handler);
        }//Constructor

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch (resultCode){
                case 1:
                    String s = (String) resultData.get("key");
                    Log.i("TEST", s);
                    break;
                case 2:
                    int i = resultData.getInt("state");
                    Log.i("TEST", "received");
                default:
                    super.onReceiveResult(resultCode, resultData);
            }//switch
        }//onReceive
    }//resultReceiver

    //Used to bind activity to the service
    BluetoothService mService = null;
    boolean isBound = false;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) iBinder;
            mService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            isBound = false;
        }
    }; //serviceConnection

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setup Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //Check if device supports Bluetooth
        if(mBluetoothAdapter == null){
            //Does not support bluetooth
            Toast.makeText(this, "Device does Not Support Bluetooth", Toast.LENGTH_LONG).show();
        } else {       //Bluetooth Supported
            //Check if bluetooth is on
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, 1);
            }

            //Runtime Permissions Call to ensure BT can be used
            int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

            //Bind Activity to Service
            Intent bindingIntent = new Intent(this, BluetoothService.class);
            bindingIntent.putExtra("receiver", mReceiver);
            bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }//onCreate

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isBound){
            unbindService(serviceConnection);
            isBound = false;
        }

        if(recieverFlag) {
            unregisterReceiver(mReciever);
        }
    }//onDestroy
    //NAV BAR=======================================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();  //Use AppCompactActivity's Method to get handle on menu inflator
        inflater.inflate(R.menu.menu, menu); //Use the inflater's inflate method to inflate our menu layout to this menu
        return true; // Retrun true to show mun in toolbar
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_Pair){
            if(!mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.startDiscovery();
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                recieverFlag = true;
                registerReceiver(mReciever, filter);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private final BroadcastReceiver mReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //Device has been discovered
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceAddress = device.getAddress();
                if(deviceAddress.equals(CONSTANTS.RASP_MAC)){
                    mService.connect(device);
                }
            }//if
        }//onReceive
    };
    //==============================================================================================
    public void callCommand (View v){
        Toast.makeText(this, "Call", Toast.LENGTH_SHORT).show();
    }

    public void returnCommand (View v){
        Toast.makeText(this, "Return", Toast.LENGTH_SHORT).show();
    }//returnCommand

    public void resumeCommand (View v){
        Toast.makeText(this, "Resume", Toast.LENGTH_SHORT).show();

        Button resume = (Button) v;
        Button stop = findViewById(R.id.stop_button);

        resume.setVisibility(View.INVISIBLE);
        resume.setEnabled(false);

        stop.setVisibility(View.VISIBLE);
        stop.setEnabled(true);
    }

    public void stopCommand (View v){
        Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();

        Button stop = (Button) v;
        Button resume = findViewById(R.id.resume_button);

        stop.setVisibility(View.INVISIBLE);
        stop.setEnabled(false);

        resume.setVisibility(View.VISIBLE);
        resume.setEnabled(true);
    }//stopCommand

    public void shutdownCommand (View v){
        Toast.makeText(this, "Shutdown", Toast.LENGTH_SHORT).show();
    }//shutdownCommand
}//MainActivity
