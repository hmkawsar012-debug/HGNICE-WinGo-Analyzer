package com.hgnice.wingo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class ScreenCaptureService extends Service {

    private static final String CHANNEL_ID = "hgnice_screen_capture";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "HGNICE Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("HGNICE WinGo Analyzer")
                .setContentText(
                    "স্ক্রিন বিশ্লেষণের অনুমতি ও প্রস্তুতির অপেক্ষায়"
                )
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
        } else {
            notification = new Notification.Builder(this)
                .setContentTitle("HGNICE WinGo Analyzer")
                .setContentText(
                    "স্ক্রিন বিশ্লেষণের অনুমতি ও প্রস্তুতির অপেক্ষায়"
                )
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build();
        }

        startForeground(NOTIFICATION_ID, notification);

        // পরের ধাপে MediaProjection দিয়ে স্ক্রিন ক্যাপচার যুক্ত হবে।
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
