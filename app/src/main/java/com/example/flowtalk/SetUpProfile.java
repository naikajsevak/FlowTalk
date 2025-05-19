package com.example.flowtalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flowtalk.databinding.ActivitySetUpProfileBinding;
import com.example.flowtalk.models.Users;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Objects;

public class SetUpProfile extends AppCompatActivity {
    ActivitySetUpProfileBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    ProgressDialog dialog;
    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetUpProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();
        dialog = new ProgressDialog(this);
        dialog.setTitle("Upload");
        dialog.setMessage("Profile updating...");

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 45);
            }
        });
        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.NameBox.getText().toString().isEmpty()) {
                    binding.NameBox.setError("Please type your name");
                    return;
                }
                if (uri != null) {
                    dialog.show();
                    StorageReference storageReference = storage.getReference().child("profiles").child(auth.getUid());
                    storageReference.putFile(uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {
                                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {

                                        Users users = new Users(uri.toString(),
                                                binding.NameBox.getText().toString(),
                                                Objects.requireNonNull(auth.getCurrentUser()).getEmail(),
                                                auth.getUid());
                                        database.getReference().child("Users").child(auth.getUid()).setValue(users)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        if (task.isSuccessful()) {
                                                            dialog.dismiss();
                                                            startActivity(new Intent(SetUpProfile.this, MainActivity.class));
                                                            finish();
                                                        }
                                                    }
                                                });
                                    }
                                });
                            }
                        }
                    });
                } else {
                    dialog.show();
                    Users users = new Users("No Image",
                            binding.NameBox.getText().toString(),
                            auth.getCurrentUser().getEmail(),
                            auth.getUid());
                    database.getReference().child("Users").child(auth.getUid()).setValue(users)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    dialog.dismiss();
                                    startActivity(new Intent(SetUpProfile.this, MainActivity.class));
                                    finish();
                                }
                            });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            if (data.getData() != null) {
                binding.imageView.setImageURI(data.getData());
                uri = data.getData();
            }
        }
    }
}