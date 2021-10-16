package com.example.lab4_multimedia;

import static com.google.android.exoplayer2.Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import java.util.List;

public class MediaPlayerFragment extends Fragment {
    private static MediaMetadataRetriever mmr;
    private SimpleExoPlayer player;
    private PlayerNotificationManager player_notif;

    public MediaPlayerFragment(Context context) {
        mmr = new MediaMetadataRetriever();
        player = new SimpleExoPlayer.Builder(context)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .build();
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                if (mediaItem != null) {
                    String title = getSongMetadata(requireContext(), (Uri) mediaItem.playbackProperties.tag, MediaMetadataRetriever.METADATA_KEY_TITLE);
                    String artist = getSongMetadata(requireContext(), (Uri) mediaItem.playbackProperties.tag, MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    Log.d("mediaMetadataChanged", title + " by " + artist);
                    Bundle song_info = new Bundle();
                    song_info.putString("title", title == null ? "Unknown" : title);
                    song_info.putString("artist", artist == null ? "Unknown" : artist);
                    getParentFragmentManager().setFragmentResult("song_info_curr", song_info);

                    if (player.getMediaItemCount() > 1) // Actualize playlist controls info on current song changing
                        sendPlaylistControlInfo();
                }
            }

            @Override
            public void onTimelineChanged(Timeline timeline, int reason) { // Actualize playlist controls info on playlist creation
                if (reason == TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                    sendPlaylistControlInfo();
                }
            }
        });

        player_notif = new PlayerNotificationManager.Builder(context, 1337, "1337").build();
        player_notif.setPlayer(player);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getParentFragmentManager().setFragmentResultListener("player_control_action", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (result.getBoolean("action_prev")) {
                    if (player.hasPreviousWindow())
                        player.seekToPreviousWindow();
                }  else if (result.getBoolean("action_next")) {
                    if (player.hasNextWindow())
                        player.seekToNextWindow();
                }
            }
        });
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player_notif != null)
            player_notif.setPlayer(null);

        if (player != null) {
            player.release();
            player = null;
        }
    }

    public void changeCurrentSong(Uri new_song_uri) {
        player.setMediaItem(new MediaItem.Builder().setUri(new_song_uri).setTag(new_song_uri).build());
        player.prepare();
        player.play();
    }

    public void createPlaylist(List<Uri> playlist) {
        player.clearMediaItems();

        player.setMediaItem(new MediaItem.Builder().setUri(playlist.get(0)).setTag(playlist.get(0)).build()); // Set the first item to auto-start playing
        for (Uri song : playlist.subList(1, playlist.size())) {
            player.addMediaItem(new MediaItem.Builder().setUri(song).setTag(song).build());
        }

        player.prepare();
        player.play();
    }

    public static String getSongMetadata(Context ctx, Uri uri, int tag) {
        try {
            mmr.setDataSource(ctx, uri);
        } catch (IllegalArgumentException e) {
            return "Unknown";
        }

        final String tag_value = mmr.extractMetadata(tag);
        return tag_value == null ? "Unknown" : tag_value;
    }

    private void sendPlaylistControlInfo() {
        Bundle hide_controls = new Bundle();
        Bundle previous_control_data = new Bundle();
        Bundle next_control_data = new Bundle();

        if (player.getCurrentWindowIndex() != 0 && player.hasPreviousWindow()) { // Don't display previous control if it's first song in playlist (when 'repeat all' is toggled)
            MediaItem previous = player.getMediaItemAt(player.getPreviousWindowIndex());

            String title = getSongMetadata(requireContext(), (Uri) previous.playbackProperties.tag, MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = getSongMetadata(requireContext(), (Uri) previous.playbackProperties.tag, MediaMetadataRetriever.METADATA_KEY_ARTIST);

            previous_control_data.putString("title", title);
            previous_control_data.putString("artist", artist);
            hide_controls.putBoolean("hide_prev", false);
        } else {
            hide_controls.putBoolean("hide_prev", true);
        }

        if (player.getCurrentWindowIndex() != player.getMediaItemCount() - 1 && player.hasNextWindow()) { // Don't display next control if it's last song in playlist (same)
            MediaItem next = player.getMediaItemAt(player.getNextWindowIndex());

            String title = getSongMetadata(requireContext(), (Uri) next.playbackProperties.tag, MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = getSongMetadata(requireContext(), (Uri) next.playbackProperties.tag, MediaMetadataRetriever.METADATA_KEY_ARTIST);

            next_control_data.putString("title", title);
            next_control_data.putString("artist", artist);
            hide_controls.putBoolean("hide_next", false);
        } else {
            hide_controls.putBoolean("hide_next", true);
        }

        getParentFragmentManager().setFragmentResult("action_hide_control", hide_controls);
        getParentFragmentManager().setFragmentResult("song_info_prev", previous_control_data);
        getParentFragmentManager().setFragmentResult("song_info_next", next_control_data);
    }
}