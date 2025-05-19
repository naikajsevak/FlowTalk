package com.example.flowtalk;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flowtalk.adapter.TopStatusAdapter;
import com.example.flowtalk.adapter.UserAdapter;
import com.example.flowtalk.databinding.ActivityMainBinding;
import com.example.flowtalk.models.Status;
import com.example.flowtalk.models.UserStatus;
import com.example.flowtalk.models.Users;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    FirebaseDatabase database;
    ArrayList<Users> list;
    UserAdapter adapter;
    ArrayList<UserStatus> userStatuses;
    TopStatusAdapter statusAdapter;
    FirebaseMessaging firebaseMessaging;
    ProgressDialog progressBar;
    Users users;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        database = FirebaseDatabase.getInstance();
        firebaseMessaging = FirebaseMessaging.getInstance();
        firebaseMessaging.getToken()
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        HashMap<String, Object> hashMap = new HashMap<>();

                        hashMap.put("token", s);
                        database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid())
                                .updateChildren(hashMap);
                    }
                });
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        progressBar = new ProgressDialog(this);
        progressBar.setMessage("Uploading image...");
        progressBar.setCancelable(false);
        list = new ArrayList<>();
        userStatuses = new ArrayList<>();

        database.getReference().child("Users").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        users = snapshot.getValue(Users.class);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        adapter = new UserAdapter(list, this);
        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userStatuses.clear();
                    for (DataSnapshot storySnapshot : snapshot.getChildren()) {
                        UserStatus userStatus = new UserStatus();
                        userStatus.setName(storySnapshot.child("name").getValue(String.class));
                        userStatus.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));
                        userStatus.setLastUpdate(storySnapshot.child("lastUpdate").getValue(Long.class));
                        ArrayList<Status> arrayList = new ArrayList<>();
                        for (DataSnapshot statusSnapshot : storySnapshot.child("statuses").getChildren()) {
                            Status sampleStatus = statusSnapshot.getValue(Status.class);
                            arrayList.add(sampleStatus);
                        }
                        userStatus.setArrayList(arrayList);
                        userStatuses.add(userStatus);
                    }
                    binding.statusRec.hideShimmerAdapter();
                    statusAdapter.notifyDataSetChanged();
                }
                binding.statusRec.hideShimmerAdapter();
                statusAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.statusRec.setLayoutManager(layoutManager);
        statusAdapter = new TopStatusAdapter(this, userStatuses);

        binding.statusRec.setAdapter
                (statusAdapter);


        binding.recyclerViewActivityChats.setAdapter(adapter);
        binding.recyclerViewActivityChats.showShimmerAdapter();
        binding.statusRec.showShimmerAdapter();
        database.getReference().child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                     Users user = snapshot1.getValue(Users.class);

                    if (user != null && user.getUserName() != null &&
                            !FirebaseAuth.getInstance().getUid().equals(user.getUserId())) {

                        database.getReference().child("Chats")
                                .child(FirebaseAuth.getInstance().getUid() + user.getUserId())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            Long timeStamp = snapshot.child("lastMsgTime").getValue(Long.class);
                                            long time = timeStamp != null ? timeStamp : 0L; // Default value if null
                                            user.setTimeStamp(time);
                                        }
                                        list.add(user); // Add the user AFTER setting the timestamp
                                        list.sort((chat1, chat2) -> Long.compare(chat2.getTimeStamp(), chat1.getTimeStamp())); // Sort after adding
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        // Handle error if necessary
                                    }
                                });
                    }
                }
                adapter.notifyDataSetChanged(); // Notify changes
                binding.recyclerViewActivityChats.hideShimmerAdapter();
                binding.view.setVisibility(View.VISIBLE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error if necessary
            }
        });
        binding.bottomNevigation.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if(id==R.id.status) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, 75);
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null){
            if (data.getData()!=null){
                progressBar.show();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                Date date = new Date();
                StorageReference reference = storage.getReference().child("status")
                        .child(date.getTime()+"");
                reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()){
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    UserStatus userStatus = new UserStatus();
                                    userStatus.setName(users.getUserName());
                                    userStatus.setProfileImage(users.getProfile());
                                    userStatus.setLastUpdate(date.getTime());

                                    HashMap<String,Object> hashMap = new HashMap<>();

                                    hashMap.put("name",userStatus.getName());
                                    hashMap.put("profileImage",userStatus.getProfileImage());
                                    hashMap.put("lastUpdate",userStatus.getLastUpdate());

                                    Status status = new Status(uri.toString(),userStatus.getLastUpdate());

                                    database.getReference().child("stories").child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                                            .updateChildren(hashMap);

                                    database.getReference().child("stories").child(FirebaseAuth.getInstance().getUid())
                                            .child("statuses").push().setValue(status);

                                    progressBar.dismiss();
                                }
                            });
                        }
                    }
                });
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        database.getReference().child("presence").child (FirebaseAuth.getInstance().getUid()).setValue("Online");
        list.sort((chat1, chat2) -> Long.compare(chat2.getTimeStamp(), chat1.getTimeStamp()));
        adapter.notifyDataSetChanged();
    }
    @Override
    protected void onPause() {
        super.onPause();
        database.getReference().child("presence").child(FirebaseAuth.getInstance().getUid()).setValue(String.valueOf(new Date().getTime()));
    }
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId(); // Get the ID of the selected menu item
       if (id == R.id.group) {
            startActivity(new Intent(this, GroupeChat.class));
        } else if (id == R.id.setting) {
            // Add behavior for R.id.setting or R.id.invite if needed
            startActivity(new Intent(MainActivity.this,SettingsActivity.class));
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top_menu, menu);

        // Find the search item in the menu
        MenuItem searchItem = menu.findItem(R.id.app_bar_search);
        SearchView searchView = (SearchView) searchItem.getActionView(); // Correct type

        // Set up the query text listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Perform search when user submits
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Update results as the user types
                adapter.getFilter().filter(newText);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
}