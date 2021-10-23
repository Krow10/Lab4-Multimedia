package com.example.lab4_multimedia.cloud_media_explorer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab4_multimedia.MainActivity;
import com.example.lab4_multimedia.R;
import com.example.lab4_multimedia.media_player.MediaPlayerFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.color.MaterialColors;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CloudMediaExplorerFragment extends BottomSheetDialogFragment {
    public static final String TAG = "CloudMediaExplorerDialog";

    private CloudLibraryContentAdapter cloud_library_adapter;
    private RecyclerView cloud_library_recyclerview;
    private ActivityResultLauncher<Intent> cloud_song_upload_result;
    private TextView cloud_library_status_info;
    private final String cloud_song_directory = "songs/" + MainActivity.firebase_auth.getUid() + "/";
    private Button shuffle_play_button;

    public CloudMediaExplorerFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(getTag(), "Dialog arguments : " + getArguments());
        if (getArguments() != null && getArguments().getParcelableArrayList("song_items_backup") != null)
            cloud_library_adapter = new CloudLibraryContentAdapter(new ArrayList<>(getArguments().getParcelableArrayList("song_items_backup")), getParentFragmentManager());
        else
            cloud_library_adapter = new CloudLibraryContentAdapter(new ArrayList<>(), getParentFragmentManager());

        cloud_library_adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                Log.d(getTag(), "Item range from fragment : start = " + positionStart + " / count = " + itemCount);
                if (cloud_library_adapter.getSongUriList().size() == 1)
                    enableShuffleButton(true);
                else if (itemCount == 0)
                    enableShuffleButton(false);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_cloud_media_explorer, container, false);

        cloud_library_recyclerview = rootView.findViewById(R.id.cloud_library_content);
        cloud_library_recyclerview.setLayoutManager(new LinearLayoutManager(getContext()));
        cloud_library_recyclerview.setAdapter(cloud_library_adapter);
        cloud_library_recyclerview.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

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

        cloud_library_status_info = rootView.findViewById(R.id.cloud_library_status_info);
        setCloudLibraryStatusInfo(MainActivity.getUsername() + "'s library");

        shuffle_play_button = rootView.findViewById(R.id.cloud_shuffle_play_button);
        shuffle_play_button.setOnClickListener(v -> { // No need to shuffle since the order of the item is already randomized through asynchronous loading from the cloud
            Bundle song_selection = new Bundle();
            song_selection.putParcelableArrayList("playlist_select", (ArrayList<? extends Parcelable>) cloud_library_adapter.getSongUriList());
            getParentFragmentManager().setFragmentResult("cloud_explorer_results", song_selection);
        });

        Button add_songs_button = rootView.findViewById(R.id.cloud_add_songs_button);
        add_songs_button.setOnClickListener(v -> {
            Intent source_intent = new Intent();
            source_intent.setType("audio/*");
            source_intent.setAction(Intent.ACTION_GET_CONTENT);
            source_intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            source_intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            cloud_song_upload_result.launch(source_intent);
        });

        refreshCloudLibrary();
        return rootView;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        Bundle b = new Bundle();
        b.putParcelableArrayList("uploading_song_items_backup", cloud_library_adapter.getUploadingItems());
        getParentFragmentManager().setFragmentResult("cloud_explorer_results", b);
        super.onDismiss(dialog);
    }

    private void uploadSong(Uri local) { // TODO : Check if song already in cloud ?
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setCustomMetadata("title", MediaPlayerFragment.getSongMetadata(requireContext(), local, MediaMetadataRetriever.METADATA_KEY_TITLE, false))
                .setCustomMetadata("artist", MediaPlayerFragment.getSongMetadata(requireContext(), local, MediaMetadataRetriever.METADATA_KEY_ARTIST, false))
                .build();

        StorageReference song_ref = MainActivity.firebase_storage.getReference().child(cloud_song_directory
                + metadata.getCustomMetadata("title") + "_"
                + metadata.getCustomMetadata("artist") + "_"
                + System.currentTimeMillis());

        CloudSongItem new_song = new CloudSongItem(local, metadata.getCustomMetadata("title"), metadata.getCustomMetadata("artist"));
        cloud_library_adapter.addSong(new_song, true);
        cloud_library_recyclerview.scrollToPosition(0);

        int song_item_position = cloud_library_adapter.getItemPosition(local);
        if (song_item_position == -1) {
            Log.e(getTag(), "Could not find item in adapter !");
            return;
        }

        UploadTask upload_task = song_ref.putFile(local, metadata);

        cloud_library_adapter.getItem(song_item_position).setUploadMax((int) upload_task.getSnapshot().getTotalByteCount());
        upload_task.addOnProgressListener(snapshot -> {
            CloudSongItem uploading_item = cloud_library_adapter.getItem(cloud_library_adapter.getItemPosition(local));
            if (uploading_item != null)
                uploading_item.setUploadCurrent((int) snapshot.getBytesTransferred());
        }).addOnCompleteListener(task -> {
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
                int item_pos = cloud_library_adapter.getItemPosition(local);
                Log.d(getTag(), "Updated url for item " + item_pos);
                if (item_pos != -1) {
                    cloud_library_adapter.getItem(item_pos).setUrl(task.getResult());
                    cloud_library_adapter.notifyItemChanged(item_pos);
                    sendCacheMetadataInfo(new_song);
                }
            } else {
                Log.e(getTag(), "Could not retrieve file url after upload !");
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void refreshCloudLibrary() {
        ClipDrawable shuffle_play_background_progress = (ClipDrawable) ((LayerDrawable)(shuffle_play_button.getBackground())).findDrawableByLayerId(R.id.clip_drawable);
        ObjectAnimator shuffle_play_progress_anim = ObjectAnimator.ofInt(shuffle_play_background_progress, "level", 0, 0);
        shuffle_play_progress_anim.setDuration(getResources().getInteger(R.integer.cloud_songs_loading_progress_speed));
        shuffle_play_progress_anim.setInterpolator(new DecelerateInterpolator());

        ValueAnimator shuffle_text_color_anim = ValueAnimator.ofObject(new ArgbEvaluator(),
                ResourcesCompat.getColor(getResources(), R.color.disabled, null),
                MaterialColors.getColor(requireContext(), R.attr.colorOnPrimary, Color.WHITE));
        shuffle_text_color_anim.setDuration(getResources().getInteger(R.integer.cloud_songs_text_color_swap_speed));
        shuffle_text_color_anim.setInterpolator(new FastOutSlowInInterpolator());
        shuffle_text_color_anim.addUpdateListener(animation -> shuffle_play_button.setTextColor((int) animation.getAnimatedValue()));

        enableShuffleButton(false);

        StorageReference song_directory = MainActivity.firebase_storage.getReference(cloud_song_directory);
        song_directory.listAll().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<StorageReference> cloud_song_items = task.getResult().getItems();
                final int cloud_songs_total = cloud_song_items.size();
                AtomicInteger cloud_songs_count = new AtomicInteger();
                AtomicBoolean swap_text_color = new AtomicBoolean(true);

                shuffle_play_progress_anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);

                        if (cloud_songs_count.get() == cloud_songs_total)
                            shuffle_play_button.setEnabled(true);
                    }
                });

                for (StorageReference song : cloud_song_items) {
                    song.getMetadata().onSuccessTask(storageMetadata -> {
                        song.getDownloadUrl().onSuccessTask(uri -> {
                            if (swap_text_color.get()) {
                                shuffle_text_color_anim.start();
                                swap_text_color.set(false);
                            }

                            cloud_songs_count.addAndGet(1);
                            shuffle_play_progress_anim.setIntValues(shuffle_play_background_progress.getLevel(), 10000*(cloud_songs_count.get() + 1)/cloud_songs_total);
                            shuffle_play_progress_anim.start();

                            CloudSongItem new_song = new CloudSongItem(uri,
                                    storageMetadata.getCustomMetadata("title"),
                                    storageMetadata.getCustomMetadata("artist"));
                            cloud_library_adapter.addSong(new_song, false);
                            sendCacheMetadataInfo(new_song);
                            Log.d("CloudData", "[" + cloud_songs_count.get() + " / " + cloud_songs_total + "] Added " + new_song.toString());
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
        try {
            Bundle new_metadata = new Bundle();
            new_metadata.putParcelable("cloud_song_cache_metadata", new_song);
            getParentFragmentManager().setFragmentResult("cloud_explorer_results", new_metadata);
        } catch (IllegalStateException e) {
            Log.e(getTag(), "Could not cache metadata for " + new_song + " : " + e.getMessage());
        }
    }

    private void setCloudLibraryStatusInfo(final String status) {
        cloud_library_status_info.setText(status);
    }

    private void enableShuffleButton(final boolean enable) {
        shuffle_play_button.setEnabled(enable);
        ((ClipDrawable) ((LayerDrawable)(shuffle_play_button.getBackground())).findDrawableByLayerId(R.id.clip_drawable)).setLevel(enable ? 10000 : 0);
    }
}