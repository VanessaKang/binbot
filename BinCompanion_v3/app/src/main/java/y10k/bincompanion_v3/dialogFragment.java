//Created by nchum on 2018-02-23.
package y10k.bincompanion_v3;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

public class dialogFragment extends DialogFragment {
    private int dialogType; //1 - Paired, 2 - Discovered
    private int dialogTitle;
    private ResultReceiver mReceiver;

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Extract information from MainActivity
        dialogType = getArguments().getInt("dialogType");
        mReceiver = getArguments().getParcelable("receiver");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1);

        //Runtime Permissions Call to ensure BT can be used
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(getActivity(), new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION
        );
    }//onCreate

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //Set VIew layout of Dialog
        View view = getActivity().getLayoutInflater().inflate(R.layout.list_fragment, null);
        ListView mList = view.findViewById(R.id.listView);
        mList.setAdapter(listAdapter);
        mList.setOnItemClickListener(mDeviceClickListener);

        if(dialogType == 1) {
            dialogTitle = R.string.pairedDeviceTitle;

            //Populate adaptor with Paired Devices
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();
                    listAdapter.add(deviceName + "\n" + deviceAddress);
                }
            }
        } else if (dialogType == 2) {
            dialogTitle = R.string.discoveredDeviceTitle;

            //TODO: Implement Discovery Fragment
            mBluetoothAdapter.startDiscovery();
            //Register Reciever
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            getActivity().registerReceiver(mBTReceiver, filter);
        }

        //Create AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(dialogTitle).setView(view);
        return builder.create();
    }//onCreateDialog

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        mBluetoothAdapter.cancelDiscovery();
        getActivity().unregisterReceiver(mBTReceiver);
    }//onDismiss
    //==============================================================================================
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            //Extract string of device address
            String deviceInfo = adapterView.getItemAtPosition(i).toString();
            String deviceAddress = deviceInfo.substring(deviceInfo.length() - 17);

            //Send deviceAddress to MainActivity
            Bundle savedData = new Bundle();
            savedData.putString("address", deviceAddress);
            mReceiver.send(Constants.OBTAINED_ADDRESS, savedData);

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
