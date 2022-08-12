package com.sahilssoft.sfchat.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sahilssoft.sfchat.R;
import com.sahilssoft.sfchat.activities.ChatActivity;
import com.sahilssoft.sfchat.models.UsersModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {
    Context context;
    ArrayList<UsersModel> usersModelList;

    public UsersAdapter(Context context, ArrayList<UsersModel> usersModelList) {
        this.context = context;
        this.usersModelList = usersModelList;
    }

    @NonNull
    @Override
    public UsersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_main_chats,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersAdapter.ViewHolder holder, int position) {
        //get data
        String uid = usersModelList.get(position).getUid();
        String name = usersModelList.get(position).getName();
        String phoneNumber = usersModelList.get(position).getPhoneNumber();
        String profileImage = usersModelList.get(position).getProfileImage();
        String about = usersModelList.get(position).getAbout();
        String token = usersModelList.get(position).getToken();

        //set data
        holder.username.setText(name);
        Glide.with(context)
                .load(profileImage)
                .placeholder(R.drawable.avatar)
                .into(holder.profile);
        //set data, last Msg
        String senderId = FirebaseAuth.getInstance().getUid();
        String senderRoom = senderId + uid;
        FirebaseDatabase.getInstance().getReference()
                .child("Chats")
                .child(senderRoom)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()) {
                            String lastMsg = snapshot.child("lastMsg").getValue(String.class);
                            long time = snapshot.child("lastMsgTime").getValue(Long.class);
                            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a",Locale.getDefault());
                            holder.msgTime.setText(dateFormat.format(new Date(time)));
                            holder.lastMsg.setText(lastMsg);
                        } else {
                            holder.lastMsg.setText("Tap to chat");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("name",name);
            intent.putExtra("profileImage",profileImage);
            intent.putExtra("token",token);
            intent.putExtra("uid",uid);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return usersModelList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView profile;
        TextView username,lastMsg,msgTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            profile =itemView.findViewById(R.id.profile);
            username =itemView.findViewById(R.id.username);
            lastMsg =itemView.findViewById(R.id.lastMsg);
            msgTime =itemView.findViewById(R.id.msgTime);
        }
    }
}
