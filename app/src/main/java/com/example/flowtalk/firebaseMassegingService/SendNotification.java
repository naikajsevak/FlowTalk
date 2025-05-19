package com.example.flowtalk.firebaseMassegingService;

import static com.android.volley.VolleyLog.TAG;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SendNotification {
    private final String title;
    private final String body;
    private final String messageId;
    private final String receiverId;
    private final Context context;
    private final String userFcmToken;
    private final String url = "https://fcm.googleapis.com/v1/projects/foodorder-9bd2b/messages:send";

    public SendNotification(String userFcmToken, String title, String body, Context context, String messageId,String receiverId) {
        this.userFcmToken = userFcmToken;
        this.title = title;
        this.body = body;
        this.context = context;
        this.messageId = messageId;
        this.receiverId=receiverId;
    }

    public void sendNotification() {
        JSONObject mainObj = new JSONObject();
        try {
            JSONObject dataObj = new JSONObject();
            dataObj.put("messageId", messageId);
            dataObj.put("senderId", FirebaseAuth.getInstance().getUid());
            dataObj.put("receiverId", receiverId);
            dataObj.put("name", title);
            dataObj.put("message", body); // Include the actual message

            JSONObject payload = new JSONObject();
            payload.put("message", new JSONObject()
                    .put("token", userFcmToken)
                    .put("data", dataObj)  // ✅ Only sending "data"
            );

            mainObj.put("message", payload);

            // Open connection
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + new AccessToken().getAccessToken());
            conn.setRequestProperty("Content-Type", "application/json; UTF-8");
            conn.setDoOutput(true);
            conn.getOutputStream().write(payload.toString().getBytes(StandardCharsets.UTF_8));

            // Read response
            Scanner scanner = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
            }
            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            Log.d(TAG, "FCM Response: " + response);
        } catch (Exception e) {
            Log.e("SendNotification", "Error creating notification JSON: " + e.getMessage(), e);
        }
    }


}
