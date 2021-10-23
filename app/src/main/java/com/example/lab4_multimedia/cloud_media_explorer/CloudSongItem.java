package com.example.lab4_multimedia.cloud_media_explorer;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

public class CloudSongItem implements Parcelable {
    private Uri song_url;
    private String song_title;
    private String song_artist;
    private int upload_max;
    private int upload_current;

    public CloudSongItem(Uri url, String title, String artist) {
        song_url = url;
        song_title = title;
        song_artist = artist;
        upload_max = 0;
        upload_current = 0;
    }

    protected CloudSongItem(Parcel in) {
        song_url = in.readParcelable(Uri.class.getClassLoader());
        song_title = in.readString();
        song_artist = in.readString();
        upload_max = in.readInt();
        upload_current = in.readInt();
    }

    public static final Creator<CloudSongItem> CREATOR = new Creator<CloudSongItem>() {
        @Override
        public CloudSongItem createFromParcel(Parcel in) {
            return new CloudSongItem(in);
        }

        @Override
        public CloudSongItem[] newArray(int size) {
            return new CloudSongItem[size];
        }
    };

    public Uri getUrl() {
        return song_url;
    }

    public String getTitle() {
        return song_title;
    }

    public String getArtist() {
        return song_artist;
    }

    public int getUploadMax() {
        return upload_max;
    }

    public int getUploadCurrent() {
        return upload_current;
    }

    public void setUrl(final Uri url) {
        this.song_url = url;
    }

    public void setTitle(final String title) {
        this.song_title = title;
    }

    public void setArtist(final String artist) {
        this.song_artist = artist;
    }

    public void setUploadMax(final int max) {
        upload_max = max;
    }

    public void setUploadCurrent(final int current) {
        upload_current = current;
        Log.d(getUrl().toString(), "Uploading " + current + " / " + getUploadMax());
    }

    public boolean isUploading() {
        return upload_current != upload_max;
    }

    @NonNull
    @Override
    public String toString() {
        return "CloudSongItem{" +
                "song_url=" + song_url +
                ", song_title='" + song_title + '\'' +
                ", song_artist='" + song_artist + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(song_url, flags);
        dest.writeString(song_title);
        dest.writeString(song_artist);
        dest.writeInt(upload_max);
        dest.writeInt(upload_current);
    }
}
