package y10k.bincompanion;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import y10k.bincompanion.service.BluetoothService;

/* DESIGN NOTES

This activity is in charge of detecting any connected BinBots an showing the user a list of the
cureently detected BinBots. The user on this screen should be able to menu new bots and select a
BinBot to view more detail information on that robot.

    1) Check to see if device supports BT (DONE)
    2) Have deactivated screen set-up
    3) Connect via Pair Button
    4) Until App is destroyed, keep in communication
    5) Should try to connect when opened(?)
    7) Threads to handle incoming data whil still allowing the use to send
    commands at any time
    8) Notify on Low Power, Signal and disconnect
    9) Notfy when command is accepted and finished
    10) Notify user on errors and ask for solutions

    Messages from BB   --->         --> Commands

    Signal Strength    --->         --> Translated Messages to user (Error Boxes, GUI implementation)
 */

public class MainActivity extends AppCompatActivity {
    //Variable Deceleration
    TextView mState, mRSSI;
    ProgressBar mBattery, mFill;

    //Bind Activity to BluetoothService
    BluetoothService mService;
    boolean isBound = false;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setup Bind to BluetoothService
         Intent bindingIntent = new Intent(this, BluetoothService.class);
         bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //====== METHODS ===============================================================================
    public void callCommand (View v){
        Toast.makeText(this, "Call", Toast.LENGTH_SHORT).show();
        mService.write(Constants.CALL_COMMAND);
    }

    public void returnCommand (View v){
        Toast.makeText(this, "Return", Toast.LENGTH_SHORT).show();
        mService.write(Constants.RETURN_COMMAND);
    }

    public void stopCommand (View v){
        Toast.makeText(this, "Resume/Stop", Toast.LENGTH_SHORT).show();
        mService.write(Constants.STOP_COMMAND);
    }

    public void shutdownCommand (View v){
        Toast.makeText(this, "Shudown", Toast.LENGTH_SHORT).show();
        mService.write(Constants.SHUTDOWN_COMMAND);

    }
    //======TOOLBAR=================================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();  //Use AppCompactActivity's Method to get handle on menu inflator
        inflater.inflate(R.menu.menu, menu); //Use the inflater's inflate method to inflate our menu layout to this menu
        return true; // Retrun true to show mun in toolbar
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_Pair){
            Intent pairingIntent = new Intent(this, PairingActivity.class);
            startActivity(pairingIntent);
        }
        return super.onOptionsItemSelected(item);
    }
    //====== CLASSES ===============================================================================
}

