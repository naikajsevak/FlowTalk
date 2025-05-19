package com.example.flowtalk.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flowtalk.ChatsActivity;
import com.example.flowtalk.R;
import com.example.flowtalk.databinding.DeleteChatsDialogBoxBinding;
import com.example.flowtalk.models.Users;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolderClass> implements Filterable {

    ArrayList<Users> list; // Original list
    ArrayList<Users> filteredList; // Filtered list
    Context context;
    String msg;
    TextView lastMsg;
    public UserAdapter(ArrayList<Users> list, Context context) {
        this.list = list;
        this.filteredList =list; // Initialize filtered list with the full list
        this.context = context;
    }

    @NonNull
    public ViewHolderClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout, parent, false);
        return new ViewHolderClass(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolderClass holder, int position) {
        Users users = filteredList.get(holder.getAdapterPosition()); // Use filteredList instead of list

        String senderId = FirebaseAuth.getInstance().getUid();
        String senderRoom = senderId + users.getUserId();
        String receiverRoom = users.getUserId()+senderId;
        try {
            // Your existing Firebase listeners for last message and unseen message
            FirebaseDatabase.getInstance().getReference().child("Chats").child(senderRoom)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            TextView timer = holder.itemView.findViewById(R.id.time);
                            lastMsg = holder.itemView.findViewById(R.id.last_msg);
                            TextView cntUnseenMsg = holder.itemView.findViewById(R.id.cnt);
                             msg = snapshot.child("unSeenMessage").getValue(String.class);
                            if (msg == null) {
                                msg = "0";
                            }
                            if (msg.equals("0")) {
                                cntUnseenMsg.setVisibility(View.GONE);
                                timer.setTextColor(Color.parseColor("#FF000000"));
                                timer.setTypeface(null, Typeface.NORMAL);
                                lastMsg.setTypeface(null, Typeface.NORMAL);
                            } else {
                                cntUnseenMsg.setText(msg);
                                cntUnseenMsg.setVisibility(View.VISIBLE);
                                timer.setTextColor(Color.parseColor("#689F38"));
                                timer.setTypeface(timer.getTypeface(), Typeface.BOLD);
                                lastMsg.setTypeface(lastMsg.getTypeface(), Typeface.BOLD);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

             FirebaseDatabase.getInstance().getReference()
                    .child("Chats").child(senderRoom)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            TextView last, LastTime;
                            ImageView imageView;
                            last = holder.itemView.findViewById(R.id.last_msg);
                            LastTime = holder.itemView.findViewById(R.id.time);
                            imageView = holder.itemView.findViewById(R.id.image_indicator);
                            imageView.setVisibility(View.GONE);
                            if (snapshot.exists()) {
                                String status = snapshot.child("status").getValue(String.class);
                                if (status == null) {
                                    status = "Offline";
                                }
                                if (!status.isEmpty()) {
                                    last.setTextColor(Color.parseColor("#689F38"));
                                    last.setTypeface(last.getTypeface(), Typeface.BOLD);
                                    last.setText(status);
                                    if (status.equals("Online") || status.equals("Offline")) {
                                        last.setTextColor(Color.parseColor("#FF000000"));
                                        if(msg.equals("0"))
                                            last.setTypeface(null, Typeface.NORMAL);
                                        else
                                            lastMsg.setTypeface(lastMsg.getTypeface(), Typeface.BOLD);

                                        String lastMsg = snapshot.child("lastMsg").getValue(String.class);
                                        Long timeObject = snapshot.child("lastMsgTime").getValue(Long.class);
                                        if(!Objects.equals(senderId, users.getUserId())) {
                                            String lastMsgId = snapshot.child("messageId").getValue(String.class);

                                            // 🔥 Fetch the delivered and seen status of the last message
                                            if (lastMsgId != null) {
                                                DatabaseReference lastMsgRef = FirebaseDatabase.getInstance()
                                                        .getReference("Chats").child(receiverRoom);
                                                lastMsgRef.child("messages").child(lastMsgId).addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot msgSnapshot) {
                                                        if (msgSnapshot.exists()) {
                                                            String delivered = msgSnapshot.child("delivered").getValue(String.class);
                                                            String seen = msgSnapshot.child("seen").getValue(String.class);

                                                            // 🔥 Update the tick indicator based on the message status

                                                            if ("true".equals(seen)) {
                                                                imageView.setImageResource(R.drawable.double_check_sky_color); // Blue double tick ✅✅
                                                            } else if ("true".equals(delivered)) {
                                                                imageView.setImageResource(R.drawable.double_check_mark); // Grey double tick ✅✅
                                                            } else {
                                                                imageView.setImageResource(R.drawable.single_check_mark); // Single tick ✅
                                                            }
                                                            imageView.setVisibility(View.VISIBLE);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                    }
                                                });
                                            }
                                        }
                                        if (timeObject != null) {
                                            long time = timeObject;
                                            last.setText(lastMsg != null ? lastMsg : "No message");
                                            LastTime.setText(calculateHours(time));
                                            if (holder.getAdapterPosition() != -1 && holder.getAdapterPosition() < list.size())
                                                list.get(holder.getAdapterPosition()).setTimeStamp(time);
                                        } else {
                                            imageView.setVisibility(View.GONE);
                                            last.setText(lastMsg != null ? lastMsg : "Tap to chat");
                                            LastTime.setText("");
                                        }
                                    }
                                }
                            } else {
                                last.setText("Tap to chat");
                                LastTime.setText("");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        holder.userName.setText(users.getUserName());
        Picasso.get().load(users.getProfile()).placeholder(R.drawable.avatar).into(holder.image);
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, ChatsActivity.class);
            intent.putExtra("name", users.getUserName());
            intent.putExtra("profileImage", users.getProfile());
            intent.putExtra("uid", users.getUserId());
            intent.putExtra("token", users.getToken());
            context.startActivity(intent);
        });

        holder.itemView.setOnLongClickListener(view -> {
            view.findViewById(R.id.user_name);
            View deleteView = LayoutInflater.from(context).inflate(R.layout.delete_chats_dialog_box, null);
            DeleteChatsDialogBoxBinding binding = DeleteChatsDialogBoxBinding.bind(deleteView);
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Delete Chats")
                    .setView(binding.getRoot())
                    .create();
            binding.yes.setOnClickListener(view1 -> {
                FirebaseDatabase.getInstance().getReference().child("Chats")
                        .child(senderRoom).setValue(null);
                dialog.dismiss();
            });
            binding.no.setOnClickListener(view12 -> dialog.dismiss());
            dialog.show();
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size(); // Use filteredList
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint.toString().toLowerCase().trim();
                if (query.isEmpty()) {
                    filteredList = list; // Reset to original list
                } else {
                    ArrayList<Users> tempList = new ArrayList<>();
                    for (Users user : list) {
                        if (user.getUserName().toLowerCase().contains(query)) {
                            tempList.add(user);
                        }
                    }
                    filteredList = tempList;
                }
                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList = (ArrayList<Users>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public static class ViewHolderClass extends RecyclerView.ViewHolder {
        ImageView image;
        TextView userName, last, time;

        public ViewHolderClass(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.profile_image);
            userName = itemView.findViewById(R.id.user_name);
            last = itemView.findViewById(R.id.last_msg);
            time = itemView.findViewById(R.id.time);
        }
    }

    private String calculateHours(long timestamp) {
        long currentTimeMillis = System.currentTimeMillis();
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp);
        cal1.set(Calendar.HOUR_OF_DAY, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(currentTimeMillis);
        cal2.set(Calendar.HOUR_OF_DAY, 0);
        cal2.set(Calendar.MINUTE, 0);
        cal2.set(Calendar.SECOND, 0);
        cal2.set(Calendar.MILLISECOND, 0);

        long millisDiff = cal2.getTimeInMillis() - cal1.getTimeInMillis();
        long dayDifference = millisDiff / (1000 * 60 * 60 * 24);

        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        if (dayDifference == 0) {
            return dateFormat.format(new Date(timestamp));
        } else if (dayDifference == 1) {
            return "yesterday";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}