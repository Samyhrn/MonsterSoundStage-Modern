package com.qualcomm.qce.allplay.controllersdk;

import android.util.Log;
import java.util.List;

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

    private android.content.Context mContext;
    private android.net.wifi.WifiManager mWifiManager;
    private android.net.wifi.WifiManager.MulticastLock mMulticastLock;
    private android.net.wifi.WifiManager.WifiLock mWifiLock;
    private String mUniqueName;
    private IControllerEventListener mControllerEventListener = null;
    private boolean mStarted = false;

    public static PlayerManager getInstance(android.content.Context context) {
        if (sInstance == null) {
            sInstance = new PlayerManager(context);
        }
        return sInstance;
    }

    public void start() {
        acquireLock();
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
        stopManager();
        mStarted = false;
    }

    public synchronized void setControllerEventListener(IControllerEventListener listener) {
        this.mControllerEventListener = listener;
    }

    public boolean isStarted() { return mStarted; }

    public List<Zone> getAvailableZones() {
        java.util.List<Zone> list = new java.util.ArrayList<>();
        Zone[] arr = getAvailableZonesArray();
        if (arr != null) for (Zone z : arr) list.add(z);
        return list;
    }

    public List<Player> getAllPlayers() {
        java.util.List<Player> list = new java.util.ArrayList<>();
        Player[] arr = getAllPlayersArray();
        if (arr != null) for (Player p : arr) list.add(p);
        return list;
    }

    public Error createZone(Player lead, java.util.List<Player> slaves) {
        return createZoneWithLead(lead, slaves.toArray(new Player[0]));
    }

    public Error createZone(java.util.List<Player> players) {
        return createZone(players.toArray(new Player[0]));
    }

    public Error editZone(Zone zone, java.util.List<Player> players) {
        return editZone(zone, players.toArray(new Player[0]));
    }

    PlayerManager(android.content.Context context) {
        mContext = context;
        create(getUniqueName(context));
        setKeyStorePath(context.getFileStreamPath("alljoyn_keystore").getAbsolutePath());
        mWifiManager = (android.net.wifi.WifiManager) context.getSystemService("wifi");
    }

    private String getUniqueName(android.content.Context context) {
        String id = android.provider.Settings.Secure.getString(
            context.getContentResolver(), "android_id");
        if (id == null || id.isEmpty()) id = java.util.UUID.randomUUID().toString();
        mUniqueName = context.getPackageName() + "-" + id;
        return mUniqueName;
    }
}
