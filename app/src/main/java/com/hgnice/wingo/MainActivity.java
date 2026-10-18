package com.hgnice.wingo;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private final ArrayList<String> history = new ArrayList<>();
    private LinearLayout layout;
    private EditText input;
    private TextView stats, prediction, historyText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showApp();
    }

    private TextView makeText(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(Color.BLACK);
        t.setPadding(8, 12, 8, 12);
        return t;
    }

    private void showApp() {
        ScrollView scroll = new ScrollView(this);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(18, 20, 18, 20);
        layout.setBackgroundColor(Color.WHITE);
        scroll.addView(layout);
        setContentView(scroll);

        TextView title = makeText("HGNICE WinGo Analyzer", 24);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Enter Big or Small");
        layout.addView(input);

        Button add = new Button(this);
        add.setText("ADD RESULT");
        layout.addView(add);
        add.setOnClickListener(v -> addResult());

        Button undo = new Button(this);
        undo.setText("UNDO LAST");
        layout.addView(undo);
        undo.setOnClickListener(v -> {
            if (!history.isEmpty()) {
                history.remove(history.size() - 1);
                update();
            }
        });

        Button clear = new Button(this);
        clear.setText("CLEAR ALL");
        layout.addView(clear);
        clear.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
            .setTitle("Clear all results?")
            .setMessage("সব ফলাফল মুছে ফেলবে?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear", (d, w) -> {
                history.clear();
                update();
            }).show());

        stats = makeText("", 17);
        layout.addView(stats);

        prediction = makeText("", 20);
        layout.addView(prediction);

        historyText = makeText("", 16);
        layout.addView(historyText);

        update();
    }

    private void addResult() {
        String value = input.getText().toString()
            .trim().toLowerCase(Locale.ROOT);

        if (value.equals("big")) {
            history.add("Big");
        } else if (value.equals("small")) {
            history.add("Small");
        } else {
            Toast.makeText(this,
                "Big অথবা Small লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        input.setText("");
        update();
    }

    private void update() {
        int big = 0, small = 0;
        for (String s : history) {
            if (s.equals("Big")) big++;
            else small++;
        }

        stats.setText("Total: " + history.size()
            + "\nBig: " + big
            + "\nSmall: " + small);

        if (history.isEmpty()) {
            prediction.setText(
                "পরবর্তী ধারণা: ফলাফল যোগ করুন");
        } else {
            int recentBig = 0, recentSmall = 0;
            int start = Math.max(0, history.size() - 10);

            for (int i = start; i < history.size(); i++) {
                if (history.get(i).equals("Big")) recentBig++;
                else recentSmall++;
            }

            String guess;
            if (recentBig > recentSmall) {
                guess = "Big";
            } else if (recentSmall > recentBig) {
                guess = "Small";
            } else if (big > small) {
                guess = "Big";
            } else if (small > big) {
                guess = "Small";
            } else {
                guess = "সমান";
            }

            if (guess.equals("সমান")) {
                prediction.setText(
                    "পরবর্তী ধারণা: স্পষ্ট দিক নেই"
                    + "\nসাম্প্রতিক ফলাফল সমান");
            } else {
                prediction.setText(
                    "পরবর্তী রাউন্ডের প্রধান ধারণা: " + guess
                    + "\nসাম্প্রতিক ১০ ফলাফল: Big "
                    + recentBig + " | Small " + recentSmall);
            }
        }

        StringBuilder sb = new StringBuilder("History:\n");
        for (int i = history.size() - 1; i >= 0; i--) {
            sb.append(history.get(i));
            if (i > 0) sb.append("  →  ");
        }
        historyText.setText(sb.toString());
    }
}
