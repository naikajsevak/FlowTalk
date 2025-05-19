package com.example.flowtalk;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flowtalk.databinding.ActivityUserProfileDetailsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class UserProfileDetailsActivity extends AppCompatActivity {
    String receiverUid,senderUid;
    ActivityUserProfileDetailsBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        String name = getIntent().getStringExtra("name");
        final String[] profile = {getIntent().getStringExtra("profileImage")};
        getIntent().getStringExtra("token");
        senderUid=getIntent().getStringExtra("sid");
        receiverUid = getIntent().getStringExtra("uid");
        FirebaseDatabase.getInstance().getReference().child("Users").child(receiverUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    profile[0] = snapshot.child("profile").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        Picasso.get().load(profile[0]).placeholder(R.drawable.avatar).into((binding.imageView));
        binding.userName.setText(name);
        getSupportActionBar().setTitle(name);
        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UserProfileDetailsActivity.this, ImageActivity.class);
                intent.putExtra("userName",name);
                intent.putExtra("image", profile[0]);
                startActivity(intent);
            }
        });
        FirebaseDatabase.getInstance().getReference("Users").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get the user data
                    String about = snapshot.child("status").getValue(String.class);
                    binding.userAbout.setText(about);
                    // Use the data (e.g., display in UI)
                } else {
                    Log.d("UserData", "User not found");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database read error
                Log.e("UserData", "Error: " + error.getMessage());
            }
        });
        FirebaseDatabase.getInstance().getReference().child("Chats").child(senderUid+receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    if (status!=null && !status.isEmpty()) {
                        binding.userStatus.setText(status);
                        if(!status.equals("Offline")){
                            binding.userStatus.setText(status);
                            binding.userStatus.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    Log.d("UserData", "User not found");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        FirebaseDatabase.getInstance().getReference().child("presence").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.getValue(String.class);
                    if (!status.isEmpty() && !status.equals("Offline")) {
                        binding.userStatus.setVisibility(View.VISIBLE);
                        if(!status.equals("Online")) {
                            binding.userStatus.setText(calculateHours(Long.valueOf(status)));
                        }
                        else {
                            binding.userStatus.setText(status);
                        }

                    }
                } else {
                    binding.userStatus.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private String calculateHours(long timestamp) {
        // Get the current time in milliseconds
        long currentTimeMillis = System.currentTimeMillis();
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp);
        cal1.set(Calendar.HOUR_OF_DAY, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(currentTimeMillis);
        cal2.set(Calendar.HOUR_OF_DAY, 0);
        cal2.set(Calendar.MINUTE, 0);
        cal2.set(Calendar.SECOND, 0);
        cal2.set(Calendar.MILLISECOND, 0);

        // Calculate the difference in days
        long millisDiff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
        long dayDifference = millisDiff / (1000 * 60 * 60 * 24);
        // Create Calendar objects for the current time and the stored timestamp
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeInMillis(currentTimeMillis);

        Calendar storedCalendar = Calendar.getInstance();
        storedCalendar.setTimeInMillis(timestamp);

        // Extract the current and stored dates

        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
        // Compare the years to handle year boundaries
        if (dayDifference == 0) {
            return "Last seen today at "+ dateFormat.format(new Date(timestamp));
        } else if (dayDifference == 1) {
            return "Last seen yesterday at "+ dateFormat.format(new Date(timestamp));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
            String lastSeenDate = sdf.format(new Date(timestamp));
            return "Last seen on " + lastSeenDate;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        FirebaseDatabase.getInstance().getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        FirebaseDatabase.getInstance().getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue(String.valueOf(new Date().getTime()));
    }
}