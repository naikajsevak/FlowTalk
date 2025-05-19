package com.example.flowtalk.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flowtalk.ChatsActivity;
import com.example.flowtalk.MainActivity;
import com.example.flowtalk.R;
import com.example.flowtalk.databinding.ItemStatusBinding;
import com.example.flowtalk.models.Status;
import com.example.flowtalk.models.UserStatus;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import omari.hamza.storyview.StoryView;
import omari.hamza.storyview.callback.StoryClickListeners;
import omari.hamza.storyview.model.MyStory;

public class TopStatusAdapter extends RecyclerView.Adapter<TopStatusAdapter.TopStatusViewHolder> {
  Context context;
  ArrayList<UserStatus> arrayList;

    public TopStatusAdapter(Context context, ArrayList<UserStatus> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public TopStatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_status,parent,false);
        return new TopStatusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopStatusViewHolder holder, int position) {
        UserStatus userStatus = arrayList.get(position);
        Status lastStatus =userStatus.getArrayList().get(userStatus.getArrayList().size()-1);

        Picasso.get().load(lastStatus.getImageUrl()).placeholder(R.drawable.avatar).into(holder.binding.image);
        holder.binding.circularStatusView.setPortionsCount(userStatus.getArrayList().size());

          holder.binding.circularStatusView.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  ArrayList<MyStory> myStories = new ArrayList<>();
                  for (Status status : userStatus.getArrayList()){
                      myStories.add(new MyStory(status.getImageUrl()));
                  }
                  new StoryView.Builder(((MainActivity)context).getSupportFragmentManager())
                          .setStoriesList(myStories) // Required
                          .setStoryDuration(5000) // Default is 2000 Millis (2 Seconds)
                          .setTitleText(userStatus.getName()) // Default is usHidden
                          .setSubtitleText("") // Default is Hidden
                          .setTitleLogoUrl(userStatus.getProfileImage()) // Default is Hidden
                          .setStoryClickListeners(new StoryClickListeners() {
                              @Override
                              public void onDescriptionClickListener(int position) {
                                  //your action
                              }

                              @Override
                              public void onTitleIconClickListener(int position) {
                                  //your action
                                  context.startActivity(new Intent(context, ChatsActivity.class));
                              }
                          }) // Optional Listeners
                          .build() // Must be called before calling show method
                          .show();
              }
          });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class TopStatusViewHolder extends RecyclerView.ViewHolder{
        ItemStatusBinding binding;
        public TopStatusViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemStatusBinding.bind(itemView);
        }
    }
}
