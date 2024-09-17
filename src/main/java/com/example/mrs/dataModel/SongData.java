package com.example.mrs.dataModel;

public class SongData {
    private final String name;
    private final String artists;
    private final String cover_url;
    private final String path;
    private final String year;

    public SongData(String name, String artists, String coverUrl, String path, String year) {
        this.name = name;
        this.artists = artists;
        cover_url = coverUrl;
        this.path = path;
        this.year = year;
    }

    public String getName() {
        return name;
    }
    public String getArtists(){
        return artists;
    }
    public String getYear() {
        return year;
    }
    public String getCover_url() {
        return cover_url;
    }
    public String getPath(){
        return path;
    }
    public String getNameArtists(){
        return name + " - " + artists;
    }
}
