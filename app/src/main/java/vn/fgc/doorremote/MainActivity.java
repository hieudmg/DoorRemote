package vn.fgc.doorremote;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_ALL = 1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private BluetoothHandler bluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionCheck();

        if (bluetooth == null) {
            bluetooth = new BluetoothHandler(this);
        }

        findViewById(R.id.button_connect).setOnClickListener(v -> {
            ConnectFragment connectFragment = new ConnectFragment();
            connectFragment.setService(bluetooth);

            connectFragment.show(this.getSupportFragmentManager(), null);
        });

        findViewById(R.id.button_up).setOnClickListener(v -> {
            if (bluetooth.isConnected()) {
                bluetooth.send(Functions.getInstance().buildCommand(Constants.DOOR_COMMANDS.UP));
            }
        });
        findViewById(R.id.button_down).setOnClickListener(v -> {
            if (bluetooth.isConnected()) {
                bluetooth.send(Functions.getInstance().buildCommand(Constants.DOOR_COMMANDS.DOWN));
            }
        });
        findViewById(R.id.button_lock).setOnClickListener(v -> {
            if (bluetooth.isConnected()) {
                bluetooth.send(Functions.getInstance().buildCommand(Constants.DOOR_COMMANDS.LOCK));
            }
        });
        findViewById(R.id.button_stop).setOnClickListener(v -> {
            if (bluetooth.isConnected()) {
                bluetooth.send(Functions.getInstance().buildCommand(Constants.DOOR_COMMANDS.STOP));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetooth.onStart();
        bluetooth.enable();
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetooth.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetooth.suppressDisconnectCallback();
        bluetooth.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bluetooth.onActivityResult(requestCode, resultCode);
    }


    void permissionCheck() {
        // Here, thisActivity is the current activity
        if (!hasPermissions(PERMISSIONS)) {
            new AlertDialog.Builder(MainActivity.this).setMessage(R.string.permission_request_message).setCancelable(false).setPositiveButton(R.string.ok, (dialog, which) -> {
                dialog.dismiss();
                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, PERMISSIONS_ALL);
            }).create().show();
        }
    }

    public boolean hasPermissions(String... permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults
    ) {
        if (requestCode == PERMISSIONS_ALL) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                permissionCheck();
            }
        }
    }

}
