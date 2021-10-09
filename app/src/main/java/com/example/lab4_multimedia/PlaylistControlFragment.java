package com.example.lab4_multimedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

public class PlaylistControlFragment extends Fragment {
    private LinearLayout previous_layout;
    private LinearLayout next_layout;
    private SongInfoFragment previous_info;
    private SongInfoFragment next_info;

    public PlaylistControlFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getParentFragmentManager().setFragmentResultListener("action_hide_control", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                previous_layout.setVisibility(result.getBoolean("hide_prev") ? View.INVISIBLE : View.VISIBLE);
                next_layout.setVisibility(result.getBoolean("hide_next") ? View.INVISIBLE : View.VISIBLE);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist_control, container, false);

        previous_layout = rootView.findViewById(R.id.playlist_control_previous_layout);
        next_layout = rootView.findViewById(R.id.playlist_control_next_layout);

        ImageButton previous = rootView.findViewById(R.id.playlist_control_previous);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle action = new Bundle();
                action.putBoolean("action_prev", true);
                getParentFragmentManager().setFragmentResult("player_control_action", action);
            }
        });

        ImageButton next = rootView.findViewById(R.id.playlist_control_next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle action = new Bundle();
                action.putBoolean("action_next", true);
                getParentFragmentManager().setFragmentResult("player_control_action", action);
            }
        });

        previous_info = new SongInfoFragment(false, SongInfoFragment.RegisterFor.SONG_INFO_PREVIOUS);
        getParentFragmentManager().beginTransaction().replace(R.id.playlist_control_previous_info, previous_info).commit();

        next_info = new SongInfoFragment(false, SongInfoFragment.RegisterFor.SONG_INFO_NEXT);
        getParentFragmentManager().beginTransaction().replace(R.id.playlist_control_next_info, next_info).commit();

        return rootView;
    }
}