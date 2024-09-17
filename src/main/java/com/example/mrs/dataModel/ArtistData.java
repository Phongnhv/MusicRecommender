package com.example.mrs.dataModel;

public class ArtistData {
    private final String name;
    private final String cover_url;
    private final String id;
    private final String external_url;
    private final int follower;

    public ArtistData (String name, String cover_url, String id, String external_url, int follower){
        this.name = name;
        this.cover_url = cover_url;
        this.id = id;
        this.external_url = external_url;
        this.follower = follower;
    }

    public String getCover_url() {
        return cover_url;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public int getFollower() {
        return follower;
    }

    public String getExternal_url() {
        return external_url;
    }
}
