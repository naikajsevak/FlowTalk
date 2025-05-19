package com.example.flowtalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.flowtalk.adapter.GroupChatAdapter;
import com.example.flowtalk.adapter.TopStatusAdapter;
import com.example.flowtalk.databinding.ActivityGroupeChatBinding;
import com.example.flowtalk.models.GroupeMessagesModel;
import com.example.flowtalk.models.MessagesModel;
import com.example.flowtalk.models.Users;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class GroupeChat extends AppCompatActivity {
    ActivityGroupeChatBinding binding;
    FirebaseDatabase database;
    FirebaseStorage storage;
    ArrayList<Users> list;
    GroupChatAdapter adapter;
    String userName,userProfilePic;
    ArrayList<GroupeMessagesModel> messagesModelArrayList;
    TopStatusAdapter statusAdapter;
    ProgressDialog dialog;
    String senderUid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupeChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        senderUid = FirebaseAuth.getInstance().getUid();
        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image...");
        dialog.setCancelable(false);
        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();
        messagesModelArrayList = new ArrayList<>();
        database.getReference().child("Users").child(senderUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    userName = snapshot.child("userName").getValue(String.class);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        adapter = new GroupChatAdapter(messagesModelArrayList,this,userName,userProfilePic);
        binding.recyclerViewActivityChats.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewActivityChats.setAdapter(adapter);

        database.getReference().child("public").child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messagesModelArrayList.clear();
                        for (DataSnapshot snapshot1:snapshot.getChildren()){
                            if(snapshot1.exists()) {
                                GroupeMessagesModel messagesModel = snapshot1.getValue(GroupeMessagesModel.class);
                                Objects.requireNonNull(messagesModel).setMessageId(snapshot1.getKey());
                                messagesModelArrayList.add(messagesModel);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        binding.recyclerViewActivityChats.scrollToPosition(messagesModelArrayList.size()-1);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        binding.leftBaseWest.setOnClickListener(view -> {
            finish();
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
              /*if(binding.msgBox.getText().toString().trim().isEmpty())
                    binding.sendAudioMsgMic.setVisibility(View.VISIBLE);
                else
                    binding.sendAudioMsgMic.setVisibility(View.GONE);*/
                    database.getReference().child("public").child("status").setValue(userName + " is typing...");
                    database.getReference().child("public").child("SenderId").setValue(senderUid);
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(runnable, 1000);
            }
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    database.getReference().child("public").child("status").setValue("");
                    database.getReference().child("public").child("SenderId").setValue("");
                }
            };
        });
        database.getReference().child("public").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    String status = snapshot.child("status").getValue(String.class);
                    String senderUid= snapshot.child("SenderId").getValue(String.class);
                    if(status!=null && !status.isEmpty() &&  senderUid!=null && !senderUid.equals(FirebaseAuth.getInstance().getUid())){
                        binding.online.setText(status);
                        binding.online.setVisibility(View.VISIBLE);
                    }
                    else
                        binding.online.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        binding.sendImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = binding.msgBox.getText().toString();
                Date date = new Date();
                GroupeMessagesModel messagesModel = new GroupeMessagesModel(senderUid,msg,date.getTime(),-1);
                binding.msgBox.setText("");
                database.getReference().child("public").child("messages").push().setValue(messagesModel);
            }
        });
        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent,25);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 25){
            if (data!=null){
                if (data.getData()!=null){
                    Uri selectedImage = data.getData();
                    Calendar calendar = Calendar.getInstance();
                    StorageReference reference = storage.getReference().child("Chats")
                            .child(calendar.getTimeInMillis()+"");
                    dialog.show();
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()){
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        dialog.dismiss();
                                        String filePath = uri.toString();
                                        String msg = binding.msgBox.getText().toString();
                                        Date date = new Date();
                                        MessagesModel messagesModel = new MessagesModel(senderUid,msg,date.getTime(),"false","false");
                                        messagesModel.setMessage("photo");
                                        messagesModel.setMsgUrl(filePath);
                                        binding.msgBox.setText("");

                                        database.getReference().child("public").child("messages")
                                                .push()
                                                .setValue(messagesModel);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        }
    }
}