package com.qualcomm.qce.allplay.controllersdk;

class WifiOnboardee implements IOnboardee {
    private ScanInfo mScanInfo;
    private ConnectionState mConnectionState = ConnectionState.DISCONNECTED;

    WifiOnboardee(String SSID, ScanInfo.AuthType authType, boolean isHidden) {
        mScanInfo = new ScanInfo(SSID, authType, isHidden);
    }

    public Error connect(String password) { return null; }
    public ScanInfo getScanInfo() { return mScanInfo; }
    public boolean isConnected() { return false; }
    public void setConnectionState(ConnectionState state) { mConnectionState = state; }
    public ConnectionState getConnectionState() { return mConnectionState; }
}
