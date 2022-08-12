package com.sahilssoft.sfchat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.devlomi.circularstatusview.CircularStatusView;
import com.sahilssoft.sfchat.R;
import com.sahilssoft.sfchat.activities.MainActivity;
import com.sahilssoft.sfchat.models.StatusModel;
import com.sahilssoft.sfchat.models.UserStatusModel;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import omari.hamza.storyview.StoryView;
import omari.hamza.storyview.callback.StoryClickListeners;
import omari.hamza.storyview.model.MyStory;

public class UserStatusAdapter extends RecyclerView.Adapter<UserStatusAdapter.ViewHolder> {

    Context context;
    ArrayList<UserStatusModel> userStatusModelList;

    public UserStatusAdapter(Context context, ArrayList<UserStatusModel> userStatusModelList) {
        this.context = context;
        this.userStatusModelList = userStatusModelList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_main_status,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        //show last status image
        StatusModel lastStatus = userStatusModelList.get(position).getStatuses().get(userStatusModelList.get(position).getStatuses().size()-1);
        Glide.with(context)
                .load(lastStatus.getImageUrl())
                .into(holder.image);
        //show portions on the status view
        holder.circular_status_view.setPortionsCount(userStatusModelList.get(position).getStatuses().size());

        holder.itemView.setOnClickListener(view -> {
            ArrayList<MyStory> myStories = new ArrayList<>();
            for (StatusModel statusModel : userStatusModelList.get(position).getStatuses()){
                myStories.add(new MyStory(statusModel.getImageUrl()));
            }
            new StoryView.Builder(((MainActivity)context).getSupportFragmentManager())
                    .setStoriesList(myStories)
                    .setStoryDuration(5000)
                    .setTitleText(userStatusModelList.get(position).getName())
                    .setSubtitleText("")
                    .setTitleLogoUrl(userStatusModelList.get(position).getProfileImage())
                    .setStoryClickListeners(new StoryClickListeners() {
                        @Override
                        public void onDescriptionClickListener(int position) {
                            //your action
                        }

                        @Override
                        public void onTitleIconClickListener(int position) {
                            //your action
                        }
                    }).build().show();
        });
    }

    @Override
    public int getItemCount() {
        return userStatusModelList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView image;
        CircularStatusView circular_status_view;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.image);
            circular_status_view = itemView.findViewById(R.id.circular_status_view);

        }
    }
}
