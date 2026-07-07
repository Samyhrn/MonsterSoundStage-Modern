package com.qualcomm.qce.allplay.controllersdk;

import java.util.List;

public class Playlist {
    private long mHandle = 0;
    private native MediaItem[] getMediaItemArray();
    public native int getCurrentIndex();
    public native MediaItem getCurrentItem();
    public native String getPlaylistUserData();
    public native int size();
    public List<MediaItem> getMediaItems() {
        java.util.ArrayList<MediaItem> list = new java.util.ArrayList<>();
        MediaItem[] arr = getMediaItemArray();
        if (arr != null) for (MediaItem m : arr) list.add(m);
        return list;
    }
}
