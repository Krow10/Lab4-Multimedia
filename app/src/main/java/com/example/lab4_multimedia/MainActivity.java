package com.example.lab4_multimedia;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lab4_multimedia.media_player.MediaPlayerMainActivity;
import com.example.lab4_multimedia.onboarding.SignInActivity;
import com.example.lab4_multimedia.onboarding.SignUpActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public static FirebaseAuth firebase_auth;
    public static FirebaseStorage firebase_storage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebase_auth = FirebaseAuth.getInstance();
        firebase_storage = FirebaseStorage.getInstance();

        Button offline_mode = findViewById(R.id.offline_mode_button);
        offline_mode.setOnClickListener(v -> {
            Intent start_media_player = new Intent(MainActivity.this, MediaPlayerMainActivity.class);
            start_media_player.putExtra("offline_mode", true);
            startActivity(start_media_player);
        });

        Button sign_in = findViewById(R.id.sign_in_button);
        sign_in.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SignInActivity.class)));

        Button sign_up = findViewById(R.id.sign_up_button);
        sign_up.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SignUpActivity.class)));
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = firebase_auth.getCurrentUser();
        if(currentUser != null) {
            Log.d("Main", "User " + currentUser.getEmail() + " is logged in !");
            Intent start_media_player = new Intent(MainActivity.this, MediaPlayerMainActivity.class);
            start_media_player.putExtra("signed_in_has", getUsername());
            startActivity(start_media_player);
            finish();
        } else {
            Log.d("Main", "No user logged in");
        }
    }

    public static String getUsername() {
        return Objects.requireNonNull(firebase_auth.getCurrentUser()).getDisplayName();
    }
}
