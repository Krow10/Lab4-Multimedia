package com.example.lab4_multimedia.cloud_media_explorer;

import android.net.Uri;

public class CloudSongItem {
    private Uri song_url;
    private String song_title;
    private String song_artist;

    public CloudSongItem(Uri url, String title, String artist) {
        song_url = url;
        song_title = title;
        song_artist = artist;
    }

    public Uri getUrl() {
        return song_url;
    }

    public String getTitle() {
        return song_title;
    }

    public String getArtist() {
        return song_artist;
    }

    void setTitle(final String title) {
        this.song_title = title;
    }

    void setArtist(final String artist) {
        this.song_artist = artist;
    }

    @Override
    public String toString() {
        return "CloudSongItem{" +
                "song_url=" + song_url +
                ", song_title='" + song_title + '\'' +
                ", song_artist='" + song_artist + '\'' +
                '}';
    }
}
