package com.example.lab4_multimedia.media_player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.lab4_multimedia.R;

public class SongInfoFragment extends Fragment {
    public enum RegisterFor {
        SONG_INFO_PREVIOUS,
        SONG_INFO_CURRENT,
        SONG_INFO_NEXT
    }

    private final RegisterFor register_for;
    private final boolean show_cover;
    private ImageView cover;
    private TextView title;
    private TextView artist;

    public SongInfoFragment(final boolean show_cover, final RegisterFor register_for) {
        this.show_cover = show_cover;
        this.register_for = register_for;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String request_key;

        if (register_for == RegisterFor.SONG_INFO_PREVIOUS)
            request_key = "song_info_prev";
        else if (register_for == RegisterFor.SONG_INFO_CURRENT)
            request_key = "song_info_curr";
        else
            request_key = "song_info_next";

        getParentFragmentManager().setFragmentResultListener(request_key, this, (requestKey, result) -> {
            title.setText(result.getString("title"));
            artist.setText(result.getString("artist"));
        });
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
}