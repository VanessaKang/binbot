//Created by Nicholas Chumney
package y10k.bincompanion_v3;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    //Variable Declaration ========================================================================
    private int mConnectionStatus = Constants.STATE_NOT_CONNECTED;
    private BluetoothAdapter mBluetoothAdapter;

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
                    updateUI();
                    break;
                default:
                    super.onReceiveResult(resultCode, resultData);
            }//switch
        }//onRecieveResult
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

        //Initialie Bluetooth
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
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

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
    // =============================================================================================
    public void updateUI(){
    }

    public void callCommand (View view) {}
    public void resumeCommand (View view) {}
    public void stopCommand (View view) {}
    public void shutdownCommand (View view) {}
    public void returnCommand (View view) {}
}
