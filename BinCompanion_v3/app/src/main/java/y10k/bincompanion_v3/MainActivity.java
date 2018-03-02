//Created by Nicholas Chumney
package y10k.bincompanion_v3;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    //Variable Declaration
    private TextView mConnect;
    private TextView mState;
    private TextView mBattery;
    private TextView mFill;
    private TextView mRSSI;


    private int mConnectionStatus = Constants.STATE_NOT_CONNECTED;
    private int mModeStatus = Constants.STATE_NOT_CONNECTED;
    private int mBatteryStatus = Constants.STATE_NOT_CONNECTED;
    private int mFillStatus = Constants.STATE_NOT_CONNECTED;
    private int mSignalStatus = Constants.STATE_NOT_CONNECTED;

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
                case Constants.STATE_CHANGE:
                    mConnectionStatus = resultData.getInt("state");

                    //Ensure that UI is updated only on the main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateUI();
                        }
                    });
                    break;

                case Constants.OBTAINED_ADDRESS:
                    String deviceAddress = resultData.getString("address");
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
                    mService.connect(device);
                    break;

                case Constants.MSG_DECODED:
                    String msg = resultData.getString("msg");
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    break;

                case Constants.ERROR_OCCURRED:
                    //TODO
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
        //Store resultReceiver to be sent to fragments
        Bundle bundle = new Bundle();
        bundle.putParcelable("receiver", mReceiver);

        dialogFragment dialogFrag = new dialogFragment();

        int id = item.getItemId();
        switch (id){
            case R.id.action_ShowPaired:
                //Create Paired Device Fragment
                bundle.putInt("dialogType", 1);
                dialogFrag.setArguments(bundle);
                dialogFrag.show(getFragmentManager(), "dialog");
                break;
            case R.id.action_Connect:
                //Create Discovered Device Fragment
                bundle.putInt("dialogType", 2);
                dialogFrag.setArguments(bundle);
                dialogFrag.show(getFragmentManager(), "dialog");
                break;
            case R.id.action_Listen:
                mService.listen();
                break;
            case R.id.action_disconnect:
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
            case Constants.STATE_CONNECTED:
                mConnect.setText(R.string.connected);
                break;
            case  Constants.STATE_CONNECTING:
                mConnect.setText(R.string.connecting);
                break;
            case Constants.STATE_LISTEN:
                mConnect.setText(R.string.listening);
                break;
            case Constants.STATE_FAILED:
                mConnect.setText(R.string.failed);
                break;
            case Constants.STATE_DISCONNECTED:
                mConnect.setText(R.string.disconnected);
                break;
            default:
                mConnect.setText(R.string.notconnected);
                break;
        }//mConnectionStatus switch

        //Mode Field
        switch (mModeStatus){
            case Constants.COLLECTION:
                mState.setText(R.string.collection);
                break;
            case  Constants.DISPOSAL:
                mState.setText(R.string.disposal);
                break;
            case Constants.TRAVEL:
                mState.setText(R.string.travel);
                break;
            case Constants.ERROR:
                mState.setText(R.string.error);
                break;
            default:
                mState.setText(R.string.notconnected);
                break;
        }//mModeStatus switch

        //Battery Field
        //TODO
        switch (mBatteryStatus){
            case Constants.BATTERY_HIGH:
                break;
            case Constants.BATTERY_MEDIUM:
                break;
            case Constants.BATTERY_LOW:
                break;
            default:
                //mBattery.setText(R.string.notconnected);
                mBattery.setText(R.string.notimplemented);
                break;
        }//mBatteryStatus switch

        //Fill Field
        //TODO
        switch (mFillStatus){
            case Constants.FILL_FULL:
                break;
            case Constants.FILL_NEARFULL:
                break;
            case Constants.FILL_PARTIAL:
                break;
            case Constants.FILL_EMPTY:
                break;
            default:
                mFill.setText(R.string.notconnected);
                break;
        }//mFillStatus switch

        //Signal Field
        //TODO
        switch (mSignalStatus){
            case Constants.SIGNAL_STRONG:
                break;
            case Constants.SIGNAL_OKAY:
                break;
            case Constants.SIGNAL_WEAK:
                break;
            default:
                //mRSSI.setText(R.string.notconnected);
                mRSSI.setText(R.string.notimplemented);
                break;
        }//mSignalStatus switch
    }//updateUI

    //Send a command call to BinBot
    public void callCommand (View view) {
        if(mConnectionStatus == Constants.STATE_CONNECTED) {
            mService.write(Constants.CALL);
        }
    }//callCommand

    //Send resume command to BinBot
    public void resumeCommand (View view) {
        if(mConnectionStatus == Constants.STATE_CONNECTED) {
            mService.write(Constants.RESUME);

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
        if (mConnectionStatus == Constants.STATE_CONNECTED) {
            mService.write(Constants.STOP);

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
        if(mConnectionStatus == Constants.STATE_CONNECTED) {
            mService.write(Constants.SHUTDOWN);
        }
    }//shutdownCommand

    //Send return command to BinBot
    public void returnCommand (View view) {
        if(mConnectionStatus == Constants.STATE_CONNECTED) {
            mService.write(Constants.RETURN);
        }
    }//returnCommand
}