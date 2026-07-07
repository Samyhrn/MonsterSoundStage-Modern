package com.qualcomm.qce.allplay.controllersdk;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PlayerManager {
    private static final String TAG = "PlayerManager";
    private static PlayerManager sInstance = null;

    static {
        Log.v(TAG, "Loading AllPlayControllerSDK");
        System.loadLibrary("AllPlayControllerSDK");
    }

    private native void create(String str);
    private native Error createZone(Player[] players);
    private native Error createZoneWithLead(Player lead, Player[] slaves);
    private native Error editZone(Zone zone, Player[] players);
    private native Player[] getAllPlayersArray();
    private native Zone[] getAvailableZonesArray();
    private native Player[] getPartyModeEnabledPlayersArray();
    public native Error deleteZone(Zone zone);
    public native String getKeyStorePath();
    native boolean isStartedNative();
    public native void refreshPlayerList();
    public native void setKeyStorePath(String path);
    native void startManager();
    native void stopManager();

    private Context mContext;
    private List<Device> mDevicesList;
    private android.os.Handler mHandler;
    private OnboardingManager mOnboardingManager;
    private WifiManager mWifiManager;
    private IControllerEventListener mControllerEventListener = null;
    private WifiManager.MulticastLock mMulticastLock = null;
    private WifiManager.WifiLock mWifiLock = null;
    private String mUniqueName = null;
    private Object mPasswordObject = new Object();
    private boolean mStarted = false;

    public static PlayerManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PlayerManager(context);
        }
        return sInstance;
    }

    public synchronized void setControllerEventListener(IControllerEventListener listener) {
        this.mControllerEventListener = listener;
    }

    public void start() {
        acquireLock();
        mOnboardingManager.start();
        startManager();
        mStarted = true;
        Log.i(TAG, "PlayerManager started");
    }

    private void acquireLock() {
        releaseLock();
        if (mWifiManager != null) {
            mWifiLock = mWifiManager.createWifiLock(mUniqueName + ".wifiLock");
            mWifiLock.acquire();
            mMulticastLock = mWifiManager.createMulticastLock(mUniqueName + ".multicastLock");
            mMulticastLock.acquire();
        }
    }

    private void releaseLock() {
        if (mWifiLock != null) { mWifiLock.release(); mWifiLock = null; }
        if (mMulticastLock != null) { mMulticastLock.release(); mMulticastLock = null; }
    }

    public void stop() {
        releaseLock();
        mOnboardingManager.stop();
        stopManager();
        mStarted = false;
    }

    public boolean isStarted() { return mStarted; }

    public List<Zone> getAvailableZones() {
        List<Zone> list = new ArrayList<>();
        Zone[] arr = getAvailableZonesArray();
        if (arr != null) for (Zone z : arr) list.add(z);
        return list;
    }

    public List<Player> getAllPlayers() {
        List<Player> list = new ArrayList<>();
        Player[] arr = getAllPlayersArray();
        if (arr != null) for (Player p : arr) list.add(p);
        return list;
    }

    public List<Device> getAllDevices() {
        synchronized (mDevicesList) {
            Collections.sort(mDevicesList);
            return mDevicesList;
        }
    }

    public Error createZone(Player lead, List<Player> slaves) {
        return createZoneWithLead(lead, slaves.toArray(new Player[0]));
    }

    public Error createZone(List<Player> players) {
        return createZone(players.toArray(new Player[0]));
    }

    public Error editZone(Zone zone, List<Player> players) {
        return editZone(zone, players.toArray(new Player[0]));
    }

    public void startOnboardingScan() { mOnboardingManager.startOnboardingScan(); }
    public void stopOnboardingScan() { mOnboardingManager.stopOnboardingScan(); }

    PlayerManager(Context context) {
        mContext = context;
        create(getUniqueAppName(context));
        setKeyStorePath(context.getFileStreamPath("alljoyn_keystore").getAbsolutePath());
        mDevicesList = Collections.synchronizedList(new ArrayList());
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mOnboardingManager = OnboardingManager.getInstance(context, mWifiManager, this);
    }

    private String getUniqueAppName(Context context) {
        String id = android.provider.Settings.Secure.getString(
            context.getContentResolver(), "android_id");
        if (id == null || id.isEmpty()) id = UUID.randomUUID().toString();
        mUniqueName = context.getPackageName() + "-" + id;
        return mUniqueName;
    }
}
