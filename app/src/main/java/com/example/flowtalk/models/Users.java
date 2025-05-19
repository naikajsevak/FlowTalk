package com.example.flowtalk.models;

public class Users {

    private String profile;
    private String userName;
    private String phoneNumber;
    private long timeStamp;
    private String userId;
    private String token;
    private String email;
    private String status;
    private String lastMessage;

    public Users(String profile, String userName, String email,String userId) {
        this.profile = profile;
        this.userName = userName;
        this.userId = userId;
        this.email=email;
    }

    public Users(String userName, String mail, String password) {
        this.userName = userName;
        this.email = mail;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Users() {
    }
    public void setEmail(String email){this.email=email;}

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}

