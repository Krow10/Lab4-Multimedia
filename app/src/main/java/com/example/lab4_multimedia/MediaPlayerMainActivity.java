package com.example.lab4_multimedia;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentResultListener;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

public class MediaPlayerMainActivity extends AppCompatActivity {
    private FirebaseAuth firebase_auth;
    private FirebaseStorage firebase_storage;
    private ActivityResultLauncher<Intent> song_library_source_result;
    private CloudMediaExplorerFragment cloud_explorer_dialog;
    private boolean offline_mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player_main);

        firebase_auth = FirebaseAuth.getInstance();
        firebase_storage = FirebaseStorage.getInstance();
        offline_mode = getIntent().getBooleanExtra("offline_mode", false);

        PlaylistControlsFragment playlist_controller = new PlaylistControlsFragment();
        SongInfoFragment playing_song_info = new SongInfoFragment(true, SongInfoFragment.RegisterFor.SONG_INFO_CURRENT);
        MediaPlayerFragment player = new MediaPlayerFragment(getApplicationContext());

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.playlist_control_container, playlist_controller)
                .replace(R.id.player_container, player)
                .replace(R.id.song_info_container, playing_song_info)
                .commit();

        getSupportFragmentManager().setFragmentResultListener("cloud_songs_selection", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                Log.d("CloudSongSelection", "Received result : " + result);
                cloud_explorer_dialog.dismiss();
                if (result.getParcelable("single") != null)
                    player.changeCurrentSong(result.getParcelable("single"));
                else if (result.getParcelableArrayList("playlist") != null)
                    player.createPlaylist(result.getParcelableArrayList("playlist"));
                else
                    Log.d("CloudSongSelection", "No song selected");
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
                                Snackbar.make(findViewById(R.id.song_library_fab), "Playlist created ! (" + data.getClipData().getItemCount() + " songs added)", Snackbar.LENGTH_LONG)
                                        .setAnchorView(findViewById(R.id.song_library_fab))
                                        .setAction("Dismiss", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {} // Snackbar automatically dismiss when action is clicked
                                        }).show();
                            } else {
                                Log.d("NewSong", data.getData().toString());
                                player.changeCurrentSong(data.getData());
                            }
                        }
                    }
                });

        BottomAppBar app_bar = findViewById(R.id.bottom_app_bar);
        app_bar.getMenu().findItem(R.id.action_sign_out).setEnabled(!offline_mode); // TODO : Remove option entirely from menu instead ?
        app_bar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_sign_out:
                        firebase_auth.signOut();
                        startActivity(new Intent(MediaPlayerMainActivity.this, MainActivity.class));
                        finish();
                        break;

                    default:
                        break;
                }

                return true;
            }
        });

        FloatingActionButton song_library_fab = findViewById(R.id.song_library_fab);
        song_library_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(getApplicationContext(), song_library_fab);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.source_local_device:
                                Intent source_intent = new Intent();
                                source_intent.setType("audio/*");
                                source_intent.setAction(Intent.ACTION_GET_CONTENT);
                                source_intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                                source_intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                                song_library_source_result.launch(source_intent);
                                break;

                            case R.id.source_cloud:
                                cloud_explorer_dialog = new CloudMediaExplorerFragment();
                                cloud_explorer_dialog.show(getSupportFragmentManager(), CloudMediaExplorerFragment.TAG);
                                break;

                            default:
                                break;
                        }

                        return true;
                    }
                });

                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.fab_song_library_source, popup.getMenu());
                popup.getMenu().findItem(R.id.source_cloud).setEnabled(!offline_mode);
                popup.show();
            }
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