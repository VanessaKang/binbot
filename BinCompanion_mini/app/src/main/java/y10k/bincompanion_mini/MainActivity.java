package y10k.bincompanion_mini;

import android.content.Intent;
import android.os.AsyncTask;
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

public class MainActivity extends AppCompatActivity {
    //Variable Decleration
    Button mCallButton, mReturnButton, mStopButton;
    TextView mNickName, mStatus, mRSSI;
    ProgressBar mFill, mBatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Assign Variables to UI elements
        mCallButton = (Button) findViewById(R.id.call_button);
        mReturnButton = (Button) findViewById(R.id.return_button);
        mStopButton = (Button) findViewById(R.id.stop_button);

        mNickName = (TextView) findViewById(R.id.nickname);
        mStatus = (TextView) findViewById(R.id.status);
        mFill = (ProgressBar) findViewById(R.id.fill);
        mBatt = (ProgressBar) findViewById(R.id.batt);
        mRSSI = (TextView) findViewById(R.id.rssi);

        //onClickListeners for UI Elements
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast toast = Toast.makeText(MainActivity.this, "Sending Call Request",Toast.LENGTH_SHORT);
                toast.show();
            }//OnClick
        });//mCallButton.setOnClickListener

        mReturnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast toast = Toast.makeText(MainActivity.this, "Sending Return Request",Toast.LENGTH_SHORT);
                toast.show();
            }//onClick
        });// mReturnButton.setOnClickListener

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast toast = Toast.makeText(MainActivity.this, "Sending Stop Request",Toast.LENGTH_SHORT);
                toast.show();
            }//onClick
        });// mReturnButton.setOnClickListener
    }//onCreate


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.pair, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    //On Click of Pair Button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_pair) {
            //Call Mehtod to Pair with BinBot in Background
            //connecttoBinBot();
            //Go to Pair Activity
            Intent startPairing = new Intent(MainActivity.this, Pairing.class);
            startActivity(startPairing);
        }

        return super.onOptionsItemSelected(item);
    }

    //Connects to BinBot in the Background
}//StatusScreen
