package com.qualcomm.qce.allplay.controllersdk;

import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class Player implements Comparable<Player> {
    private long mHandle = 0;
    private String mID;

    private native synchronized void destroy();

    private native String[] getInputSelectorArray();

    public native String getActiveInputSelector();

    public native String getDisplayName();

    public native int getMaxVolume();

    public native int getVolume();

    public native boolean isAudioSupported();

    public native boolean isInputSelectorModeSupported();

    public native boolean isInterruptible();

    public native boolean isPartyModeEnabled();

    public native boolean isPhotoSupported();

    public native boolean isVideoSupported();

    public native boolean isVolumeEnabled();

    public native Error setInputSelector(String str);

    public native Error setVolume(int i);

    public boolean equals(Object otherPlayer) {
        if (otherPlayer == null || !(otherPlayer instanceof Player)) {
            return false;
        }
        return this.mID.equalsIgnoreCase(((Player) otherPlayer).getID());
    }

    public int hashCode() {
        return this.mID.hashCode();
    }

    @Override // java.lang.Comparable
    public int compareTo(Player otherPlayer) {
        if (otherPlayer == null) {
            return getDisplayName() == null ? 0 : 1;
        }
        if (getDisplayName() == null) {
            return otherPlayer.getDisplayName() != null ? -1 : 0;
        }
        return otherPlayer.getDisplayName() != null ? getDisplayName().compareTo(otherPlayer.getDisplayName()) : 1;
    }

    public String getID() {
        return this.mID;
    }

    public List<String> getInputSelectorList() {
        List<String> inputs = new ArrayList<>();
        String[] inputArray = getInputSelectorArray();
        if (inputArray != null) {
            for (String str : inputArray) {
                inputs.add(str);
            }
        }
        return inputs;
    }

    protected void finalize() throws Throwable {
        try {
            destroy();
        } finally {
            super.finalize();
        }
    }

    Player(String id) {
        this.mID = null;
        this.mID = id;
    }
}
