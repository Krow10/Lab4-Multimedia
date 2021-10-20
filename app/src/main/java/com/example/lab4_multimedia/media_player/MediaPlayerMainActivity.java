package com.example.lab4_multimedia.media_player;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
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
    private boolean offline_mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player_main);

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
                ArrayList<String> cloud_song_cache_metadata = result.getStringArrayList("cloud_song_cache_metadata");
                ArrayList<String> cloud_song_update_metadata = result.getStringArrayList("cloud_song_update_metadata");
                String cloud_song_remove = result.getString("cloud_song_remove");
                String change_player_state_for_preview = result.getString("change_player_state_for_preview");

                if (single_song_select != null) {
                    cloud_explorer_dialog.dismiss();
                    player.changeCurrentSong(single_song_select);
                } else if (playlist_select != null) {
                    cloud_explorer_dialog.dismiss();
                    player.createPlaylist(playlist_select);
                } else if (cloud_song_cache_metadata != null) {
                    // String array should be formatted in this order : [Uri, title, artist]
                    player.cacheMetadata(new CloudSongItem(Uri.parse(cloud_song_cache_metadata.get(0)),
                            cloud_song_cache_metadata.get(1),
                            cloud_song_cache_metadata.get(2)));
                } else if (cloud_song_update_metadata != null) {
                    // String array should be formatted in this order : [Uri, title, artist]
                    final String song_url = cloud_song_update_metadata.get(0);
                    final String song_title = cloud_song_update_metadata.get(1);
                    final String song_artist = cloud_song_update_metadata.get(2);

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
                        }
                    });
                } else if (cloud_song_remove != null) {
                    MainActivity.firebase_storage.getReferenceFromUrl(cloud_song_remove).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(getClass().getName(), "Song " + cloud_song_remove + " successfully deleted !");
                            player.removeFromQueue(Uri.parse(cloud_song_remove));
                        }
                    });
                } else if (change_player_state_for_preview != null) {
                    if (change_player_state_for_preview.equals("pause"))
                        player.pauseCurrentSong();
                    else
                        player.resumeCurrentSong();
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
                                Snackbar.make(findViewById(R.id.song_library_fab), "Playlist created ! (" + data.getClipData().getItemCount() + " songs added)", Snackbar.LENGTH_LONG)
                                        .setAnchorView(findViewById(R.id.song_library_fab))
                                        .setAction("Dismiss", v -> {}).show();
                            } else {
                                Log.d("NewSong", data.getData().toString());
                                player.changeCurrentSong(data.getData());
                            }
                        }
                    }
                });

        BottomAppBar app_bar = findViewById(R.id.bottom_app_bar);
        app_bar.getMenu().findItem(R.id.action_sign_out).setEnabled(!offline_mode); // TODO : Remove option entirely from menu instead ?
        app_bar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_sign_out) {
                MainActivity.firebase_auth.signOut();
                startActivity(new Intent(MediaPlayerMainActivity.this, MainActivity.class));
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
                    cloud_explorer_dialog = new CloudMediaExplorerFragment();
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
                Snackbar.make(findViewById(android.R.id.content), "Welcome back " + getIntent().getStringExtra("signed_in_has") + " !", Snackbar.LENGTH_LONG)
                        .setAnchorView(findViewById(R.id.song_library_fab))
                        .show();
        }); // Wait for FAB to be ready before anchoring the snackbar's view
    }

    @Override
    protected void onDestroy() {
        ((NotificationManager)(getSystemService(Context.NOTIFICATION_SERVICE))).cancelAll();
        super.onDestroy();
    }
}