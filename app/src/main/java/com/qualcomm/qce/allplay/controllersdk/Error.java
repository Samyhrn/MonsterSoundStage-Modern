package com.qualcomm.qce.allplay.controllersdk;

public enum Error {
    OK, FAILED, INVALID_ARGS, UNSUPPORTED, TIMEOUT, NOT_FOUND, CANCELLED, UNKNOWN;

    public int getErrorCode() { return ordinal(); }
}
