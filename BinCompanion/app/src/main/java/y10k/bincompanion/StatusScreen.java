package y10k.bincompanion;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/* This activity is responsible for displaying the information of the desired BinBot. Through
this activity a use may send a CAll or Return command to the BinBot
 */
public class StatusScreen extends AppCompatActivity {

    //Variable Decleration
    Button mCallButton, mReturnButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status_screen);

        //Assign Variables to UI elements
        mCallButton = (Button) findViewById(R.id.call_button);
        mReturnButton = (Button) findViewById(R.id.return_button);

        //onClickListeners for UI Elements
        mCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast toast = Toast.makeText(StatusScreen.this, "Sending Call Request",Toast.LENGTH_SHORT);
                toast.show();
            }//OnClick
        });//mCallButton.setOnClickListener

        mReturnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast toast = Toast.makeText(StatusScreen.this, "Sending Return Request",Toast.LENGTH_SHORT);
                toast.show();
            }//onClick
        });// mReturnButton.setOnClickListener
    }//onCreate


}//StatusScreen
