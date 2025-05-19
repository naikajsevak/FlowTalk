package com.example.flowtalk;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flowtalk.models.Users;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;

public class SignIn extends AppCompatActivity {
    // declare object
    ProgressDialog progressDialog;
    TextView textView,createAccountView,forgotPassword;
    Button button;
    EditText password;
    EditText email;
    FirebaseAuth auth;
    private boolean isPasswordVisible = false;
    ImageView facebook;
    ImageView google;

    GoogleSignInClient googleSignInClient;
    FirebaseDatabase firebaseDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        //getSupportActionBar().hide();
        // assigning id to the particular object
        auth = FirebaseAuth.getInstance();
        google = findViewById(R.id.google);
        createAccountView = findViewById(R.id.create);
        textView = findViewById(R.id.textView2);
        button = findViewById(R.id.signup);
        password = findViewById(R.id.Password);
        email = findViewById(R.id.emailAddress);
        forgotPassword = findViewById(R.id.forgot_psw);
        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignIn.this,ForgetPasswordActivity.class));
            }
        });
        createAccountView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignIn.this,SignUp.class));
                finish();
            }
        });
        password.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = password.getCompoundDrawables()[2]; // DrawableEnd (right icon)
                if (drawableEnd != null && event.getRawX() >= (password.getRight() - drawableEnd.getBounds().width())) {
                    // Toggle password visibility
                    if (isPasswordVisible) {
                        // Hide password
                        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        password.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.password_icon, 0, R.drawable.ic_visibility_off, 0);
                    } else {
                        // Show password
                        password.setInputType(InputType.TYPE_CLASS_TEXT);
                        password.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.password_icon, 0, R.drawable.ic_visibility_on, 0);
                    }
                    isPasswordVisible = !isPasswordVisible;
                    // Move cursor to the end
                    password.setSelection(password.getText().length());
                    return true;
                }
            }
            return false;
        });
        progressDialog = new ProgressDialog(SignIn.this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        facebook = findViewById(R.id.facebook);

        // progressbar
        progressDialog.setTitle("Login");
        progressDialog.setMessage("Login to your Account");

        // remove action bar from activity
       Objects.requireNonNull(getSupportActionBar()).hide();

        // google sign in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        // handle click event for sign in button

        button.setOnClickListener(v -> {
            if (email.getText().toString().isEmpty()) {
                email.setError("Enter Email Address");
                return;
            }
            else if( password.getText().toString().isEmpty()){
                password.setError("Enter password");
                return;
            }
            else {
                progressDialog.show();
                auth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                        .addOnCompleteListener(task -> {
                            progressDialog.dismiss();
                            if (task.isSuccessful()) {
                                Intent intent = new Intent(SignIn.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(SignIn.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        // here we set intent for switching signIn to signUp activity
        textView.setOnClickListener(v -> {
            Intent intent = new Intent(SignIn.this, SignUp.class);
            startActivity(intent);
        });
        // handle click event for google sign in button
        google.setOnClickListener(v -> signIn());

        // here we check the current user is sign in or not
        if (auth.getCurrentUser() != null) {
            Intent intent = new Intent(SignIn.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }


    // google sign in code
    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult.launch(signInIntent);
    }
    public ActivityResultLauncher<Intent> startActivityForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Task<GoogleSignInAccount> task = null ;
                if ((result.getResultCode() == Activity.RESULT_OK)) {
                    task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                }
                handleSignInResult(Objects.requireNonNull(task));

            }
    );
    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                Users users = new Users();
                users.setUserName(account.getDisplayName());
                users.setEmail(account.getEmail());
                users.setUserId(account.getId());
                users.setProfile(Objects.requireNonNull(account.getPhotoUrl()).toString());
                firebaseDatabase.getReference().child("Users").child(Objects.requireNonNull(users.getUserName())).setValue(users);
                Intent intent = new Intent(SignIn.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("TAG", "signInResult:failed code=" + e.getStatusCode());
            //  updateUI(null);
        }
    }

}