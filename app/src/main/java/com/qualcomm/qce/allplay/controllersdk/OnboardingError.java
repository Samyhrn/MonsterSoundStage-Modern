package com.qualcomm.qce.allplay.controllersdk;

public class OnboardingError {
    public enum OnboardingErrorCode { NONE, AUTH_FAILED, CONNECTION_FAILED, TIMEOUT, UNKNOWN }
    public OnboardingErrorCode getErrorCode() { return OnboardingErrorCode.NONE; }
}
