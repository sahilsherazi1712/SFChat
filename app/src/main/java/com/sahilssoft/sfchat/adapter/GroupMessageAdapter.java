package com.sahilssoft.sfchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.sahilssoft.sfchat.models.UsersModel;

import java.util.ArrayList;

public class GroupMessageAdapter extends RecyclerView.Adapter {

    Context context;
    ArrayList<MessageModel> messageModelList;

    final int ITEM_SEND = 1;
    final int ITEM_RECEIVE = 2;

    public GroupMessageAdapter(Context context, ArrayList<MessageModel> messageModelList) {
        this.context = context;
        this.messageModelList = messageModelList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType==ITEM_SEND){
            return new SenderViewHolder(LayoutInflater.from(context).inflate(R.layout.item_send_group,parent,false));
        }else{
            return new ReceiverViewHolder(LayoutInflater.from(context).inflate(R.layout.item_receive_group,parent,false));
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
                viewHolder.feelingsSendGroup.setImageResource(reactions[pos]);
                viewHolder.feelingsSendGroup.setVisibility(View.VISIBLE);
            }else{
                ReceiverViewHolder viewHolder =(ReceiverViewHolder) holder;
                viewHolder.feelingsReceiveGroup.setImageResource(reactions[pos]);
                viewHolder.feelingsReceiveGroup.setVisibility(View.VISIBLE);
            }

            message.setFeelings(pos);

            FirebaseDatabase.getInstance()
                    .getReference("Public")
                    .child(message.getMessageId()).setValue(message);

            return true;
        });

        if (holder.getClass() == SenderViewHolder.class) {
            SenderViewHolder viewHolder = (SenderViewHolder) holder;

            if (message.getMessage().equals("photo")){
                viewHolder.msgSendGroup.setVisibility(View.GONE);
                viewHolder.imgSendGroup.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.img_placeholder)
                        .into(viewHolder.imgSendGroup);
            }

            //to get data of the user to show name of message sender in group
            FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(message.getSenderId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            UsersModel usersModel = snapshot.getValue(UsersModel.class);
                            viewHolder.nameSendGroup.setText("@"+usersModel.getName());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

            viewHolder.msgSendGroup.setText(message.getMessage());

            if (message.getFeelings()>=0){
                viewHolder.feelingsSendGroup.setVisibility(View.VISIBLE);
                viewHolder.feelingsSendGroup.setImageResource(reactions[message.getFeelings()]);
            }else{
                viewHolder.feelingsSendGroup.setVisibility(View.INVISIBLE);
            }

            viewHolder.msgSendGroup.setOnTouchListener(new View.OnTouchListener() {
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
            viewHolder.imgSendGroup.setOnTouchListener(new View.OnTouchListener() {
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
                        viewHolder.nameSendGroup.setVisibility(View.GONE);
                        viewHolder.viewSendGroup.setVisibility(View.GONE);

                        FirebaseDatabase.getInstance()
                                .getReference("Public")
                                .child(message.getMessageId()).setValue(message);

                        dialog.dismiss();
                    });
                    delete.setOnClickListener(view2 -> {
                        message.setMessage("This message is removed.");
                        message.setFeelings(-1);
                        viewHolder.nameSendGroup.setVisibility(View.GONE);
                        viewHolder.viewSendGroup.setVisibility(View.GONE);

                        FirebaseDatabase.getInstance()
                                .getReference("Public")
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
                viewHolder.msgReceiveGroup.setVisibility(View.GONE);
                viewHolder.imgReceiveGroup.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.img_placeholder)
                        .into(viewHolder.imgReceiveGroup);
            }

            //to get data of the user to show name of message sender in group
            FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(message.getSenderId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            UsersModel usersModel = snapshot.getValue(UsersModel.class);
                            viewHolder.nameReceiveGroup.setText("@"+usersModel.getName());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

            viewHolder.msgReceiveGroup.setText(message.getMessage());

            if (message.getFeelings()>=0){
                viewHolder.feelingsReceiveGroup.setVisibility(View.VISIBLE);
                viewHolder.feelingsReceiveGroup.setImageResource(reactions[message.getFeelings()]);
            }else{
                viewHolder.feelingsReceiveGroup.setVisibility(View.INVISIBLE);
            }

            viewHolder.msgReceiveGroup.setOnTouchListener(new View.OnTouchListener() {
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
            viewHolder.imgReceiveGroup.setOnTouchListener(new View.OnTouchListener() {
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

                    everyone.setOnClickListener(view2 -> {
                        message.setMessage("This message is removed");
                        message.setFeelings(-1);
                        viewHolder.nameReceiveGroup.setVisibility(View.GONE);
                        viewHolder.viewReceiveGroup.setVisibility(View.GONE);

                        FirebaseDatabase.getInstance()
                                .getReference("Public")
                                .child(message.getMessageId()).setValue(message);
                        dialog.dismiss();
                    });
                    delete.setOnClickListener(view2 -> {
                        message.setMessage("This message is removed");
                        message.setFeelings(-1);
                        viewHolder.nameReceiveGroup.setVisibility(View.GONE);
                        viewHolder.viewReceiveGroup.setVisibility(View.GONE);

                        FirebaseDatabase.getInstance()
                                .getReference("Public")
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

        ImageView imgSendGroup,feelingsSendGroup;
        TextView msgSendGroup,nameSendGroup;
        View viewSendGroup;

        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSendGroup = itemView.findViewById(R.id.imgSendGroup);
            msgSendGroup = itemView.findViewById(R.id.msgSendGroup);
            feelingsSendGroup = itemView.findViewById(R.id.feelingsSendGroup);
            nameSendGroup = itemView.findViewById(R.id.nameSendGroup);
            viewSendGroup = itemView.findViewById(R.id.viewSendGroup);
        }
    }

    private class ReceiverViewHolder extends RecyclerView.ViewHolder {

        ImageView imgReceiveGroup,feelingsReceiveGroup;
        TextView msgReceiveGroup,nameReceiveGroup;
        View viewReceiveGroup;

        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            imgReceiveGroup =itemView.findViewById(R.id.imgReceiveGroup);
            msgReceiveGroup =itemView.findViewById(R.id.msgReceiveGroup);
            feelingsReceiveGroup =itemView.findViewById(R.id.feelingsReceiveGroup);
            nameReceiveGroup =itemView.findViewById(R.id.nameReceiveGroup);
            viewReceiveGroup =itemView.findViewById(R.id.viewReceiveGroup);
        }
    }
}
