package vn.fgc.doorremote;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class BluetoothService extends Service {
    private static final String LOG_TAG = "BluetoothService";

    public BluetoothService() {
    }

    private static Intent getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Start Foreground Intent");

            Intent notificationIntent = getIntent(getApplicationContext(), MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            String channelId = "myid";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "name";
                String description = "des";
                int importance = NotificationManager.IMPORTANCE_LOW;
                NotificationChannel channel = new NotificationChannel(channelId, name, importance);
                channel.setDescription(description);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            String message = "";
            int temperature, humidity;
            temperature = (int) intent.getFloatExtra("temperature", -1000f);
            humidity = (int) intent.getFloatExtra("humidity", -1000f);

            if (temperature > -999f) {
                message += String.format("Temperature: %d℃", temperature);
            }
            if (humidity > -999f) {
                message += String.format("\nHumidity: %d%%", humidity);
            }

            Notification notification;
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
//                    .setContentTitle(getString(R.string.app_name))
                    .setTicker(getString(R.string.app_name))
                    .setContentTitle("Enjoy the trip!")
                    .setContentText(message)
                    .setSmallIcon(R.drawable.ic_smart_helmet)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true);
            notification = notificationBuilder.build();
            notification.defaults = 0;
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    notification);
        } else if (intent.getAction().equals(
                Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
