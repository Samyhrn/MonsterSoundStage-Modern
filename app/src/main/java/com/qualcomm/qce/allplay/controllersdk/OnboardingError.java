package com.qualcomm.qce.allplay.controllersdk;

public class OnboardingError {
    public enum OnboardingErrorCode { NONE, AUTH_FAILED, CONNECTION_FAILED, TIMEOUT, UNKNOWN }

    // Public fields accessed directly by native code via GetFieldID
    public OnboardingErrorCode errorCode = OnboardingErrorCode.NONE;
    public String errorMessage = "";

    public OnboardingError() {}
    public OnboardingError(OnboardingErrorCode code, String message) {
        this.errorCode = code;
        this.errorMessage = message;
    }
}
