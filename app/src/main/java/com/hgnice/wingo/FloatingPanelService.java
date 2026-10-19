package com.hgnice.wingo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public class FloatingPanelService extends Service {

    private WindowManager windowManager;
    private LinearLayout panel;
    private TextView info;
    private TextView stats;
    private boolean receiverRegistered = false;

    private final BroadcastReceiver textReceiver =
            new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            String text = intent.getStringExtra(
                    ScreenCaptureService.OCR_EXTRA);

            if (text == null || text.trim().isEmpty()) return;

            info.setText("স্ক্রিন থেকে শনাক্ত করা লেখা:\n\n" + text);

            String upper = text.toUpperCase(Locale.ROOT);
            int big = countWord(upper, "BIG");
            int small = countWord(upper, "SMALL");

            stats.setText(
                    "শনাক্ত BIG: " + big
                    + "\nশনাক্ত SMALL: " + small
                    + "\n\nএটি স্ক্রিনের লেখার হিসাব।"
                    + "\nপরবর্তী ফলাফলের নিশ্চয়তা নয়।"
            );
        }
    };

    private int countWord(String text, String word) {
        int count = 0;
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                .compile("\\b" + word + "\\b")
                .matcher(text);

        while (matcher.find()) count++;
        return count;
    }

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
        panel.setPadding(20, 16, 20, 16);
        panel.setBackgroundColor(Color.rgb(25, 25, 45));

        TextView title = new TextView(this);
        title.setText("HGNICE WINGO");
        title.setTextColor(Color.CYAN);
        title.setTextSize(18);
        panel.addView(title);

        ScrollView scroll = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        info = new TextView(this);
        info.setText(
                "LIVE SCREEN TEXT\n"
                + "স্ক্রিন ক্যাপচারের অনুমতি দিন।\n"
                + "তারপর এখানে শনাক্ত করা লেখা দেখা যাবে।"
        );
        info.setTextColor(Color.WHITE);
        info.setTextSize(14);
        content.addView(info);

        stats = new TextView(this);
        stats.setTextColor(Color.GREEN);
        stats.setTextSize(14);
        stats.setPadding(0, 12, 0, 0);
        content.addView(stats);

        scroll.addView(content);
        panel.addView(scroll);

        int overlayType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType =
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
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

        IntentFilter filter = new IntentFilter(
                ScreenCaptureService.OCR_ACTION);

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                    textReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(textReceiver, filter);
        }
        receiverRegistered = true;
    }

    @Override
    public void onDestroy() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(textReceiver);
            } catch (Exception ignored) {
            }
            receiverRegistered = false;
        }

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
