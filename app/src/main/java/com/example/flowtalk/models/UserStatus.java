package com.example.flowtalk.models;

import java.util.ArrayList;

public class UserStatus {
    private String name,profileImage;
    private long lastUpdate;
    private ArrayList<Status> arrayList;

    public UserStatus(String name, String profileImage, long lastUpdate, ArrayList<Status> arrayList) {
        this.name = name;
        this.profileImage = profileImage;
        this.lastUpdate = lastUpdate;
        this.arrayList = arrayList;
    }

    public UserStatus(){}

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

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public ArrayList<Status> getArrayList() {
        return arrayList;
    }

    public void setArrayList(ArrayList<Status> arrayList) {
        this.arrayList = arrayList;
    }
}
