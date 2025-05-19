package com.example.flowtalk;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flowtalk.databinding.ActivityForgetPasswordBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgetPasswordActivity extends AppCompatActivity {
    FirebaseAuth auth;
    ActivityForgetPasswordBinding binding;
    ProgressBar progressBar;
    String email;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressBar = new ProgressBar(this);
        binding=ActivityForgetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               onBackPressed();
            }
        });
        binding.resetPswBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email=binding.email.getText().toString().trim();
                if(!TextUtils.isEmpty(email)){
                    resetPassword();
                }else {
                    binding.email.setError("Email field can't be empty");

                }
            }
        });

    }
    private void resetPassword()
    {
        progressBar.setVisibility(View.VISIBLE);
        binding.resetPswBtn.setVisibility(View.INVISIBLE);
        auth.sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(ForgetPasswordActivity.this, "Reset password link has been sent to your registered Email", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ForgetPasswordActivity.this,SignIn.class);
                startActivity(intent);
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ForgetPasswordActivity.this, "Technical Issue", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.INVISIBLE);
                binding.resetPswBtn.setVisibility(View.VISIBLE);
            }
        });
    }
}