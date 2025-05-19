package com.example.flowtalk;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.flowtalk.databinding.ActivityCallBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;

public class CallActivity extends AppCompatActivity {
    ActivityCallBinding binding;
    RtcEngine rtcEngine;
    MediaPlayer ringtonePlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();
        checkPermissions();
        String profile=getIntent().getStringExtra("receiverProfile");
        String recieverId=getIntent().getStringExtra("receiverUid");
        String recieverName=getIntent().getStringExtra("receiverName");
        String senderId= FirebaseAuth.getInstance().getCurrentUser().getUid();
        binding.userName.setText(recieverName);
        Picasso.get().load(profile).placeholder(R.drawable.avatar).into((binding.profileImage));

        // Initialize RtcEngine
        try {
            rtcEngine = RtcEngine.create(getApplicationContext(), String.valueOf(R.string.app_id), new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    Log.d("Agora", "Joined channel: " + channel);
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    Log.d("Agora", "User offline: " + uid);
                }

                @Override
                public void onLeaveChannel(IRtcEngineEventHandler.RtcStats stats) {
                    Log.d("Agora", "Left channel");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Get a reference to the Calls node
        DatabaseReference callsRef = FirebaseDatabase.getInstance().getReference("Calls");

        // Generate a unique channel ID for the call
        String channelId = UUID.randomUUID().toString();

        // Prepare the call data
        Map<String, Object> callData = new HashMap<>();
        callData.put("callerId", senderId);      // Replace with actual caller ID
        callData.put("receiverId", recieverId);    // Replace with actual receiver ID
        callData.put("channelId", R.string.AppChannelName);     // Unique Agora channel ID
        callData.put("status", "incoming");       // Call status: incoming
        callData.put("timestamp", System.currentTimeMillis()); // Timestamp of call initiation

        // Push the call data to the Firebase Realtime Database
        callsRef.push().setValue(callData);
        binding.endCallButton.setOnClickListener(view -> {
            if (rtcEngine != null) {
                rtcEngine.leaveChannel();
            }

            // Update call status in Firebase
            FirebaseDatabase.getInstance().getReference("Calls")
                    .orderByChild("status").equalTo("ringing")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot callSnapshot : dataSnapshot.getChildren()) {
                                callSnapshot.getRef().child("status").setValue("ended");
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e("FirebaseError", databaseError.getMessage());
                        }
                    });

            finish();
        });
        startCall(senderId,recieverId, String.valueOf(R.string.AppChannelName),String.valueOf(R.string.AppToken));
        FirebaseDatabase.getInstance().getReference("Calls").orderByChild("status").equalTo("ringing").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot callSnapshot : dataSnapshot.getChildren()) {
                    String callerId = callSnapshot.child("callerId").getValue(String.class);
                    String channelId = callSnapshot.child("channelName").getValue(String.class);
                    String token = String.valueOf(R.string.AppToken); // Replace with token generation logic

                    // If this is the receiver
                    if (FirebaseAuth.getInstance().getCurrentUser().getUid().equals(callSnapshot.child("receiverId").getValue(String.class))) {
                        // Notify the user and join the channel
                        joinAgoraChannel(channelId, token);
                        callSnapshot.getRef().child("status").setValue("answered");
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("FirebaseError", databaseError.getMessage());
            }
        });

    }
    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 1);
                break;
            }
        }
    }
    private void startCall(String callerId, String receiverId, String channelName, String token) {
        DatabaseReference callsRef = FirebaseDatabase.getInstance().getReference("Calls");

        HashMap<String, Object> callData = new HashMap<>();
        callData.put("callerId", callerId);
        callData.put("receiverId", receiverId);
        callData.put("channelName", channelName);
        callData.put("callType", "voice");
        callData.put("status", "ringing");

        // Push the call data
        callsRef.push().setValue(callData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Notify receiver and start Agora call
                joinAgoraChannel(channelName, token);
            } else {
                Log.e("FirebaseError", "Failed to notify receiver");
            }
        });
    }


    private void joinAgoraChannel(String channelName, String token) {
        if (rtcEngine != null) {
            rtcEngine.joinChannel(token, channelName, "extraInfo", 0);
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
        FirebaseDatabase.getInstance().getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue("Offline");
    }
    private void startRingtone() {
        if (ringtonePlayer == null) {
            //ringtonePlayer = MediaPlayer.create(this, R.raw.ringtone); // Add a ringtone file (e.g., ringtone.mp3) in `res/raw/`
            ringtonePlayer.setLooping(true);
            ringtonePlayer.start();
        }
    }
    private void stopRingtone() {
        if (ringtonePlayer != null) {
            ringtonePlayer.stop();
            ringtonePlayer.release();
            ringtonePlayer = null;
        }
    }

}