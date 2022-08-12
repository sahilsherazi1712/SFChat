package com.sahilssoft.sfchat.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sahilssoft.sfchat.R;
import com.sahilssoft.sfchat.adapter.GroupMessageAdapter;
import com.sahilssoft.sfchat.adapter.MessageAdapter;
import com.sahilssoft.sfchat.models.MessageModel;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupChatActivity extends AppCompatActivity {

    ImageView chatGroupLink,chatGroupCamera,chatGroupSend;
    EditText chatGroupMsg;
    RecyclerView chatGroupRV;

    FirebaseAuth auth;

    ProgressDialog dialog;

    ArrayList<MessageModel> messageModelList;
    GroupMessageAdapter groupMessageAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        chatGroupLink = findViewById(R.id.chatGroupLink);
        chatGroupCamera = findViewById(R.id.chatGroupCamera);
        chatGroupSend = findViewById(R.id.chatGroupSend);
        chatGroupMsg = findViewById(R.id.chatGroupMsg);
        chatGroupRV = findViewById(R.id.chatGroupRV);

        auth = FirebaseAuth.getInstance();
        dialog = new ProgressDialog(this);
        dialog.setTitle("Please Wait");
        dialog.setCanceledOnTouchOutside(false);

        messageModelList = new ArrayList<>();
        groupMessageAdapter = new GroupMessageAdapter(this,messageModelList);
        chatGroupRV.setAdapter(groupMessageAdapter);

        //show messages in group chat activity
        FirebaseDatabase.getInstance().getReference("Public").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageModelList.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()){
                    MessageModel messageModel = snapshot1.getValue(MessageModel.class);
                    messageModel.setMessageId(snapshot1.getKey());
                    messageModelList.add(messageModel);
                }
                groupMessageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //send messages to database
        chatGroupSend.setOnClickListener(view -> {
            String messageTxt = chatGroupMsg.getText().toString();
            Date date = new Date();
            MessageModel messageModel = new MessageModel(messageTxt,auth.getUid(),date.getTime());
            chatGroupMsg.setText("");

            FirebaseDatabase.getInstance()
                    .getReference("Public")
                    .push()
                    .setValue(messageModel);
        });

        //link image button
        chatGroupLink.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent,45);
        });

        getSupportActionBar().setTitle("Local Group Chat");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null){
            Uri imageUrl = data.getData();

            dialog.setMessage("Sending selected image ...");
            dialog.show();

            Calendar calendar = Calendar.getInstance();
            StorageReference storageReference = FirebaseStorage.getInstance().getReference("Public").child(calendar.getTimeInMillis()+"");
            storageReference.putFile(imageUrl).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()){
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String downloadImageUri = uri.toString();
                                String messageTxt = chatGroupMsg.getText().toString();
                                Date date = new Date();
                                MessageModel messageModel = new MessageModel(messageTxt,auth.getUid(),date.getTime());
                                messageModel.setImageUrl(downloadImageUri);
                                messageModel.setMessage("photo");
                                chatGroupMsg.setText("");

                                FirebaseDatabase.getInstance()
                                        .getReference("Public")
                                        .push()
                                        .setValue(messageModel);
                                dialog.dismiss();
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}