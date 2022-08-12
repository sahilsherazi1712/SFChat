package com.sahilssoft.sfchat.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;
import com.sahilssoft.sfchat.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        getSupportActionBar().hide();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //from splash to main activity
                FirebaseAuth auth = FirebaseAuth.getInstance();
                if (auth.getCurrentUser() != null){
                    startActivity(new Intent(SplashActivity.this,MainActivity.class));
                    finish();
                }else{
                    startActivity(new Intent(SplashActivity.this,PhoneNumberActivity.class));
                    finish();
                }
            }
        },2000);
    }
}