package com.example.flowtalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.flowtalk.databinding.ActivitySignUpBinding;
import com.example.flowtalk.models.Users;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class SignUp extends AppCompatActivity {

    private FirebaseAuth auth;
    FirebaseDatabase database;
    ProgressDialog progressDialog;
    ActivitySignUpBinding binding;
    private boolean isPasswordVisible = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();
        binding.already.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, SignIn.class);
            startActivity(intent);
            finish();
        });

        binding.textView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();

            // Get screen width in pixels
            int screenWidthPixels = displayMetrics.widthPixels;

            // Convert width to dp (density-independent pixels)
            float screenWidthDp = screenWidthPixels / displayMetrics.density;
            if (screenWidthDp > 500) {
                binding.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            } else {
                binding.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            }
        });

        auth= FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        progressDialog = new ProgressDialog(SignUp.this);
        progressDialog.setTitle("Create Account");
        progressDialog.setMessage("User created ");

        binding.Password.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = binding.Password.getCompoundDrawables()[2]; // DrawableEnd (right icon)
                if (drawableEnd != null && event.getRawX() >= (binding.Password.getRight() - drawableEnd.getBounds().width())) {
                    // Toggle password visibility
                    if (isPasswordVisible) {
                        // Hide password
                        binding.Password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        binding.Password.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.password_icon, 0, R.drawable.ic_visibility_off, 0);
                    } else {
                        // Show password
                        binding.Password.setInputType(InputType.TYPE_CLASS_TEXT);
                        binding.Password.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.password_icon, 0, R.drawable.ic_visibility_on, 0);
                    }
                    isPasswordVisible = !isPasswordVisible;
                    // Move cursor to the end
                    binding.Password.setSelection(binding.Password.getText().length());
                    return true;
                }
            }
            return false;
        });

        binding.signup.setOnClickListener(v -> {
            if(binding.userName.getText().toString().isEmpty() || binding.emailAddress.getText().toString().isEmpty() || binding.Password.getText().toString().isEmpty())
            {
                Toast.makeText(SignUp.this, "All Field Required", Toast.LENGTH_SHORT).show();
            }
            else {
                progressDialog.show();
                auth.createUserWithEmailAndPassword
                                (binding.emailAddress.getText().toString(), binding.Password.getText().toString()).
                        addOnCompleteListener(task -> {
                            progressDialog.dismiss();
                            if (task.isSuccessful()) {
                                Users users = new Users(binding.userName.getText().toString(), binding.emailAddress.getText().toString(), binding.Password.getText().toString());
                                String id = Objects.requireNonNull(Objects.requireNonNull(task.getResult()).getUser()).getUid();
                               // database.getReference("Users").child("Users").child(id).setValue(users);
                                startActivity(new Intent(SignUp.this,SetUpProfile.class));
                                finish();

                            } else {
                                Toast.makeText(SignUp.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                System.out.println(task.getException().getMessage());
                            }
                        });
            }
        });

    }
}