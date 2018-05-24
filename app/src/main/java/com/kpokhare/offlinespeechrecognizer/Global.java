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
}
