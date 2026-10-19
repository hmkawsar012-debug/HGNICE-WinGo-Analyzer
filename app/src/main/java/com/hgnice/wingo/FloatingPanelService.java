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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingPanelService extends Service {

    private WindowManager wm;
    private LinearLayout panel;
    private WindowManager.LayoutParams params;

    private int cyan = Color.rgb(35, 230, 210);
    private int dark = Color.rgb(15, 22, 32);

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showLogin();
    }

    private LinearLayout makePanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(22, 18, 22, 18);
        box.setBackgroundColor(dark);
        return box;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, 8, 0, 8);
        return t;
    }

    private void displayPanel(LinearLayout view) {
        if (panel != null && panel.isAttachedToWindow()) {
            wm.removeView(panel);
        }

        panel = view;

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 12;
        params.y = 160;

        // ইনপুট লেখার জন্য প্যানেলকে ফোকাসযোগ্য রাখা হবে।
        params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        wm.addView(panel, params);
    }

    private void showLogin() {
        LinearLayout box = makePanel();

        box.addView(text("HGNICE WINGO", 22, cyan));
        box.addView(text("নিজস্ব অ্যাকাউন্টে লগইন", 15, Color.WHITE));

        EditText username = new EditText(this);
        username.setSingleLine(true);
        username.setHint("Username তৈরি করুন");
        username.setTextColor(Color.WHITE);
        username.setHintTextColor(Color.LTGRAY);
        box.addView(username);

        EditText password = new EditText(this);
        password.setSingleLine(true);
        password.setHint("Password তৈরি করুন");
        password.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setTextColor(Color.WHITE);
        password.setHintTextColor(Color.LTGRAY);
        box.addView(password);

        Button login = new Button(this);
        login.setText("LOGIN");
        box.addView(login);

        TextView note = text(
                "প্রথমবার একটি Username ও Password দিন।\n"
                + "এটি আপাতত স্থানীয় ডেমো লগইন।",
                12, Color.LTGRAY);
        box.addView(note);

        login.setOnClickListener(v -> {
            String u = username.getText().toString().trim();
            String p = password.getText().toString();

            if (u.isEmpty() || p.length() < 4) {
                Toast.makeText(this,
                        "Username দিন এবং অন্তত ৪ অক্ষরের Password দিন",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // ডেমো: সার্ভার-ভিত্তিক অ্যাকাউন্ট যাচাই এখনো নেই।
            showDashboard(u);
        });

        displayPanel(box);
    }

    private void showDashboard(String username) {
        LinearLayout box = makePanel();

        box.addView(text("HGNICE WINGO", 22, cyan));
        box.addView(text("স্বাগতম, " + username, 15, Color.WHITE));
        box.addView(text("LIVE SCREEN", 15, cyan));
        box.addView(text(
                "স্ক্রিন বিশ্লেষণ চালু করতে\nমূল অ্যাপ থেকে অনুমতি দিন।",
                14, Color.WHITE));
        box.addView(text(
                "BIG: --     SMALL: --\n"
                + "Period: শনাক্ত হয়নি\n"
                + "History: অপেক্ষমাণ",
                14, Color.GREEN));
        box.addView(text(
                "ফলাফল নিশ্চিতভাবে অনুমান করা যায় না।",
                12, Color.LTGRAY));

        Button logout = new Button(this);
        logout.setText("LOG OUT");
        box.addView(logout);
        logout.setOnClickListener(v -> showLogin());

        displayPanel(box);
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
