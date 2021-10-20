package com.example.lab4_multimedia.media_player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.lab4_multimedia.R;

public class PlaylistControlsFragment extends Fragment {
    private ConstraintLayout previous_layout;
    private ConstraintLayout next_layout;

    public PlaylistControlsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getParentFragmentManager().setFragmentResultListener("action_hide_control", this, (requestKey, result) -> {
            previous_layout.setVisibility(result.getBoolean("hide_prev") ? View.INVISIBLE : View.VISIBLE);
            next_layout.setVisibility(result.getBoolean("hide_next") ? View.INVISIBLE : View.VISIBLE);
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist_controls, container, false);

        previous_layout = rootView.findViewById(R.id.playlist_control_previous_layout);
        previous_layout.setOnClickListener(v -> {
            Bundle action = new Bundle();
            action.putBoolean("action_prev", true);
            getParentFragmentManager().setFragmentResult("player_control_action", action);
        });

        next_layout = rootView.findViewById(R.id.playlist_control_next_layout);
        next_layout.setOnClickListener(v -> {
            Bundle action = new Bundle();
            action.putBoolean("action_next", true);
            getParentFragmentManager().setFragmentResult("player_control_action", action);
        });

        SongInfoFragment previous_info = new SongInfoFragment(false, SongInfoFragment.RegisterFor.SONG_INFO_PREVIOUS);
        getParentFragmentManager().beginTransaction().replace(R.id.playlist_control_previous_info, previous_info).commit();

        SongInfoFragment next_info = new SongInfoFragment(false, SongInfoFragment.RegisterFor.SONG_INFO_NEXT);
        getParentFragmentManager().beginTransaction().replace(R.id.playlist_control_next_info, next_info).commit();

        return rootView;
    }
}