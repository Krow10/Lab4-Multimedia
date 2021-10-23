package com.example.lab4_multimedia.media_player;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentResultListener;

import com.example.lab4_multimedia.MainActivity;
import com.example.lab4_multimedia.R;
import com.example.lab4_multimedia.cloud_media_explorer.CloudMediaExplorerFragment;
import com.example.lab4_multimedia.cloud_media_explorer.CloudSongItem;
import com.example.lab4_multimedia.onboarding.SignInActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.storage.StorageMetadata;

import java.util.ArrayList;
import java.util.List;

public class MediaPlayerMainActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> song_library_source_result;
    private CloudMediaExplorerFragment cloud_explorer_dialog;
    private ArrayList<CloudSongItem> song_items_backup;
    private boolean offline_mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player_main);

        cloud_explorer_dialog = new CloudMediaExplorerFragment();
        song_items_backup = new ArrayList<>();
        offline_mode = getIntent().getBooleanExtra("offline_mode", false);

        PlaylistControlsFragment playlist_controller = new PlaylistControlsFragment();
        SongInfoFragment playing_song_info = new SongInfoFragment(true, SongInfoFragment.RegisterFor.SONG_INFO_CURRENT);
        MediaPlayerFragment player = new MediaPlayerFragment(getApplicationContext());

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.playlist_control_container, playlist_controller)
                .replace(R.id.player_container, player)
                .replace(R.id.song_info_container, playing_song_info)
                .commit();

        getSupportFragmentManager().setFragmentResultListener("cloud_explorer_results", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                Uri single_song_select = result.getParcelable("single_song_select");
                List<Uri> playlist_select = result.getParcelableArrayList("playlist_select");
                CloudSongItem cloud_song_cache_metadata = result.getParcelable("cloud_song_cache_metadata");
                CloudSongItem cloud_song_update_metadata = result.getParcelable("cloud_song_update_metadata");
                CloudSongItem cloud_song_remove = result.getParcelable("cloud_song_remove");
                String change_player_state_for_preview = result.getString("change_player_state_for_preview");
                ArrayList<CloudSongItem> uploading_song_items_backup = result.getParcelableArrayList("uploading_song_items_backup");

                if (single_song_select != null) {
                    cloud_explorer_dialog.dismiss();
                    player.changeCurrentSong(single_song_select);
                } else if (playlist_select != null) {
                    cloud_explorer_dialog.dismiss();
                    player.createPlaylist(playlist_select);
                } else if (cloud_song_cache_metadata != null) {
                    // String array should be formatted in this order : [Uri, title, artist]
                    player.cacheMetadata(new CloudSongItem(cloud_song_cache_metadata.getUrl(),
                            cloud_song_cache_metadata.getTitle(),
                            cloud_song_cache_metadata.getArtist()));
                } else if (cloud_song_update_metadata != null) {
                    // String array should be formatted in this order : [Uri, title, artist]
                    final String song_url = cloud_song_update_metadata.getUrl().toString();
                    final String song_title = cloud_song_update_metadata.getTitle();
                    final String song_artist = cloud_song_update_metadata.getArtist();

                    StorageMetadata new_metadata = new StorageMetadata.Builder()
                            .setCustomMetadata("title", song_title)
                            .setCustomMetadata("artist", song_artist)
                            .build();

                    MainActivity.firebase_storage.getReferenceFromUrl(song_url).updateMetadata(new_metadata).addOnCompleteListener(new OnCompleteListener<StorageMetadata>() {
                        @Override
                        public void onComplete(@NonNull Task<StorageMetadata> task) {
                            Log.d(getClass().getName(), "Update metadata for " + song_url + " successfully : " + song_title + " / " + song_artist);
                            player.cacheMetadata(new CloudSongItem(Uri.parse(song_url), song_title, song_artist));
                            player.updateItemsMetadata(Uri.parse(song_url));
                            showSnackbar("✅ Successfully updated metadata for \"" + song_title + " - " + song_artist + "\"",
                                    cloud_explorer_dialog.requireView());
                        }
                    });
                } else if (cloud_song_remove != null) {
                    MainActivity.firebase_storage.getReferenceFromUrl(cloud_song_remove.getUrl().toString()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(getClass().getName(), "Song " + cloud_song_remove + " successfully deleted !");
                            player.removeFromQueue(cloud_song_remove.getUrl());
                            showSnackbar("✅ Successfully deleted \"" + cloud_song_remove.getTitle() + " - " + cloud_song_remove.getArtist() + "\"",
                                    cloud_explorer_dialog.requireView());
                        }
                    });
                } else if (change_player_state_for_preview != null) {
                    if (change_player_state_for_preview.equals("pause"))
                        player.pauseCurrentSong();
                    else
                        player.resumeCurrentSong();
                } else if (uploading_song_items_backup != null) {
                    song_items_backup = uploading_song_items_backup;
                }
            }
        });

        song_library_source_result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) { // checking empty selection
                        if (data.getClipData() != null) { // checking multiple selection or not
                            List<Uri> new_playlist = new ArrayList<>();
                            for (int i = 0; i < data.getClipData().getItemCount(); i++) { // NB : Selection order is preserved from chooser activity depending on devices
                                Uri song_uri = data.getClipData().getItemAt(i).getUri();
                                new_playlist.add(song_uri);
                                Log.d("NewSong", song_uri.toString());
                            }

                            player.createPlaylist(new_playlist);
                            // Snackbar automatically dismiss when action is clicked
                            showSnackbar("Playlist created ! (" + data.getClipData().getItemCount() + " songs added)", findViewById(R.id.song_library_fab));
                        } else {
                            Log.d("NewSong", data.getData().toString());
                            player.changeCurrentSong(data.getData());
                        }
                    }
                }
            });

        BottomAppBar app_bar = findViewById(R.id.bottom_app_bar);
        app_bar.getMenu().findItem(R.id.action_sign_in_or_out).setTitle(
                offline_mode ? getResources().getString(R.string.onboarding_sign_in) : getResources().getString(R.string.sign_out));
        app_bar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_sign_in_or_out) {
                if (!offline_mode)
                    MainActivity.firebase_auth.signOut();

                startActivity(new Intent(MediaPlayerMainActivity.this, offline_mode ? SignInActivity.class : MainActivity.class));
                finish();
            }

            return true;
        });

        FloatingActionButton song_library_fab = findViewById(R.id.song_library_fab);
        song_library_fab.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(getApplicationContext(), song_library_fab);
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.source_local_device) {
                    Intent source_intent = new Intent();
                    source_intent.setType("audio/*");
                    source_intent.setAction(Intent.ACTION_GET_CONTENT);
                    source_intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                    source_intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    song_library_source_result.launch(source_intent);
                } else if (itemId == R.id.source_cloud) {
                    Bundle backup_items = new Bundle();
                    backup_items.putParcelableArrayList("song_items_backup", song_items_backup);
                    cloud_explorer_dialog.setArguments(backup_items);
                    cloud_explorer_dialog.show(getSupportFragmentManager(), CloudMediaExplorerFragment.TAG);
                }

                return true;
            });

            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.fab_song_library_source, popup.getMenu());
            popup.getMenu().findItem(R.id.source_cloud).setEnabled(!offline_mode);
            popup.show();
        });

        song_library_fab.post(() -> {
            if (getIntent().hasExtra("signed_in_has"))
                showSnackbar("Welcome back " + getIntent().getStringExtra("signed_in_has") + " !", findViewById(R.id.song_library_fab));
        }); // Wait for FAB to be ready before anchoring the snackbar's view
    }

    @Override
    protected void onDestroy() {
        ((NotificationManager)(getSystemService(Context.NOTIFICATION_SERVICE))).cancelAll();
        super.onDestroy();
    }

    private void showSnackbar(final String text, View anchor) {
        Snackbar.make(anchor, text, Snackbar.LENGTH_LONG)
                .setAnchorView(anchor)
                .setAction("Dismiss", v -> {}).show();
    }
}