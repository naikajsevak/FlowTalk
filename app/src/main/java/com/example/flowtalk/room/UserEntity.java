package com.example.flowtalk.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.vo.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    public String userId;

    public String userName;
    public String profileImage;
    public long timeStamp;
}