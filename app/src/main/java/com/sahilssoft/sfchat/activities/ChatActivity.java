package com.sahilssoft.sfchat.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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
import com.sahilssoft.sfchat.adapter.MessageAdapter;
import com.sahilssoft.sfchat.models.MessageModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    ImageView chatBack,chatLink,chatCamera,chatSend;
    CircleImageView chatImg;
    TextView chatName,chatStatus;
    EditText chatMsg;
    RecyclerView chatRV;

    ImageView ChatSettingImg;

    ArrayList<MessageModel> messageModelList;
    MessageAdapter messageAdapter;

    FirebaseAuth auth;
    ProgressDialog dialog;

    String name, profileImage, token, senderId, receiverId, senderRoom, receiverRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //it is important to show the custom toolbar with menu
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("");

        chatBack = findViewById(R.id.chatBack);
        chatImg = findViewById(R.id.chatImg);
        chatName = findViewById(R.id.chatName);
        chatStatus = findViewById(R.id.chatStatus);
        chatMsg = findViewById(R.id.chatMsg);
        chatLink = findViewById(R.id.chatLink);
        chatCamera = findViewById(R.id.chatCamera);
        chatSend = findViewById(R.id.chatSend);
        chatRV = findViewById(R.id.chatRV);
        ChatSettingImg = findViewById(R.id.ChatSettingImg);

        auth = FirebaseAuth.getInstance();
        dialog = new ProgressDialog(this);
        dialog.setTitle("Please Wait");
        dialog.setCanceledOnTouchOutside(false);

        name = getIntent().getStringExtra("name");
        profileImage = getIntent().getStringExtra("profileImage");
        token = getIntent().getStringExtra("token");

        chatName.setText(name);
        Glide.with(this)
                .load(profileImage)
                .placeholder(R.drawable.img_placeholder)
                .into(chatImg);

        //declare these lines as it is
        receiverId = getIntent().getStringExtra("uid");
        senderId = FirebaseAuth.getInstance().getUid();

        //set online/offline
        FirebaseDatabase.getInstance().getReference("Presence").child(receiverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    String status = snapshot.getValue(String.class);
                    if(!status.isEmpty()) {
                        if(status.equals("Offline")) {
                            chatStatus.setVisibility(View.GONE);
                        } else {
                            chatStatus.setText(status);
                            chatStatus.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        chatBack.setOnClickListener(view -> {
            onBackPressed();
        });

        senderRoom = senderId + receiverId;
        receiverRoom = receiverId + senderId;

        messageModelList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this,messageModelList,senderRoom,receiverRoom);
        chatRV.setAdapter(messageAdapter);

        //load/show messages
        FirebaseDatabase.getInstance().getReference("Chats")
                .child(senderRoom)
                .child("Messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messageModelList.clear();
                        for(DataSnapshot snapshot1 : snapshot.getChildren()) {
                            MessageModel message = snapshot1.getValue(MessageModel.class);
                            message.setMessageId(snapshot1.getKey());
                            messageModelList.add(message);
                        }

                        messageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //send message to database
        chatSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageTxt = chatMsg.getText().toString();

                Date date = new Date();
                MessageModel message = new MessageModel(messageTxt, auth.getUid(), date.getTime());
                chatMsg.setText("");

                String randomKey = FirebaseDatabase.getInstance().getReference().push().getKey();

                HashMap<String, Object> lastMsgObj = new HashMap<>();
                lastMsgObj.put("lastMsg", message.getMessage());
                lastMsgObj.put("lastMsgTime", date.getTime());

                FirebaseDatabase.getInstance().getReference().child("Chats").child(senderRoom).updateChildren(lastMsgObj);
                FirebaseDatabase.getInstance().getReference().child("Chats").child(receiverRoom).updateChildren(lastMsgObj);

                FirebaseDatabase.getInstance().getReference().child("Chats")
                        .child(senderRoom)
                        .child("Messages")
                        .child(randomKey)
                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        FirebaseDatabase.getInstance().getReference().child("Chats")
                                .child(receiverRoom)
                                .child("Messages")
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                sendNotification(name, message.getMessage(), token);
                            }
                        });
                    }
                });

            }
        });


        chatLink.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent,45);
        });
        chatCamera.setOnClickListener(view -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent,46);
        });
        //show the typing visibility
        Handler handler = new Handler();
        chatMsg.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                FirebaseDatabase.getInstance().getReference("Presence").child(senderId).setValue("typing ...");
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        FirebaseDatabase.getInstance().getReference("Presence").child(senderId).setValue("Online");
                    }
                },1000);
            }
        });
    }

    private void sendNotification(String name, String message, String token) {
        try {
            RequestQueue queue = Volley.newRequestQueue(ChatActivity.this);

            JSONObject data = new JSONObject();
            data.put("title",name);
            data.put("body",message);

            JSONObject notificationData = new JSONObject();
            notificationData.put("notification",data);
            notificationData.put("to",token);

            String url = "https://fcm.googleapis.com/fcm/send";

            JsonObjectRequest request = new JsonObjectRequest(url, notificationData, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String,String> map = new HashMap<>();
                    String key = "Key=AAAA0Tdu0r4:APA91bG0nk5S75oudOyIanolfWZ03tF-ijTWQ8UMnC1pOwapMMJQh-KSMBg1RCKtsPeewJUzRTTmeD8Ado53IfqlbV6sJS0Fer6AFqDn7OqSsPPJoWEUi8HM9b3Nn0sAbp0tGnJOOk-z";
                    map.put("Content-Type","application/json");
                    map.put("Authorization",key);

                    return map;
                }
            };

            queue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 45) {
            if(data != null) {
                if(data.getData() != null) {
                    Uri imageUri = data.getData();

                    dialog.setMessage("Sending selected image ...");
                    dialog.show();
                    sendImageToFireBase(imageUri);
                }
            }
        }
        if(requestCode == 46){
            Bitmap bitmap = (Bitmap)data.getExtras().get("data");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,bytes);
            String path = MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(),bitmap,"val",null);
            Uri imageUri = Uri.parse(path);

            dialog.setMessage("Sending selected image ...");
            dialog.show();
            sendImageToFireBase(imageUri);
        }
    }

    private void sendImageToFireBase(Uri imageUri) {
        Calendar calendar = Calendar.getInstance();
        StorageReference reference = FirebaseStorage.getInstance().getReference().child("Chats").child(calendar.getTimeInMillis() + "");
        reference.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()) {
                    reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String filePath = uri.toString();

                            String messageTxt = chatMsg.getText().toString();

                            Date date = new Date();
                            MessageModel message = new MessageModel(messageTxt, auth.getUid(), date.getTime());
                            message.setMessage("photo");
                            message.setImageUrl(filePath);
                            chatMsg.setText("");

                            String randomKey = FirebaseDatabase.getInstance().getReference().push().getKey();

                            HashMap<String, Object> lastMsgObj = new HashMap<>();
                            lastMsgObj.put("lastMsg", message.getMessage());
                            lastMsgObj.put("lastMsgTime", date.getTime());

                            FirebaseDatabase.getInstance().getReference().child("Chats").child(senderRoom).updateChildren(lastMsgObj);
                            FirebaseDatabase.getInstance().getReference().child("Chats").child(receiverRoom).updateChildren(lastMsgObj);

                            FirebaseDatabase.getInstance().getReference().child("Chats")
                                    .child(senderRoom)
                                    .child("Messages")
                                    .child(randomKey)
                                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    FirebaseDatabase.getInstance().getReference().child("Chats")
                                            .child(receiverRoom)
                                            .child("Messages")
                                            .child(randomKey)
                                            .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            dialog.dismiss();
                                            sendNotification(name,message.getMessage(),token);
                                        }
                                    });
                                }
                            });

                            //Toast.makeText(ChatActivity.this, filePath, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.videoCall:
                Toast.makeText(this, "Video call is clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.voiceCall:
                Toast.makeText(this, "Voice call is clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.profile:
                FirebaseDatabase.getInstance().getReference("Users").child(receiverId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        String profileImage = ""+snapshot.child("profileImage").getValue();
                        String name = ""+snapshot.child("name").getValue();
                        String phoneNumber = ""+snapshot.child("phoneNumber").getValue();
                        String about = ""+snapshot.child("about").getValue();

                        View view = LayoutInflater.from(ChatActivity.this).inflate(R.layout.dialog_receiver_profile,null);
                        AlertDialog dialog = new AlertDialog.Builder(ChatActivity.this)
                                .setView(view)
                                .create();

                        //setup up views
                        CircleImageView profile_img = view.findViewById(R.id.profile_img);
                        TextView profile_name = view.findViewById(R.id.profile_name);
                        TextView profile_phone = view.findViewById(R.id.profile_phone);
                        Button profile_Ok = view.findViewById(R.id.profile_Ok);

                        //set data
                        Glide.with(ChatActivity.this)
                                .load(profileImage)
                                .placeholder(R.drawable.img_placeholder)
                                .into(profile_img);
                         profile_name.setText("Name: "+name);
                         profile_phone.setText("Phone No: "+phoneNumber);
                        dialog.show();

                        profile_Ok.setOnClickListener(view1 -> {
                            dialog.dismiss();
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                break;
            case R.id.settings:
                View view = LayoutInflater.from(ChatActivity.this).inflate(R.layout.dialog_setting_bg,null);
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setView(view)
                        .create();

                view.findViewById(R.id.btn1).setOnClickListener(view1 -> {
                    ChatSettingImg.setImageResource(R.drawable.bg1);
                    dialog.dismiss();
                });
                view.findViewById(R.id.btn2).setOnClickListener(view1 -> {
                    ChatSettingImg.setImageResource(R.drawable.bg2);
                    dialog.dismiss();
                });
                view.findViewById(R.id.btn3).setOnClickListener(view1 -> {
                    ChatSettingImg.setImageResource(R.drawable.bg3);
                    dialog.dismiss();
                });
                view.findViewById(R.id.btn4).setOnClickListener(view1 -> {
                    ChatSettingImg.setImageResource(R.drawable.bg4);
                    dialog.dismiss();
                });
                view.findViewById(R.id.btn5).setOnClickListener(view1 -> {
                    ChatSettingImg.setImageResource(R.drawable.bg5);
                    dialog.dismiss();
                });
                view.findViewById(R.id.btn6).setOnClickListener(view1 -> {
                    ChatSettingImg.setImageResource(R.drawable.bg_img);
                    dialog.dismiss();
                });

                dialog.show();
                break;
            case R.id.block:
                Toast.makeText(this, "Block is clicked", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        FirebaseDatabase.getInstance().getReference().child("Presence").child(currentId).setValue("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        String currentId = FirebaseAuth.getInstance().getUid();
        FirebaseDatabase.getInstance().getReference().child("Presence").child(currentId).setValue("Offline");
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return super.onNavigateUp();
    }
}