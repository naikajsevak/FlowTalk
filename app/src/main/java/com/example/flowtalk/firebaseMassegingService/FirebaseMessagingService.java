package com.example.flowtalk.firebaseMassegingService;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.flowtalk.R;
import com.example.flowtalk.SplashScreen;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final String CHANNEL_ID = "flowtalk_channel";
    private static final String CHANNEL_NAME = "FlowTalk Notifications";
    private static final String CHANNEL_DESCRIPTION = "Channel for FlowTalk app notifications";
    private String profileImage;

    long[] pattern = {0, 10, 100, 200};

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        updateNewToken(token);
    }
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        // ✅ Extract data
        Map<String, String> data = message.getData();
        String senderId = data.get("senderId");
        String receiverId = data.get("receiverId");
        String messageId = data.get("messageId");
        String messageText = data.get("message"); // 🔥 Extract the actual message
        String senderName = data.get("name"); // 🔥 Extract sender's name

        // ✅ Ensure notification shows the correct data
        if (senderName == null || senderName.isEmpty()) senderName = "New Message";
        if (messageText == null || messageText.isEmpty()) messageText = "You have a new message";

        // ✅ Update "delivered" in Firebase
        if (receiverId != null && senderId != null && messageId != null) {
            String receiverRoom = receiverId + senderId;
            DatabaseReference chatRef = FirebaseDatabase.getInstance()
                    .getReference("Chats").child(receiverRoom).child("messages");

            chatRef.child(messageId).child("delivered").setValue("true");
        }

        // ✅ Show the notification manually (Always)
        showNotification(senderName, messageText, senderId);
    }


    private void showNotification(String senderName, String messageText, String senderId) {
        Intent intent = new Intent(this, SplashScreen.class);
        intent.putExtra("userId", senderId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.chat_icon)
                .setContentTitle(senderName) // 🔥 Now shows sender's name
                .setContentText(messageText) // 🔥 Now shows the actual message text
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(100, builder.build());
        }
    }

    private void updateNewToken(String token) {
        // Implement token update logic here
        if (FirebaseAuth.getInstance().getUid()!=null) {
            FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).
                    child("FirebaseMessagingToken").setValue(token);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(CHANNEL_DESCRIPTION);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setVibrationPattern(pattern);
        channel.setLightColor(R.color.black);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
}

