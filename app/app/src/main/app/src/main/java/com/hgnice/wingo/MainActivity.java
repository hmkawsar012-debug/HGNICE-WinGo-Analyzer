package com.hgnice.wingo;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.view.View;
import java.util.ArrayList;

public class MainActivity extends Activity {
    ArrayList<String> results = new ArrayList<>();
    LinearLayout layout;
    EditText input;
    TextView history, stats;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(Color.WHITE);
        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("HGNICE WinGo Analyzer");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        input = new EditText(this);
        input.setHint("Enter Big or Small");
        layout.addView(input);

        Button add = new Button(this);
        add.setText("Add Result");
        layout.addView(add);

        Button undo = new Button(this);
        undo.setText("Undo Last");
        layout.addView(undo);

        Button clear = new Button(this);
        clear.setText("Clear All");
        layout.addView(clear);

        stats = new TextView(this);
        stats.setTextColor(Color.BLACK);
        stats.setTextSize(18);
        layout.addView(stats);

        history = new TextView(this);
        history.setTextColor(Color.DKGRAY);
        history.setTextSize(16);
        layout.addView(history);

        add.setOnClickListener(v -> {
            String s = input.getText().toString().trim().toLowerCase();
            if (s.equals("big") || s.equals("বড়") || s.equals("বড়"))
                results.add("Big");
            else if (s.equals("small") || s.equals("ছোট"))
                results.add("Small");
            else {
                Toast.makeText(this, "Enter Big or Small", Toast.LENGTH_SHORT).show();
                return;
            }
            input.setText("");
            update();
        });

        undo.setOnClickListener(v -> {
            if (!results.isEmpty()) results.remove(results.size() - 1);
            update();
        });

        clear.setOnClickListener(v -> {
            results.clear();
            update();
        });

        setContentView(scroll);
        update();
    }

    void update() {
        int big = 0, small = 0;
        StringBuilder h = new StringBuilder("History (oldest first):\n");
        for (int i = 0; i < results.size(); i++) {
            String r = results.get(i);
            if (r.equals("Big")) big++; else small++;
            h.append(i + 1).append(". ").append(r).append("\n");
        }
        stats.setText("Total: " + results.size()
                + "\nBig: " + big + "\nSmall: " + small
                + "\nPast results are not a reliable prediction.");
        history.setText(h.toString());
    }
}
