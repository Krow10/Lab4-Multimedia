package com.example.lab4_multimedia.media_player;

import static com.google.android.exoplayer2.Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lab4_multimedia.R;
import com.example.lab4_multimedia.cloud_media_explorer.CloudSongItem;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MediaPlayerFragment extends Fragment {
    private static MediaMetadataRetriever mmr;
    private static HashMap<Uri, CloudSongItem> external_metadata_cache;
    private SimpleExoPlayer player;
    private final PlayerNotificationManager player_notif;

    public MediaPlayerFragment(Context context) {
        mmr = new MediaMetadataRetriever();
        player = new SimpleExoPlayer.Builder(context)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .build();
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                if (mediaItem != null && mediaItem.playbackProperties != null) {
                    updateItemsMetadata((Uri) mediaItem.playbackProperties.tag);
                } else {
                    Bundle song_info = new Bundle();
                    song_info.putString("title", "Choose a song from your preferred library");
                    song_info.putString("artist", "Hit the floating button below !");
                    getParentFragmentManager().setFragmentResult("song_info_curr", song_info);
                }
            }

            @Override
            public void onTimelineChanged(@NonNull Timeline timeline, int reason) { // Actualize playlist controls info on playlist creation
                if (reason == TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                    sendPlaylistControlInfo();
                }
            }
        });

        player_notif = new PlayerNotificationManager.Builder(context, 1337, "1337").build();
        player_notif.setPlayer(player);

        external_metadata_cache = new HashMap<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getParentFragmentManager().setFragmentResultListener("player_control_action", this, (requestKey, result) -> {
            if (result.getBoolean("action_prev")) {
                if (player.hasPreviousWindow())
                    player.seekToPreviousWindow();
            }  else if (result.getBoolean("action_next")) {
                if (player.hasNextWindow())
                    player.seekToNextWindow();
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
            rewind_button.setOnLongClickListener(v -> {
                player.seekTo(0);
                return true;
            });
        }

        ImageButton forward_button = player_view.findViewById(R.id.exo_ffwd);
        if (forward_button != null) {
            forward_button.setOnLongClickListener(v -> {
                player.seekTo(player.getDuration());
                return true;
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

    public boolean isExternalUri(Uri uri) {
        return URLUtil.isValidUrl(uri.toString()) && !(URLUtil.isFileUrl(uri.toString()) || URLUtil.isContentUrl(uri.toString()));
    }

    public void changeCurrentSong(Uri new_song_uri) {
        player.setMediaItem(new MediaItem.Builder().setUri(new_song_uri).setTag(new_song_uri).build());
        player.prepare();
        player.play();
    }

    public void pauseCurrentSong() {
        if (player.isPlaying())
            player.pause();
    }

    public void resumeCurrentSong() {
        if (player.getCurrentMediaItem() != null && !player.isPlaying())
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

    public void cacheMetadata(CloudSongItem song_info) {
        external_metadata_cache.put(song_info.getUrl(), song_info);
    }

    public void updateItemsMetadata(Uri song_uri) {
        MediaItem current_media_item = player.getCurrentMediaItem();
        if (current_media_item != null
                && current_media_item.playbackProperties != null
                && current_media_item.playbackProperties.tag != null
                && current_media_item.playbackProperties.tag.equals(song_uri)) { // Update metadata if song is currently playing
            String title = getSongMetadata(requireContext(), song_uri, MediaMetadataRetriever.METADATA_KEY_TITLE, isExternalUri(song_uri));
            String artist = getSongMetadata(requireContext(), song_uri, MediaMetadataRetriever.METADATA_KEY_ARTIST, isExternalUri(song_uri));
            Log.d("mediaMetadataChanged", title + " by " + artist);
            Bundle song_info = new Bundle();
            song_info.putString("title", title);
            song_info.putString("artist", artist);
            getParentFragmentManager().setFragmentResult("song_info_curr", song_info);
        }

        if (player.getMediaItemCount() > 1) // Actualize playlist controls info on current song changing
            sendPlaylistControlInfo();
    }

    public void removeFromQueue(Uri song_uri) {
        for (int i = 0; i < player.getMediaItemCount(); ++i) {
            MediaItem current_media_item = player.getMediaItemAt(i);
            if (current_media_item.playbackProperties != null
                    && current_media_item.playbackProperties.tag != null
                    && current_media_item.playbackProperties.tag.equals(song_uri)) {
                Log.d("RemoveFromQueue", "Current : " + current_media_item.playbackProperties.tag + " / Searching : " + song_uri.toString());
                player.removeMediaItem(i);
                break;
            }
        }
    }

    public static String getSongMetadata(@Nullable Context ctx, Uri uri, int tag, boolean external_uri) {
        String tag_value = null;

        try {
            if (external_uri) {
                if (tag == MediaMetadataRetriever.METADATA_KEY_TITLE)
                    tag_value = Objects.requireNonNull(external_metadata_cache.get(uri)).getTitle();
                else if (tag == MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    tag_value = Objects.requireNonNull(external_metadata_cache.get(uri)).getArtist();
            } else {
                mmr.setDataSource(ctx, uri);
                tag_value = mmr.extractMetadata(tag);
            }
        } catch (IllegalArgumentException e) {
            return "Unknown";
        }

        return tag_value == null ? "Unknown" : tag_value;
    }

    private void sendPlaylistControlInfo() {
        Bundle hide_controls = new Bundle();
        Bundle previous_control_data = new Bundle();
        Bundle next_control_data = new Bundle();

        if (player.getCurrentWindowIndex() != 0 && player.hasPreviousWindow()) { // Don't display previous control if it's first song in playlist (when 'repeat all' is toggled)
            MediaItem previous = player.getMediaItemAt(player.getPreviousWindowIndex());
            if (previous.playbackProperties != null && previous.playbackProperties.tag != null) {
                Uri previous_uri = (Uri) previous.playbackProperties.tag;

                String title = getSongMetadata(requireContext(), previous_uri, MediaMetadataRetriever.METADATA_KEY_TITLE, isExternalUri(previous_uri));
                String artist = getSongMetadata(requireContext(), previous_uri, MediaMetadataRetriever.METADATA_KEY_ARTIST, isExternalUri(previous_uri));

                previous_control_data.putString("title", title);
                previous_control_data.putString("artist", artist);
                hide_controls.putBoolean("hide_prev", false);
            }
        } else {
            hide_controls.putBoolean("hide_prev", true);
        }

        if (player.getCurrentWindowIndex() != player.getMediaItemCount() - 1 && player.hasNextWindow()) { // Don't display next control if it's last song in playlist (same)
            MediaItem next = player.getMediaItemAt(player.getNextWindowIndex());
            if (next.playbackProperties != null && next.playbackProperties.tag != null) {
                Uri next_uri = (Uri) next.playbackProperties.tag;

                String title = getSongMetadata(requireContext(), next_uri, MediaMetadataRetriever.METADATA_KEY_TITLE, isExternalUri(next_uri));
                String artist = getSongMetadata(requireContext(), next_uri, MediaMetadataRetriever.METADATA_KEY_ARTIST, isExternalUri(next_uri));

                next_control_data.putString("title", title);
                next_control_data.putString("artist", artist);
                hide_controls.putBoolean("hide_next", false);
            }
        } else {
            hide_controls.putBoolean("hide_next", true);
        }

        getParentFragmentManager().setFragmentResult("action_hide_control", hide_controls);
        getParentFragmentManager().setFragmentResult("song_info_prev", previous_control_data);
        getParentFragmentManager().setFragmentResult("song_info_next", next_control_data);
    }
}