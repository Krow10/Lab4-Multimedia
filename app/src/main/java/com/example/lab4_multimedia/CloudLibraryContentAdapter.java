package com.example.lab4_multimedia;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CloudLibraryContentAdapter extends RecyclerView.Adapter<CloudLibraryContentAdapter.CloudSongViewHolder> {
    private ArrayList<CloudSongItem> song_library;

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

    public CloudLibraryContentAdapter(ArrayList<CloudSongItem> data) {
        this.song_library = data;
    }

    @NonNull
    @Override
    public CloudSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cloud_song_item, parent, false);

        // TODO : Setup long click listener for editing tags ?
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });

        return new CloudSongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CloudSongViewHolder holder, int position) {
        Log.d("BindViewHolder", "Title : " + song_library.get(position).getTitle() + " / Artist : " + song_library.get(position).getArtist());
        holder.getSongTitleView().setText(song_library.get(position).getTitle());
        holder.getSongArtistView().setText(song_library.get(position).getArtist());
    }

    @Override
    public int getItemCount() {
        return song_library.size();
    }

    public void addSong(CloudSongItem new_song) {
        song_library.add(new_song);
        notifyItemInserted(getItemCount() - 1);
    }

    public void clearSongs() {
        int size = getItemCount();
        for (int i = 0; i < size; ++i) {
            song_library.remove(0);
            notifyItemRemoved(0);
        }
    }
}
