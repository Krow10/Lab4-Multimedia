package com.example.lab4_multimedia.cloud_media_explorer;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab4_multimedia.R;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CloudLibraryContentAdapter extends RecyclerView.Adapter<CloudLibraryContentAdapter.CloudSongViewHolder> {
    private final SimpleExoPlayer background_song_preview_player;
    private final ArrayList<CloudSongItem> song_library;
    private final FragmentManager parent_dialog_fm;

    public static class CloudSongItemListener implements View.OnClickListener {
        private final FragmentManager fm;
        private final Uri song_url;

        public CloudSongItemListener(FragmentManager fm, Uri song_url) {
            this.fm = fm;
            this.song_url = song_url;
        }

        @Override
        public void onClick(View v) {
            Bundle song_selection = new Bundle();
            song_selection.putParcelable("single_song_select", song_url);
            fm.setFragmentResult("cloud_explorer_results", song_selection);
        }
    }

    public static class CloudSongViewHolder extends RecyclerView.ViewHolder {
        public final TextView song_title_view;
        public final TextView song_artist_view;
        public final ImageButton song_edit_metadata;
        public final ImageButton song_cloud_remove;
        public final ProgressBar song_upload_progress;

        public CloudSongViewHolder(@NonNull View itemView) {
            super(itemView);

            song_title_view = itemView.findViewById(R.id.cloud_song_title);
            song_artist_view = itemView.findViewById(R.id.cloud_song_artist);
            song_edit_metadata = itemView.findViewById(R.id.cloud_song_edit_metadata_button);
            song_cloud_remove = itemView.findViewById(R.id.cloud_song_remove_cloud_song_button);
            song_upload_progress = itemView.findViewById(R.id.cloud_song_upload_progress);
        }
    }

    public CloudLibraryContentAdapter(ArrayList<CloudSongItem> data, FragmentManager p_fm) {
        this.background_song_preview_player = new SimpleExoPlayer.Builder(Objects.requireNonNull(p_fm.findFragmentByTag("CloudMediaExplorerDialog"))
                .requireContext())
                .build();
        this.song_library = data;
        this.parent_dialog_fm = p_fm;
        Log.d(getClass().getName(), "Initial cloud song data : " + data);
    }

    @NonNull
    @Override
    public CloudSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cloud_song_item, parent, false);
        return new CloudSongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CloudSongViewHolder holder, int position) {
        // Prevent lint warning about "position" not relevant outside the body of this function (dialog onClick handler)
        final int item_position = holder.getBindingAdapterPosition();
        final CloudSongItem song_item = getItem(item_position);

        holder.song_title_view.setText(song_item.getTitle());
        holder.song_artist_view.setText(song_item.getArtist());
        holder.song_edit_metadata.setOnClickListener(v -> { // TODO : Sanitize title / artist input
            View edit_dialog_view = LayoutInflater.from(v.getContext()).inflate(R.layout.cloud_song_edit_metadata_dialog, null);

            EditText dialog_edit_title = edit_dialog_view.findViewById(R.id.dialog_edit_metadata_title);
            EditText dialog_edit_artist = edit_dialog_view.findViewById(R.id.dialog_edit_metadata_artist);
            dialog_edit_title.setText(song_item.getTitle());
            dialog_edit_artist.setText(song_item.getArtist());

            AlertDialog.Builder edit_dialog = new AlertDialog.Builder(v.getContext());
            edit_dialog.setTitle("Playing song in the background to help you fill the details \uD83C\uDFB6")
                .setView(edit_dialog_view)
                .setPositiveButton("OK", (dialog, which) -> {
                    song_item.setTitle(dialog_edit_title.getText().toString());
                    song_item.setArtist(dialog_edit_artist.getText().toString());
                    notifyItemChanged(item_position);

                    ArrayList<String> new_song_data = new ArrayList<>();
                    new_song_data.add(song_item.getUrl().toString());
                    new_song_data.add(song_item.getTitle());
                    new_song_data.add(song_item.getArtist());

                    Bundle new_metadata = new Bundle();
                    new_metadata.putStringArrayList("cloud_song_update_metadata", new_song_data);
                    parent_dialog_fm.setFragmentResult("cloud_explorer_results", new_metadata);
                }).setOnDismissListener(dialog -> {
                    background_song_preview_player.stop();

                    Bundle resume_playing_song = new Bundle();
                    resume_playing_song.putString("change_player_state_for_preview", "resume");
                    parent_dialog_fm.setFragmentResult("cloud_explorer_results", resume_playing_song);
                }).show();

            Bundle pause_playing_song = new Bundle();
            pause_playing_song.putString("change_player_state_for_preview", "pause");
            parent_dialog_fm.setFragmentResult("cloud_explorer_results", pause_playing_song);

            background_song_preview_player.setMediaItem(new MediaItem.Builder().setUri(song_item.getUrl()).build());
            background_song_preview_player.prepare();
            background_song_preview_player.play();
        });
        holder.song_cloud_remove.setOnClickListener(v -> {
            AlertDialog.Builder remove_dialog = new AlertDialog.Builder(v.getContext());
            remove_dialog.setTitle("Do you want to remove this song from the cloud ? ⚠")
                .setPositiveButton("YES", (dialog, which) -> {
                    Bundle removed_song = new Bundle();
                    removed_song.putString("cloud_song_remove", song_item.getUrl().toString());

                    song_library.remove(item_position);
                    notifyItemRemoved(item_position);
                    notifyItemRangeChanged(item_position, getItemCount());

                    parent_dialog_fm.setFragmentResult("cloud_explorer_results", removed_song);
                }).setNegativeButton("CANCEL", null).show();
        });

        if (song_item.isUploading()) {
            holder.itemView.setOnClickListener(null);
            holder.song_upload_progress.setVisibility(View.VISIBLE);
            holder.song_upload_progress.setMax(song_item.getUploadMax());
            holder.song_upload_progress.setProgress(song_item.getUploadCurrent());

            holder.song_title_view.setEnabled(false);
            holder.song_artist_view.setEnabled(false);
            holder.song_edit_metadata.setEnabled(false);
            holder.song_cloud_remove.setEnabled(false);
        } else {
            holder.itemView.setOnClickListener(new CloudSongItemListener(parent_dialog_fm, song_item.getUrl()));
            holder.song_upload_progress.setVisibility(View.INVISIBLE);

            holder.song_title_view.setEnabled(true);
            holder.song_artist_view.setEnabled(true);
            holder.song_edit_metadata.setEnabled(true);
            holder.song_cloud_remove.setEnabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return song_library.size();
    }

    public CloudSongItem getItem(int position) {
        return (position < 0 || position >= getItemCount()) ? null : song_library.get(position);
    }

    public int getItemPosition(Uri ref) {
        for (CloudSongItem item : song_library) {
            if (item.getUrl().equals(ref))
                return song_library.indexOf(item);
        }

        return -1;
    }

    public ArrayList<CloudSongItem> getUploadingItems() {
        ArrayList<CloudSongItem> filtered_items = new ArrayList<>();
        for (CloudSongItem item : song_library) {
            if (item.isUploading())
                filtered_items.add(item);
        }

        return filtered_items;
    }

    public List<Uri> getSongUriList() {
        List<Uri> uris = new ArrayList<>();
        for (int i = 0; i < getItemCount(); ++i) {
            if (!getItem(i).isUploading())
                uris.add(getItem(i).getUrl());
        }

        return uris;
    }

    public void addSong(CloudSongItem new_song, boolean addToTop) {
        int insert_index = addToTop ? 0 : getItemCount();
        song_library.add(insert_index, new_song);
        notifyItemInserted(insert_index);
    }

    public void clearSongs() {
        int size = getItemCount();
        for (int i = 0; i < size; ++i) {
            song_library.remove(0);
            notifyItemRemoved(0);
        }
    }
}
