package com.example.lab4_multimedia;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerControlView;

import java.util.List;

public class MediaPlayerFragment extends Fragment {
    private SimpleExoPlayer player;

    public MediaPlayerFragment(Context context, Player.Listener song_info_listener) {
        player = new SimpleExoPlayer.Builder(context).build();
        player.addListener(song_info_listener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media_player, container, false);

        PlayerControlView player_view = rootView.findViewById(R.id.player_view);
        player_view.setPlayer(player);

        ImageButton rewind_button = player_view.findViewById(R.id.exo_rew);
        if (rewind_button != null) {
            rewind_button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    player.seekTo(0);
                    return true;
                }
            });
        }

        ImageButton forward_button = player_view.findViewById(R.id.exo_ffwd);
        if (forward_button != null) {
            forward_button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    player.seekTo(player.getDuration());
                    return true;
                }
            });
        }

        return rootView;
    }

    public void changeSong(Uri new_song_uri) {
        player.setMediaItem(MediaItem.fromUri(new_song_uri));
        player.prepare();

        if (player.isPlaying())
            player.play();
    }

    public void createPlaylist(List<Uri> playlist) {
        player.clearMediaItems();
        for (Uri song : playlist) {
            player.addMediaItem(MediaItem.fromUri(song));
        }
    }
}