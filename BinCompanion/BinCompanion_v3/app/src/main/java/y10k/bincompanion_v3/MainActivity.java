//Created by Nicholas Chumney
package y10k.bincompanion_v3;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    //CONSTANTS DECLARATION

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

    //State Identifiers
    static final int COLLECTION = 2;
    static final int DISPOSAL = 3;
    static final int TRAVEL = 1;
    static final int ERROR = 0;

    //Fill Level Identifiers
    static final int FILL_FULL = 0;
    static final int FILL_NEARFULL = 1;
    static final int FILL_PARTIAL = 2;
    static final int FILL_EMPTY = 3;

    //Battery Level Identifiers
    static final int BATTERY_HIGH = 40;
    static final int BATTERY_MEDIUM = 41;
    static final int BATTERY_LOW = 42;

    //Signal Strength Identifiers
    static final int SIGNAL_STRONG = 50;
    static final int SIGNAL_OKAY = 51;
    static final int SIGNAL_WEAK = 52;

    // Command Strings
    static final String CALL = "call";
    static final String RETURN = "return";
    static final String STOP = "stop";
    static final String RESUME = "resume";
    static final String SHUTDOWN = "shutdown";
    static final String DISCONNECT = "disconnect";

    // Dialog Types
    static final int PAIRED_DIALOG = 90;
    static final int DISCOVER_DIALOG = 91;
    static final int ERROR_DIALOG = 92;

    //VARIABLE DECLARATION
    private TextView mConnect;
    private TextView mState;
    private TextView mBattery;
    private TextView mFill;
    private TextView mRSSI;


    //TODO: SAVE VARIABLE STATES SO WHEN SWITIVHONG TO LANDSCAPE UI DOES NOT (LIFECYCLE ISSUE)
    private int mConnectionStatus = STATE_NOT_CONNECTED;
    private int mModeStatus = STATE_NOT_CONNECTED;
    private int mBatteryStatus = STATE_NOT_CONNECTED;
    private int mFillStatus = STATE_NOT_CONNECTED;
    private int mSignalStatus = STATE_NOT_CONNECTED;

    protected BluetoothAdapter mBluetoothAdapter;

    //Result Receiver used to handle data retrieved by the service
    final ResultReceiver mReceiver = new BinCompanionReceiver(null);
    private class BinCompanionReceiver extends ResultReceiver{
        protected BinCompanionReceiver(Handler handler) {
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

                    //TODO:ADJUST FOR DESTINATION TRAVELLING TO
                    mModeStatus = Character.getNumericValue(status[0]);
                    mFillStatus = Character.getNumericValue(status[1]);

                    //TESTING /////////////////////////
                    String test = null;
                    try {
                        test = new String(status, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(getApplicationContext(), test, Toast.LENGTH_SHORT).show();
                    ///////////////////////////////////

                    updateUI();
                    break;

                case ERROR_OCCURRED:
                    //TODO: Displays Error Dialog
                    createDialog(ERROR_DIALOG);
                    break;

                default:
                    super.onReceiveResult(resultCode, resultData);
            }//switch
        }//onReceiverResult
    }//BinCompanionReceiver

    //Service Connection used to bind the main activity with the service
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
    }; //ServiceConnection
    // =============================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialise Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Initialize the UI
        mConnect = findViewById(R.id.connected);
        mState = findViewById(R.id.status);
        mBattery = findViewById(R.id.battery);
        mFill = findViewById(R.id.fill);
        mRSSI = findViewById(R.id.rssi);

        updateUI();

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
    }//onDestroy

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();  //Use AppCompactActivity's Method to get handle on menu inflator
        inflater.inflate(R.menu.menu, menu); //Use the inflater's inflate method to inflate our menu layout to this menu
        return true; // Retrun true to show mun in toolbar
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_ShowPaired:
                //Create Paired Device Fragment
                createDialog(PAIRED_DIALOG);
                break;
            case R.id.action_Connect:
                //Create Discovered Device Fragment
                createDialog(DISCOVER_DIALOG);
                break;
            case R.id.action_Listen:
                mService.listen();
                break;
            case R.id.action_disconnect:
                //Message to inform of disconnection
                mService.write(DISCONNECT);
                mService.disconnect();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    // =============================================================================================
    //Updates the user UI to reflect any new information recieved
    public void updateUI(){
        //TODO: Finish Switch Statements
        //Connection Field
        switch (mConnectionStatus){
            case STATE_CONNECTED:
                mConnect.setText(R.string.connected);
                break;
            case STATE_CONNECTING:
                mConnect.setText(R.string.connecting);
                break;
            case STATE_LISTEN:
                mConnect.setText(R.string.listening);
                break;
            case STATE_FAILED:
                mConnect.setText(R.string.failed);
                break;
            case STATE_DISCONNECTED:
                mConnect.setText(R.string.disconnected);
                break;
            default:
                mConnect.setText(R.string.notconnected);
                break;
        }//mConnectionStatus switch

        //Mode Field
        switch (mModeStatus){
            case COLLECTION:
                mState.setText(R.string.collection);
                break;
            case DISPOSAL:
                mState.setText(R.string.disposal);
                break;
            case TRAVEL:
                mState.setText(R.string.travel);
                break;
            case ERROR:
                mState.setText(R.string.error);
                break;
            default:
                mState.setText(R.string.notconnected);
                break;
        }//mModeStatus switch

        //Fill Field
        //TODO
        switch (mFillStatus){
            case FILL_FULL:
                mFill.setText(R.string.full);
                break;
            case FILL_NEARFULL:
                mFill.setText(R.string.partial_full);
                break;
            case FILL_PARTIAL:
                mFill.setText(R.string.near_empty);
                break;
            case FILL_EMPTY:
                mFill.setText(R.string.empty);
                break;
            default:
                mFill.setText(R.string.notconnected);
                break;
        }//mFillStatus switch

        //Battery Field
        //TODO
        switch (mBatteryStatus){
            case BATTERY_HIGH:
                break;
            case BATTERY_MEDIUM:
                break;
            case BATTERY_LOW:
                break;
            default:
                //mBattery.setText(R.string.notconnected);
                mBattery.setText(R.string.notimplemented);
                break;
        }//mBatteryStatus switch

        //Signal Field
        //TODO
        switch (mSignalStatus){
            case SIGNAL_STRONG:
                break;
            case SIGNAL_OKAY:
                break;
            case SIGNAL_WEAK:
                break;
            default:
                //mRSSI.setText(R.string.notconnected);
                mRSSI.setText(R.string.notimplemented);
                break;
        }//mSignalStatus switch
    }//updateUI

    public void createDialog(int dialogType){
        //Store information required for dialog in Bundle
        Bundle bundle = new Bundle();
        bundle.putParcelable("receiver", mReceiver);
        bundle.putInt("dialogType", dialogType);

        //Create Dialog Fragment
        dialogFragment dialogFrag = new dialogFragment();
        dialogFrag.setArguments(bundle);

        switch (dialogType){
            case PAIRED_DIALOG:
                dialogFrag.show(getFragmentManager(), "dialog");
                break;
            case DISCOVER_DIALOG:
                break;
            case ERROR_DIALOG:
                break;
        }//switch
    }//createDialog

    //Send a command call to BinBot
    public void callCommand (View view) {
        if(mConnectionStatus == STATE_CONNECTED) {
            mService.write(CALL);
        }
    }//callCommand

    //Send resume command to BinBot
    public void resumeCommand (View view) {
        if(mConnectionStatus == STATE_CONNECTED) {
            mService.write(RESUME);

            Button resume = (Button) view;
            Button stop = findViewById(R.id.stop_button);

            resume.setVisibility(View.INVISIBLE);
            resume.setEnabled(false);

            stop.setVisibility(View.VISIBLE);
            stop.setEnabled(true);
        }
    }

    //Send Stop command to BinBot
    public void stopCommand (View view) {
        if (mConnectionStatus == STATE_CONNECTED) {
            mService.write(STOP);

            Button stop = (Button) view;
            Button resume = findViewById(R.id.resume_button);

            stop.setVisibility(View.INVISIBLE);
            stop.setEnabled(false);

            resume.setVisibility(View.VISIBLE);
            resume.setEnabled(true);
        }
    }//stopCommand

    //Send shutdown command to BinBot
    public void shutdownCommand (View view) {
        if(mConnectionStatus == STATE_CONNECTED) {
            mService.write(SHUTDOWN);
        }
    }//shutdownCommand

    //Send return command to BinBot
    public void returnCommand (View view) {
        if(mConnectionStatus == STATE_CONNECTED) {
            mService.write(RETURN);
        }
    }//returnCommand
}