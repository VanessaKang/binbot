package y10k.bincompanion_v2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ButtonBarLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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


        Use Bundle to store current variable inputs?
        check variable status on start up?
*/
public class MainActivity extends AppCompatActivity {
    //VARIABLE DECLARATION==========================================================================
    private int mConnectionStatus = CONSTANTS.NOT_CONNECTED;
    private TextView mConnectedField;
    private TextView mStatusField;
    private TextView mFillField;
    private TextView mBatteryField;
    private TextView mRSSIField;

    private Messenger mMessenger = null;
    private boolean isBound = false;

    //Handles messages from Bluetooth Service
    class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case CONSTANTS.PROVIDESTATE:
                    mConnectionStatus = (int) msg.obj;
                    break;
                case CONSTANTS.UPDATE:
                    //TODO
                    break;
                default:
                    super.handleMessage(msg);
            }//switch
        }//handleMessage
    }//ServiceHandler

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mMessenger = new Messenger(service);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mMessenger = null;
            isBound = false;
        }
    };

    //MAIN METHODS =================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Bind to Service
        Intent bindingIntent = new Intent(this, BluetoothService.class);
        bindService(bindingIntent, mConnection, Context.BIND_AUTO_CREATE);

        //Initialize GUI
        mConnectedField = findViewById(R.id.connected);
        mStatusField = findViewById(R.id.status);
        mFillField = findViewById(R.id.fill);
        mBatteryField = findViewById(R.id.battery);
        mRSSIField = findViewById(R.id.rssi);


        if(mConnectionStatus == CONSTANTS.NOT_CONNECTED){
            mConnectedField.setText(CONSTANTS.NOT_CONNECTED_STRING);
            mStatusField.setText(CONSTANTS.NOT_CONNECTED_STRING);
            mFillField.setText(CONSTANTS.NOT_CONNECTED_STRING);
            mBatteryField.setText(CONSTANTS.NOT_CONNECTED_STRING);
            mRSSIField.setText(CONSTANTS.NOT_CONNECTED_STRING);
        }
    }//onCreate

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Unbind from Service
        if(isBound){
            unbindService(mConnection);
            isBound = false;
        }
    }
    //==============================================================================================
    public void callCommand (View v){
        Toast.makeText(this, "Call", Toast.LENGTH_SHORT).show();
        Message msg = Message.obtain(null, CONSTANTS.CALL,null);
        try {
            mMessenger.send(msg);
        } catch (RemoteException e){
            e.printStackTrace();
        }//try/catch
    }

    public void returnCommand (View v){
        Toast.makeText(this, "Return", Toast.LENGTH_SHORT).show();
        Message msg = Message.obtain(null, CONSTANTS.RETURN,null);
        try {
            mMessenger.send(msg);
        } catch (RemoteException e){
            e.printStackTrace();
        }//try/catch
    }//returnCommand

    public void resumeCommand (View v){
        Toast.makeText(this, "Resume", Toast.LENGTH_SHORT).show();

        Button resume = (Button) v;
        Button stop = findViewById(R.id.stop_button);

        resume.setVisibility(View.INVISIBLE);
        resume.setEnabled(false);

        stop.setVisibility(View.VISIBLE);
        stop.setEnabled(true);

        Message msg = Message.obtain(null, CONSTANTS.RESUME,null);
        try {
            mMessenger.send(msg);
        } catch (RemoteException e){
            e.printStackTrace();
        }//try/catch
    }

    public void stopCommand (View v){
        Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();

        Button stop = (Button) v;
        Button resume = findViewById(R.id.resume_button);

        stop.setVisibility(View.INVISIBLE);
        stop.setEnabled(false);

        resume.setVisibility(View.VISIBLE);
        resume.setEnabled(true);

        Message msg = Message.obtain(null, CONSTANTS.STOP,null);
        try {
            mMessenger.send(msg);
        } catch (RemoteException e){
            e.printStackTrace();
        }//try/catch
    }//stopCommand

    public void shutdownCommand (View v){
        Toast.makeText(this, "Shutdown", Toast.LENGTH_SHORT).show();
        Message msg = Message.obtain(null, CONSTANTS.SHUTDOWN,null);
        try {
            mMessenger.send(msg);
        } catch (RemoteException e){
            e.printStackTrace();
        }//try/catch
    }//shutdownCommand

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
        if (id == R.id.action_Pair){
            Intent pairingIntent = new Intent(this, PairingActivity.class);
            startActivity(pairingIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}
