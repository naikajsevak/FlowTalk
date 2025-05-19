package com.example.flowtalk.adapter;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flowtalk.ImageActivity;
import com.example.flowtalk.R;
import com.example.flowtalk.databinding.DeleteDialogBinding;
import com.example.flowtalk.databinding.SampleRecieverBinding;
import com.example.flowtalk.databinding.SampleSenderBinding;
import com.example.flowtalk.models.MessagesModel;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter {

    Context context;
    ArrayList<MessagesModel> messages;
    private MediaPlayer mediaPlayer = null;
    String recieverName;
    final int ITEM_SENT = 1;
    final int ITEM_RECEIVE = 2;
    String senderRoom;
    String receiverRoom;

    FirebaseRemoteConfig remoteConfig;

    public ChatAdapter(String recieverName,Context context, ArrayList<MessagesModel> messages, String senderRoom, String receiverRoom) {
        remoteConfig = FirebaseRemoteConfig.getInstance();
        this.context = context;
        this.messages = messages;
        this.senderRoom = senderRoom;
        this.receiverRoom = receiverRoom;
        this.recieverName=recieverName;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == ITEM_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.sample_reciever, parent, false);
            return new ReceiverViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.sample_sender, parent, false);
            return new SentViewHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        MessagesModel message = messages.get(position);
        if(FirebaseAuth.getInstance().getUid().equals(message.getUid())) {
            return ITEM_SENT;
        } else {
            return ITEM_RECEIVE;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessagesModel message = messages.get(position);

        int reactions[] = new int[]{
                R.drawable.ic_fb_like,
                R.drawable.ic_fb_love,
                R.drawable.ic_fb_laugh,
                R.drawable.ic_fb_wow,
                R.drawable.ic_fb_sad,
                R.drawable.ic_fb_angry
        };

        ReactionsConfig config = new ReactionsConfigBuilder(context)
                .withReactions(reactions)
                .build();
        ReactionPopup popup = new ReactionPopup(context, config, (pos) -> {
            if(message.getMessage().equals("This message is removed."))
                return false;
            if(pos < 0)
                return false;
            if(holder.getClass() == SentViewHolder.class) {
                SentViewHolder viewHolder = (SentViewHolder)holder;
                viewHolder.binding.senderFeeling.setImageResource(reactions[pos]);
                viewHolder.binding.senderFeeling.setVisibility(View.VISIBLE);
            } else {
                ReceiverViewHolder viewHolder = (ReceiverViewHolder)holder;
                viewHolder.binding.receiverFeeling.setImageResource(reactions[pos]);
                viewHolder.binding.receiverFeeling.setVisibility(View.VISIBLE);
            }

            message.setFeeling(pos);

            FirebaseDatabase.getInstance().getReference()
                    .child("Chats")
                    .child(senderRoom)
                    .child("messages")
                    .child(message.getMessageId()).setValue(message);

            FirebaseDatabase.getInstance().getReference()
                    .child("Chats")
                    .child(receiverRoom)
                    .child("messages")
                    .child(message.getMessageId()).setValue(message);

            /*FirebaseDatabase.getInstance().getReference()
                    .child("Chats")
                    .child(senderRoom)
                    .child("lastMsg").setValue("you reacted "+message.getFeeling()+" to "+message.getMessage());

            FirebaseDatabase.getInstance().getReference()
                    .child("Chats")
                    .child(receiverRoom)
                    .child("lastMsg").setValue("reacted "+message.getFeeling()+" to "+message.getMessage());*/

            return true; // true is closing popup, false is requesting a new selection
        });


        if (holder.getClass() == ReceiverViewHolder.class) {
            ReceiverViewHolder viewHolder = (ReceiverViewHolder) holder;

            // Reset all views to a clean state
            viewHolder.binding.imageReceiver.setVisibility(View.GONE);
            viewHolder.binding.receiverText.setVisibility(View.GONE);
            viewHolder.binding.videoTag.setVisibility(View.GONE);
            viewHolder.binding.downloadVideo.setVisibility(View.GONE);
            viewHolder.binding.youtubeLink.setVisibility(View.GONE);
            viewHolder.binding.audioLayout.setVisibility(View.GONE);
            viewHolder.binding.pdfLayout.setVisibility(View.GONE);

            // ✅ Fetch the latest seen status from receiver's room
            // ✅ Only update tick for the latest message
            DatabaseReference messageRef = FirebaseDatabase.getInstance()
                    .getReference("Chats")
                    .child(receiverRoom)
                    .child("messages")
                    .child(message.getMessageId()); // Track each message's ID

// ✅ Fetch the tick status when loading messages
            messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String delivered = snapshot.child("delivered").getValue(String.class);
                        String seen = snapshot.child("seen").getValue(String.class);
                        // ✅ Set tick status when messages are first loaded
                        if ("true".equals(seen)) {
                            viewHolder.binding.seenIndicator.setImageResource(R.drawable.double_check_sky_color); // Blue double tick
                        } else if ("true".equals(delivered)) {
                            viewHolder.binding.seenIndicator.setImageResource(R.drawable.double_check_mark); // Grey double tick
                        } else {
                            viewHolder.binding.seenIndicator.setImageResource(R.drawable.single_check_mark); // Single tick
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            // Bind data based on the type of message
            if(message.getMessageType().equals("pdf")){
                viewHolder.binding.pdfLayout.setVisibility(View.VISIBLE);
                if(message.getMsgUrl()!=null){
                    viewHolder.binding.pdfName.setText(message.getTitle());
                    viewHolder.binding.pageCount.setText(message.getPageCount()+" pages");
                    viewHolder.binding.pdfSize.setText(formatFileSize(Long.parseLong(message.getFileSize())));

                    viewHolder.binding.pdfLayout.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getMsgUrl()));
                        context.startActivity(intent);
                    });
                }
            }
            else if (message.getMessage().equals("audio")) {
                // Show the audio layout
                viewHolder.binding.audioLayout.setVisibility(View.VISIBLE);

                // Reset UI components to avoid recycled view issues
                viewHolder.binding.audioSeekBar.setProgress(0);
                viewHolder.binding.playAudioButton.setBackgroundResource(R.drawable.ic_play);
                viewHolder.binding.audioDuration.setText("00:00");

                // MediaPlayer management
                MediaPlayer[] mediaPlayer = {null}; // Use an array to avoid lambda scope issues
                Handler handler = new Handler();

                // Play/Pause button logic
                viewHolder.binding.playAudioButton.setOnClickListener(v -> {
                    if (mediaPlayer[0] == null) {
                        // Create a new MediaPlayer instance
                        mediaPlayer[0] = new MediaPlayer();


                        try {
                            mediaPlayer[0].setDataSource(message.getMsgUrl());
                            mediaPlayer[0].prepareAsync(); // Use async preparation
                            mediaPlayer[0].setOnPreparedListener(mp -> {
                                // Enable the play button and set total duration
                                viewHolder.binding.playAudioButton.setEnabled(true);
                                viewHolder.binding.audioSeekBar.setMax(mp.getDuration());

                                // Start playback
                                mp.start();
                                viewHolder.binding.playAudioButton.setBackgroundResource(R.drawable.ic_pause);

                                // Start updating SeekBar
                                startSeekBarUpdater(
                                        viewHolder.binding.audioSeekBar,
                                        viewHolder.binding.audioDuration,
                                        mediaPlayer[0],
                                        handler
                                );
                            });

                            // Handle playback completion
                            mediaPlayer[0].setOnCompletionListener(mp -> {
                                viewHolder.binding.playAudioButton.setBackgroundResource(R.drawable.ic_play);
                                viewHolder.binding.audioSeekBar.setProgress(0);
                                viewHolder.binding.audioDuration.setText("00:00");
                                handler.removeCallbacksAndMessages(null); // Stop SeekBar updates
                            });

                        } catch (IOException e) {
                            e.printStackTrace();
                            viewHolder.binding.playAudioButton.setEnabled(true); // Re-enable on error
                        }
                    } else if (mediaPlayer[0].isPlaying()) {
                        // Pause playback
                        mediaPlayer[0].pause();
                        handler.removeCallbacksAndMessages(null); // Stop SeekBar updates
                        viewHolder.binding.playAudioButton.setBackgroundResource(R.drawable.ic_play);
                    } else {
                        // Resume playback
                        mediaPlayer[0].start();
                        viewHolder.binding.playAudioButton.setBackgroundResource(R.drawable.ic_pause);

                        // Resume updating SeekBar
                        startSeekBarUpdater(
                                viewHolder.binding.audioSeekBar,
                                viewHolder.binding.audioDuration,
                                mediaPlayer[0],
                                handler
                        );
                    }
                });

                // SeekBar change listener
                viewHolder.binding.audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && mediaPlayer[0] != null) {
                            mediaPlayer[0].seekTo(progress);
                            String currentDuration = formatDuration(progress);
                            viewHolder.binding.audioDuration.setText(currentDuration);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                // Reset MediaPlayer when the view is recycled
                viewHolder.itemView.setOnClickListener(null);
            }




            else if (message.getMessage().equals("photo")) {
                viewHolder.binding.imageReceiver.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(message.getMsgUrl())
                        .placeholder(R.drawable.avatar)
                        .into(viewHolder.binding.imageReceiver);
            }
            else if (message.getMessage().equals("video")) {
                viewHolder.binding.videoTag.setVisibility(View.VISIBLE);
                viewHolder.binding.downloadVideo.setVisibility(View.VISIBLE);

                // Set video URI for playback
                Uri videoUri = Uri.parse(message.getMsgUrl());
                viewHolder.binding.videoTag.setVideoURI(videoUri);
                viewHolder.binding.videoTag.setOnPreparedListener(mp -> {
                    mp.setLooping(false); // Video won't loop
                });

                // Play video when clicked
                viewHolder.binding.videoTag.setOnClickListener(v -> {
                    viewHolder.binding.videoTag.start();
                });

                // Handle download button
                viewHolder.binding.downloadVideo.setOnClickListener(v -> {
                    downloadVideo(message.getMsgUrl(), context);
                });
            } else if (message.getMessageType().equals("youtube")) {
                viewHolder.binding.youtubeLink.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(message.getMsgUrl())
                        .placeholder(R.drawable.avatar) // Placeholder image
                        .into(viewHolder.binding.thumbnailImageView);

                // Set the title
                viewHolder.binding.titleTextView.setText(message.getTitle()); // Optionally replace with actual video title if fetched

                // Set the link text and make it clickable
                viewHolder.binding.linkTextView.setText(message.getMessage());
                viewHolder.binding.linkTextView.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getMessage()));
                    intent.setPackage("com.google.android.youtube"); // Open in YouTube app
                    context.startActivity(intent);
                });
            }
            else {
                // For text messages
                if(message.getMessage().equals("This message is removed."))
                    viewHolder.binding.seenIndicator.setVisibility(View.GONE);
                viewHolder.binding.receiverText.setVisibility(View.VISIBLE);
                viewHolder.binding.receiverText.setText(message.getMessage());
            }
        if(message.getFeeling() >= 0) {
                viewHolder.binding.receiverFeeling.setImageResource(reactions[message.getFeeling()]);
                viewHolder.binding.receiverFeeling.setVisibility(View.VISIBLE);
            } else {
                viewHolder.binding.receiverFeeling.setVisibility(View.GONE);
            }

            viewHolder.binding.receiverText.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    boolean isFeelingsEnabled = remoteConfig.getBoolean("isFeelingsEnabled");
                    if(isFeelingsEnabled)
                        popup.onTouch(v, event);
                    return false;
                }
            });

            viewHolder.binding.receiverFeeling.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    popup.onTouch(v, event);
                    return false;
                }
            });
            viewHolder.binding.imageReceiver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, ImageActivity.class);
                    intent.putExtra("userName",recieverName);
                    intent.putExtra("image",message.getMsgUrl());
                    context.startActivity(intent);
                }
            });


            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    TextView text=v.findViewById(R.id.receiver_text);
                    View view = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null);
                    DeleteDialogBinding binding = DeleteDialogBinding.bind(view);
                    if(text.getText().toString().equals("This message is removed."))
                        binding.everyone.setVisibility(View.GONE);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setView(binding.getRoot())
                            .create();
                    binding.everyone.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            message.setMessage("This message is removed.");
                            message.setMessageType("");
                            message.setFeeling(-1);
                            FirebaseDatabase.getInstance().getReference().child("Chats")
                                    .child(receiverRoom).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if(messages.get(messages.size()-1).getMessageId().equals(message.getMessageId()))
                                            {
                                                FirebaseDatabase.getInstance().getReference()
                                                        .child("Chats")
                                                        .child(senderRoom)
                                                        .child("lastMsg").setValue(message.getMessage());

                                                FirebaseDatabase.getInstance().getReference()
                                                        .child("Chats")
                                                        .child(receiverRoom)
                                                        .child("lastMsg").setValue(message.getMessage());
                                            }
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });

                            FirebaseDatabase.getInstance().getReference()
                                    .child("Chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(message);

                            FirebaseDatabase.getInstance().getReference()
                                    .child("Chats")
                                    .child(receiverRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(message);

                            dialog.dismiss();
                        }
                    });

                    binding.delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            FirebaseDatabase.getInstance().getReference()
                                    .child("Chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(message.getMessageId()).setValue(null);
                            if(messages.get(messages.size()-1).getMessageId().equals(message.getMessageId()))
                            {
                                messages.remove(messages.size()-1);
                                if(!messages.isEmpty()) {
                                    if(messages.get(messages.size()-1).getMessageType().equals("youtube"))
                                    {
                                        FirebaseDatabase.getInstance().getReference()
                                                .child("Chats")
                                                .child(senderRoom)
                                                .child("lastMsg").setValue(messages.get(messages.size() - 1).getTitle());
                                    }
                                    else {
                                        FirebaseDatabase.getInstance().getReference()
                                                .child("Chats")
                                                .child(senderRoom)
                                                .child("lastMsg").setValue(messages.get(messages.size() - 1).getMessage());
                                    }
                                    FirebaseDatabase.getInstance().getReference()
                                            .child("Chats")
                                            .child(senderRoom)
                                            .child("lastMsgTime").setValue(messages.get(messages.size()-1).getTimestamp());
                                }
                                else {
                                    FirebaseDatabase.getInstance().getReference()
                                            .child("Chats")
                                            .child(senderRoom).setValue(null);
                                }
                            }
                            else {
                                for(int i=0;i<messages.size();i++)
                                {
                                    if(messages.get(i).getMessageId().equals(message.getMessageId())) {
                                        messages.remove(i);
                                        break;
                                    }
                                }
                            }
                            dialog.dismiss();
                        }
                    });

                    binding.cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dialog.show();

                    return false;
                }
            });
        } else {
            SentViewHolder viewHolder = (SentViewHolder) holder;

            // Reset visibility of views to avoid repeating content due to view recycling
            viewHolder.binding.imageSender.setVisibility(View.GONE);
            viewHolder.binding.senderText.setVisibility(View.GONE);
            viewHolder.binding.videoView.setVisibility(View.GONE);
            viewHolder.binding.downloadVideo.setVisibility(View.GONE);
            viewHolder.binding.youtubeLink.setVisibility(View.GONE);
            viewHolder.binding.audioLayout.setVisibility(View.GONE);
            viewHolder.binding.pdfLayout.setVisibility(View.GONE);
            if(message.getMessageType().equals("pdf")){
                viewHolder.binding.pdfLayout.setVisibility(View.VISIBLE);
                if(message.getMsgUrl()!=null){
                    viewHolder.binding.pdfName.setText(message.getTitle());
                    viewHolder.binding.pageCount.setText(message.getPageCount()+" pages");
                    viewHolder.binding.pdfSize.setText(formatFileSize(Long.parseLong(message.getFileSize())));
                    viewHolder.binding.pdfLayout.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getMsgUrl()));
                        context.startActivity(intent);
                    });
                }
            }
            else if (message.getMessage().equals("audio")) {
                // Show the audio layout
                viewHolder.binding.audioLayout.setVisibility(View.VISIBLE);

                // Reset UI components to avoid recycled view issues
                viewHolder.binding.audioSeekBar.setProgress(0);
                viewHolder.binding.playAudioButton.setBackgroundResource(R.drawable.ic_play);
                viewHolder.binding.audioDuration.setText("00:00");

                // MediaPlayer management
                MediaPlayer[] mediaPlayer = {null}; // Use an array to avoid lambda scope issues
                Handler handler = new Handler();

                // Play/Pause button logic
                viewHolder.binding.playAudioButton.setOnClickListener(v -> {
                    if (mediaPlayer[0] == null) {
                        // Create a new MediaPlayer instance
                        mediaPlayer[0] = new MediaPlayer();

                        // Disable the play button while preparing the media
                        viewHolder.binding.playAudioButton.setEnabled(false);

                        try {
                            mediaPlayer[0].setDataSource(message.getMsgUrl());
                            mediaPlayer[0].prepareAsync(); // Use async preparation
                            mediaPlayer[0].setOnPreparedListener(mp -> {
                                // Enable the play button and set total duration
                                viewHolder.binding.playAudioButton.setEnabled(true);
                                viewHolder.binding.audioSeekBar.setMax(mp.getDuration());

                                // Start playback
                                mp.start();
                                viewHolder.binding.playAudioButton.setBackgroundResource(R.drawable.ic_pause);

                                // Start updating SeekBar
                                startSeekBarUpdater(
                                        viewHolder.binding.audioSeekBar,
                                        viewHolder.binding.audioDuration,
                                        mediaPlayer[0],
                                        handler
                                );
                            });

                            // Handle playback completion
                            mediaPlayer[0].setOnCompletionListener(mp -> {
                                viewHolder.binding.playAudioButton.setBackgroundResource(R.drawable.ic_play);
                                viewHolder.binding.audioSeekBar.setProgress(0);
                                viewHolder.binding.audioDuration.setText("00:00");
                                handler.removeCallbacksAndMessages(null); // Stop SeekBar updates
                            });

                        } catch (IOException e) {
                            e.printStackTrace();
                            viewHolder.binding.playAudioButton.setEnabled(true); // Re-enable on error
                        }
                    } else if (mediaPlayer[0].isPlaying()) {
                        // Pause playback
                        mediaPlayer[0].pause();
                        handler.removeCallbacksAndMessages(null); // Stop SeekBar updates
                        viewHolder.binding.playAudioButton.setBackgroundResource(R.drawable.ic_play);
                    } else {
                        // Resume playback
                        mediaPlayer[0].start();
                        viewHolder.binding.playAudioButton.setBackgroundResource(R.drawable.ic_pause);

                        // Resume updating SeekBar
                        startSeekBarUpdater(
                                viewHolder.binding.audioSeekBar,
                                viewHolder.binding.audioDuration,
                                mediaPlayer[0],
                                handler
                        );
                    }
                });

                // SeekBar change listener
                viewHolder.binding.audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && mediaPlayer[0] != null) {
                            mediaPlayer[0].seekTo(progress);
                            String currentDuration = formatDuration(progress);
                            viewHolder.binding.audioDuration.setText(currentDuration);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

                // Reset MediaPlayer when the view is recycled
                viewHolder.itemView.setOnClickListener(null);
            }


            // Handle image messages
            else if (message.getMessage().equals("photo")) {
                viewHolder.binding.imageSender.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(message.getMsgUrl())
                        .placeholder(R.drawable.avatar)
                        .into(viewHolder.binding.imageSender);

                // Add click listener for viewing the image in full screen
                viewHolder.binding.imageSender.setOnClickListener(view -> {
                    Intent intent = new Intent(context, ImageActivity.class);
                    intent.putExtra("userName", recieverName);
                    intent.putExtra("image", message.getMsgUrl());
                    context.startActivity(intent);
                });
            }

            // Handle video messages
            else if (message.getMessage().equals("video")) {
                //viewHolder.binding.videoView.setVisibility(View.VISIBLE);
              //  viewHolder.binding.downloadVideo.setVisibility(View.VISIBLE);

                // Set video URI for playback
                Uri videoUri = Uri.parse(message.getMsgUrl());
                viewHolder.binding.videoView.setVideoURI(videoUri);

                // Prepare the video for playback
                viewHolder.binding.videoView.setOnPreparedListener(mp -> {
                    mp.setLooping(false); // Video won't loop
                });

                // Play video when clicked
                viewHolder.binding.videoView.setOnClickListener(v -> {
                    viewHolder.binding.videoView.start();
                });

                // Handle the download button click
                viewHolder.binding.downloadVideo.setOnClickListener(v -> {
                    viewHolder.binding.downloadVideo.setVisibility(View.GONE); // Hide download button after clicked
                    downloadVideo(message.getMsgUrl(), context); // Trigger the download logic
                });
            }
            // Handle youtube video link
            else if (message.getMessageType().equals("youtube")) {
                viewHolder.binding.youtubeLink.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(message.getMsgUrl())
                        .placeholder(R.drawable.avatar) // Placeholder image
                        .into(viewHolder.binding.thumbnailImageView);

                // Set the title
                viewHolder.binding.titleTextView.setText(message.getTitle()); // Optionally replace with actual video title if fetched

                // Set the link text and make it clickable
                viewHolder.binding.linkTextView.setText(message.getMessage());
                viewHolder.binding.linkTextView.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.getMessage()));
                    intent.setPackage("com.google.android.youtube"); // Open in YouTube app
                    context.startActivity(intent);
                });
            }
            // Handle text messages
            else{
                viewHolder.binding.senderText.setVisibility(View.VISIBLE);
                viewHolder.binding.senderText.setText(message.getMessage());
            }

            // Handle reactions
            if (message.getFeeling() >= 0) {
                viewHolder.binding.senderFeeling.setImageResource(reactions[message.getFeeling()]);
                viewHolder.binding.senderFeeling.setVisibility(View.VISIBLE);
            } else {
                viewHolder.binding.senderFeeling.setVisibility(View.GONE);
            }

            viewHolder.binding.senderText.setOnTouchListener((v, event) -> {
                popup.onTouch(v, event);
                return false;
            });
            viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    View v = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null);
                    DeleteDialogBinding binding = DeleteDialogBinding.bind(view);
                    binding.everyone.setVisibility(View.GONE);
                    AlertDialog dialog = new AlertDialog.Builder(context)
                            .setTitle("Delete Message")
                            .setView(binding.getRoot())
                            .create();

                    dialog.show();
                    return false;
                }
            });
            // Add reaction support for images (long click)
            // Long click listener for reaction popup
            viewHolder.binding.imageSender.setOnLongClickListener(view -> {
                MotionEvent motionEvent = MotionEvent.obtain(
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        MotionEvent.ACTION_DOWN,
                        view.getX(),
                        view.getY(),
                        0
                );

                popup.onTouch(view, motionEvent);
                motionEvent.recycle(); // Clean up MotionEvent
                return true; // Indicate the event was handled
            });

            // Long click listener for deleting messages
            viewHolder.itemView.setOnLongClickListener(v -> {
                View dialogView = LayoutInflater.from(context).inflate(R.layout.delete_dialog, null);
                DeleteDialogBinding binding = DeleteDialogBinding.bind(dialogView);
                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle("Delete Message")
                        .setView(binding.getRoot())
                        .create();
                binding.everyone.setVisibility(View.GONE);
                binding.delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FirebaseDatabase.getInstance().getReference()
                                .child("Chats")
                                .child(senderRoom)
                                .child("messages")
                                .child(message.getMessageId()).setValue(null);
                        if(messages.get(messages.size()-1).getMessageId().equals(message.getMessageId()))
                        {
                            messages.remove(messages.size()-1);
                            if(!messages.isEmpty()) {
                                if(messages.get(messages.size()-1).getMessageType().equals("youtube"))
                                {
                                    FirebaseDatabase.getInstance().getReference()
                                            .child("Chats")
                                            .child(senderRoom)
                                            .child("lastMsg").setValue(messages.get(messages.size() - 1).getTitle());
                                    FirebaseDatabase.getInstance().getReference()
                                            .child("Chats")
                                            .child(senderRoom)
                                            .child("lastMsgTime").setValue(messages.get(messages.size() - 1).getTimestamp());
                                }
                                else {
                                    FirebaseDatabase.getInstance().getReference()
                                            .child("Chats")
                                            .child(senderRoom)
                                            .child("lastMsg").setValue(messages.get(messages.size() - 1).getMessage());
                                    FirebaseDatabase.getInstance().getReference()
                                            .child("Chats")
                                            .child(senderRoom)
                                            .child("lastMsgTime").setValue(messages.get(messages.size() - 1).getTimestamp());
                                }
                            }
                            else {
                                FirebaseDatabase.getInstance().getReference()
                                        .child("Chats")
                                        .child(senderRoom)
                                        .child("lastMsg").setValue("");
                                FirebaseDatabase.getInstance().getReference()
                                        .child("Chats")
                                        .child(senderRoom)
                                        .child("lastMsgTime").setValue("");
                            }
                        }
                        else {
                            for(int i=0;i<messages.size();i++)
                            {
                                if(messages.get(i).getMessageId().equals(message.getMessageId())) {
                                    messages.remove(i);
                                    break;
                                }
                            }
                        }
                        dialog.dismiss();
                    }
                });
                binding.cancel.setOnClickListener(v1 -> dialog.dismiss());
                dialog.show();

                return false;
            });
        }
    }
    private void downloadVideo(String videoUrl, Context context) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(videoUrl);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ChatAppVideo_" + System.currentTimeMillis() + ".mp4");
        request.setTitle("Downloading Video");
        request.setDescription("Your video is being downloaded...");

        // Enqueue the download request
        if (downloadManager != null) {
            downloadManager.enqueue(request);
            Toast.makeText(context, "Downloading video...", Toast.LENGTH_SHORT).show();
        }
    }
    private String formatDuration(int durationMs) {
        int minutes = durationMs / 60000;
        int seconds = (durationMs % 60000) / 1000;
        return String.format("%02d:%02d", minutes, seconds);
    }
    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";  // Bytes
        } else if (sizeInBytes < 1024 * 1024) {
            return (sizeInBytes / 1024) + " KB";  // Kilobytes
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024));  // Megabytes
        } else {
            return String.format("%.2f GB", sizeInBytes / (1024.0 * 1024 * 1024));  // Gigabytes
        }
    }



    // Updated method to accept required views instead of ViewHolder
    private void startSeekBarUpdater(SeekBar audioSeekBar, TextView audioDuration, MediaPlayer mediaPlayer, Handler handler) {
        Runnable updateSeekBarTask = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    audioSeekBar.setProgress(currentPosition);

                    // Update the current playback duration
                    audioDuration.setText(formatDuration(currentPosition));

                    handler.postDelayed(this, 1000); // Update every second
                }
            }
        };
        handler.post(updateSeekBarTask);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
    public class SentViewHolder extends RecyclerView.ViewHolder {

        SampleSenderBinding binding;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            binding =  SampleSenderBinding.bind(itemView);
        }
    }
    public class ReceiverViewHolder extends RecyclerView.ViewHolder {

        SampleRecieverBinding binding;

        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            binding =  SampleRecieverBinding.bind(itemView);
        }
    }
}