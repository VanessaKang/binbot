package y10k.bincompanion_v4;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class AlertFragment extends DialogFragment {
    //CONSTANT DECLARATION
    //Dialog Types
    static final int CONNECT_DIALOG = 40;
    static final int ERROR_DIALOG = 41;
    static final int NO_BT_DIALOG = 42;

    //Result Receiver Code
    static final int OBTAINED_ADDRESS = 7;

    //VARIABLE DECLARATION
    private int dialogType;
    private int dialogTitle;

    private View view;
    private ResultReceiver mReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> listAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Extract information from MainActivity
        dialogType = getArguments().getInt("dialogType");
        mReceiver = getArguments().getParcelable("receiver");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //Set VIew layout of Dialog
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_alert, null);
        TextView mText = view.findViewById(R.id.fragText);
        ListView mList = view.findViewById(R.id.fragList);
        mList.setAdapter(listAdapter);
        mList.setOnItemClickListener(mDeviceClickListener);

        switch (dialogType){
            case CONNECT_DIALOG:
                //Set Appropriate Title
                dialogTitle = R.string.deviceTitle;

                //Populate adaptor with Paired Devices
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceAddress = device.getAddress();
                        listAdapter.add(deviceName + "\n" + deviceAddress);
                    }
                } else {
                    mText.setVisibility(View.VISIBLE);
                    mText.setText(R.string.nopairMsg);
                }
                break;
            case ERROR_DIALOG:
                mList.setVisibility(View.GONE);
                mText.setVisibility(View.VISIBLE);

                //Set Appropriate Title and Content
                dialogTitle = R.string.errorTitle;
                mText.setText(R.string.errorMsg);
                break;
            case NO_BT_DIALOG:
                mList.setVisibility(View.GONE);
                mText.setVisibility(View.VISIBLE);

                //Set Appropriate Title and Content
                dialogTitle = R.string.errorTitle;
                mText.setText(R.string.noBTMsg);
                break;
        }//switch

        //Create AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(dialogTitle).setView(view);
        return builder.create();
    }//onCreateDialog

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }
    //=============================================================================================
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

    final BroadcastReceiver mBTReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Discovery has found a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();

                listAdapter.add(deviceName + "\n" + deviceAddress);
                listAdapter.notifyDataSetChanged();
            }
        }
    };
}
