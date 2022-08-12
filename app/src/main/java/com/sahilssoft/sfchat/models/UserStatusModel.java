package com.sahilssoft.sfchat.models;

import java.util.ArrayList;

public class UserStatusModel {
    private String name,profileImage;
    private long lastUpdated;
    private ArrayList<StatusModel> statuses;

    public UserStatusModel() {
    }

    public UserStatusModel(String name, String profileImage, long lastUpdated, ArrayList<StatusModel> statuses) {
        this.name = name;
        this.profileImage = profileImage;
        this.lastUpdated = lastUpdated;
        this.statuses = statuses;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public ArrayList<StatusModel> getStatuses() {
        return statuses;
    }

    public void setStatuses(ArrayList<StatusModel> statuses) {
        this.statuses = statuses;
    }
}
