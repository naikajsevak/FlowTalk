package com.example.flowtalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flowtalk.databinding.ActivitySettingsBinding;
import com.example.flowtalk.models.Users;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    ActivitySettingsBinding binding;
    FirebaseStorage storage;
    FirebaseAuth auth;
    FirebaseDatabase database;
    ProgressDialog progressDialog;
    String profileImage;
    Uri imageUir;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Objects.requireNonNull(getSupportActionBar()).setTitle("Setting");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseDatabase.getInstance().getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue("Offline");
                FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        auth.signOut();
                        Intent intent = new Intent(SettingsActivity.this,SignIn.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
                auth.signOut();
                startActivity(new Intent(SettingsActivity.this,SignIn.class));
            }
        });
        binding.profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, ImageActivity.class);
                intent.putExtra("userName",binding.etUser.getText().toString());
                intent.putExtra("image",profileImage);
                startActivity(intent);
            }
        });
        progressDialog = new ProgressDialog(SettingsActivity.this);
        progressDialog.setTitle("Update Profile");
        progressDialog.setMessage("Profile Updating...");
        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        binding.save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(binding.etUser.getText().toString().isEmpty())
                {
                    Toast.makeText(SettingsActivity.this, "Set your name", Toast.LENGTH_SHORT).show();
                }
                else {
                    String status = binding.Status.getText().toString();
                    String userName = binding.etUser.getText().toString();
                    HashMap<String, Object> obj = new HashMap<>();
                    obj.put("userName", userName);
                    obj.put("status", status);
                    database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid())
                            .updateChildren(obj);
                    Toast.makeText(SettingsActivity.this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        database.getReference().child("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Users users = snapshot.getValue(Users.class);
                        if(users!=null) {
                            if(!Objects.equals(users.getProfile(), "")) {
                                Picasso.get().load(Objects.requireNonNull(users).getProfile()).placeholder(R.drawable.avtar).into(binding.profileImage);
                                profileImage=users.getProfile();
                            }
                            binding.Status.setText(users.getStatus());
                            binding.etUser.setText(users.getUserName());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.plus.setOnClickListener(view -> {
            progressDialog.show();
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent,33);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data!= null && data.getData()!=null){
            Uri file = data.getData();
            binding.profileImage.setImageURI(file);
            final StorageReference reference = storage.getReference("profile_picture").child("profile_picture")
                    .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));
            reference.putFile(file).addOnSuccessListener(taskSnapshot -> reference.getDownloadUrl().addOnSuccessListener(uri -> {
                database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid())
                        .child("profile").setValue(uri.toString());
                progressDialog.dismiss();
                Toast.makeText(SettingsActivity.this, "profile picture updated", Toast.LENGTH_SHORT).show();
            }));
        }
        else
            progressDialog.dismiss();
    }
    @Override
    protected void onResume() {
        super.onResume();
        FirebaseDatabase.getInstance().getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue("Online");
    }
    @Override
    protected void onPause() {
        super.onPause();
      //  if(auth!=null)
           // FirebaseDatabase.getInstance().getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue("Offline");
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Go back to the previous activity
        return true;
    }
}