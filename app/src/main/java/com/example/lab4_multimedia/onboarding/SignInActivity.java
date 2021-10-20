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

import com.example.lab4_multimedia.R;
import com.example.lab4_multimedia.media_player.MediaPlayerMainActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class SignInActivity extends AppCompatActivity {
    private FirebaseAuth firebase_auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        firebase_auth = FirebaseAuth.getInstance();

        MaterialToolbar navigation_bar = findViewById(R.id.sign_in_navigation_bar);
        setSupportActionBar(navigation_bar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // From @dira (https://stackoverflow.com/a/10697453)
        SpannableString ss = new SpannableString("Don't have an account ? Sign Up");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Intent redirect = new Intent(SignInActivity.this, SignUpActivity.class);
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

        TextView sign_in_redirect = findViewById(R.id.sign_in_redirect);
        sign_in_redirect.setText(ss);
        sign_in_redirect.setMovementMethod(LinkMovementMethod.getInstance());
        sign_in_redirect.setHighlightColor(Color.TRANSPARENT);

        Button sign_in_action = findViewById(R.id.sign_in_action_button);
        sign_in_action.setOnClickListener(v -> {
            final String email = Objects.requireNonNull(((TextInputEditText) (findViewById(R.id.sign_in_email))).getText()).toString();
            final String password = Objects.requireNonNull(((TextInputEditText) (findViewById(R.id.sign_in_password))).getText()).toString();

            firebase_auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("SignIn", "User signed in : " + email);
                    Intent start_media_player = new Intent(SignInActivity.this, MediaPlayerMainActivity.class);
                    start_media_player.putExtra("signed_in_has", Objects.requireNonNull(firebase_auth.getCurrentUser()).getEmail());
                    start_media_player.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(start_media_player);
                    finish();
                } else {
                    Log.w("SignIn", "Failed : " + task.getException());
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