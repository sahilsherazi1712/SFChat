package com.sahilssoft.sfchat.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.hbb20.CountryCodePicker;
import com.sahilssoft.sfchat.R;

public class PhoneNumberActivity extends AppCompatActivity {

    CountryCodePicker ccp;
    EditText phoneNumberEt;
    Button continueBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number);
        //hide action bar
        getSupportActionBar().hide();

        ccp = findViewById(R.id.ccp);
        phoneNumberEt = findViewById(R.id.phoneNumberEt);
        continueBtn = findViewById(R.id.continueBtn);

        continueBtn.setOnClickListener(view -> {
            String phoneNum = phoneNumberEt.getText().toString();
            String phoneNumber = ccp.getSelectedCountryCodeWithPlus()+phoneNum;
            Intent intent = new Intent(PhoneNumberActivity.this,OTPActivity.class);
            intent.putExtra("phoneNumber",phoneNumber);
            startActivity(intent);
            finish();
        });

    }
}