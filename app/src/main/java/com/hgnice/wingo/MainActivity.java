package com.hgnice.wingo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import java.util.ArrayList;

public class MainActivity extends Activity {
    ArrayList<String> results = new ArrayList<>();
    LinearLayout page;
    EditText entry;
    TextView counts, guess, history;
    private static final int SCREEN_CAPTURE_REQUEST = 1001;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(20, 20, 20, 20);
        page.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("HGNICE WinGo Analyzer");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        page.addView(title);

        Button panelButton = new Button(this);
        panelButton.setText("চালু করুন: ভাসমান প্যানেল");
        page.addView(panelButton);

        panelButton.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
            } else {
                startService(new Intent(this, FloatingPanelService.class));
                Toast.makeText(this, "ভাসমান প্যানেল চালুর অনুরোধ পাঠানো হয়েছে",
                    Toast.LENGTH_SHORT).show();
            }
        });

        Button captureButton = new Button(this);
        captureButton.setText("স্ক্রিন বিশ্লেষণের অনুমতি দিন");
        page.addView(captureButton);

        captureButton.setOnClickListener(v -> {
            MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(
                    MEDIA_PROJECTION_SERVICE);
            startActivityForResult(
                manager.createScreenCaptureIntent(),
                SCREEN_CAPTURE_REQUEST
            );
        });

        entry = new EditText(this);
        entry.setHint("Big অথবা Small লিখুন");
        page.addView(entry);

        Button add = new Button(this);
        add.setText("ADD RESULT");
        page.addView(add);

        add.setOnClickListener(v -> {
            String s = entry.getText().toString().trim();

            if (s.equalsIgnoreCase("Big") ||
                    s.equalsIgnoreCase("Small")) {
                results.add(s.substring(0, 1).toUpperCase()
                    + s.substring(1).toLowerCase());
                entry.setText("");
                refresh();
            } else {
                entry.setError("Big অথবা Small লিখুন");
            }
        });

        Button undo = new Button(this);
        undo.setText("UNDO LAST");
        page.addView(undo);

        undo.setOnClickListener(v -> {
            if (!results.isEmpty()) {
                results.remove(results.size() - 1);
                refresh();
            }
        });

        counts = new TextView(this);
        counts.setTextColor(Color.BLACK);
        counts.setTextSize(18);
        page.addView(counts);

        guess = new TextView(this);
        guess.setTextColor(Color.BLUE);
        guess.setTextSize(22);
        guess.setPadding(0, 20, 0, 20);
        page.addView(guess);

        history = new TextView(this);
        history.setTextColor(Color.DKGRAY);
        history.setTextSize(16);
        page.addView(history);

        setContentView(page);
        refresh();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCREEN_CAPTURE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                Intent serviceIntent =
                    new Intent(this, ScreenCaptureService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);

                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                Toast.makeText(this,
                    "অনুমতি পাওয়া গেছে; পরের ধাপে ক্যাপচার যুক্ত হবে",
                    Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "স্ক্রিন অনুমতি দেওয়া হয়নি",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    void refresh() {
        int big = 0, small = 0;

        for (String s : results) {
            if (s.equals("Big")) big++;
            else small++;
        }

        counts.setText("মোট: " + results.size()
            + "\nBig: " + big + "\nSmall: " + small);

        if (results.isEmpty()) {
            guess.setText("পরবর্তী ধারণা: ফলাফল যোগ করুন");
        } else {
            int rb = 0, rs = 0;
            int start = Math.max(0, results.size() - 10);

            for (int i = start; i < results.size(); i++) {
                if (results.get(i).equals("Big")) rb++;
                else rs++;
            }

            String prediction;
            if (rb > rs) prediction = "Big";
            else if (rs > rb) prediction = "Small";
            else prediction = "স্পষ্ট দিক নেই";

            guess.setText("পরিসংখ্যানভিত্তিক ধারণা: " + prediction
                + "\nসাম্প্রতিক Big: " + rb
                + " | Small: " + rs
                + "\nএটি নিশ্চিত ভবিষ্যদ্বাণী নয়।");
        }

        StringBuilder sb = new StringBuilder("History:\n");
        for (int i = results.size() - 1; i >= 0; i--) {
            sb.append(results.get(i)).append("\n");
        }
        history.setText(sb.toString());
    }
    }
