package com.qualcomm.qce.allplay.controllersdk;

public class ScanInfo {
    public enum AuthType { OPEN, WEP, WPA, WPA2, WPA3, ANY }
    public String SSID;
    public AuthType authType;
    public boolean isHidden;
    public boolean isWPSEnabled;
    public ScanInfo() {}
    public ScanInfo(String ssid, AuthType auth, boolean hidden) {
        SSID = ssid; authType = auth; isHidden = hidden;
    }
}
