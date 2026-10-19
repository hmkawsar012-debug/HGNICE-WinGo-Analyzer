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
    private View miniButton;
    private boolean minimized = false;
    private boolean receiverRegistered = false;

    private String latestOcrText = "স্ক্রিন ক্যাপচারের অপেক্ষায়";

    private final int cyan = Color.rgb(35, 230, 210);
    private final int dark = Color.rgb(12, 15, 27);

    private final BroadcastReceiver ocrReceiver =
            new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ScreenCaptureService.ACTION_OCR_UPDATE.equals(
                    intent.getAction())) {
                return;
            }

            String text = intent.getStringExtra(
                    ScreenCaptureService.EXTRA_OCR_TEXT);

            if (text != null && !text.trim().isEmpty()) {
                latestOcrText = text;

                if (info != null) {
                    String display = text.length() > 700
                            ? text.substring(0, 700) + "..."
                            : text;

                    info.setText("LIVE SCREEN TEXT\n" + display);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        IntentFilter filter = new IntentFilter(
                ScreenCaptureService.ACTION_OCR_UPDATE);

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                    ocrReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(ocrReceiver, filter);
        }
        receiverRegistered = true;

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

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private WindowManager.LayoutParams newParams() {
        WindowManager.LayoutParams p =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        overlayType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT);

        p.gravity = Gravity.TOP | Gravity.START;
        p.x = 30;
        p.y = 180;
        return p;
    }

    private void showPanel() {
        panel = makeBox();

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = makeText("HGNICE WINGO", 17, cyan);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button minimize = new Button(this);
        minimize.setText("−");
        bar.addView(minimize);

        Button close = new Button(this);
        close.setText("×");
        bar.addView(close);

        panel.addView(bar);

        info = makeText("LIVE SCREEN TEXT\n" + latestOcrText,
                14, Color.WHITE);
        panel.addView(info);

        TextView status = makeText(
                "BIG / SMALL: এখনো যাচাই হয়নি\n"
                + "Period: শনাক্ত হওয়ার অপেক্ষায়\n"
                + "এটি নিশ্চিত ভবিষ্যদ্বাণী নয়।",
                13, cyan);
        panel.addView(status);

        params = newParams();

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

        panel = null;
        info = null;
        minimized = true;

        TextView mini = makeText("HG", 18, cyan);
        mini.setGravity(Gravity.CENTER);
        mini.setBackgroundColor(dark);
        miniButton = mini;

        params.width = dp(58);
        params.height = dp(58);

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
                            if (mini.isAttachedToWindow()) {
                                wm.removeView(mini);
                            }
                            miniButton = null;
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
        if (wm != null) {
            if (panel != null && panel.isAttachedToWindow()) {
                wm.removeView(panel);
            }

            if (miniButton != null && miniButton.isAttachedToWindow()) {
                wm.removeView(miniButton);
            }
        }

        if (receiverRegistered) {
            try {
                unregisterReceiver(ocrReceiver);
            } catch (IllegalArgumentException ignored) {
            }
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
                        }
