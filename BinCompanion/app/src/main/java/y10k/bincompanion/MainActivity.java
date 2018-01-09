package y10k.bincompanion;

import android.Manifest;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

/* DESIGN NOTES

This activity is in charge of detecting any connected BinBots an showing the user a list of the
cureently detected BinBots. The user on this screen should be able to menu new bots and select a
BinBot to view more detail information on that robot.

    1) Check to see if device supports BT (DONE)
    2) Have deactivated screen set-up
    3) Connect via Pair Button
    4) Until App is destoyed, keep in communication
    5) Should try to menu when opened(?)
    7) Threads to handle incoming data whil still allowing the use to send
    commands at any time
    8) Notify on Low Power, SIgnal and disconnect
    9) Notfy when command is accepted and finished
    10) Notify user on errors and ask for solutions

    Messages from BB   --->         --> Commands

    Signal Strength    --->         --> Translated Messages to user (Error Boxes, GUI implementation)
 */

public class MainActivity extends AppCompatActivity {
    //Variable Deceleration

    //TODO: define a handler so that this activity can send/recieve messages on BT Thread

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize GUI Elements

        //Setup Default Fields


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //====== METHODS ==============================================================================
    public void callCommand (View view) {
        //TODO: Trigger Call Command vis BTServer
    }

    public void returnCommand (View view) {
        //TODO: Trigger Return Comman via BTServer
    }


    public void stopCommand (View view) {
        //TODO Trigger Stop Command via BTServer
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
            //TODO: Set action of pair button
            Intent pairingIntent = new Intent(this, PairingActivity.class);
            startActivity(pairingIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    //=======CLASSES + OBJECTS =====================================================================
}

