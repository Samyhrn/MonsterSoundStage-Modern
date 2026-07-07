package com.qualcomm.qce.allplay.controllersdk;

import java.util.ArrayList;
import java.util.List;

public class Device implements Comparable<Device> {
    public static final int UNDEFINED_CHARGE_LEVEL = 255;
    public static final int UNDEFINED_TIME_UNTIL_CHARGED = -1;
    public static final int UNDEFINED_TIME_UNTIL_DISCHARGED = -1;

    private String mID;
    private long mHandle = 0;
    private IOnboardee mOnboardee = null;

    private native synchronized void destroy();
    private native String getDisplayNameNative();
    private native ScanInfo[] getScanInfoArray();

    public native Error checkForNewFirmware();
    public native Error clearUpdateStatus();
    public native Error factoryReset();
    public native int getChargeLevel();
    public native String getEthernetIPAddress();
    public native String getEthernetMacAddress();
    public native double getFirmwareUpdateProgress();
    public native String getFirmwareVersion();
    public native OnboardingError getLastOnboardingError();
    public native String getManufacturer();
    public native String getModelNumber();
    public native NetworkInterface getNetworkInterface();
    public native String getNewFirmwareUrl();
    public native String getNewFirmwareVersion();
    public native int getTimeUntilBatteryCharged();
    public native int getTimeUntilBatteryDischarged();
    public native UpdateStatus getUpdateStatus();
    public native String getWifiIPAddress();
    public native String getWifiMacAddress();

    public String getID() { return mID; }
    public String getDisplayName() { return getDisplayNameNative(); }
    void setID(String id) { mID = id; }
    IOnboardee getOnboardee() { return mOnboardee; }
    void setOnboardee(IOnboardee o) { mOnboardee = o; }
    boolean hasValidConnection() { return mOnboardee != null && mOnboardee.isConnected(); }

    public List<ScanInfo> getScanInfoList() {
        List<ScanInfo> list = new ArrayList<>();
        ScanInfo[] arr = getScanInfoArray();
        if (arr != null) for (ScanInfo s : arr) list.add(s);
        return list;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Device)) return false;
        return mID.equalsIgnoreCase(((Device)other).getID());
    }

    public int hashCode() { return mID != null ? mID.hashCode() : 0; }

    public int compareTo(Device other) {
        if (other == null) return getDisplayName() != null ? 1 : 0;
        if (getDisplayName() == null) return other.getDisplayName() != null ? -1 : 0;
        return getDisplayName().compareTo(other.getDisplayName());
    }

    Device(String id) { mID = id; }
    Device(IOnboardee onboardee) {
        mOnboardee = onboardee;
        mID = onboardee.getScanInfo() != null ? onboardee.getScanInfo().SSID : null;
    }

    protected void finalize() throws Throwable {
        try { destroy(); } finally { super.finalize(); }
    }
}
