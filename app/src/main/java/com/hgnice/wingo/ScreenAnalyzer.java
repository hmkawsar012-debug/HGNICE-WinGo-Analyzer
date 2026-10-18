package com.hgnice.wingo;

public class ScreenAnalyzer {

    public static String analyze(String history) {
        if (history == null || history.trim().isEmpty()) {
            return "আগের ফলাফল যোগ করুন";
        }

        String[] results = history.trim().split("\\s+");
        int big = 0;
        int small = 0;

        int start = Math.max(0, results.length - 10);

        for (int i = start; i < results.length; i++) {
            String result = results[i].trim();

            if (result.equalsIgnoreCase("BIG")) {
                big++;
            } else if (result.equalsIgnoreCase("SMALL")) {
                small++;
            }
        }

        if (big > small) {
            return "ধারণা: BIG";
        } else if (small > big) {
            return "ধারণা: SMALL";
        } else {
            return "ধারণা: সমান — নিশ্চিত ধারণা নেই";
        }
    }
}
