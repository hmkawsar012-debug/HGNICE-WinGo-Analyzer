package com.hgnice.wingo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final int SCREEN_CAPTURE_REQUEST = 1001;
    private static final String PREFS = "hgnice_data";
    private static final String RESULTS_KEY = "results";
    private static final String PREDICTIONS_KEY = "predictions";

    private final ArrayList<String> results = new ArrayList<>();
    private final ArrayList<String> predictions = new ArrayList<>();

    private LinearLayout page;
    private EditText entry;
    private TextView counts;
    private TextView guess;
    private TextView history;
    private TextView ocrText;

    private SharedPreferences prefs;

    private final BroadcastReceiver ocrReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ScreenCaptureService.ACTION_OCR_UPDATE.equals(
                    intent.getAction())) {
                return;
            }

            String text = intent.getStringExtra(
                    ScreenCaptureService.EXTRA_OCR_TEXT);

            if (text != null && !text.trim().isEmpty() && ocrText != null) {
                String shown = text.trim();

                if (shown.length() > 1800) {
                    shown = shown.substring(0, 1800) + "...";
                }

                ocrText.setText("শনাক্ত করা স্ক্রিনের লেখা:\n" + shown);
            }
        }
    };

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadSavedData();
        buildPage();
        refresh();
        registerOcrReceiver();
    }

    private void buildPage() {
        ScrollView scroll = new ScrollView(this);

        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(16), dp(16), dp(24));
        page.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("HGNICE WinGo Analyzer");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(0, 110, 90));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(16));
        page.addView(title);

        Button panelButton = makeButton("ভাসমান প্যানেল চালু করুন");
        page.addView(panelButton);

        panelButton.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                startService(new Intent(this, FloatingPanelService.class));
                toast("ভাসমান প্যানেল চালুর অনুরোধ পাঠানো হয়েছে");
            }
        });

        Button captureButton = makeButton("লাইভ স্ক্রিন বিশ্লেষণ চালু করুন");
        page.addView(captureButton);

        captureButton.setOnClickListener(v -> {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(
                            MEDIA_PROJECTION_SERVICE);

            if (manager != null) {
                startActivityForResult(
                        manager.createScreenCaptureIntent(),
                        SCREEN_CAPTURE_REQUEST);
            } else {
                toast("স্ক্রিন ক্যাপচার চালু করা যাচ্ছে না");
            }
        });

        TextView inputTitle = makeText(
                "আগের ফলাফল যোগ করো: BIG অথবা SMALL", 17, Color.BLACK);
        page.addView(inputTitle);

        entry = new EditText(this);
        entry.setSingleLine(true);
        entry.setHint("উদাহরণ: BIG");
        page.addView(entry);

        Button addButton = makeButton("ফলাফল যোগ করুন");
        page.addView(addButton);

        addButton.setOnClickListener(v -> addResult());

        Button undoButton = makeButton("শেষ ফলাফল মুছুন");
        page.addView(undoButton);

        undoButton.setOnClickListener(v -> undoResult());

        Button clearButton = makeButton("সব ইতিহাস মুছুন");
        page.addView(clearButton);

        clearButton.setOnClickListener(v -> {
            results.clear();
            predictions.clear();
            saveData();
            refresh();
            toast("ইতিহাস মুছে দেওয়া হয়েছে");
        });

        counts = makeText("", 17, Color.BLACK);
        counts.setPadding(0, dp(16), 0, dp(8));
        page.addView(counts);

        guess = makeText("", 20, Color.rgb(0, 100, 180));
        guess.setPadding(0, dp(10), 0, dp(14));
        page.addView(guess);

        TextView accuracyTitle = makeText(
                "ধারণার সঠিকতার হিসাব", 18, Color.BLACK);
        page.addView(accuracyTitle);

        TextView accuracyInfo = makeText(
                "সঠিকতার হিসাব দেখতে নিচের ইতিহাসে আগের ধারণা "
                        + "ও প্রকৃত ফলাফল তুলনা করা হবে।", 14, Color.DKGRAY);
        page.addView(accuracyInfo);

        history = makeText("", 15, Color.DKGRAY);
        history.setPadding(0, dp(10), 0, dp(10));
        page.addView(history);

        ocrText = makeText(
                "স্ক্রিনের লেখা এখনো পাওয়া যায়নি।", 14, Color.BLACK);
        ocrText.setPadding(0, dp(12), 0, dp(12));
        page.addView(ocrText);

        TextView note = makeText(
                "সতর্কতা: BIG/SMALL ধারণা কেবল আগের ফলাফলের "
                        + "পরিসংখ্যান। এটি পরবর্তী ফলাফল নিশ্চিত করে না। "
                        + "গেমের ফলাফল এলোমেলো হতে পারে।", 14,
                Color.rgb(170, 40, 40));
        page.addView(note);

        scroll.addView(page);
        setContentView(scroll);
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        return button;
    }

    private TextView makeText(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private void addResult() {
        String value = entry.getText().toString().trim();

        if (value.equalsIgnoreCase("BIG")) {
            results.add("BIG");
        } else if (value.equalsIgnoreCase("SMALL")) {
            results.add("SMALL");
        } else {
            entry.setError("শুধু BIG অথবা SMALL লিখো");
            return;
        }

        entry.setText("");
        saveData();
        refresh();
    }

    private void undoResult() {
        if (results.isEmpty()) {
            toast("মুছে ফেলার মতো ফলাফল নেই");
            return;
        }

        results.remove(results.size() - 1);

        if (!predictions.isEmpty()) {
            predictions.remove(predictions.size() - 1);
        }

        saveData();
        refresh();
    }

    private String getPrediction() {
        if (results.isEmpty()) {
            return "ফলাফল যোগ করো";
        }

        int big = 0;
        int small = 0;
        int start = Math.max(0, results.size() - 10);

        for (int i = start; i < results.size(); i++) {
            if ("BIG".equals(results.get(i))) {
                big++;
            } else {
                small++;
            }
        }

        if (big > small) {
            return "BIG";
        } else if (small > big) {
            return "SMALL";
        }

        return "সমান";
    }

    private void refresh() {
        if (counts == null || guess == null || history == null) {
            return;
        }

        int big = 0;
        int small = 0;

        for (String result : results) {
            if ("BIG".equals(result)) {
                big++;
            } else {
                small++;
            }
        }

        counts.setText(
                "মোট ফলাফল: " + results.size()
                        + "\nBIG: " + big
                        + "\nSMALL: " + small);

        String prediction = getPrediction();

        if ("BIG".equals(prediction) || "SMALL".equals(prediction)) {
            guess.setText("পরিসংখ্যানভিত্তিক ধারণা: " + prediction
                    + "\nএটি নিশ্চিত ভবিষ্যদ্বাণী নয়।");
        } else {
            guess.setText("পরবর্তী ধারণা: " + prediction);
        }

        int checked = 0;
        int correct = 0;
        StringBuilder sb = new StringBuilder("সাম্প্রতিক ইতিহাস:\n");

        for (int i = results.size() - 1; i >= 0; i--) {
            sb.append(i + 1).append(". ")
                    .append(results.get(i));

            if (i < predictions.size()) {
                String oldPrediction = predictions.get(i);

                if ("BIG".equals(oldPrediction)
                        || "SMALL".equals(oldPrediction)) {
                    checked++;
                    if (oldPrediction.equals(results.get(i))) {
                        correct++;
                    }

                    sb.append(" | আগের ধারণা: ")
                            .append(oldPrediction)
                            .append(oldPrediction.equals(results.get(i))
                                    ? " ✓" : " ✗");
                }
            }

            sb.append("\n");
        }

        history.setText(sb.toString());

        // Accuracy is only counted for predictions saved before each result.
        // Predictions are recorded for the next result after each refresh.
        String accuracy = checked == 0
                ? "এখনো যাচাই করার মতো তথ্য নেই"
                : "মিলেছে: " + correct + "/" + checked
                    + " (" + (correct * 100 / checked) + "%)";

        // Append accuracy to the statistics area.
        counts.setText(counts.getText() + "\n" + accuracy);

        saveData();
    }

    private void saveData() {
        StringBuilder resultData = new StringBuilder();
        for (String s : results) {
            if (resultData.length() > 0) resultData.append(",");
            resultData.append(s);
        }

        StringBuilder predictionData = new StringBuilder();
        for (String s : predictions) {
            if (predictionData.length() > 0) predictionData.append(",");
            predictionData.append(s);
        }

        prefs.edit()
                .putString(RESULTS_KEY, resultData.toString())
                .putString(PREDICTIONS_KEY, predictionData.toString())
                .apply();
    }

    private void loadSavedData() {
        String savedResults = prefs.getString(RESULTS_KEY, "");
        String savedPredictions = prefs.getString(PREDICTIONS_KEY, "");

        if (!savedResults.isEmpty()) {
            for (String s : savedResults.split(",")) {
                if ("BIG".equals(s) || "SMALL".equals(s)) {
                    results.add(s);
                }
            }
        }

        if (!savedPredictions.isEmpty()) {
            for (String s : savedPredictions.split(",")) {
                predictions.add(s);
            }
        }

        while (predictions.size() < results.size()) {
            predictions.add("—");
        }

        while (predictions.size() > results.size()) {
            predictions.remove(predictions.size() - 1);
        }
    }

    private void registerOcrReceiver() {
        IntentFilter filter = new IntentFilter(
                ScreenCaptureService.ACTION_OCR_UPDATE);

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                    ocrReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(ocrReceiver, filter);
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SCREEN_CAPTURE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                Intent serviceIntent = new Intent(
                        this, ScreenCaptureService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);

                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                toast("স্ক্রিন ক্যাপচার চালু করার অনুরোধ পাঠানো হয়েছে");
            } else {
                toast("স্ক্রিন অনুমতি দেওয়া হয়নি");
            }
        }
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(ocrReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        super.onDestroy();
    }
                    }
