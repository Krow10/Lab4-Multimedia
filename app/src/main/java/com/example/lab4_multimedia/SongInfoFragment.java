package com.example.lab4_multimedia;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

public class SongInfoFragment extends Fragment {
    private boolean show_cover;
    private ImageView cover;
    private TextView title;
    private TextView artist;

    public SongInfoFragment(final boolean show_cover) { this.show_cover = show_cover; }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_song_info, container, false);

        cover = rootView.findViewById(R.id.song_info_cover);
        title = rootView.findViewById(R.id.song_info_title);
        artist = rootView.findViewById(R.id.song_info_artist);

        if (!show_cover)
            cover.setVisibility(View.GONE);

        return rootView;
    }

    public Player.Listener createSongInfoListener() {
        return new Player.Listener() {
            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                title.setText("Unknown");
                artist.setText("Unknown");

                for (int k = 0; k < trackGroups.length; ++k){
                    TrackGroup tg = trackGroups.get(k);
                    Metadata meta = tg.getFormat(k).metadata;

                    for (int i = 0; i < meta.length(); ++i) {
                        final String entry_content = meta.get(i).toString();
                        final String tag = entry_content.substring(0, 4);
                        final String value = entry_content.substring(entry_content.indexOf("value=") + 6);
                        Log.d("MetaInfo", tag + " : " + value);

                        switch (tag) {
                            case "TIT2": // Title tag
                                title.setText(value);
                                break;
                            case "TPE1": // Artist tag
                                artist.setText(value);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        };
    }
}