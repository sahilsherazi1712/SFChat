package com.sahilssoft.sfchat.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cooltechworks.views.shimmer.ShimmerRecyclerView;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sahilssoft.sfchat.R;
import com.sahilssoft.sfchat.adapter.UserStatusAdapter;
import com.sahilssoft.sfchat.adapter.UsersAdapter;
import com.sahilssoft.sfchat.models.StatusModel;
import com.sahilssoft.sfchat.models.UserStatusModel;
import com.sahilssoft.sfchat.models.UsersModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    ArrayList<UsersModel> usersModelList;
    UsersAdapter usersAdapter;

    ArrayList<UserStatusModel> userStatusModelList;
    UserStatusAdapter userStatusAdapter;

    ShimmerRecyclerView mainChatsRV,mainStatusRV;
    FirebaseAuth auth;

    BottomNavigationView bottomNavigationView;

    UsersModel user;

    ImageView chatBgImg;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //background controls
        chatBgImg = findViewById(R.id.chatBgImg);

        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);

        remoteConfig.fetchAndActivate().addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                //main activity
                String backgroundImage = remoteConfig.getString("backgroundImage");
                Glide.with(MainActivity.this)
                        .load(backgroundImage)
                        .into(chatBgImg);

                //toolbar customization
                String toolbarColor = remoteConfig.getString("toolbarColor");
                String toolbarImage = remoteConfig.getString("toolbarImage");
                Boolean isToolbarImageEnabled = remoteConfig.getBoolean("isToolbarImageEnabled");
                if (isToolbarImageEnabled){
                    Glide.with(MainActivity.this)
                            .load(toolbarImage)
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    getSupportActionBar().setBackgroundDrawable(resource);
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                }
                            });
                }else{
                    getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(toolbarColor)));
                }

            }
        });

        mainChatsRV = findViewById(R.id.mainChatsRV);
        mainStatusRV = findViewById(R.id.mainStatusRV);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        auth = FirebaseAuth.getInstance();

        dialog = new ProgressDialog(this);
        dialog.setTitle("Please Wait");
        dialog.setCanceledOnTouchOutside(false);

        //get token and store it
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
                //setup token to save
                HashMap<String,Object> hashMap = new HashMap<>();
                hashMap.put("token",s);

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                reference.child(auth.getUid()).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Toast.makeText(MainActivity.this, "Token: "+s, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        usersModelList = new ArrayList<>();
        userStatusModelList = new ArrayList<>();

        usersAdapter = new UsersAdapter(MainActivity.this,usersModelList);
        userStatusAdapter = new UserStatusAdapter(MainActivity.this,userStatusModelList);

        mainChatsRV.setAdapter(usersAdapter);
        mainStatusRV.setAdapter(userStatusAdapter);

        mainChatsRV.showShimmerAdapter();
        mainStatusRV.showShimmerAdapter();

        //show chatting user
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersModelList.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()){
                    UsersModel usersModel = snapshot1.getValue(UsersModel.class);
                    if (!usersModel.getUid().equals(auth.getUid())){ ///to clear that current user is not shown
                        usersModelList.add(usersModel);
                    }
                }
                mainChatsRV.hideShimmerAdapter();
                usersAdapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //show status
        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("Stories");
        reference1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    userStatusModelList.clear();
                    for(DataSnapshot storySnapshot : snapshot.getChildren()) {
                        UserStatusModel status = new UserStatusModel();
                        status.setName(storySnapshot.child("name").getValue(String.class));
                        status.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));
                        status.setLastUpdated(storySnapshot.child("lastUpdated").getValue(Long.class));

                        ArrayList<StatusModel> statuses = new ArrayList<>();

                        for(DataSnapshot statusSnapshot : storySnapshot.child("Statuses").getChildren()) {
                            StatusModel sampleStatus = statusSnapshot.getValue(StatusModel.class);
                            statuses.add(sampleStatus);
                        }

                        status.setStatuses(statuses);
                        userStatusModelList.add(status);
                    }
                    mainStatusRV.hideShimmerAdapter();
                    userStatusAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

     /////bottom navigation view clicks
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                switch (id){
                    case R.id.status:
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent,75);
                        break;
                    case R.id.groupChat:
                        startActivity(new Intent(MainActivity.this,GroupChatActivity.class));
                        break;
                }
                return false;
            }
        });

        //get user to save its data to stories
        DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference("Users");
        reference2.child(auth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                user = snapshot.getValue(UsersModel.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null){
            Uri imageUri = data.getData();
            Date date =new Date();


            dialog.setMessage("Updating your status ...");
            dialog.show();
            StorageReference storageReference = FirebaseStorage.getInstance().getReference("Statuses").child(date.getTime()+"");
            storageReference.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()){
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                UserStatusModel userStatus = new UserStatusModel();
                                userStatus.setName(user.getName());
                                userStatus.setProfileImage(user.getProfileImage());
                                userStatus.setLastUpdated(date.getTime());

                                HashMap<String, Object> obj = new HashMap<>();
                                obj.put("name", userStatus.getName());
                                obj.put("profileImage", userStatus.getProfileImage());
                                obj.put("lastUpdated", userStatus.getLastUpdated());

                                String imageUrl = uri.toString();
                                StatusModel status = new StatusModel(imageUrl, userStatus.getLastUpdated());

                                FirebaseDatabase.getInstance().getReference("Stories")
                                        .child(auth.getUid())
                                        .updateChildren(obj);

                                FirebaseDatabase.getInstance().getReference("Stories")
                                        .child(auth.getUid())
                                        .child("Statuses")
                                        .push()
                                        .setValue(status);
                                dialog.dismiss();
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_top_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.invite:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT,"Check app via\n"
                        +"https://play.google.com/store/apps/details?id="
                        +getApplicationContext().getPackageName());
                startActivity(Intent.createChooser(intent,"Share app via"));
                break;
            case R.id.about:
                startActivity(new Intent(MainActivity.this,AboutActivity.class));
                break;
            case R.id.logOut:
                auth.signOut();
                startActivity(new Intent(MainActivity.this,PhoneNumberActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentId = auth.getUid();
        FirebaseDatabase.getInstance().getReference("Presence").child(currentId).setValue("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        String currentId = auth.getUid();
        FirebaseDatabase.getInstance().getReference("Presence").child(currentId).setValue("Offline");
    }
}