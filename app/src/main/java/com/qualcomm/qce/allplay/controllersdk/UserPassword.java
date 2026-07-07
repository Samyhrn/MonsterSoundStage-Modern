package com.qualcomm.qce.allplay.controllersdk;

public class UserPassword {
    public String userId = "";
    public String password = "";

    public UserPassword() {}
    public UserPassword(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    public String getUserId()       { return userId; }
    public String getPassword()     { return password; }
    public boolean userCancelAuth() { return false; }
}
