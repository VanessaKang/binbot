package y10k.bincompanion;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


/* This activity is in charge of detecting any connected BinBots an showing the user a list of the
cureently detected BinBots. The user on this screen should be able to pair new bots and select a
BinBot to view more detail information on that robot.
 */

public class MainActivity extends AppCompatActivity {

    //Variable Decleartion
    private TextView mBotView;
    private Button mPairButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Assign variables to UI elements
        mBotView = (TextView) findViewById(R.id.bot_view);
        mPairButton = (Button) findViewById(R.id.pair_button);

        //onClickListeners for UI Elements
        mPairButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startPairing = new Intent(MainActivity.this, Pairing.class);
                startActivity(startPairing);
            }//onClick
        }); //mPairButton.setOnClickListener

        //Load COnnected bots to interdace
        loadConnectedBots();
    } //onCreate

    private void loadConnectedBots(){
        //TODO: Link Paired Bots to this screen
        //TODO: SEt Onclick to each created bot and jump to status screen corresponding to that bot
    }
}
