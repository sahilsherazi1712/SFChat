package com.sahilssoft.sfchat.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sahilssoft.sfchat.R;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupProfileActivity extends AppCompatActivity {

    CircleImageView profileImg;
    EditText nameEt;
    Button setupProfileBtn;
    FirebaseAuth auth;
    Uri imageUri;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_profile);

        getSupportActionBar().hide();

        profileImg = findViewById(R.id.profileImg);
        nameEt = findViewById(R.id.nameEt);
        setupProfileBtn = findViewById(R.id.setupProfileBtn);

        auth = FirebaseAuth.getInstance();
        dialog = new ProgressDialog(this);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setTitle("Please Wait");

        profileImg.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent,45);
        });

        setupProfileBtn.setOnClickListener(view -> {
            //declare data
            dialog.setMessage("Process continue ...");
            dialog.show();

            String name = nameEt.getText().toString();
            String phoneNumber = auth.getCurrentUser().getPhoneNumber();
            String uid = auth.getUid();

            if (TextUtils.isEmpty(name)){
                nameEt.setError("Please Enter Your Name");
                return;
            }

            if(imageUri == null){
                //save data without image
                //setup data to save
                dialog.setMessage("Saving your information ...");
                dialog.show();

                HashMap<String,Object> hashMap = new HashMap<>();
                hashMap.put("uid",""+uid);
                hashMap.put("name",""+name);
                hashMap.put("phoneNumber",""+phoneNumber);
                hashMap.put("profileImage","");

                //save data
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                reference.child(auth.getUid()).setValue(hashMap).addOnSuccessListener(unused -> {
                    dialog.dismiss();
                    startActivity(new Intent(SetupProfileActivity.this,MainActivity.class));
                    finish();
                });
            }else{
                //save data with image

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Profile").child(auth.getUid());
                storageReference.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                dialog.setMessage("Saving your information ...");
                                dialog.show();

                                String downloadImageUri = uri.toString();
                                //setup data to save
                                HashMap<String,Object> hashMap = new HashMap<>();
                                hashMap.put("uid",""+uid);
                                hashMap.put("name",""+name);
                                hashMap.put("phoneNumber",""+phoneNumber);
                                hashMap.put("profileImage",""+downloadImageUri);
                                //save data
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                                reference.child(auth.getUid()).setValue(hashMap)
                                        .addOnSuccessListener(unused -> {
                                            dialog.dismiss();
                                            startActivity(new Intent(SetupProfileActivity.this, MainActivity.class));
                                            finish();
                                        });
                            }
                        });
                    }
                });

            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null){
            //img to store into db
            imageUri = data.getData();
            //img to set on profile
            profileImg.setImageURI(imageUri);
        }
    }
}