package com.example.lab4_multimedia;

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
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignInActivity extends AppCompatActivity {
    private FirebaseAuth firebase_auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        firebase_auth = FirebaseAuth.getInstance();

        MaterialToolbar navigation_bar = (MaterialToolbar) findViewById(R.id.sign_in_navigation_bar);
        setSupportActionBar(navigation_bar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        sign_in_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = ((EditText)(findViewById(R.id.sign_in_email))).getText().toString();
                final String password = ((EditText)(findViewById(R.id.sign_in_password))).getText().toString();

                firebase_auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("SignIn", "User signed in : " + email);
                            Intent start_media_player = new Intent(SignInActivity.this, MediaPlayerMainActivity.class);
                            start_media_player.putExtra("signed_in_has", firebase_auth.getCurrentUser().getEmail());
                            start_media_player.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(start_media_player);
                            finish();
                        } else {
                            Log.w("SignIn", "Failed : " + task.getException());
                        }
                    }
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();

        return super.onOptionsItemSelected(item);
    }
}