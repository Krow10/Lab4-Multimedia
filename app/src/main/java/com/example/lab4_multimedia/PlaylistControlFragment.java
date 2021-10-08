package com.example.lab4_multimedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;

public class PlaylistControlFragment extends Fragment {
    private SongInfoFragment info;

    public PlaylistControlFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist_control, container, false);

        ImageButton previous = rootView.findViewById(R.id.playlist_control_previous);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Play previous playlist song
            }
        });

        info = new SongInfoFragment(false);
        getParentFragmentManager().beginTransaction().replace(R.id.playlist_control_previous_info, info).commit();

        return rootView;
    }
}