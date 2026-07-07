package com.qualcomm.qce.allplay.controllersdk;

public class Zone implements Comparable<Zone> {
    private long mHandle = 0;
    private String mID;

    private native synchronized void destroy();
    private native Player[] getPlayersArray();
    private native Player[] getSlavePlayersArray();
    private native Error updatePlaylistArray(MediaItem[] items, int index, String userData);

    public native Error forcePrevious();
    public native String getDisplayName();
    public native Player getLeadPlayer();
    public native int getMaxVolume();
    public native int getPlayerPosition();
    public native PlayerState getPlayerState();
    public native Playlist getPlaylist();
    public native int getVolume();
    public native boolean isAudioSupported();
    public native boolean isInputSelectorModeSupported();
    public native boolean isInterruptible();
    public native boolean isLoopModeEnabled();
    public native boolean isNextEnabled();
    public native boolean isPartyModeEnabled();
    public native boolean isPhotoSupported();
    public native boolean isPreviousEnabled();
    public native boolean isSeekEnabled();
    public native boolean isShuffleModeEnabled();
    public native boolean isVideoSupported();
    public native boolean isVolumeEnabled();
    public native Error next();
    public native Error pause();
    public native Error play();
    public native Error previous();
    public native Error setPlayerPosition(int pos);
    public native Error setVolume(int volume);
    public native Error stop();

    public String getID() { return mID; }

    public java.util.List<Player> getPlayers() {
        java.util.List<Player> list = new java.util.ArrayList<>();
        Player[] arr = getPlayersArray();
        if (arr != null) for (Player p : arr) list.add(p);
        return list;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Zone)) return false;
        return mID.equalsIgnoreCase(((Zone)other).getID());
    }

    public int hashCode() { return mID.hashCode(); }

    public int compareTo(Zone other) {
        if (other == null) return getDisplayName() == null ? 0 : 1;
        if (getDisplayName() == null) return other.getDisplayName() != null ? -1 : 0;
        return getDisplayName().compareTo(other.getDisplayName());
    }

    synchronized void updateZoneID(String zoneID) { mID = zoneID; }

    Zone(String id) { mID = id; }

    protected void finalize() throws Throwable {
        try { destroy(); } finally { super.finalize(); }
    }
}
