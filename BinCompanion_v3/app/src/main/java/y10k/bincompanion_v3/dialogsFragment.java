package y10k.bincompanion_v3;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by nchum on 2018-03-13.
 */

/*
    Dialogs will not be able to run a bluetooth scan for me, will have to use an actual fragment
 */

public class dialogsFragment extends DialogFragment {
    //CONSTANTS DECLARATION
    //Result Receiver Code
    static final int OBTAINED_ADDRESS = 7;
    static final int PAIRED_DIALOG = 90;
    static final int DISCOVER_DIALOG = 91;
    static final int ERROR_DIALOG = 92;

    //VARIABLE DECLARATION
    private int dialogType;
    private int dialogTitle;

    private ResultReceiver mReceiver;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Extract information from MainActivity
        dialogType = getArguments().getInt("dialogType");
        mReceiver = getArguments().getParcelable("receiver");

        //Set Dialog window settings based on type requested
        switch(dialogType) {
            case PAIRED_DIALOG:
                dialogTitle = R.string.pairedDeviceTitle;
                break;
            case DISCOVER_DIALOG:
                dialogTitle = R.string.discoveredDeviceTitle;
                break;
            case ERROR_DIALOG:
                dialogTitle = R.string.errorTitle;
                break;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_fragment, null);
        ListView mList = view.findViewById(R.id.listView);
        TextView mText = view.findViewById(R.id.textView);
        //mList.setAdapter(listAdapter);
        //mList.setOnItemClickListener(mDeviceClickListener);

        //Create AlertDialog
        switch (dialogType){
            case PAIRED_DIALOG:
                break;
            case DISCOVER_DIALOG:
                break;
            case ERROR_DIALOG:
                break;
        }//switch

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(dialogTitle).setView(view);
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    //=============================================================================
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            //Extract string of device address
            String deviceInfo = adapterView.getItemAtPosition(i).toString();
            String deviceAddress = deviceInfo.substring(deviceInfo.length() - 17);

            //Send deviceAddress to MainActivity
            Bundle savedData = new Bundle();
            savedData.putString("address", deviceAddress);
            mReceiver.send(OBTAINED_ADDRESS, savedData);

            //Close Dialog Window
            dismiss();
        }//onItemClick
    };
}
