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
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> songLibrarySourceResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PlaylistControlFragment playlist_controller = new PlaylistControlFragment();
        SongInfoFragment song_info = new SongInfoFragment(true, SongInfoFragment.RegisterFor.SONG_INFO_CURRENT);
        MediaPlayerFragment player = new MediaPlayerFragment(getApplicationContext());

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.playlist_control_container, playlist_controller)
                .replace(R.id.player_container, player)
                .replace(R.id.song_info_container, song_info)
                .commit();

        songLibrarySourceResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
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
                                Snackbar.make(findViewById(R.id.song_library_fab), "Playlist created ! (" + data.getClipData().getItemCount() + " songs added)", Snackbar.LENGTH_SHORT)
                                        .setAnchorView(findViewById(R.id.song_library_fab))
                                        .show();
                            } else {
                                Log.d("NewSong", data.getData().toString());
                                player.changeCurrentSong(data.getData());
                            }
                        }
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
                                // TODO : Get song from local device
                                Intent source_intent = new Intent();
                                source_intent.setType("audio/*");
                                source_intent.setAction(Intent.ACTION_GET_CONTENT);
                                source_intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                                source_intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                                songLibrarySourceResult.launch(source_intent);
                                break;

                            default:
                                break;
                        }

                        return true;
                    }
                });

                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.fab_song_library_source, popup.getMenu());
                popup.show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        ((NotificationManager)(getSystemService(Context.NOTIFICATION_SERVICE))).cancelAll();
        super.onDestroy();
    }
}