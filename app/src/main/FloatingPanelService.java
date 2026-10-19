package com.hgnice.wingo;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingPanelService extends Service {

    private WindowManager windowManager;
    private LinearLayout panel;

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager)
                getSystemService(WINDOW_SERVICE);

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(24, 18, 24, 18);
        panel.setBackgroundColor(Color.rgb(25, 25, 45));

        TextView title = new TextView(this);
        title.setText("HGNICE WINGO");
        title.setTextColor(Color.CYAN);
        title.setTextSize(18);
        panel.addView(title);

        TextView info = new TextView(this);
        info.setText(
            "LIVE HISTORY\n" +
            "ফলাফল বিশ্লেষণের জন্য প্রস্তুত\n" +
            "স্ক্রিন ক্যাপচার এখনো যুক্ত হয়নি"
        );
        info.setTextColor(Color.WHITE);
        info.setTextSize(14);
        panel.addView(info);

        int overlayType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            overlayType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 20;
        params.y = 150;

        windowManager.addView(panel, params);
    }

    @Override
    public void onDestroy() {
        if (windowManager != null && panel != null
                && panel.isAttachedToWindow()) {
            windowManager.removeView(panel);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    }
