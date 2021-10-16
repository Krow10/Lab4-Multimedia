package com.example.lab4_multimedia.cloud_media_explorer;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab4_multimedia.R;

import java.util.ArrayList;
import java.util.List;

public class CloudLibraryContentAdapter extends RecyclerView.Adapter<CloudLibraryContentAdapter.CloudSongViewHolder> {
    private ArrayList<CloudSongItem> song_library;
    private FragmentManager parent_dialog_fm;

    public class CloudSongItemListener implements View.OnClickListener {
        private FragmentManager fm;
        private Uri song_url;

        public CloudSongItemListener(FragmentManager fm, Uri song_url) {
            this.fm = fm;
            this.song_url = song_url;
        }

        @Override
        public void onClick(View v) {
            Bundle song_selection = new Bundle();
            song_selection.putParcelable("single", song_url);
            fm.setFragmentResult("cloud_songs_selection", song_selection);
        }
    }

    public class CloudSongViewHolder extends RecyclerView.ViewHolder {
        private TextView song_title_view;
        private TextView song_artist_view;

        public CloudSongViewHolder(@NonNull View itemView) {
            super(itemView);

            song_title_view = itemView.findViewById(R.id.cloud_song_title);
            song_artist_view = itemView.findViewById(R.id.cloud_song_artist);
        }

        public TextView getSongTitleView() {
            return song_title_view;
        }

        public TextView getSongArtistView() {
            return song_artist_view;
        }
    }

    public CloudLibraryContentAdapter(ArrayList<CloudSongItem> data, FragmentManager fm) {
        this.song_library = data;
        this.parent_dialog_fm = fm;
    }

    @NonNull
    @Override
    public CloudSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cloud_song_item, parent, false);
        return new CloudSongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CloudSongViewHolder holder, int position) {
        Log.d("BindViewHolder", "Title : " + song_library.get(position).getTitle() + " / Artist : " + song_library.get(position).getArtist());
        holder.getSongTitleView().setText(song_library.get(position).getTitle());
        holder.getSongArtistView().setText(song_library.get(position).getArtist());
        holder.itemView.setOnClickListener(new CloudSongItemListener(parent_dialog_fm, song_library.get(position).getUrl()));
    }

    @Override
    public int getItemCount() {
        return song_library.size();
    }

    public void addSong(CloudSongItem new_song) {
        song_library.add(new_song);
        notifyItemInserted(getItemCount() - 1);
    }

    public List<Uri> getSongUriList() {
        List<Uri> uris = new ArrayList<>();
        for (int i = 0; i < getItemCount(); ++i)
            uris.add(song_library.get(i).getUrl());

        return uris;
    }

    public void clearSongs() {
        int size = getItemCount();
        for (int i = 0; i < size; ++i) {
            song_library.remove(0);
            notifyItemRemoved(0);
        }
    }
}
