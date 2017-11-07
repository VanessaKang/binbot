package y10k.bincompanion;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/* THis activity is reponsible for pairing BinBots to the app. Users can provide a "tag" for
each BinBot
 */

public class Pairing extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);
    }
}
