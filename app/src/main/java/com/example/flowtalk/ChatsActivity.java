package com.example.flowtalk;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.flowtalk.adapter.ChatAdapter;
import com.example.flowtalk.databinding.ActivityChatsBinding;
import com.example.flowtalk.firebaseMassegingService.SendNotification;
import com.example.flowtalk.models.MessagesModel;
import com.example.flowtalk.models.Users;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatsActivity extends AppCompatActivity {
    ActivityChatsBinding binding;

    ChatAdapter adapter;
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording;
    private CountDownTimer countDownTimer;
    private boolean isButtonPressed = false;
    private Handler timerHandler = new Handler();
    private long startTime = 0;

    ArrayList<Users> list;
    Uri photoURI;
    String senderRoom;
    String receiverRoom,currentPhotoPath;
    ArrayList<MessagesModel> messagesModelArrayList;
    FirebaseDatabase database;
    FirebaseStorage storage;
    ProgressDialog dialog;
    String senderUid,senderName,isReceiverStatusOnline="Offline";
    private static int count;


    String receiverUid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();
        //setSupportActionBar(binding.toolbar);
        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();
        messagesModelArrayList = new ArrayList<>();
        String name = getIntent().getStringExtra("name");
        String profile = getIntent().getStringExtra("profileImage");
        String token = getIntent().getStringExtra("token");
        receiverUid = getIntent().getStringExtra("uid");
        binding.name.setText(name);
        list = new ArrayList<>();
        binding.name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatsActivity.this,UserProfileDetailsActivity.class);
                intent.putExtra("name",name);
                intent.putExtra("profileImage",profile);
                intent.putExtra("uid",receiverUid);
                intent.putExtra("sid",senderUid);
                intent.putExtra("token",token);
                startActivity(intent);
            }
        });
        Picasso.get().load(profile).placeholder(R.drawable.avatar).into((binding.profileImage));
        binding.leftBaseWest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        senderUid = FirebaseAuth.getInstance().getUid();
        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image...");
        dialog.setCancelable(false);
        senderRoom = senderUid + receiverUid;
        database.getReference().child("Users").child(senderUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                senderName = snapshot.child("userName").getValue(String.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        receiverRoom = receiverUid + senderUid ;
        adapter = new ChatAdapter(name,this,messagesModelArrayList,senderRoom,receiverRoom);
        database.getReference().child("Chats").child(receiverRoom).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    String data=snapshot.child("unSeenMessage").getValue(String.class);
                    if(data!=null)
                        count = Integer.parseInt(data);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        binding.recyclerViewActivityChats.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewActivityChats.setAdapter(adapter);
        database.getReference().child("Chats")
                .child(senderRoom).child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int position=-1;
                        messagesModelArrayList.clear();
                        for (DataSnapshot snapshot1:snapshot.getChildren()){
                            MessagesModel messagesModel = snapshot1.getValue(MessagesModel.class);
                            Objects.requireNonNull(messagesModel).setMessageId(snapshot1.getKey());
                            String seen=snapshot1.child(messagesModel.getMessageId()).child("seen").getValue(String.class);
                            if(position == -1 && Objects.equals(seen, "false"))
                                position=messagesModelArrayList.size()-1;
                            messagesModelArrayList.add(messagesModel);
                        }
                        adapter.notifyDataSetChanged();
                        if(position==-1)
                            position=messagesModelArrayList.size()-1;
                        binding.recyclerViewActivityChats.scrollToPosition(position);

                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        binding.recyclerViewActivityChats.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    // Get the indices of the first and last visible items
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                    // Mark messages as seen
                    for (int i = firstVisibleItemPosition; i <= lastVisibleItemPosition; i++) {
                        String messageId=messagesModelArrayList.get(i).getMessageId();
                        messagesModelArrayList.get(i).setSeen("true");
                        database.getReference().child("Chats").child(senderRoom)
                                .child("messages")
                                .child(messageId).child("seen").setValue("true");
                    }
                }
            }
        });
        database.getReference().child("presence").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String status = snapshot.getValue(String.class);
                    if (status!=null && !status.isEmpty()) {
                        binding.online.setVisibility(View.VISIBLE);
                        if(!status.equals("Online")){
                            binding.online.setText(calculateHours(Long.parseLong(status)));
                        }
                        else {
                            binding.online.setText(status);
                        }
                    }
                    else
                        binding.online.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        database.getReference().child("Chats").child(senderRoom).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String status = snapshot.child("status").getValue(String.class);
                    if (status!=null && !status.isEmpty()) {
                        if(!status.equals("Offline")) {
                            binding.online.setText(status);
                            isReceiverStatusOnline=status;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.sendImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.msgBox.getText().toString().trim().isEmpty())
                    return;

                String msg = binding.msgBox.getText().toString().trim();
                Date date = new Date();
                String randomKey = database.getReference().push().getKey();

                if (randomKey == null || randomKey.isEmpty()) {
                    Toast.makeText(ChatsActivity.this, "random key problem", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if the message is a YouTube link (supports both videos and shorts)
                String youtubePattern = "(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/(watch\\?v=|shorts/)?[a-zA-Z0-9_-]{11}";
                Pattern pattern = Pattern.compile(youtubePattern);
                Matcher matcher = pattern.matcher(msg);

                if (matcher.find()) {
                    // It's a YouTube link (video or short), handle it
                    binding.msgBox.setText("");
                    String youtubeLink = matcher.group();
                    handleYouTubeLink(youtubeLink, randomKey, date);
                    increaseCountOfUnseenMsg();
                } else {
                    // Regular text message
                    MessagesModel messagesModel = new MessagesModel(senderUid, msg, date.getTime(), "false","false");
                    messagesModel.setMessageId(randomKey);
                    binding.msgBox.setText("");
                    // Save message to sender and receiver rooms
                    database.getReference().child("Chats")
                            .child(senderRoom)
                            .child("messages")
                            .child(randomKey)
                            .setValue(messagesModel)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    HashMap<String, Object> hashMap = new HashMap<>();
                                    hashMap.put("messageId",messagesModel.getMessageId());
                                    database.getReference().child("Chats").child(senderRoom)
                                            .updateChildren(hashMap);
                                    database.getReference().child("Chats")
                                            .child(receiverRoom)
                                            .child("messages")
                                            .child(randomKey)
                                            .setValue(messagesModel)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    HashMap<String, Object> hashMap = new HashMap<>();
                                                    hashMap.put("lastMsg", messagesModel.getMessage());
                                                    hashMap.put("lastMsgTime", date.getTime());

                                                    database.getReference().child("Chats").child(senderRoom)
                                                            .updateChildren(hashMap);
                                                    database.getReference().child("Chats").child(receiverRoom)
                                                            .updateChildren(hashMap);
                                                    increaseCountOfUnseenMsg();
                                                    if(isReceiverStatusOnline.equals("Offline")) {
                                                        database.getReference("Users").child(receiverUid)
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                        String FCMTOKEN = snapshot.child("FirebaseMessagingToken").getValue(String.class);
                                                                        SendNotification notificationObj = new SendNotification(FCMTOKEN, senderName, msg, getApplicationContext(),messagesModel.getMessageId(),receiverUid);
                                                                        notificationObj.sendNotification();
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                                    }
                                                                });
                                                    }
                                                }
                                            });
                                }
                            });
                }
            }
        });
        binding.sendAudioMsgMic.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isButtonPressed = true;
                    if(hasPermissions())
                        requestPermissionsIfNeeded();
                    // Start the countdown timer
                    countDownTimer = new CountDownTimer(3000, 1000) { // 3 seconds countdown
                        @Override
                        public void onTick(long millisUntilFinished) {
                            int secondsRemaining = (int) (millisUntilFinished / 1000);
                            binding.containerBox.setVisibility(View.GONE);
                            binding.countdownText.setVisibility(View.VISIBLE);
                            binding.countdownText.setText("Recording will start in " + secondsRemaining + " seconds");
                        }

                        @Override
                        public void onFinish() {
                            if (isButtonPressed) {
                                // Start recording after countdown
                                startRecording();
                                Toast.makeText(ChatsActivity.this, "Recording started", Toast.LENGTH_SHORT).show();
                                isRecording = true;
                            }
                            binding.countdownText.setVisibility(View.GONE);
                            binding.containerBox.setVisibility(View.VISIBLE);
                        }
                    }.start();

                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isButtonPressed = false;

                    // Stop countdown if button is released early
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                        binding.countdownText.setVisibility(View.GONE);
                        binding.containerBox.setVisibility(View.VISIBLE);
                    }

                    // Stop recording if it has started
                    if (isRecording) {
                        stopRecording();
                        sendAudioMessage(audioFilePath);
                        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
                        isRecording = false;
                    } else {
                        Toast.makeText(this, "Recording canceled", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
            return true;
        });


        binding.attachment.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*"); // Allows selection of any file type
            String[] mimeTypes = {"image/*", "video/*","application/pdf"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes); // Restrict to images and videos
            startActivityForResult(intent, 25);
        });
        binding.camera.setOnClickListener(view -> {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            } else {
                // Prompt user to choose between photo or video
                String[] options = {"Capture Photo", "Capture Video"};
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose an Option");
                builder.setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Photo capturing logic (unchanged)
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            // Create a file to store the photo
                            File photoFile = null;
                            try {
                                photoFile = createImageFile();
                            } catch (IOException ex) {
                                Log.e("CameraError", "Error creating file: " + ex.getMessage());
                            }
                            // Proceed only if the file was successfully created
                            if (photoFile != null) {
                                photoURI = FileProvider.getUriForFile(this,
                                        getApplicationContext().getPackageName() + ".provider",
                                        photoFile);
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                                startActivityForResult(takePictureIntent, 1);
                            }
                        }
                    } else if (which == 1) {
                        // Video capturing logic
                        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(takeVideoIntent, 2); // Request code 2 for video
                        }
                    }
                });
                builder.show();
            }
        });


        final Handler handler = new Handler();
        binding.msgBox.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void afterTextChanged(Editable editable) {
                if(binding.msgBox.getText().toString().trim().isEmpty())
                    binding.sendAudioMsgMic.setVisibility(View.VISIBLE);
                else
                    binding.sendAudioMsgMic.setVisibility(View.GONE);

                database.getReference().child("Chats").child(receiverRoom).child("status").setValue("typing...");
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(runnable,1000);
            }
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    database.getReference().child("Chats").child(receiverRoom).child("status").setValue("Online");
                }
            };
        });
        // Initialize call buttons in onCreate
        binding.icCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Trigger voice call
                initiateCall("audio");
            }
        });
       /* binding.icVedio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Trigger video call
                initiateCall("video");
            }
        });*/
    }
    // Method to handle call initiation
    private void initiateCall(String callType) {
        Intent intent = new Intent(ChatsActivity.this, CallActivity.class);
        intent.putExtra("receiverUid", receiverUid); // Pass receiver details
        intent.putExtra("receiverName", binding.name.getText().toString());
        intent.putExtra("receiverProfile", getIntent().getStringExtra("profileImage"));
        startActivity(intent);
    }

    private void increaseCountOfUnseenMsg()
    {
        database.getReference().child("Chats").child(senderRoom).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    String status = snapshot.child("status").getValue(String.class);
                    if(status==null || status.equals("Offline")){
                        count++;
                        database.getReference().child("Chats").child(receiverRoom).child("unSeenMessage").
                                setValue(String.valueOf(count));
                    }
                }catch (Exception e){
                    Log.e("naikaj sevak",e.getMessage());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        database.getReference().child("Chats").child(senderRoom).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    String status = snapshot.child("status").getValue(String.class);
                    if(status!=null && status.equals("Online")){
                        count=0;
                    }

                }catch (Exception e){
                    Log.e("naikaj sevak",e.getMessage());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
   @Override
   protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
       super.onActivityResult(requestCode, resultCode, data);
       if (resultCode == RESULT_OK) {
           increaseCountOfUnseenMsg();
           Uri selectedImage = null;
           if (requestCode == 1 && photoURI != null) {
               // If the image was captured using the camera
               selectedImage = photoURI;
           } else if (requestCode == 25 && data != null && data.getData() != null) {
               // If the image was selected from the gallery
               selectedImage = data.getData();
               String mimeType = getContentResolver().getType(selectedImage);
               if (mimeType.startsWith("video/")) {
                   uploadVideoToFirebase(selectedImage);
                   return;
               }
               if (mimeType.startsWith("application/pdf")) {
                   Uri pdfUri = data.getData(); // Get the PDF file's URI
                   uploadPDF(pdfUri);
                   return;
               }
           }
           else if(requestCode==2 && data!=null && data.getData()!=null)
           {
                uploadVideoToFirebase(data.getData());
                return;
           }
           if (selectedImage != null) {
               // Handle image upload
               Calendar calendar = Calendar.getInstance();
               StorageReference reference = storage.getReference().child("Chats")
                       .child(calendar.getTimeInMillis() + "");
               dialog.show();

               reference.putFile(selectedImage).addOnCompleteListener(task -> {
                   if (task.isSuccessful()) {
                       reference.getDownloadUrl().addOnSuccessListener(uri -> {
                           dialog.dismiss();
                           String filePath = uri.toString();
                           String msg = binding.msgBox.getText().toString();
                           Date date = new Date();

                           // Create message model for the image
                           MessagesModel messagesModel = new MessagesModel(senderUid, msg, date.getTime(),"false","false");
                           messagesModel.setMessage("photo"); // Indicating it's a photo
                           messagesModel.setMsgUrl(filePath);
                           binding.msgBox.setText("");

                           // Generate a unique key for the message
                           String randomKey = database.getReference().push().getKey();
                           messagesModel.setMessageId(randomKey);
                           // Save message to sender's chat
                           database.getReference().child("Chats")
                                   .child(senderRoom)
                                   .child("messages")
                                   .child(randomKey)
                                   .setValue(messagesModel).addOnSuccessListener(unused -> {
                                       HashMap<String, Object> hashMap1 = new HashMap<>();
                                       hashMap1.put("messageId",messagesModel.getMessageId());
                                       database.getReference().child("Chats").child(senderRoom)
                                               .updateChildren(hashMap1);
                                       // Save message to receiver's chat
                                       database.getReference().child("Chats")
                                               .child(receiverRoom)
                                               .child("messages")
                                               .child(randomKey)
                                               .setValue(messagesModel).addOnSuccessListener(unused1 -> {
                                                   if(isReceiverStatusOnline.equals("Offline")) {
                                                       database.getReference("Users").child(receiverUid)
                                                               .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                   @Override
                                                                   public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                       String FCMTOKEN = snapshot.child("FirebaseMessagingToken").getValue(String.class);
                                                                       SendNotification notificationObj = new SendNotification(FCMTOKEN, senderName, "photo", getApplicationContext(),messagesModel.getMessageId(),receiverUid);
                                                                       notificationObj.sendNotification();
                                                                   }

                                                                   @Override
                                                                   public void onCancelled(@NonNull DatabaseError error) {

                                                                   }
                                                               });
                                                   }
                                                   HashMap<String, Object> hashMap = new HashMap<>();
                                                   hashMap.put("lastMsg", messagesModel.getMessage());
                                                   hashMap.put("lastMsgTime", date.getTime());
                                                   // Update last message for both chats
                                                   database.getReference().child("Chats").child(senderRoom)
                                                           .updateChildren(hashMap);
                                                   database.getReference().child("Chats").child(receiverRoom)
                                                           .updateChildren(hashMap);
                                               });
                                   });
                       });
                   }
               });
           }
       }
   }

    private void uploadPDF(Uri pdfUri) {
        if (pdfUri != null) {
            // **Extract metadata before upload**
            String fileName = getFileName(pdfUri);
            long fileSize = getFileSize(pdfUri);
            int pageCount = getPdfPageCount(pdfUri);

            // Display metadata for debugging


            // **Continue with upload process**
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading PDF...");
            progressDialog.show();

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference().child("pdfs/" + fileName);

            storageRef.putFile(pdfUri)
                    .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                progressDialog.dismiss();
                                sendPDFMessage(uri.toString(), fileName, fileSize, pageCount);
                            }))
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to upload PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
    private String getFileName(Uri uri) {
        String fileName = "unknown.pdf";
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex);
            }
            cursor.close();
        }
        return fileName;
    }
    private long getFileSize(Uri uri) {
        long fileSize = 0;
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                fileSize = cursor.getLong(sizeIndex);
            }
            cursor.close();
        }
        return fileSize;
    }
    private int getPdfPageCount(Uri uri) {
        int pageCount = 0;
        try {
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null) {
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                PdfRenderer pdfRenderer = new PdfRenderer(parcelFileDescriptor);
                pageCount = pdfRenderer.getPageCount();
                pdfRenderer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pageCount;
    }

    private void sendPDFMessage(String pdfUrl, String fileName, long fileSize, int pageCount) {
        MessagesModel message = new MessagesModel(senderUid,fileName,new Date().getTime(),"false","false");
        message.setMsgUrl(pdfUrl); // Add a field for PDF URLs in your Message model
        message.setMessageType("pdf");
        message.setTitle(fileName);
        message.setFileSize(String.valueOf(fileSize));
        message.setPageCount(String.valueOf(pageCount));
        String randomKey = database.getReference().push().getKey();
        message.setMessageId(randomKey);
        // Save message to Firebase
        FirebaseDatabase.getInstance().getReference().child("Chats")
                .child(senderRoom)
                .child("messages")
                .child(randomKey)
                .setValue(message)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("messageId",message.getMessageId());
                        database.getReference().child("Chats").child(senderRoom)
                                .updateChildren(hashMap);
                        FirebaseDatabase.getInstance().getReference().child("Chats")
                                .child(receiverRoom)
                                .child("messages")
                                .child(randomKey)
                                .setValue(message)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        HashMap<String, Object> hashMap = new HashMap<>();
                                        hashMap.put("lastMsg", message.getTitle());
                                        hashMap.put("lastMsgTime", new Date().getTime());
                                        FirebaseDatabase.getInstance().getReference().child("Chats").child(senderRoom)
                                                .updateChildren(hashMap);
                                        FirebaseDatabase.getInstance().getReference().child("Chats").child(receiverRoom)
                                                .updateChildren(hashMap);


                                        if(isReceiverStatusOnline.equals("Offline")){
                                            FirebaseDatabase.getInstance().getReference("Users").child(receiverUid)
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                            String FCMTOKEN = snapshot.child("FirebaseMessagingToken").getValue(String.class);
                                                            SendNotification notificationObj = new SendNotification(FCMTOKEN,senderName , "pdf file", getApplicationContext(), message.getMessageId(),receiverUid);
                                                            notificationObj.sendNotification();
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                        }
                                                    });
                                        }
                                    }
                                });
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
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("Pictures");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file path for later use
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void uploadVideoToFirebase(Uri videoUri) {
        // Create a unique file name for the video
        Calendar calendar = Calendar.getInstance();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("Chats")
                .child(calendar.getTimeInMillis() + ".mp4");

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading Video...");
        progressDialog.show();

        // Upload the video to Firebase Storage
        storageReference.putFile(videoUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Get the download URL of the uploaded video
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    progressDialog.dismiss();

                    // Send the video to Firebase Realtime Database
                    String videoUrl = uri.toString();
                    sendVideoMessage(videoUrl);
                });
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to upload video", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void handleYouTubeLink(String youtubeLink, String randomKey, Date date) {
        String videoId = extractYouTubeVideoId(youtubeLink);

        if (videoId != null) {
            String videoThumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

            // Fetch metadata
            fetchYouTubeMetadata(videoId, youtubeLink, videoThumbnailUrl, randomKey, date);
        } else {
            Log.e("YouTubeLink", "Invalid YouTube Link");
        }
    }
    private String extractYouTubeVideoId(String youtubeLink) {
        String videoId = null;
        String pattern = "(?<=watch\\?v=|youtu\\.be/|youtube\\.com/embed/|shorts/)[^#&?\\n]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(youtubeLink);

        if (matcher.find()) {
            videoId = matcher.group();
        }
        return videoId;
    }

    // Fetch metadata and save to Firebase
    private void fetchYouTubeMetadata(String videoId, String youtubeLink, String videoThumbnailUrl, String randomKey, Date date) {
        String apiKey = "AIzaSyC9uurWCX4W7F38hIYAlOq9qs1Iv3pUEsw";
        String url = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + videoId + "&key=" + apiKey;

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray items = response.getJSONArray("items");
                        if (items.length() > 0) {
                            JSONObject snippet = items.getJSONObject(0).getJSONObject("snippet");
                            String title = snippet.getString("title");

                            // Create a message model for YouTube link
                            MessagesModel messagesModel = new MessagesModel(senderUid, youtubeLink, date.getTime(), "false","false");
                            messagesModel.setMsgUrl(videoThumbnailUrl); // Save thumbnail URL
                            messagesModel.setMessageType("youtube");
                            messagesModel.setTitle(title);
                            messagesModel.setMessageId(randomKey);
                            // Save message to Firebase
                            FirebaseDatabase.getInstance().getReference().child("Chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(randomKey)
                                    .setValue(messagesModel)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            HashMap<String, Object> hashMap = new HashMap<>();
                                            hashMap.put("messageId",messagesModel.getMessageId());
                                            database.getReference().child("Chats").child(senderRoom)
                                                    .updateChildren(hashMap);
                                            FirebaseDatabase.getInstance().getReference().child("Chats")
                                                    .child(receiverRoom)
                                                    .child("messages")
                                                    .child(randomKey)
                                                    .setValue(messagesModel)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            HashMap<String, Object> hashMap = new HashMap<>();
                                                            hashMap.put("lastMsg", title);
                                                            hashMap.put("lastMsgTime", date.getTime());

                                                            FirebaseDatabase.getInstance().getReference().child("Chats").child(senderRoom)
                                                                    .updateChildren(hashMap);
                                                            FirebaseDatabase.getInstance().getReference().child("Chats").child(receiverRoom)
                                                                    .updateChildren(hashMap);


                                                            if(isReceiverStatusOnline.equals("Offline")){
                                                            FirebaseDatabase.getInstance().getReference("Users").child(receiverUid)
                                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                        @Override
                                                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                            String FCMTOKEN = snapshot.child("FirebaseMessagingToken").getValue(String.class);
                                                                            SendNotification notificationObj = new SendNotification(FCMTOKEN,senderName , youtubeLink, getApplicationContext(),messagesModel.getMessageId(),receiverUid);
                                                                            notificationObj.sendNotification();
                                                                        }

                                                                        @Override
                                                                        public void onCancelled(@NonNull DatabaseError error) {

                                                                        }
                                                                    });
                                                            }
                                                        }
                                                    });
                                        }
                                    });
                        }
                    } catch (JSONException e) {
                        Log.e("naikaj",e.getMessage());
                    }
                },
                error -> Log.e("YouTubeMetadata", "Error fetching metadata", error));

        requestQueue.add(request);
    }
    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Permissions are granted by default for older Android versions
    }

    private void startRecording() {
        try {
            String fileName = "AUDIO_" + System.currentTimeMillis() + ".3gp";
            audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/" + fileName;
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;

            // Show recording indicator and start timer
            binding.recordingIndicator.setVisibility(View.VISIBLE);
            startTimer();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to start recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            isRecording = false;
        }
    }
    private void stopRecording() {
        try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;

                // Hide recording indicator and stop timer
                binding.recordingIndicator.setVisibility(View.GONE);
                stopTimer();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Toast.makeText(this, "Recording stop failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(updateTimerRunnable, 0);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(updateTimerRunnable);
        binding.recordingTimer.setText("Recording..."); // Reset timer text
    }

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsedTime = System.currentTimeMillis() - startTime;
            int seconds = (int) (elapsedTime / 1000) % 60;
            int minutes = (int) (elapsedTime / (1000 * 60)) % 60;
            String time = String.format("%02d:%02d", minutes, seconds);
            binding.recordingTimer.setText(time);
            timerHandler.postDelayed(this, 500);
        }
    };

    private void sendAudioMessage(String filePath) {
        Uri audioUri = Uri.fromFile(new File(filePath));
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("Chats/audio/" + System.currentTimeMillis() + ".mp3");

        storageRef.putFile(audioUri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String audioUrl = uri.toString();

                // Create a message model with audioUrl
                String randomKey = database.getReference().push().getKey();
                MessagesModel messagesModel = new MessagesModel(senderUid, "audio", new Date().getTime(), "false","false");
                messagesModel.setMsgUrl(audioUrl);
                messagesModel.setMessageId(randomKey);
                // Save the message to the database
                FirebaseDatabase.getInstance().getReference()
                        .child("Chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(randomKey)
                        .setValue(messagesModel)
                        .addOnSuccessListener(unused -> {
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("messageId",messagesModel.getMessageId());
                            database.getReference().child("Chats").child(senderRoom)
                                    .updateChildren(hashMap);
                            FirebaseDatabase.getInstance().getReference()
                                    .child("Chats")
                                    .child(receiverRoom)
                                    .child("messages")
                                    .child(randomKey)
                                    .setValue(messagesModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            increaseCountOfUnseenMsg();
                                            if (isReceiverStatusOnline.equals("Offline")) {
                                                database.getReference("Users").child(receiverUid)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                String FCMTOKEN = snapshot.child("FirebaseMessagingToken").getValue(String.class);
                                                                SendNotification notificationObj = new SendNotification(FCMTOKEN, senderName, "audio", getApplicationContext(), messagesModel.getMessageId(), receiverUid);
                                                                notificationObj.sendNotification();
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                            }
                                                        });
                                            }
                                            HashMap<String, Object> lastMsgMap = new HashMap<>();
                                            lastMsgMap.put("lastMsg", "audio");
                                            lastMsgMap.put("lastMsgTime", System.currentTimeMillis());
                                            FirebaseDatabase.getInstance().getReference()
                                                    .child("Chats").child(senderRoom).updateChildren(lastMsgMap);
                                            FirebaseDatabase.getInstance().getReference()
                                                    .child("Chats").child(receiverRoom).updateChildren(lastMsgMap);
                                        }
                                    });
                        });
            });
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to send audio", Toast.LENGTH_SHORT).show());
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId(); // Get the ID of the selected menu item

        if (id == R.id.status) {
            startActivity(new Intent(this, UserProfileDetailsActivity.class));
        } else if (id == R.id.chats) {
            // Add behavior for R.id.setting or R.id.invite if needed
            //startActivity(new Intent(ChatsActivity.this,SettingsActivity.class));
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }
    private void sendVideoMessage(String videoUrl) {
        Date date = new Date();
        // Create a message object
        MessagesModel messagesModel = new MessagesModel(senderUid, "video", date.getTime(),"false","false");
        messagesModel.setMsgUrl(videoUrl); // Set the video URL
        binding.msgBox.setText("");
        // Generate a unique key for the message
        String randomKey = FirebaseDatabase.getInstance().getReference().push().getKey();
        messagesModel.setMessageId(randomKey);
        // Save message to sender's chat
        FirebaseDatabase.getInstance().getReference().child("Chats")
                        .child(senderRoom).
                         child("messages").child(randomKey).setValue(messagesModel).addOnSuccessListener(unused -> {
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("messageId",messagesModel.getMessageId());
                    database.getReference().child("Chats").child(senderRoom)
                            .updateChildren(hashMap);
                    FirebaseDatabase.getInstance().getReference().child("Chats")
                            .child(receiverRoom)
                            .child("messages")
                            .child(randomKey)
                            .setValue(messagesModel).addOnSuccessListener(unused1 -> {
                                // Save message to receiver's chat
                                if(isReceiverStatusOnline.equals("Offline")) {
                                    database.getReference("Users").child(receiverUid)
                                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                    String FCMTOKEN = snapshot.child("FirebaseMessagingToken").getValue(String.class);
                                                    SendNotification notificationObj = new SendNotification(FCMTOKEN, senderName, "video", getApplicationContext(),messagesModel.getMessageId(),receiverUid);
                                                    notificationObj.sendNotification();
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError error) {

                                                }
                                            });
                                }
                                HashMap<String, Object> hashMap1 = new HashMap<>();
                                hashMap1.put("lastMsg", messagesModel.getMessage());
                                hashMap1.put("lastMsgTime", date.getTime());
                                // Update last message for both chats
                                database.getReference().child("Chats").child(senderRoom)
                                        .updateChildren(hashMap1);
                                database.getReference().child("Chats").child(receiverRoom)
                                        .updateChildren(hashMap1);
                            });
                         });
    }
   @Override
   protected void onResume() {
       super.onResume();
       database.getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue("Online");
       database.getReference().child("Chats").child(receiverRoom).child("status").setValue("Online");
       database.getReference().child("Chats").child(senderRoom).child("unSeenMessage").setValue("0");
   }
    @Override
    protected void onPause() {
        super.onPause();
        database.getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue(String.valueOf(new Date().getTime()));
        database.getReference().child("Chats").child(receiverRoom).child("status").setValue("Offline");
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Go back to the previous activity
        return true;
    }
    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        101
                );
            }
        }
    }

}