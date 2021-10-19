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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab4_multimedia.R;
import com.example.lab4_multimedia.media_player.MediaPlayerFragment;
import com.example.lab4_multimedia.media_player.MediaPlayerMainActivity;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;

public class CloudMediaExplorerFragment extends BottomSheetDialogFragment {
    public static String TAG = "CloudMediaExplorerDialog";

    private ActivityResultLauncher<Intent> cloud_song_upload_result;
    private RecyclerView cloud_library_content;
    private CloudLibraryContentAdapter cloud_library_adapter;
    private final String cloud_song_directory = "songs/" + MediaPlayerMainActivity.firebase_auth.getUid() + "/";

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

        cloud_library_content = rootView.findViewById(R.id.cloud_library_content);
        cloud_library_content.setLayoutManager(new LinearLayoutManager(getContext()));
        cloud_library_content.setAdapter(cloud_library_adapter);
        cloud_library_content.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        cloud_song_upload_result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
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
//                        Snackbar.make(findViewById(R.id.song_library_fab), "Playlist created ! (" + data.getClipData().getItemCount() + " songs added)", Snackbar.LENGTH_LONG)
//                                .setAnchorView(findViewById(R.id.song_library_fab))
//                                .setAction("Dismiss", new View.OnClickListener() {
//                                    @Override
//                                    public void onClick(View v) {} // Snackbar automatically dismiss when action is clicked
//                                }).show();
                    }
                }
            }
        });

        // TODO : Prevent crash when no data => Disable until all data received AND add progress animation on button for metadata retrieval
        Button shuffle_play = rootView.findViewById(R.id.cloud_shuffle_play_button);
        shuffle_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // Not really shuffled since the order of the item is already randomized through asynchronous loading from the cloud
                Bundle song_selection = new Bundle();
                song_selection.putParcelableArrayList("playlist", (ArrayList<? extends Parcelable>) cloud_library_adapter.getSongUriList());
                getParentFragmentManager().setFragmentResult("cloud_songs_selection", song_selection);
            }
        });

        Button add_songs = rootView.findViewById(R.id.cloud_add_songs_button);
        add_songs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent source_intent = new Intent();
                source_intent.setType("audio/*");
                source_intent.setAction(Intent.ACTION_GET_CONTENT);
                source_intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                source_intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                cloud_song_upload_result.launch(source_intent);
            }
        });

        return rootView;
    }

    private void uploadSong(Uri local) { // TODO : Add song to beginning of list + progress bar
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setCustomMetadata("title", MediaPlayerFragment.getSongMetadata(requireContext(), local, MediaMetadataRetriever.METADATA_KEY_TITLE, false))
                .setCustomMetadata("artist", MediaPlayerFragment.getSongMetadata(requireContext(), local, MediaMetadataRetriever.METADATA_KEY_ARTIST, false))
                .build();

        StorageReference song_ref = MediaPlayerMainActivity.firebase_storage.getReference().child(cloud_song_directory
                + metadata.getCustomMetadata("title") + "_"
                + metadata.getCustomMetadata("artist") + "_"
                + System.currentTimeMillis());
        song_ref.putFile(local, metadata).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() { // TODO : Check if song already in cloud ?
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    Log.d(getTag(), "Uploaded file (" + local + ") successfully !");
                } else {
                    Log.e(getTag(), "Could not upload file (" + local + ") : " + task.getException());
                }
            }
        }).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful())
                    throw task.getException();

                return song_ref.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    CloudSongItem new_song = new CloudSongItem(task.getResult(), metadata.getCustomMetadata("title"), metadata.getCustomMetadata("artist"));
                    cloud_library_adapter.addSong(new_song);
                    sendCacheMetadataInfo(new_song);
                } else {
                    Log.e(getTag(), "Could not retrieve file url after upload !");
                }
            }
        });
    }

    private void refreshCloudLibrary() { // TODO : Sort song from most recent first
        cloud_library_adapter.clearSongs();
        StorageReference song_directory = MediaPlayerMainActivity.firebase_storage.getReference(cloud_song_directory);
        song_directory.listAll().addOnCompleteListener(new OnCompleteListener<ListResult>() {
            @Override
            public void onComplete(@NonNull Task<ListResult> task) {
                if (task.isSuccessful()) {
                    for (StorageReference song : task.getResult().getItems()) {
                        song.getMetadata().onSuccessTask(new SuccessContinuation<StorageMetadata, Object>() {
                            @NonNull
                            @Override
                            public Task<Object> then(StorageMetadata storageMetadata) throws Exception {
                                song.getDownloadUrl().onSuccessTask(new SuccessContinuation<Uri, Object>() {
                                    @NonNull
                                    @Override
                                    public Task<Object> then(Uri uri) throws Exception {
                                        CloudSongItem new_song = new CloudSongItem(uri,
                                                storageMetadata.getCustomMetadata("title"),
                                                storageMetadata.getCustomMetadata("artist"));
                                        cloud_library_adapter.addSong(new_song);
                                        sendCacheMetadataInfo(new_song);
                                        Log.d("CloudData", "Added " + new_song.toString());
                                        return null;
                                    }
                                });

                                return null;
                            }
                        });
                    }
                } else {
                    Log.w("Cloud", "Could not retrieve song library for user (path = " + cloud_song_directory + ") : " + task.getException());
                }
            }
        });
    }

    private void sendCacheMetadataInfo(CloudSongItem new_song) {
        Bundle new_metadata = new Bundle();
        ArrayList<String> new_song_data = new ArrayList<>();
        new_song_data.add(new_song.getUrl().toString());
        new_song_data.add(new_song.getTitle());
        new_song_data.add(new_song.getArtist());
        new_metadata.putStringArrayList("metadata", new_song_data);
        getParentFragmentManager().setFragmentResult("cloud_songs_selection", new_metadata);
    }
}