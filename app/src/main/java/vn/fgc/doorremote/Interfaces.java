package vn.fgc.doorremote;

import android.bluetooth.BluetoothDevice;

interface OnBluetoothTurningOn {
    void onEvent();
}

interface OnBluetoothOn {
    void onEvent();
}

interface OnBluetoothTurningOff {
    void onEvent();
}

interface OnBluetoothOff {
    void onEvent();
}

interface OnUserDeniedActivation {
    void onEvent();
}

interface OnDiscoveryStarted {
    void onEvent();
}

interface OnDiscoveryFinished {
    void onEvent();
}

interface OnDeviceFound {
    void onEvent(BluetoothDevice device);
}

interface OnDevicePaired {
    void onEvent(BluetoothDevice device);
}

interface OnDeviceUnpaired {
    void onEvent(BluetoothDevice device);
}

interface OnDeviceAuthenticated {
    void onEvent(BluetoothDevice device);
}

interface OnDiscoveryError {
    void onEvent(String message);
}

interface OnDeviceConnected {
    void onEvent(BluetoothDevice device);
}

interface OnDeviceDisconnected {
    void onEvent(BluetoothDevice device, String message);
}

interface OnMessage {
    void onEvent(String message);
}

interface OnDeviceError {
    void onEvent(String message);
}

interface OnConnectError {
    void onEvent(BluetoothDevice device, String message);
}

interface OnListItemButtonClickListener {
    void onEvent(ListDataWrapper device);
}