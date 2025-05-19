package com.example.flowtalk.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users ORDER BY timeStamp DESC")
    List<UserEntity> getAllUsersSorted();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUsers(List<UserEntity> users);

    @Query("DELETE FROM users")
    void deleteAll();
}