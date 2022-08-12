package com.sahilssoft.sfchat.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.sahilssoft.sfchat.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        findViewById(R.id.rateUs).setOnClickListener(view -> {
            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id="+ getApplicationContext().getPackageName());
            Intent intent = new Intent(Intent.ACTION_VIEW,uri);
            startActivity(intent);
        });

        getSupportActionBar().hide();
    }
}