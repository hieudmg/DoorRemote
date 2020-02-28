package vn.fgc.doorremote;

import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class ListDataWrapper {
    private String deviceAddress;
    private String deviceName;
    private boolean recognized;
    private long lastConnectedTime;

    public ListDataWrapper(String deviceName, String deviceAddress, boolean recognized, long lastConnectedTime) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.recognized = recognized;
        this.lastConnectedTime = lastConnectedTime;
    }

    public ListDataWrapper(JSONObject object) {
        try {
            this.deviceName = object.getString("name");
            this.deviceAddress = object.getString("address");
            this.recognized = object.getBoolean("recognized");
            this.lastConnectedTime = object.getLong("last_time");
        } catch (JSONException e) {
//            e.printStackTrace();
        }
    }

    public boolean sameAs(@Nullable ListDataWrapper obj) {
        if (obj != null) {
            return this.deviceAddress.equals(obj.deviceAddress);
        }
        return false;
    }

    String getName() {
        return deviceName;
    }

    String getAddress() {
        return deviceAddress;
    }

    long getLastConnectedTimestamp() {
        return lastConnectedTime;
    }

    String getLastConnectedTime() {
        return DateFormat.format("HH:mm dd/MM/yyyy", new Date(lastConnectedTime)).toString();
    }

    public boolean isRecognized() {
        return recognized;
    }

    @NonNull
    @Override
    public String toString() {
        return toJSON().toString();
    }

    @NonNull
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", deviceName);
            jsonObject.put("address", deviceAddress);
            jsonObject.put("recognized", recognized);
            jsonObject.put("last_time", lastConnectedTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
