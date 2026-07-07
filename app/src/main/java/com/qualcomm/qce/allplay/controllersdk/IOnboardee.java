package com.qualcomm.qce.allplay.controllersdk;

public interface IOnboardee {
    Error connect(String password);
    ScanInfo getScanInfo();
    boolean isConnected();
    void setConnectionState(ConnectionState state);
    ConnectionState getConnectionState();
}
