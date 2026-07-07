package com.qualcomm.qce.allplay.controllersdk;

public class PlaylistData {
    public MediaItem[] mediaItems;
    public int currentIndex;
    public String playlistUserData;
    public Error error;
    public int start;

    public PlaylistData() {}

    public void setError(Error e)             { this.error = e; }
    public void setMediaItemList(MediaItem[] items) { this.mediaItems = items; }
    public void setStart(int s)               { this.start = s; }
}
