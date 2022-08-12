package com.sahilssoft.sfchat.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.mukesh.OtpView;
import com.sahilssoft.sfchat.R;

import java.util.concurrent.TimeUnit;

public class OTPActivity extends AppCompatActivity {

    TextView phoneLbl,timer;
    OtpView otpView;
    Button continueBtn,resendBtn;

    String verificationCode;

    FirebaseAuth auth;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otpactivity);

        String phoneNumber = getIntent().getStringExtra("phoneNumber");

        phoneLbl = findViewById(R.id.phoneLbl);
        otpView = findViewById(R.id.otpView);
        continueBtn = findViewById(R.id.continueBtn);
        timer = findViewById(R.id.timer);
        resendBtn = findViewById(R.id.resendBtn);

        auth = FirebaseAuth.getInstance();
        dialog = new ProgressDialog(this);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setTitle("Please Wait");

        timer.setVisibility(View.INVISIBLE);
        resendBtn.setVisibility(View.INVISIBLE);

        phoneLbl.setText("Verify "+phoneNumber);

        sendVerificationCode(phoneNumber);
        dialog.setMessage("Process Proceeding ...");
        dialog.show();

        continueBtn.setOnClickListener(view -> {
            dialog.setMessage("All done ...");
            dialog.show();
            String code = otpView.getText().toString();
            resendBtn.setVisibility(View.INVISIBLE);
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCode,code);
            auth.signInWithCredential(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    dialog.dismiss();
                    startActivity(new Intent(OTPActivity.this,SetupProfileActivity.class));
                    finishAffinity();
                }else{
                    Toast.makeText(this, "Error:"+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        new CountDownTimer(60000,1000){
            @Override
            public void onTick(long l) {
                timer.setVisibility(View.VISIBLE);
                timer.setText("Resend Code Within "+l/1000+" Seconds");
            }

            @Override
            public void onFinish() {
                timer.setVisibility(View.INVISIBLE);
                resendBtn.setVisibility(View.VISIBLE);
            }
        }.start();

        resendBtn.setOnClickListener(view -> {
            resendBtn.setVisibility(View.INVISIBLE);

            dialog.setMessage("Process Proceeding ...");
            dialog.show();
            sendVerificationCode(phoneNumber);
            new CountDownTimer(60000,1000){
                @Override
                public void onTick(long l) {
                    timer.setVisibility(View.VISIBLE);
                    timer.setText("Resend Code Within "+l/1000+" Seconds");
                }
                @Override
                public void onFinish() {
                    timer.setVisibility(View.INVISIBLE);
                    resendBtn.setVisibility(View.VISIBLE);
                }
            }.start();
        });
    }

    private void sendVerificationCode(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder()
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(OTPActivity.this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        String code = phoneAuthCredential.getSmsCode();
                        otpView.setText(code);
                        if (code != null){
                            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationCode,code);
                            auth.signInWithCredential(credential).addOnCompleteListener(task -> {
                                if (task.isSuccessful()){
                                    dialog.dismiss();
                                    startActivity(new Intent(OTPActivity.this,SetupProfileActivity.class));
                                    finishAffinity();
                                }else{
                                    Toast.makeText(OTPActivity.this, "Error:"+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {

                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        verificationCode = s;
                        dialog.dismiss();

                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                        otpView.requestFocus();
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }
}