package com.kpokhare.offlinespeechrecognizer;

import android.app.Application;

public class Global extends Application {
    private String deviceID;

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public static int countWordsUsingSplit(String input) {
        //Log.d(LOG_TAG_DEBUG,"countWordsUsingSplit");
        if (input == null || input.isEmpty()) {
            return 0;
        }
        String[] words = input.split("\\s+");
        return words.length;
    }

    public static int CountOfSubstringInString(String string, String substring) {
        //Log.d(LOG_TAG_DEBUG,"CountOfSubstringInString");
        int count = 0;
        int idx = 0;
        while ((idx = string.indexOf(substring, idx)) != -1) {
            idx++;
            count++;
        }
        return count;
    }
}
