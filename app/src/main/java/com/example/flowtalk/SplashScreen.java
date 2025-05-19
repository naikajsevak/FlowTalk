package com.example.flowtalk;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flowtalk.databinding.ActivitySplashScreenBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashScreen extends AppCompatActivity {
    ActivitySplashScreenBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();
        // Hide the status bar
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        if(getIntent().getExtras()!=null && getIntent().getExtras().getString("userId")!=null)
        {
            String userId=getIntent().getExtras().getString("userId");
            Toast.makeText(this, userId, Toast.LENGTH_SHORT).show();
            FirebaseDatabase.getInstance().getReference("Users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Intent intent = new Intent(SplashScreen.this, ChatsActivity.class);
                        intent.putExtra("name", snapshot.child("userName").getValue(String.class));
                        intent.putExtra("profileImage", snapshot.child("profile").getValue(String.class));
                        intent.putExtra("uid", userId);
                        intent.putExtra("token", snapshot.child("token").getValue(String.class));
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        }
        else {
            binding.lottieAnimation.animate().translationY(-2000).setDuration(2000).setStartDelay(2900);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(SplashScreen.this, SignIn.class));
                    finish();
                }
            }, 5000);
        }
    }
}