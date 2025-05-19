package com.example.flowtalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flowtalk.databinding.ActivityOtpBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class OtpActivity extends AppCompatActivity {
    ActivityOtpBinding binding;
    FirebaseAuth auth;
    ProgressDialog dialog;
    String phone ,verification;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //getSupportActionBar().hide();
        auth = FirebaseAuth.getInstance();
        phone = getIntent().getStringExtra("phone");
        binding.phoneLabel.setText(phone);
        dialog = new ProgressDialog(this);
        dialog.setTitle("Verification");
        dialog.setMessage("Code Sending...");
        dialog.show();
        initiateOtp();
        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(binding.otpView.getOTP().isEmpty()){
                    Toast.makeText(getApplicationContext(), "Blank field can not be processed", Toast.LENGTH_LONG).show();
                }
                else if (verification == null || verification.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Verification ID is missing. Please try again.", Toast.LENGTH_LONG).show();
                }
                else {
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verification,binding.otpView.getOTP());
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });
    }

    private void initiateOtp() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phone, 60, TimeUnit.SECONDS, this, new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signInWithPhoneAuthCredential(phoneAuthCredential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("YourTag", "" + e.getMessage(), e);
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        verification = s;
                        dialog.dismiss();
                    }
                });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(OtpActivity.this, SetUpProfile.class));
                            finish();

                        } else {
                            Toast.makeText(getApplicationContext(), "Sign in Code Error", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}