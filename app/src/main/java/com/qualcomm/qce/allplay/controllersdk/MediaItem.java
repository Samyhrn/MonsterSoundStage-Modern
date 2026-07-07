package com.qualcomm.qce.allplay.controllersdk;

public class MediaItem {
    // Core fields
    public String title = "", artist = "", album = "", streamUrl = "", uri = "";
    public String channel = "", country = "", contentSource = "", description = "";
    public String subTitle = "", thumbnailUrl = "", genre = "", userData = "";
    public String mimeType = "";
    public int duration = 0;
    private String mediumDescription = "";

    public MediaItem() {}

    // Getters
    public String getTitle()                         { return title; }
    public String getArtist()                        { return artist; }
    public String getAlbum()                         { return album; }
    public String getStreamUrl()                     { return streamUrl; }
    public String getChannel()                       { return channel; }
    public String getCountry()                       { return country; }
    public String getContentSource()                 { return contentSource; }
    public String getDescription()                   { return description; }
    public int    getDuration()                      { return duration; }
    public String getGenre()                         { return genre; }
    public String getMediumDescription(String lang)  { return mediumDescription; }
    public String getSubTitle()                      { return subTitle; }
    public String getThumbnailUrl()                  { return thumbnailUrl; }
    public String getUserData()                      { return userData; }

    // Builder-style setters (return MediaItem for chaining)
    public MediaItem setTitle(String v)                             { title = v;             return this; }
    public MediaItem setArtist(String v)                           { artist = v;            return this; }
    public MediaItem setAlbum(String v)                            { album = v;             return this; }
    public MediaItem setStreamUrl(String v)                        { streamUrl = v;         return this; }
    public MediaItem setChannel(String v)                          { channel = v;           return this; }
    public MediaItem setCountry(String v)                          { country = v;           return this; }
    public MediaItem setContentSource(String v)                    { contentSource = v;     return this; }
    public MediaItem setDescription(String v)                      { description = v;       return this; }
    public MediaItem setDuration(int v)                            { duration = v;          return this; }
    public MediaItem setGenre(String v)                            { genre = v;             return this; }
    public MediaItem setMediumDescription(String desc, String lang){ mediumDescription=desc;return this; }
    public MediaItem setSubTitle(String v)                         { subTitle = v;          return this; }
    public MediaItem setThumbnailUrl(String v)                     { thumbnailUrl = v;      return this; }
    public MediaItem setUserData(String v)                         { userData = v;          return this; }
}
