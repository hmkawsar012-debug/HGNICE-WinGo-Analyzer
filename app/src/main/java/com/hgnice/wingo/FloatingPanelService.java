package com.hgnice.wingo;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingPanelService extends Service {

    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private LinearLayout panel;
    private TextView info;
    private boolean minimized = false;

    private final int cyan = Color.rgb(35, 230, 210);
    private final int dark = Color.rgb(12, 15, 27);

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showPanel();
    }

    private TextView makeText(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(8, 8, 8, 8);
        return t;
    }

    private LinearLayout makeBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(12, 10, 12, 10);
        box.setBackgroundColor(dark);
        return box;
    }

    private void showPanel() {
        panel = makeBox();

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = makeText("HGNICE WINGO", 17, cyan);
        bar.addView(title, new LinearLayout.LayoutParams(
                0, -2, 1));

        Button minimize = new Button(this);
        minimize.setText("−");
        bar.addView(minimize);

        Button close = new Button(this);
        close.setText("×");
        bar.addView(close);

        panel.addView(bar);

        info = makeText(
                "LIVE HISTORY\n"
                + "স্ক্রিন ক্যাপচার চালু হলে\n"
                + "শনাক্ত করা লেখা এখানে আসবে।",
                14, Color.WHITE);
        panel.addView(info);

        panel.addView(makeText(
                "BIG / SMALL: অপেক্ষমাণ\n"
                + "Period: অপেক্ষমাণ\n"
                + "ফলাফল নিশ্চিত নয়।",
                14, cyan));

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 30;
        params.y = 180;

        // শিরোনাম ধরে প্যানেল টেনে সরানো যাবে।
        title.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x;
                        startY = params.y;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = startX
                                + (int) (event.getRawX() - downX);
                        params.y = startY
                                + (int) (event.getRawY() - downY);
                        if (panel != null && panel.isAttachedToWindow()) {
                            wm.updateViewLayout(panel, params);
                        }
                        return true;
                }
                return false;
            }
        });

        minimize.setOnClickListener(v -> showMiniButton());
        close.setOnClickListener(v -> stopSelf());

        wm.addView(panel, params);
    }

    private void showMiniButton() {
        if (panel != null && panel.isAttachedToWindow()) {
            wm.removeView(panel);
        }

        minimized = true;

        TextView mini = makeText("HG", 18, cyan);
        mini.setGravity(Gravity.CENTER);
        mini.setBackgroundColor(dark);

        params.width = dp(58);
        params.height = dp(58);
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        mini.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;
            private boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x;
                        startY = params.y;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - downX);
                        int dy = (int) (event.getRawY() - downY);
                        if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                            moved = true;
                        }
                        params.x = startX + dx;
                        params.y = startY + dy;
                        wm.updateViewLayout(mini, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            wm.removeView(mini);
                            minimized = false;
                            showPanel();
                        }
                        return true;
                }
                return false;
            }
        });

        wm.addView(mini, params);
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onDestroy() {
        if (wm != null && panel != null
                && panel.isAttachedToWindow()) {
            wm.removeView(panel);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
                                }
