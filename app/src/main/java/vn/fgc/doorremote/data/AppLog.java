package vn.fgc.doorremote.data;

import android.text.format.DateFormat;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Calendar;

@Entity(tableName = "applog")
public class AppLog {
    @PrimaryKey(autoGenerate = true)
    public int logId;
    @ColumnInfo(name = "type")
    public String type;
    @ColumnInfo(name = "timestamp")
    public long timestamp;
    @ColumnInfo(name = "content")
    public String content;

    @Ignore
    public AppLog(String type, String content) {
        this.type = type;
        this.timestamp = Calendar.getInstance().getTime().getTime();
        this.content = content;
    }

    public AppLog(int logId, long timestamp, String type, String content) {
        this.logId = logId;
        this.type = type;
        this.timestamp = timestamp;
        this.content = content;
    }

    public String toView() {
        return String.format("[%s]: %s", DateFormat.format("HH:mm:ss dd-MM-yyyy", timestamp).toString(), content);
    }

}
