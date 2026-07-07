package com.qualcomm.qce.allplay.controllersdk;

import android.content.Context;
import android.net.wifi.WifiManager;

public class OnboardingManager {
    private static OnboardingManager sInstance;

    public static OnboardingManager getInstance(Context ctx, WifiManager wifi, PlayerManager pm) {
        if (sInstance == null) sInstance = new OnboardingManager();
        return sInstance;
    }

    public void start() {}
    public void stop() {}
    public void startOnboardingScan() {}
    public void stopOnboardingScan() {}
    public boolean isOnboarding() { return false; }
    public boolean isOnboardingInProgress() { return false; }
    public boolean isInConnection() { return false; }
    public String getCurrentSSID() { return null; }
    public String getOnboardingDeviceID() { return null; }
    public void setOnboardingDeviceID(String id) {}
    public void setConnectedOnboardee(IOnboardee o) {}
    public void finishOnboarding(boolean success) {}
}
