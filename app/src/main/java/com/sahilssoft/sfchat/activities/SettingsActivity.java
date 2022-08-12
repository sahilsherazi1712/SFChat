package com.sahilssoft.sfchat.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

public class SettingsActivity extends AppCompatActivity {

    CircleImageView profileImg;
    EditText nameEt,aboutEt;
    Button updateProfileBtn;
    FirebaseAuth auth;
    Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        profileImg = findViewById(R.id.profileImg);
        nameEt = findViewById(R.id.nameEt);
        aboutEt = findViewById(R.id.aboutEt);
        updateProfileBtn = findViewById(R.id.updateProfileBtn);

        auth = FirebaseAuth.getInstance();

        loadInfo();

        profileImg.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent,45);
        });

        updateProfileBtn.setOnClickListener(view -> {
            String name = nameEt.getText().toString();
            String about = aboutEt.getText().toString();

            if (imageUri == null){
                //setup data to save
                HashMap<String,Object> hashMap = new HashMap<>();
                hashMap.put("name",""+name);
                hashMap.put("about",""+about);
                hashMap.put("profileImage","");
                //save data
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                reference.child(auth.getUid()).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(SettingsActivity.this, "Your Changes are saved", Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Profile").child(auth.getUid());
                storageReference.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String downloadImageUrl = uri.toString();
                                //setup data to save
                                HashMap<String,Object> hashMap = new HashMap<>();
                                hashMap.put("name",""+name);
                                hashMap.put("about",""+about);
                                hashMap.put("profileImage",""+downloadImageUrl);
                                //save data
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                                reference.child(auth.getUid()).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(SettingsActivity.this, "Your Changes are saved", Toast.LENGTH_SHORT).show();
                                    }
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
            //image to store
            imageUri = data.getData();
            //set image profile
            profileImg.setImageURI(imageUri);
        }
    }

    private void loadInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(auth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //get data
                String uid = ""+snapshot.child("uid").getValue();
                String name = ""+snapshot.child("name").getValue();
                String phoneNumber = ""+snapshot.child("phoneNumber").getValue();
                String profileImage = ""+snapshot.child("profileImage").getValue();
                String about = ""+snapshot.child("about").getValue();

                //set data
                nameEt.setText(name);
                aboutEt.setText(about);
                Glide.with(SettingsActivity.this)
                        .load(profileImage)
                        .placeholder(R.drawable.avatar)
                        .into(profileImg);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        getSupportActionBar().setTitle("Profile Update");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}