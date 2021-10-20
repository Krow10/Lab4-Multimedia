package com.example.lab4_multimedia.cloud_media_explorer;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab4_multimedia.MainActivity;
import com.example.lab4_multimedia.R;
import com.example.lab4_multimedia.media_player.MediaPlayerFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Objects;

public class CloudMediaExplorerFragment extends BottomSheetDialogFragment {
    public static final String TAG = "CloudMediaExplorerDialog";

    private ActivityResultLauncher<Intent> cloud_song_upload_result;
    private CloudLibraryContentAdapter cloud_library_adapter;
    private final String cloud_song_directory = "songs/" + MainActivity.firebase_auth.getUid() + "/";

    public CloudMediaExplorerFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cloud_library_adapter = new CloudLibraryContentAdapter(new ArrayList<>(), getParentFragmentManager());
        refreshCloudLibrary();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_cloud_media_explorer, container, false);

        RecyclerView cloud_library_content = rootView.findViewById(R.id.cloud_library_content);
        cloud_library_content.setLayoutManager(new LinearLayoutManager(getContext()));
        cloud_library_content.setAdapter(cloud_library_adapter);
        cloud_library_content.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        cloud_song_upload_result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null) { // checking empty selection
                    if (data.getClipData() != null) { // checking multiple selection or not
                        for (int i = 0; i < data.getClipData().getItemCount(); i++) { // NB : Selection order is preserved from chooser activity depending on devices
                            Uri song_uri = data.getClipData().getItemAt(i).getUri();
                            uploadSong(song_uri);
                        }
                    } else {
                        uploadSong(data.getData());
                        Log.d("NewSong", data.getData().toString());
                    }
                }
            }
        });

        // TODO : Prevent crash when no data => Disable until all data received AND add progress animation on button for metadata retrieval
        Button shuffle_play = rootView.findViewById(R.id.cloud_shuffle_play_button);
        shuffle_play.setOnClickListener(v -> { // No need to shuffle since the order of the item is already randomized through asynchronous loading from the cloud
            Bundle song_selection = new Bundle();
            song_selection.putParcelableArrayList("playlist_select", (ArrayList<? extends Parcelable>) cloud_library_adapter.getSongUriList());
            getParentFragmentManager().setFragmentResult("cloud_explorer_results", song_selection);
        });

        Button add_songs = rootView.findViewById(R.id.cloud_add_songs_button);
        add_songs.setOnClickListener(v -> {
            Intent source_intent = new Intent();
            source_intent.setType("audio/*");
            source_intent.setAction(Intent.ACTION_GET_CONTENT);
            source_intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            source_intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            cloud_song_upload_result.launch(source_intent);
        });

        return rootView;
    }

    private void uploadSong(Uri local) { // TODO : Add song to beginning of list + progress bar
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setCustomMetadata("title", MediaPlayerFragment.getSongMetadata(requireContext(), local, MediaMetadataRetriever.METADATA_KEY_TITLE, false))
                .setCustomMetadata("artist", MediaPlayerFragment.getSongMetadata(requireContext(), local, MediaMetadataRetriever.METADATA_KEY_ARTIST, false))
                .build();

        StorageReference song_ref = MainActivity.firebase_storage.getReference().child(cloud_song_directory
                + metadata.getCustomMetadata("title") + "_"
                + metadata.getCustomMetadata("artist") + "_"
                + System.currentTimeMillis());
        // TODO : Check if song already in cloud ?
        song_ref.putFile(local, metadata).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(getTag(), "Uploaded file (" + local + ") successfully !");
            } else {
                Log.e(getTag(), "Could not upload file (" + local + ") : " + task.getException());
            }
        }).continueWithTask(task -> {
            if (!task.isSuccessful())
                throw Objects.requireNonNull(task.getException());

            return song_ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                CloudSongItem new_song = new CloudSongItem(task.getResult(), metadata.getCustomMetadata("title"), metadata.getCustomMetadata("artist"));
                cloud_library_adapter.addSong(new_song);
                sendCacheMetadataInfo(new_song);
            } else {
                Log.e(getTag(), "Could not retrieve file url after upload !");
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void refreshCloudLibrary() { // TODO : Sort song from most recent first
        cloud_library_adapter.clearSongs();
        StorageReference song_directory = MainActivity.firebase_storage.getReference(cloud_song_directory);
        song_directory.listAll().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (StorageReference song : task.getResult().getItems()) {
                    song.getMetadata().onSuccessTask(storageMetadata -> {
                        song.getDownloadUrl().onSuccessTask(uri -> {
                            CloudSongItem new_song = new CloudSongItem(uri,
                                    storageMetadata.getCustomMetadata("title"),
                                    storageMetadata.getCustomMetadata("artist"));
                            cloud_library_adapter.addSong(new_song);
                            sendCacheMetadataInfo(new_song);
                            Log.d("CloudData", "Added " + new_song.toString());
                            return null;
                        });

                        return null;
                    });
                }
            } else {
                Log.w("Cloud", "Could not retrieve song library for user (path = " + cloud_song_directory + ") : " + task.getException());
            }
        });
    }

    private void sendCacheMetadataInfo(CloudSongItem new_song) {
        ArrayList<String> new_song_data = new ArrayList<>();
        new_song_data.add(new_song.getUrl().toString());
        new_song_data.add(new_song.getTitle());
        new_song_data.add(new_song.getArtist());

        Bundle new_metadata = new Bundle();
        new_metadata.putStringArrayList("cloud_song_cache_metadata", new_song_data);
        getParentFragmentManager().setFragmentResult("cloud_explorer_results", new_metadata);
    }
}