package com.example.lab4_multimedia.onboarding;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lab4_multimedia.MainActivity;
import com.example.lab4_multimedia.R;
import com.example.lab4_multimedia.media_player.MediaPlayerMainActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        MaterialToolbar navigation_bar = findViewById(R.id.sign_up_navigation_bar);
        setSupportActionBar(navigation_bar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // From @dira (https://stackoverflow.com/a/10697453)
        SpannableString ss = new SpannableString("Already have an account ? Sign In");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Intent redirect = new Intent(SignUpActivity.this, SignInActivity.class);
                redirect.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(redirect);
                finish();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        ss.setSpan(clickableSpan, ss.toString().indexOf("Sign"), ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView sign_up_redirect = findViewById(R.id.sign_up_redirect);
        sign_up_redirect.setText(ss);
        sign_up_redirect.setMovementMethod(LinkMovementMethod.getInstance());
        sign_up_redirect.setHighlightColor(Color.TRANSPARENT);

        Button sign_up_action = findViewById(R.id.sign_up_action_button);
        sign_up_action.setOnClickListener(v -> {
            final String username = Objects.requireNonNull(((TextInputEditText) (findViewById(R.id.sign_up_username))).getText()).toString();
            final String email = Objects.requireNonNull(((TextInputEditText) (findViewById(R.id.sign_up_email))).getText()).toString();
            final String password = Objects.requireNonNull(((TextInputEditText) (findViewById(R.id.sign_up_password))).getText()).toString();

            MainActivity.firebase_auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Objects.requireNonNull(MainActivity.firebase_auth.getCurrentUser()).updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(username).build()).addOnCompleteListener(task1 -> {
                            Log.d("SignUp", "User created : " + MainActivity.firebase_auth.getCurrentUser());
                            Intent start_media_player = new Intent(SignUpActivity.this, MediaPlayerMainActivity.class);
                            start_media_player.putExtra("signed_in_has", MainActivity.getUsername());
                            start_media_player.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(start_media_player);
                            finish();
                        });
                } else {
                    Log.w("SignUp", "Failed : " + task.getException());
                }
            });
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();

        return super.onOptionsItemSelected(item);
    }
}