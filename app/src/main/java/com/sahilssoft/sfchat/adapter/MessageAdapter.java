package com.sahilssoft.sfchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.sahilssoft.sfchat.R;
import com.sahilssoft.sfchat.models.MessageModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter {
    Context context;
    ArrayList<MessageModel> messageModelList;
    String senderRoom, receiverRoom;

    FirebaseRemoteConfig remoteConfig;
    final int ITEM_SEND = 1;
    final int ITEM_RECEIVE = 2;

    public MessageAdapter(Context context, ArrayList<MessageModel> messageModelList, String senderRoom, String receiverRoom) {
        this.context = context;
        this.messageModelList = messageModelList;
        this.senderRoom = senderRoom;
        this.receiverRoom = receiverRoom;
        remoteConfig = FirebaseRemoteConfig.getInstance();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType==ITEM_SEND){
            return new SenderViewHolder(LayoutInflater.from(context).inflate(R.layout.item_send,parent,false));
        }else{
            return new ReceiverViewHolder(LayoutInflater.from(context).inflate(R.layout.item_receive,parent,false));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (FirebaseAuth.getInstance().getUid().equals(messageModelList.get(position).getSenderId())){
            return ITEM_SEND;
        }else{
            return ITEM_RECEIVE;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messageModelList.get(position);
        //set feelings
        //declare feelings array
        int reactions[] = new int[]{
                R.drawable.ic_fb_like,
                R.drawable.ic_fb_love,
                R.drawable.ic_fb_lovee,
                R.drawable.ic_fb_laugh,
                R.drawable.ic_fb_wow,
                R.drawable.ic_fb_sad,
                R.drawable.ic_fb_angry,
        };
        //config declare
        ReactionsConfig config = new ReactionsConfigBuilder(context)
                .withReactions(reactions)
                .build();
        //set config to popup menu
        ReactionPopup popup = new ReactionPopup(context,config, (pos) -> {
            //enable auto feelings
            if (pos < 0)
                return false;

            if (holder.getClass()==SenderViewHolder.class){
                SenderViewHolder viewHolder =(SenderViewHolder) holder;
                viewHolder.feelingsSend.setImageResource(reactions[pos]);
                viewHolder.feelingsSend.setVisibility(View.VISIBLE);
            }else{
                ReceiverViewHolder viewHolder =(ReceiverViewHolder) holder;
                viewHolder.feelingsReceive.setImageResource(reactions[pos]);
                viewHolder.feelingsReceive.setVisibility(View.VISIBLE);
            }

            message.setFeelings(pos);

            FirebaseDatabase.getInstance().getReference()
                    .child("Chats")
                    .child(senderRoom)
                    .child("Messages")
                    .child(message.getMessageId()).setValue(message);
            FirebaseDatabase.getInstance().getReference()
                    .child("Chats")
                    .child(receiverRoom)
                    .child("Messages")
                    .child(message.getMessageId()).setValue(message);

            return true;
        });

        if (holder.getClass() == SenderViewHolder.class) {
            SenderViewHolder viewHolder = (SenderViewHolder) holder;

            if (message.getMessage().equals("photo")){
                viewHolder.msgSend.setVisibility(View.GONE);
                viewHolder.imgSend.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.img_placeholder)
                        .into(viewHolder.imgSend);
            }

            viewHolder.msgSend.setText(message.getMessage());
            //set msg time
            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            viewHolder.timeSend.setText(dateFormat.format(message.getTimeStamp()));

            if (message.getFeelings()>=0){
                viewHolder.feelingsSend.setVisibility(View.VISIBLE);
                viewHolder.feelingsSend.setImageResource(reactions[message.getFeelings()]);
            }else{
                viewHolder.feelingsSend.setVisibility(View.INVISIBLE);
            }

            viewHolder.msgSend.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    //backend changes
                    boolean isFeelingsEnabled = remoteConfig.getBoolean("isFeelingsEnabled");
                    if (isFeelingsEnabled){
                        popup.onTouch(view, motionEvent);
                    }else{
                        Toast.makeText(context, "This feature is disabled temporarily!", Toast.LENGTH_SHORT).show();
                    }
                    //popup.onTouch(view, motionEvent);
                    return false;
                }
            });
            viewHolder.imgSend.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    popup.onTouch(view,motionEvent);
                    return false;
                }
            });

            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    View view1 = LayoutInflater.from(context).inflate(R.layout.dialog_delete,null);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setMessage("Do you want to delete: "+message.getMessage())
                            .setView(view1)
                            .create();

                    TextView everyone = view1.findViewById(R.id.everyone);
                    TextView delete = view1.findViewById(R.id.delete);
                    TextView cancel = view1.findViewById(R.id.cancel);

                    //backend changes
//                    if (remoteConfig.getBoolean("isEveryoneDeletionEnabled")){
//                        everyone.setVisibility(View.VISIBLE);
//                    }else{
//                        everyone.setVisibility(View.GONE);
//                    }

                    everyone.setOnClickListener(view2 -> {
                        message.setMessage("This message is removed");
                        message.setFeelings(-1);
                        viewHolder.timeSend.setVisibility(View.GONE);

                        FirebaseDatabase.getInstance().getReference()
                                .child("Chats")
                                .child(senderRoom)
                                .child("Messages")
                                .child(message.getMessageId()).setValue(message);
                        FirebaseDatabase.getInstance().getReference()
                                .child("Chats")
                                .child(receiverRoom)
                                .child("Messages")
                                .child(message.getMessageId()).setValue(message);
                        dialog.dismiss();
                    });
                    delete.setOnClickListener(view2 -> {
                        message.setMessage("This message is removed");
                        message.setFeelings(-1);
                        viewHolder.timeSend.setVisibility(View.GONE);

                        FirebaseDatabase.getInstance().getReference()
                                .child("Chats")
                                .child(senderRoom)
                                .child("Messages")
                                .child(message.getMessageId()).setValue(message);
                        dialog.dismiss();
                    });
                    cancel.setOnClickListener(view2 -> {
                        dialog.dismiss();
                    });
                    dialog.show();
                    return false;
                }
            });
        }else{
            ReceiverViewHolder viewHolder = (ReceiverViewHolder) holder;

            if (message.getMessage().equals("photo")){
                viewHolder.msgReceive.setVisibility(View.GONE);
                viewHolder.imgReceive.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.img_placeholder)
                        .into(viewHolder.imgReceive);
            }

            viewHolder.msgReceive.setText(message.getMessage());

            //set msg time
            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            viewHolder.timeReceive.setText(dateFormat.format(new Date(message.getTimeStamp())));

//            FirebaseDatabase.getInstance().getReference()
//                    .child("Chats")
//                    .child(receiverRoom)
//                    .child(message.getMessageId())
//                    .addValueEventListener(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            if(snapshot.exists()) {
//                                long time = snapshot.child("timeStamp").getValue(Long.class);
//                                SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
//                                viewHolder.timeReceive.setText(dateFormat.format(new Date(time)));
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//
//                        }
//                    });

            if (message.getFeelings()>=0){
                viewHolder.feelingsReceive.setVisibility(View.VISIBLE);
                viewHolder.feelingsReceive.setImageResource(reactions[message.getFeelings()]);
            }else{
                viewHolder.feelingsReceive.setVisibility(View.INVISIBLE);
            }

            viewHolder.msgReceive.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    //boolean isFeelingsEnabled = remoteConfig.getBoolean("isFeelingsEnabled");
                    //            if (isFeelingsEnabled){
                    //            }else{
                    //                Toast.makeText(context, "This Feeling is disabled temporarily", Toast.LENGTH_SHORT).show();
                    //            }
                    popup.onTouch(view,motionEvent);
                    return false;
                }
            });
            viewHolder.imgReceive.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    popup.onTouch(view,motionEvent);
                    return false;
                }
            });

            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    View view1 = LayoutInflater.from(context).inflate(R.layout.dialog_delete,null);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setMessage(message.getMessage())
                            .setView(view1)
                            .create();

                    TextView everyone = view1.findViewById(R.id.everyone);
                    TextView delete = view1.findViewById(R.id.delete);
                    TextView cancel = view1.findViewById(R.id.cancel);

                    everyone.setOnClickListener(view2 -> {
                        message.setMessage("This message is removed");
                        message.setFeelings(-1);
                        viewHolder.timeReceive.setVisibility(View.GONE);

                        FirebaseDatabase.getInstance().getReference()
                                .child("Chats")
                                .child(senderRoom)
                                .child("Messages")
                                .child(message.getMessageId()).setValue(message);
                        FirebaseDatabase.getInstance().getReference()
                                .child("Chats")
                                .child(receiverRoom)
                                .child("Messages")
                                .child(message.getMessageId()).setValue(message);
                        dialog.dismiss();
                    });
                    delete.setOnClickListener(view2 -> {
                        message.setMessage("This message is removed");
                        message.setFeelings(-1);
                        viewHolder.timeReceive.setVisibility(View.GONE);

                        FirebaseDatabase.getInstance().getReference()
                                .child("Chats")
                                .child(senderRoom)
                                .child("Messages")
                                .child(message.getMessageId()).setValue(message);
                        dialog.dismiss();
                    });
                    cancel.setOnClickListener(view2 -> {
                        dialog.dismiss();
                    });
                    dialog.show();
                    return false;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return messageModelList.size();
    }

    public class SenderViewHolder extends RecyclerView.ViewHolder {

        ImageView imgSend,feelingsSend;
        TextView msgSend,timeSend;

        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSend = itemView.findViewById(R.id.imgSend);
            msgSend = itemView.findViewById(R.id.msgSend);
            feelingsSend = itemView.findViewById(R.id.feelingsSend);
            timeSend = itemView.findViewById(R.id.timeSend);
        }
    }

    private class ReceiverViewHolder extends RecyclerView.ViewHolder {

        ImageView imgReceive,feelingsReceive;
        TextView msgReceive,timeReceive;

        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            imgReceive =itemView.findViewById(R.id.imgReceive);
            msgReceive =itemView.findViewById(R.id.msgReceive);
            feelingsReceive =itemView.findViewById(R.id.feelingsReceive);
            timeReceive =itemView.findViewById(R.id.timeReceive);
        }
    }
}
