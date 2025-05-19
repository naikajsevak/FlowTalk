package com.example.flowtalk;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.flowtalk.databinding.ActivityVideoCallBinding;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class VideoCallActivity extends AppCompatActivity {
    ActivityVideoCallBinding binding;
    private static final int PERMISSION_REQ_ID = 22;
    private String appId= "c92d4ee37df4485da334c694425e85d7";
    private String channelName= "myUniqueChannel";
    private int uId=0;
    private String token="007eJxTYAia/v3fwjMvTmz9pLp7x5+Xxx63PVtx5iHDLrfuept/ShNDFBiSLY1STFJTjc1T0kxMLExTEo2NTZLNLE1MjExTgVzzmb0B6Q2BjAyvMjMZGRkgEMTnZ8itDM3LLCxNdc5IzMtLzWFgAADTryki";
    private boolean isJoin=false;
    private RtcEngine agoraEngine = null;
    private SurfaceView localSurfaceView = null;

    private SurfaceView remoteSurfaceView = null;


    private static final String[] REQUESTED_PERMISSIONS = new String[] {
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        /*RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
        int timestamp = (int) ((System.currentTimeMillis() / 1000) + expirationTimeInSeconds);

        System.out.println("UID token");
        String result = tokenBuilder.buildTokenWithUid(
                appId, appCertificate,
                channelName, uid, RtcTokenBuilder2.Role.ROLE_PUBLISHER, timestamp, timestamp
        );
        System.out.println(result);

        token = result;*/

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
        setupVideoSDKEngine();
        binding.joinBtn.setOnClickListener(view -> {

        });
        binding.leaveBtn.setOnClickListener(view -> {

        });
    }
    private boolean checkSelfPermission() {
        return !(ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        agoraEngine.stopPreview();
        agoraEngine.leaveChannel();

        new Thread(() -> {
            RtcEngine.destroy();
            agoraEngine = null;
        }).start();
    }

    public void showMessage(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        getApplicationContext(),
                        message,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
    private void setupVideoSDKEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = appId;
            config.mEventHandler = mRtcEventHandler;
            agoraEngine = RtcEngine.create(config);
            // By default, the video module is disabled, call enableVideo to enable it.
            agoraEngine.enableVideo();
        } catch (Exception e) {
            showMessage(e.toString());
        }
    }
    private IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            showMessage("Remote user joined " + uid);

            // Set the remote video view
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupRemoteVideo(uid);
                }
            });
        }
        private SurfaceView remoteSurfaceView;

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            isJoin = true;
            showMessage("Joined Channel " + channel);
        }
        private void joinChannel(View view) {
            if (checkSelfPermission()) {
                ChannelMediaOptions options = new ChannelMediaOptions();

                options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
                options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
                setupLocalVideo();
                localSurfaceView.setVisibility(View.VISIBLE);
                agoraEngine.startPreview();
                agoraEngine.joinChannel(token, channelName, uId, options);
            } else {
                Toast.makeText(getApplicationContext(), "Permissions was not granted", Toast.LENGTH_SHORT)
                        .show();
            }
        }
        private void leaveChannel(View view) {
            if (!isJoin) {
                showMessage("Join a channel first");
            } else {
                agoraEngine.leaveChannel();
                showMessage("You left the channel");
                if (remoteSurfaceView != null) remoteSurfaceView.setVisibility(View.GONE);
                if (localSurfaceView != null) localSurfaceView.setVisibility(View.GONE);
                isJoin = false;
            }
        }
        @Override
        public void onUserOffline(int uid, int reason) {
            showMessage("Remote user offline " + uid + " " + reason);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (remoteSurfaceView != null) {
                        remoteSurfaceView.setVisibility(View.GONE);
                    }
                }
            });
        }
    };
   private void setupRemoteVideo(int uid) {
        remoteSurfaceView = new SurfaceView(this);
        remoteSurfaceView.setZOrderMediaOverlay(true);
        binding.remoteUser.addView(remoteSurfaceView);
        agoraEngine.setupRemoteVideo(
                new VideoCanvas(
                        remoteSurfaceView,
                        VideoCanvas.RENDER_MODE_FIT,
                        uid
                )
        );
        remoteSurfaceView.setVisibility(View.VISIBLE);
    }
    private void setupLocalVideo() {
        localSurfaceView = new SurfaceView(this);
        binding.localUser.addView(localSurfaceView);
        agoraEngine.setupLocalVideo(
                new VideoCanvas(
                        localSurfaceView,
                        VideoCanvas.RENDER_MODE_HIDDEN,
                        0
                )
        );
    }
}