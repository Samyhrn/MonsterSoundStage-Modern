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

    public DeviceData getDeviceData(String deviceId, String displayName) {
        return new DeviceData(deviceId, displayName);
    }
    public void onDeviceSaved(Device device, int status) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceConnectionStateChanged(device, ConnectionState.CONNECTED);
    }

    public void onZoneAdded(Zone zone) {        if (mControllerEventListener != null) mControllerEventListener.onZoneAdded(zone);
    }
    public void onZoneRemoved(Zone zone) {
        if (mControllerEventListener != null) mControllerEventListener.onZoneRemoved(zone);
    }
    public void onZonePlayerStateChanged(Zone zone, PlayerState state) {
        if (mControllerEventListener != null) mControllerEventListener.onZonePlayerStateChanged(zone, state);
    }
    public void onZonePlaybackError(Zone zone, int index, Error code, String desc) {
        if (mControllerEventListener != null) mControllerEventListener.onZonePlaybackError(zone, index, code, desc);
    }
    public void onZonePlayersListChanged(Zone zone) {
        if (mControllerEventListener != null) mControllerEventListener.onZonePlayersListChanged(zone);
    }
    public void onZoneIDChanged(Zone zone, String oldID) {
        if (mControllerEventListener != null) mControllerEventListener.onZoneIDChanged(zone, oldID);
    }
    public void onZoneControlsEnabledChanged(Zone zone) {
        if (mControllerEventListener != null) mControllerEventListener.onZoneControlsEnabledChanged(zone);
    }
    public void onDeviceAdded(Device device) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceAdded(device);
    }
    public void onDeviceRemoved(Device device) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceRemoved(device);
    }
    public void onDeviceConnectionStateChanged(Device device, ConnectionState state) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceConnectionStateChanged(device, state);
    }
    public void onDeviceDisplayNameChanged(Device device, String name) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceDisplayNameChanged(device, name);
    }
    public void onDeviceBatteryStatusChanged(Device device, boolean bat, int cl, int tub, int tuc) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceBatteryStatusChanged(device, bat, cl, tub, tuc);
    }
    public void onDeviceUpdateStarted(Device device) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceUpdateStarted(device);
    }
    public void onDeviceAutoUpdateChanged(Device device, boolean auto) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceAutoUpdateChanged(device, auto);
    }
    public void onDeviceUpdateAvailable(Device device) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceUpdateAvailable(device);
    }
    public void onDeviceUpdateStatusChanged(Device device, UpdateStatus status) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceUpdateStatusChanged(device, status);
    }
    public void onDeviceUpdateProgressChanged(Device device, double progress) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceUpdateProgressChanged(device, progress);
    }
    public void onDeviceUpdatePhysicalRebootRequired(Device device) {
        if (mControllerEventListener != null) mControllerEventListener.onDeviceUpdatePhysicalRebootRequired(device);
    }
    public UserPassword onDevicePasswordRequested(Device device) {
        if (mControllerEventListener != null) return mControllerEventListener.onDevicePasswordRequested(device);
        return new UserPassword();
    }
    public void onPlayerVolumeStateChanged(Player player, int volume) {
        if (mControllerEventListener != null) mControllerEventListener.onPlayerVolumeStateChanged(player, volume);
    }
    public void onPlayerDisplayNameChanged(Player player, String name) {
        if (mControllerEventListener != null) mControllerEventListener.onPlayerDisplayNameChanged(player, name);
    }
    public void onPlayerInterruptibleChanged(Player player, boolean i) {
        if (mControllerEventListener != null) mControllerEventListener.onPlayerInterruptibleChanged(player, i);
    }
    public void onPlayerPartyModeEnabledChanged(Player player, boolean e) {
        if (mControllerEventListener != null) mControllerEventListener.onPlayerPartyModeEnabledChanged(player, e);
    }
    public void onPlayerVolumeEnabledChanged(Player player, boolean e) {
        if (mControllerEventListener != null) mControllerEventListener.onPlayerVolumeEnabledChanged(player, e);
    }
    public void onPlayerInputSelectorChanged(Player player, String input) {
        if (mControllerEventListener != null) mControllerEventListener.onPlayerInputSelectorChanged(player, input);
    }
    public void onPlaylistChanged(Playlist playlist) {
        if (mControllerEventListener != null) mControllerEventListener.onPlaylistChanged(playlist);
    }
    public void onPlaylistLoopStateChanged(Playlist playlist, LoopMode mode) {
        if (mControllerEventListener != null) mControllerEventListener.onPlaylistLoopStateChanged(playlist, mode);
    }
    public void onPlaylistShuffleStateChanged(Playlist playlist, ShuffleMode mode) {
        if (mControllerEventListener != null) mControllerEventListener.onPlaylistShuffleStateChanged(playlist, mode);
    }
    public void onOnboardingStateChanged(String deviceID, OnboardingState state) {
        if (mControllerEventListener != null) mControllerEventListener.onOnboardingStateChanged(deviceID, state);
    }

    /** Inner class used by native code to pass device identification data. */
    public static class DeviceData {
        public String  deviceId    = "";
        public String  displayName = "";
        public Device  device      = null;
        public boolean isNew       = false;
        public int     status      = 0;

        public DeviceData() {}
        public DeviceData(String deviceId, String displayName) {
            this.deviceId    = deviceId;
            this.displayName = displayName;
        }

        public static DeviceData create(String deviceId, String displayName) {
            return new DeviceData(deviceId, displayName);
        }
    }
}
