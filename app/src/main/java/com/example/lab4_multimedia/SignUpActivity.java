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

public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth firebase_auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        firebase_auth = FirebaseAuth.getInstance();

        MaterialToolbar navigation_bar = (MaterialToolbar) findViewById(R.id.sign_up_navigation_bar);
        setSupportActionBar(navigation_bar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        sign_up_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = ((EditText)(findViewById(R.id.sign_up_username))).getText().toString(); // TODO : Use this field :))
                final String email = ((EditText)(findViewById(R.id.sign_up_email))).getText().toString();
                final String password = ((EditText)(findViewById(R.id.sign_up_password))).getText().toString();

                firebase_auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("SignUp", "User created : " + email);
                            Intent start_media_player = new Intent(SignUpActivity.this, MediaPlayerMainActivity.class);
                            start_media_player.putExtra("signed_in_has", firebase_auth.getCurrentUser().getEmail());
                            start_media_player.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(start_media_player);
                            finish();
                        } else {
                            Log.w("SignUp", "Failed : " + task.getException());
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