package y10k.bincompanion_v4;

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

public class MainActivity extends AppCompatActivity {
    //CONSTANTS DECLARATION
    //Result Receiver Codes
    static final int STATE_CHANGE = 6;
    static final int OBTAINED_ADDRESS = 7;
    static final int UPDATE = 9;

    // Command Strings
    static final String CALL = "call";
    static final String RETURN = "return";
    static final String STOP = "stop";
    static final String RESUME = "resume";
    static final String DISCONNECT = "disconnect";

    //Byte Array identifiers
    static final int MODE = 0;
    static final int FILL = 1;
    static final int DESTINATION = 2;
    //static final int ERROR_CODE = 3;

    // Connection Status
    static final int STATE_NOT_CONNECTED = 10;
    static final int STATE_CONNECTING = 11; //to be used for client
    static final int STATE_CONNECTED = 13;
    static final int STATE_DISCONNECTED = 14;
    static final int STATE_FAILED = 15;

    //Mode Identifiers
    static final int ERROR = 0;
    static final int TRAVEL = 1;
    static final int COLLECTION = 2;
    static final int DISPOSAL = 3;

    //Fill Level Identifiers
    static final int FILL_FULL = 0;
    static final int FILL_NEAR = 1;
    static final int FILL_PARTIAL = 2;
    static final int FILL_EMPTY = 3;

    //Destination Identifiers
    static final int TO_COLLECT = 0;
    static final int TO_DISPOSE = 1;

    //Dialog Types
    static final int CONNECT_DIALOG = 40;
    static final int ERROR_DIALOG = 41;
    static final int NO_BT_DIALOG = 42;

    //VARIABLE DECLARATION
    private int mConnectionStatus = STATE_NOT_CONNECTED;
    private int mModeStatus = STATE_NOT_CONNECTED;
    private int mFillStatus = STATE_NOT_CONNECTED;
    private int mNextDestinationStatus = STATE_NOT_CONNECTED;

    private boolean isStopped = false;

    private TextView mConnect;
    private TextView mMode;
    private TextView mFill;

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
                    //Store int to appropriate variables and update UI
                    byte[] status = resultData.getByteArray("status");

                    mModeStatus = Character.getNumericValue(status[MODE]);
                    mFillStatus = Character.getNumericValue(status[FILL]);
                    mNextDestinationStatus = Character.getNumericValue(status[DESTINATION]);

                    updateUI();
                    //TESTING /////////////////////////
                    /*
                    String test = null;
                    try {
                        test = new String(status, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(), test, Toast.LENGTH_SHORT).show();
                    */
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

        //Initialize the UI
        mConnect = findViewById(R.id.connectStatus);
        mMode = findViewById(R.id.modeStatus);
        mFill = findViewById(R.id.fillStatus);
        updateUI();

        Button mCallButton = findViewById(R.id.call_button);
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mConnectionStatus == STATE_CONNECTED) {
                    sendCommand(CALL);
                }//if
            }//onClick
        });//onClickListener

        Button mReturnButton = findViewById(R.id.return_button);
        mReturnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mConnectionStatus == STATE_CONNECTED) {
                    sendCommand(RETURN);
                }//if
            }//onClick
        });//onClickListener

        Button mStopButton = findViewById(R.id.stop_button);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mConnectionStatus == STATE_CONNECTED) {
                    sendCommand(STOP);
                    isStopped = true;
                    toggleCommandButton();
                }//if
            }//onClick
        });//onClickListener

        Button mResumeButton = findViewById(R.id.resume_button);
        mResumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mConnectionStatus == STATE_CONNECTED) {
                    sendCommand(RESUME);
                    isStopped = false;
                    toggleCommandButton();
                }//If
            }//onClick
        });//onClickListener

        //Check if device supports bluetooth
        if(mBluetoothAdapter == null){
            //Does not support bluetooth
            createDialog(NO_BT_DIALOG);
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
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isBound){
            unbindService(mConnection);
            isBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflator = getMenuInflater();
        inflator.inflate(R.menu.menubar, menu);
        return true;
    }//onCreateOptionsMenu

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_connect:
                createDialog(CONNECT_DIALOG);
                break;
            case R.id.action_disconnect:
                if(mConnectionStatus == STATE_CONNECTED) {
                    sendCommand(DISCONNECT);
                } else {
                    mService.disconnect();
                }
                break;
        }//switch
        return super.onOptionsItemSelected(item);
    }//onOptionsItemSelected
    //==========================================================================================
    private void updateUI(){
        //Connection Field
        switch (mConnectionStatus){
            case STATE_CONNECTED:
                mConnect.setText(R.string.connected);
                break;
            case STATE_CONNECTING:
                mConnect.setText(R.string.connecting);
                break;
            case STATE_FAILED:
                mConnect.setText(R.string.failed);
                break;
            case STATE_DISCONNECTED:
                Button resume = findViewById(R.id.resume_button);
                Button stop = findViewById(R.id.stop_button);
                TextView stopped = findViewById(R.id.stopped_status);

                mModeStatus = STATE_NOT_CONNECTED;
                mFillStatus = STATE_NOT_CONNECTED;
                isStopped = false;

                resume.setVisibility(View.INVISIBLE);
                resume.setEnabled(false);
                stop.setVisibility(View.VISIBLE);
                stop.setEnabled(true);
                stopped.setVisibility(View.INVISIBLE);

                mConnect.setText(R.string.disconnected);
                break;
            default:
                mConnect.setText(R.string.not_connected);
                break;
        }//mConnectionStatus switch

        //Mode Field
        switch (mModeStatus) {
            case COLLECTION:
                mMode.setText(R.string.collection);
                break;
            case DISPOSAL:
                mMode.setText(R.string.disposal);
                break;
            case TRAVEL:
                switch (mNextDestinationStatus) {
                    case TO_COLLECT:
                        mMode.setText(R.string.travelCollect);
                        break;
                    case TO_DISPOSE:
                        mMode.setText(R.string.travelDispose);
                        break;
                }
                break;
            case ERROR:
                mMode.setText(R.string.error);
                createDialog(ERROR_DIALOG);
                break;
            default:
                mMode.setText(R.string.not_connected);
                break;
        }//mModeStatus switch

        //Fill Field
        switch (mFillStatus){
            case FILL_FULL:
                mFill.setText(R.string.full);
                break;
            case FILL_NEAR:
                mFill.setText(R.string.partial_full);
                break;
            case FILL_PARTIAL:
                mFill.setText(R.string.near_empty);
                break;
            case FILL_EMPTY:
                mFill.setText(R.string.empty);
                break;
            default:
                mFill.setText(R.string.not_connected);
                break;
        }//mFillStatus switch
    }//updateUI

    private void createDialog(int dialogType){
        //Store information required for dialog in Bundle
        Bundle bundle = new Bundle();
        bundle.putParcelable("receiver", mReceiver);
        bundle.putInt("dialogType", dialogType);

        //Create Dialog Fragment
        AlertFragment dialogFrag = new AlertFragment();
        dialogFrag.setArguments(bundle);
        dialogFrag.show(getSupportFragmentManager(), "dialog");
    }//createDialog

    private void sendCommand(String cmd){
        mService.write(cmd);
    }//sendCommand

    private void toggleCommandButton (){
        Button resume = findViewById(R.id.resume_button);
        Button stop = findViewById(R.id.stop_button);
        TextView stopped = findViewById(R.id.stopped_status);

        if (stop.getVisibility() == View.VISIBLE) {
            stop.setVisibility(View.INVISIBLE);
            stop.setEnabled(false);

            resume.setVisibility(View.VISIBLE);
            resume.setEnabled(true);

            stopped.setVisibility(View.VISIBLE);
        } else {
            resume.setVisibility(View.INVISIBLE);
            resume.setEnabled(false);

            stop.setVisibility(View.VISIBLE);
            stop.setEnabled(true);

            stopped.setVisibility(View.INVISIBLE);
        }
    }
}
