package vn.fgc.doorremote;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import vn.fgc.doorremote.data.AppLog;
import vn.fgc.doorremote.data.LogDatabase;
import vn.fgc.doorremote.data.TinyDB;
import vn.fgc.doorremote.ultilities.ThreadHelper;

enum DeviceState {
    UNAUTHENTICATED, AUTHENTICATING, AUTHENTICATED
}

public class BluetoothHandler {
    private static final int REQUEST_ENABLE_BT = 1111;
    private static final String TAG = "fgc_doorremote";
    Handler tempHumidHandler;
    Runnable tempHumidRunnable;
    private Activity activity;
    private Context context;
    private UUID uuid;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private BluetoothDevice device, devicePair;
    private BufferedReader input;
    private OutputStream out;
    private OnBluetoothTurningOn onBluetoothTurningOn;
    private OnBluetoothOn onBluetoothOn;
    private OnBluetoothTurningOff onBluetoothTurningOff;
    private OnBluetoothOff onBluetoothOff;
    private OnUserDeniedActivation onUserDeniedActivation;
    private OnDiscoveryStarted onDiscoveryStarted;
    private OnDiscoveryFinished onDiscoveryFinished;
    private OnDeviceFound onDeviceFound;
    private OnDevicePaired onDevicePaired;
    private OnDeviceUnpaired onDeviceUnpaired;
    private OnDiscoveryError onDiscoveryError;
    private OnDeviceConnected onDeviceConnected;
    private OnDeviceDisconnected onDeviceDisconnected;
    private OnMessage onRawMessage;
    private List<OnMessage> onMessages = new ArrayList<>();
    private OnDeviceError onDeviceError;
    private OnConnectError onConnectError;
    private OnDeviceAuthenticated onDeviceAuthenticated;
    private AlertDialog loadingDialog;
    private AlertDialog authDialog;
    private boolean isSuppressDisconnectCallback;
    private boolean connected;

    private boolean runOnUi;
    private final BroadcastReceiver pairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d("BLE", "PAIR RECEIVER");

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    context.unregisterReceiver(pairReceiver);
                    if (onDevicePaired != null) {
                        ThreadHelper.run(runOnUi, activity, () -> onDevicePaired.onEvent(devicePair));
                    }
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    context.unregisterReceiver(pairReceiver);
                    if (onDeviceUnpaired != null) {
                        ThreadHelper.run(runOnUi, activity, () -> onDeviceUnpaired.onEvent(devicePair));
                    }
                }
            }
        }
    };
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                ThreadHelper.run(runOnUi, activity, () -> {
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            if (onBluetoothOff != null) {
                                onBluetoothOff.onEvent();
                            }
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            if (onBluetoothTurningOff != null) {
                                onBluetoothTurningOff.onEvent();
                            }
                            break;
                        case BluetoothAdapter.STATE_ON:
                            if (onBluetoothOn != null) {
                                onBluetoothOn.onEvent();
                            }
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            if (onBluetoothTurningOn != null) {
                                onBluetoothTurningOn.onEvent();
                            }
                            break;
                    }
                });
            }
        }
    };
    private DeviceState deviceState;
    private boolean isScanning;
    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        if (state == BluetoothAdapter.STATE_OFF) {
                            if (onDiscoveryError != null) {
                                ThreadHelper.run(runOnUi, activity, () -> onDiscoveryError.onEvent("Bluetooth turned off"));
                            }
                        }
                        isScanning = false;
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        if (onDiscoveryStarted != null) {
                            ThreadHelper.run(runOnUi, activity, () -> onDiscoveryStarted.onEvent());
                        }
                        isScanning = true;
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        context.unregisterReceiver(scanReceiver);
                        if (onDiscoveryFinished != null) {
                            ThreadHelper.run(runOnUi, activity, () -> onDiscoveryFinished.onEvent());
                        }
                        isScanning = false;
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (onDeviceFound != null) {
                            ThreadHelper.run(runOnUi, activity, () -> onDeviceFound.onEvent(device));
                        }
                        break;
                }
            }
        }
    };

    public BluetoothHandler(Activity activity) {
        this.activity = activity;
        initialize(activity, UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
    }

    public BluetoothHandler(Context context) {
        this.context = context;
        initialize(context, UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
    }

    public BluetoothHandler(Context context, UUID uuid) {
        initialize(context, uuid);
    }

    private void initialize(Context context, UUID uuid) {
        this.context = context;
        this.uuid = uuid;
        this.connected = false;
        this.runOnUi = false;
    }

    public void onStart() {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        context.registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    public void onStop() {
        context.unregisterReceiver(bluetoothReceiver);
    }

    public void enable() {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        }
    }

    public void disable() {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.disable();
            }
        }
    }

    public Context getContext() {
        return context;
    }

    public BluetoothSocket getSocket() {
        return socket;
    }

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public boolean isEnabled() {
        if (bluetoothAdapter != null) {
            return bluetoothAdapter.isEnabled();
        }
        return false;
    }

    public void setOnBluetoothTurningOn(OnBluetoothTurningOn onBluetoothTurningOn) {
        this.onBluetoothTurningOn = onBluetoothTurningOn;
    }

    public void setOnBluetoothOn(OnBluetoothOn onBluetoothOn) {
        this.onBluetoothOn = onBluetoothOn;
    }

    public void setOnBluetoothTurningOff(OnBluetoothTurningOff onBluetoothTurningOff) {
        this.onBluetoothTurningOff = onBluetoothTurningOff;
    }

    public void setOnBluetoothOff(OnBluetoothOff onBluetoothOff) {
        this.onBluetoothOff = onBluetoothOff;
    }

    public void setOnUserDeniedActivation(OnUserDeniedActivation onUserDeniedActivation) {
        this.onUserDeniedActivation = onUserDeniedActivation;
    }

    public void setOnDiscoveryStarted(OnDiscoveryStarted onDiscoveryStarted) {
        this.onDiscoveryStarted = onDiscoveryStarted;
    }

    public void setOnDiscoveryFinished(OnDiscoveryFinished onDiscoveryFinished) {
        this.onDiscoveryFinished = onDiscoveryFinished;
    }

    public void setOnDeviceFound(OnDeviceFound onDeviceFound) {
        this.onDeviceFound = onDeviceFound;
    }

    public void setOnDevicePaired(OnDevicePaired onDevicePaired) {
        this.onDevicePaired = onDevicePaired;
    }

    public void setOnDeviceUnpaired(OnDeviceUnpaired onDeviceUnpaired) {
        this.onDeviceUnpaired = onDeviceUnpaired;
    }

    public void setOnDiscoveryError(OnDiscoveryError onDiscoveryError) {
        this.onDiscoveryError = onDiscoveryError;
    }

    public void setOnDeviceConnected(OnDeviceConnected onDeviceConnected) {
        this.onDeviceConnected = onDeviceConnected;
    }

    public void setOnDeviceDisconnected(OnDeviceDisconnected onDeviceDisconnected) {
        this.onDeviceDisconnected = onDeviceDisconnected;
    }

    private void setOnRawMessage(OnMessage onRawMessage) {
        this.onRawMessage = onRawMessage;
    }

    public void addOnMessages(OnMessage onMessage) {
        if (!this.onMessages.contains(onMessage)) {
            this.onMessages.add(onMessage);
        }
        Log.e("Add on message size ", "" + this.onMessages.size());
    }

    public void removeOnMessages(OnMessage onMessages) {
        this.onMessages.remove(onMessages);
    }

    public void setOnDeviceError(OnDeviceError onDeviceError) {
        this.onDeviceError = onDeviceError;
    }

    public void setOnConnectError(OnConnectError onConnectError) {
        this.onConnectError = onConnectError;
    }

    public void setCallbackOnUI(Activity activity) {
        this.activity = activity;
        this.runOnUi = true;
    }

    public void onActivityResult(int requestCode, final int resultCode) {
        if (onUserDeniedActivation != null) {
            if (requestCode == REQUEST_ENABLE_BT) {
                ThreadHelper.run(runOnUi, activity, () -> {
                    if (resultCode == Activity.RESULT_CANCELED) {
                        onUserDeniedActivation.onEvent();
                    }
                });
            }
        }
    }

    public void connectToAddress(String address, boolean insecureConnection) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        connectToDevice(device, insecureConnection);
    }

    public void connectToAddress(String address) {
        connectToAddress(address, false);
    }

    public void connectToName(String name, boolean insecureConnection) {
        for (BluetoothDevice blueDevice : bluetoothAdapter.getBondedDevices()) {
            if (blueDevice.getName().equals(name)) {
                connectToDevice(blueDevice, insecureConnection);
                return;
            }
        }
    }

    public void connectToName(String name) {
        connectToName(name, false);
    }

    public void connectToDevice(BluetoothDevice device, boolean insecureConnection) {
        new ConnectThread(device, insecureConnection).start();
    }

    public void connectToDevice(BluetoothDevice device) {
        connectToDevice(device, false);
    }

    public void disconnect() {
        suppressDisconnectCallback();
        try {
            socket.close();
        } catch (final IOException e) {
            if (onDeviceError != null) {
                ThreadHelper.run(runOnUi, activity, () -> onDeviceError.onEvent(e.getMessage()));
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void send(String msg, String charset) {
        try {
            if (!TextUtils.isEmpty(charset)) {
                out.write(msg.getBytes(charset));//Eg: "US-ASCII"
            } else {
                out.write(msg.getBytes());//Sending as UTF-8 as default
            }
        } catch (final IOException e) {
            connected = false;
            ThreadHelper.run(true, activity, () -> {
                if (!isSuppressDisconnectCallback) {
                    new AlertDialog.Builder(context).setMessage(R.string.lost_connection_prompt).setPositiveButton(R.string.yes, (dialog, which) -> {
                        dialog.dismiss();
                        connectToDevice(device);
                        loadingDialog.show();
                    }).setNegativeButton(R.string.no, (dialog, which) -> {
                        dialog.dismiss();
                    }).create().show();
                    isSuppressDisconnectCallback = false;
                }
            });

            try {
                LogDatabase.getInstance(context).logDao().addLog(new AppLog("device", "Disconnected with " + device.getName() + ", " + device.getAddress()));
            } catch (Exception ignored) {

            }

            if (onDeviceDisconnected != null) {
                ThreadHelper.run(runOnUi, activity, () -> {
                    onDeviceDisconnected.onEvent(device, e.getMessage());

                    try {
                        tempHumidHandler.removeCallbacks(tempHumidRunnable);
                    } catch (Exception ignored) {

                    }

                    Intent stopIntent = new Intent(context, BluetoothService.class);
                    stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                    context.startService(stopIntent);
                });
            }
        }
    }

    public void send(String msg) {
        send(msg, null);
    }

    public List<BluetoothDevice> getPairedDevices() {
        return new ArrayList<>(bluetoothAdapter.getBondedDevices());
    }

    public void startScanning() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        context.registerReceiver(scanReceiver, filter);
        bluetoothAdapter.startDiscovery();
    }

    public void stopScanning() {
//        context.unregisterReceiver(scanReceiver);
        bluetoothAdapter.cancelDiscovery();
    }

    public void pair(BluetoothDevice device) {
        context.registerReceiver(pairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        devicePair = device;
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (final Exception e) {
            if (onDiscoveryError != null) {
                ThreadHelper.run(runOnUi, activity, () -> onDiscoveryError.onEvent(e.getMessage()));
            }
        }
    }

    public void unpair(BluetoothDevice device) {
        context.registerReceiver(pairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        devicePair = device;
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (final Exception e) {
            if (onDiscoveryError != null) {
                ThreadHelper.run(runOnUi, activity, () -> onDiscoveryError.onEvent(e.getMessage()));
            }
        }
    }

    public void suppressDisconnectCallback() {
        this.isSuppressDisconnectCallback = true;
    }

    void connectToDevice(String deviceName, String deviceAddress) {
        if (TextUtils.isEmpty(deviceName)) {
            return;
        }
        deviceState = DeviceState.UNAUTHENTICATED;
        loadingDialog = new AlertDialog.Builder(context).setMessage(context.getString(R.string.connecting_to) + ' ' + deviceName + "...").setView(R.layout.progressbar_layout).setNegativeButton(R.string.cancel, (dialog, which) -> {
            dialog.dismiss();
            try {
                disconnect();
            } catch (Exception ignored) {

            }
            Toast.makeText(context, context.getString(R.string.connection_canceled), Toast.LENGTH_LONG).show();
        }).setCancelable(false).create();
        loadingDialog.show();

        authDialog = new AlertDialog.Builder(context).setMessage(R.string.authenticating).setPositiveButton(R.string.cancel, (dialog, which) -> {
            dialog.dismiss();
            deviceState = DeviceState.UNAUTHENTICATED;
            disconnect();
        }).setView(R.layout.progressbar_layout).setCancelable(false).create();

        setOnRawMessage(message -> {
            if (deviceState == DeviceState.UNAUTHENTICATED) {
                if (message.startsWith("AUTH OK")) {
                    deviceState = DeviceState.AUTHENTICATING;
                    send("AOK\n");
                }
            } else if (deviceState == DeviceState.AUTHENTICATING) {
                if (message.startsWith("AOK")) {
                    deviceState = DeviceState.AUTHENTICATED;
                    TinyDB tinyDB = new TinyDB(context);
                    ArrayList<String> authenticatedDevices = tinyDB.getListString(context.getString(R.string.key_authenticated_devices));
                    ArrayList<String> listUnique = new ArrayList<>();
                    for (int i = 0; i < authenticatedDevices.size(); i++) {
                        try {
                            JSONObject obj = new JSONObject(authenticatedDevices.get(i));
                            if (!deviceAddress.equals(obj.getString("address"))) {
                                listUnique.add(authenticatedDevices.get(i));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    String deviceAlias = new ListDataWrapper(device.getName(), deviceAddress, true, System.currentTimeMillis()).toString();
                    listUnique.add(deviceAlias);
                    tinyDB.putListString(context.getString(R.string.key_authenticated_devices), listUnique);
                    Log.e(TAG, "Pref: " + authenticatedDevices.toString());
                    tinyDB.putString(context.getString(R.string.key_last_connected_device), deviceAlias);
                    if (onDeviceAuthenticated != null) {
                        onDeviceAuthenticated.onEvent(device);
                    }
                    LogDatabase.getInstance(context).logDao().addLog(new AppLog("device", "Connected with " + device.getName() + ", " + device.getAddress()));

                    ThreadHelper.run(true, activity, () -> {
                        authDialog.dismiss();
                        if (onDeviceConnected != null) {
                            onDeviceConnected.onEvent(device);
                        }

                        Toast.makeText(activity, "Authenticated", Toast.LENGTH_LONG).show();
                    });
                }
            } else if (deviceState == DeviceState.AUTHENTICATED) {
                if (message.startsWith("dht")) {
                    String[] parts = message.split(",");

                    Intent startIntent = new Intent(context, BluetoothService.class);
                    startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                    startIntent.putExtra("temperature", Float.valueOf(parts[2]));
                    startIntent.putExtra("humidity", Float.valueOf(parts[1]));
                    context.startService(startIntent);
                }
                for (OnMessage onMessage : onMessages) {
                    onMessage.onEvent(message);
                }
            }
            Log.e(TAG, "message " + message);
        });

        try {
            connectToAddress(deviceAddress);
            loadingDialog.show();
        } catch (Exception ignored) {
            loadingDialog.dismiss();
        }
    }

    public void setOnDeviceAuthenticated(OnDeviceAuthenticated onDeviceAuthenticated) {
        this.onDeviceAuthenticated = onDeviceAuthenticated;
    }

    public boolean isScanning() {
        return isScanning;
    }

    private class ReceiveThread extends Thread implements Runnable {
        public void run() {
            String msg;
            try {
                while ((msg = input.readLine()) != null) {
                    if (onRawMessage != null) {
                        final String msgCopy = msg;
                        ThreadHelper.run(runOnUi, activity, () -> onRawMessage.onEvent(msgCopy));
                    }
                }
            } catch (final IOException e) {
                connected = false;
                ThreadHelper.run(true, activity, () -> {
                    if (!isSuppressDisconnectCallback) {
                        new AlertDialog.Builder(context).setMessage(R.string.lost_connection_prompt).setPositiveButton(R.string.yes, (dialog, which) -> {
                            dialog.dismiss();
                            connectToDevice(device);
                            loadingDialog.show();
                        }).setNegativeButton(R.string.no, (dialog, which) -> {
                            dialog.dismiss();
                        }).create().show();
                        isSuppressDisconnectCallback = false;
                    }
                });
                LogDatabase.getInstance(context).logDao().addLog(new AppLog("device", "Disconnected with " + device.getName() + ", " + device.getAddress()));

                if (onDeviceDisconnected != null) {
                    ThreadHelper.run(runOnUi, activity, () -> {
                        onDeviceDisconnected.onEvent(device, e.getMessage());

                        try {
                            tempHumidHandler.removeCallbacks(tempHumidRunnable);
                        } catch (Exception ignored) {

                        }

                        Intent stopIntent = new Intent(context, BluetoothService.class);
                        stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                        context.startService(stopIntent);
                    });
                }
            }
        }
    }

    private class ConnectThread extends Thread {
        ConnectThread(BluetoothDevice device, boolean insecureConnection) {
            BluetoothHandler.this.device = device;
            try {
                if (insecureConnection) {
                    BluetoothHandler.this.socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                } else {
                    BluetoothHandler.this.socket = device.createRfcommSocketToServiceRecord(uuid);
                }
            } catch (IOException e) {
                if (onDeviceError != null) {
                    onDeviceError.onEvent(e.getMessage());
                }
            }
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                try {
                    socket.connect();
                } catch (Exception e) {
                    socket.connect();
                }
                out = socket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;

                new ReceiveThread().start();

                ThreadHelper.run(true, activity, () -> {
                    loadingDialog.dismiss();
                    authDialog.show();
                    new Handler().postDelayed(() -> {
                        send("AUT\n");
                    }, 1000);
                    new Handler().postDelayed(() -> {
                        if (deviceState != DeviceState.AUTHENTICATED) {
                            ThreadHelper.run(true, activity, () -> {
                                Toast.makeText(context, "Authentication failed", Toast.LENGTH_LONG).show();
                                disconnect();
                                authDialog.dismiss();
                                deviceState = DeviceState.UNAUTHENTICATED;
                            });
                        }
                    }, 5000);
                    Toast.makeText(context, context.getString(R.string.connection_success), Toast.LENGTH_LONG).show();
                });
//                if (onDeviceConnected != null) {
//                    ThreadHelper.run(runOnUi, activity, () -> {
//                        onDeviceConnected.onEvent(device);
//                    });
//                }
            } catch (final IOException e) {
                e.printStackTrace();
                ThreadHelper.run(true, activity, () -> {
                    loadingDialog.dismiss();
                    new AlertDialog.Builder(context).setMessage(R.string.connection_failed_try_again).setCancelable(false).setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss()).create().show();
                });
                if (onConnectError != null) {
                    ThreadHelper.run(runOnUi, activity, () -> onConnectError.onEvent(device, e.getMessage()));
                }

                try {
                    socket.close();
                } catch (final IOException closeException) {
                    if (onDeviceError != null) {
                        ThreadHelper.run(runOnUi, activity, () -> onDeviceError.onEvent(closeException.getMessage()));
                    }
                }
            }
        }
    }
}
