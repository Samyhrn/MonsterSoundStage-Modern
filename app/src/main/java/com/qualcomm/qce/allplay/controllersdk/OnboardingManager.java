package com.qualcomm.qce.allplay.controllersdk;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;

public class OnboardingManager {
    private static OnboardingManager sInstance;

    private IOnboardee mConnectedOnboardee;
    private IOnboardee mConnectingOnboardee;
    private Context mContext;
    private Handler mHandler;
    private java.util.Map<String, IOnboardee> mOnboardeesMap;
    private PlayerManager mPlayerManager;
    private IOnboardee mTargetConnectOnboardee;
    private WifiManager mWifiManager;
    private String mCurrentSSID;
    private String mOnboardingDeviceID;
    private boolean mScan = false;

    public static OnboardingManager getInstance(Context ctx, WifiManager wifi, PlayerManager pm) {
        if (sInstance == null) sInstance = new OnboardingManager(ctx, wifi, pm);
        return sInstance;
    }

    OnboardingManager(Context ctx, WifiManager wifi, PlayerManager pm) {
        mContext = ctx;
        mWifiManager = wifi;
        mPlayerManager = pm;
        mHandler = new Handler(ctx.getMainLooper());
        mOnboardeesMap = new java.util.concurrent.ConcurrentHashMap<>();
    }

    public void start() {}
    public void stop() {}

    public void startOnboardingScan() { mScan = true; }
    public void stopOnboardingScan() { mScan = false; }
    public boolean isOnboarding() { return false; }
    public boolean isOnboardingInProgress() { return false; }
    public boolean isInConnection() { return false; }
    public String getCurrentSSID() { return mCurrentSSID; }
    public String getOnboardingDeviceID() { return mOnboardingDeviceID; }
    public void setOnboardingDeviceID(String id) { mOnboardingDeviceID = id; }
    public void setConnectedOnboardee(IOnboardee o) { mConnectedOnboardee = o; }

    public void finishOnboarding(boolean success) {
        if (mConnectedOnboardee != null) {
            mConnectedOnboardee.setConnectionState(
                success ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED);
        }
    }
}
