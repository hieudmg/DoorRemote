package vn.fgc.doorremote;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class ConnectFragment extends DialogFragment {

    private static final String TAG = "fgc_doorremote";
    private BluetoothHandler bluetooth;

    public ConnectFragment() {
        // Required empty public constructor
    }

    public void setService(BluetoothHandler bluetooth) {
        this.bluetooth = bluetooth;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        DevicesListAdapter adapter = new DevicesListAdapter(getContext(), null, false);
        ListView deviceList = view.findViewById(R.id.list_found_devices);
        deviceList.setAdapter(adapter);
        ProgressBar loadingProgressBar = view.findViewById(R.id.content_loading);
        loadingProgressBar.setIndeterminate(true);
        Switch scanSwitch = view.findViewById(R.id.switch_discover);
        Log.d(TAG, "onViewCreated: ");

        deviceList.setOnItemClickListener((parent, view1, position, id) -> {
            try {
                bluetooth.stopScanning();
                loadingProgressBar.setVisibility(View.GONE);
            } catch (Exception ignored) {

            }
            bluetooth.connectToDevice(adapter.getItem(position).getName(), adapter.getItem(position).getAddress());
            dismiss();
        });

        bluetooth.setOnDiscoveryStarted(() -> {
            loadingProgressBar.setVisibility(View.VISIBLE);
            scanSwitch.setChecked(true);
        });

        bluetooth.setOnDiscoveryFinished(() -> {
            loadingProgressBar.setVisibility(View.GONE);
            if (adapter.getCount() == 0) {
                Toast.makeText(bluetooth.getContext(), "No Device Found", Toast.LENGTH_LONG).show();
            }
            scanSwitch.setChecked(false);
        });


        scanSwitch.setOnClickListener((v) -> {
            if (scanSwitch.isChecked()) {
                bluetooth.setOnDeviceFound(device -> {
                    if (TextUtils.isEmpty(device.getName())) {
                        return;
                    }
                    adapter.addDevice(new ListDataWrapper(device.getName(), device.getAddress(), false, 0));
                    adapter.notifyDataSetChanged();
                });
                if (!bluetooth.isScanning()) {
                    bluetooth.startScanning();
                }
            } else {
                try {
                    bluetooth.stopScanning();
                } catch (Exception e) {
                    Log.d(TAG, "onViewCreated: " + e.getMessage());
                }
            }
        });

        bluetooth.setOnDeviceFound(device -> {
            Log.d(TAG, "Device found" + device.getName());
            if (TextUtils.isEmpty(device.getName())) {
                return;
            }
            adapter.addDevice(new ListDataWrapper(device.getName(), device.getAddress(), false, 0));
            adapter.notifyDataSetChanged();
        });

        if (bluetooth.isScanning()) {
            scanSwitch.setChecked(true);
            loadingProgressBar.setVisibility(View.VISIBLE);
        } else {
            scanSwitch.setChecked(false);
            loadingProgressBar.setVisibility(View.GONE);
        }

        if (!bluetooth.isScanning()) {
            bluetooth.startScanning();
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        bluetooth.stopScanning();
        super.onDestroyView();
    }
}
