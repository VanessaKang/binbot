package b.y10k.bincompanion_v4;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    //CONSTANTS DECLARATION
    // Connection Status
    static final int STATE_NOT_CONNECTED = 10;
    static final int STATE_CONNECTING = 11; //to be used for client
    static final int STATE_CONNECTED = 13;
    static final int STATE_DISCONNECTED = 14;
    static final int STATE_FAILED = 15;

    //Result Receiver Codes
    static final int STATE_CHANGE = 6;
    static final int OBTAINED_ADDRESS = 7;
    static final int UPDATE = 9;

    //VARIABLE DECLARATION
    private int mConnectionStatus = STATE_NOT_CONNECTED;
    private BluetoothAdapter mBluetoothAdapter;

    //Used to handle data sent throughout the application
    final ResultReceiver mReceiver = new BinCompanionReceiver(null);
    private class BinCompanionReceiver extends ResultReceiver{
        public BinCompanionReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            switch (resultCode){
                case STATE_CHANGE:
                    mConnectionStatus = resultData.getInt("state");

                    //Ensure that UI is updated only on the main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateUI();
                        }
                    });
                    break;

                case OBTAINED_ADDRESS:
                    String deviceAddress = resultData.getString("address");
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                    mService.connect(device);
                    break;

                case UPDATE:
                    //TODO: Store int to appropriate variables and update UI
                    byte[] status = resultData.getByteArray("status");
                    //TESTING /////////////////////////
                    String test = null;
                    try {
                        test = new String(status, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(), test, Toast.LENGTH_SHORT).show();
                    ///////////////////////////////////
                    break;
                default:
                    super.onReceiveResult(resultCode, resultData);
            }//switch
        }//onReceiveResult
    }//BinCompanionReceiver

    //Used to bind the main activity to BluetoothService
    private BluetoothService mService = null;
    private boolean isBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {
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
    };//mConnection
    //=========================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialise Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Check if device supports bluetooth
        if(mBluetoothAdapter == null){
            //Does not support bluetooth
            //TODO: Deal with no support
        } else {
            //Check if on
            if (!mBluetoothAdapter.isEnabled()){
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, 1);
            }//if

            //Runtime Permissions Call to ensure BT can be used
            int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION
            );

            //Bind Activity to Service
            Intent bindingIntent = new Intent(this, BluetoothService.class);
            bindingIntent.putExtra("receiver", mReceiver);
            bindService(bindingIntent, mConnection, Context.BIND_AUTO_CREATE);
        }//if
    }//onCreate

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isBound){
            unbindService(mConnection);
            isBound = false;
        }
    }
    //==========================================================================================
    private void updateUI(){}//updateUI
    private void createDialog(){}//createDialog
    private void sendCommand(){}//sendCommand
}
