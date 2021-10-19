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
import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab4_multimedia.R;
import com.example.lab4_multimedia.media_player.MediaPlayerFragment;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;

public class CloudMediaExplorerFragment extends BottomSheetDialogFragment {
    public static String TAG = "CloudMediaExplorerDialog";

    private FirebaseStorage firebase_storage;
    private ActivityResultLauncher<Intent> cloud_song_upload_result;

    private RecyclerView cloud_library_content;
    private CloudLibraryContentAdapter cloud_library_adapter;

    public CloudMediaExplorerFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebase_storage = FirebaseStorage.getInstance();
        cloud_library_adapter = new CloudLibraryContentAdapter(new ArrayList<>(), getParentFragmentManager(), getChildFragmentManager());
        refreshCloudLibrary();

        getChildFragmentManager().setFragmentResultListener("cloud_song_editing", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (result.getStringArrayList("update_cloud_song_metadata") != null) {
                    // String array should be formatted in this order : [Uri, title, artist]
                    final String song_url = result.getStringArrayList("update_cloud_song_metadata").get(0);
                    final String song_title = result.getStringArrayList("update_cloud_song_metadata").get(1);
                    final String song_artist = result.getStringArrayList("update_cloud_song_metadata").get(2);

                    StorageMetadata new_metadata = new StorageMetadata.Builder()
                            .setCustomMetadata("title", song_title)
                            .setCustomMetadata("artist", song_artist)
                            .build();

                    firebase_storage.getReferenceFromUrl(song_url).updateMetadata(new_metadata).addOnCompleteListener(new OnCompleteListener<StorageMetadata>() {
                        @Override
                        public void onComplete(@NonNull Task<StorageMetadata> task) {
                            Log.d(getTag(), "Update metadata for " + song_url + " successfully : " + song_title + " / " + song_artist);
                        }
                    });
                }
            }
        });
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

        // TODO : Prevent crash when no data => Disable until all data received ?
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

//    @Override
//    public void onResume() {
//        // Resize the dialog (from @JJ86, https://stackoverflow.com/a/19133940)
//        DisplayMetrics metrics = getResources().getDisplayMetrics();
//        int width = metrics.widthPixels;
//        int height = metrics.heightPixels;
//        requireDialog().getWindow().setLayout((6 * width)/7, (3 * height)/4);
//
//        super.onResume();
//    }

    private void uploadSong(Uri local) {
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setCustomMetadata("title", MediaPlayerFragment.getSongMetadata(requireContext(), local, MediaMetadataRetriever.METADATA_KEY_TITLE, false))
                .setCustomMetadata("artist", MediaPlayerFragment.getSongMetadata(requireContext(), local, MediaMetadataRetriever.METADATA_KEY_ARTIST, false))
                .build();

        StorageReference song_ref = firebase_storage.getReference().child("songs/" + local.getLastPathSegment());
        song_ref.putFile(local, metadata).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
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
                if (task.isSuccessful())
                    cloud_library_adapter.addSong(new CloudSongItem(task.getResult(), metadata.getCustomMetadata("title"), metadata.getCustomMetadata("artist")));
                else
                    Log.e(getTag(), "Could not retrieve file url after upload !");
            }
        });
    }

    private void refreshCloudLibrary() {
        cloud_library_adapter.clearSongs();
        StorageReference song_directory = firebase_storage.getReference("songs");
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
                                        Log.d("CloudData", "Added " + new_song.toString());
                                        return null;
                                    }
                                });

                                return null;
                            }
                        });
                    }
                } else {
                    Log.w("Cloud", "Could not retrieve song library : " + task.getException());
                }
            }
        });
    }
}