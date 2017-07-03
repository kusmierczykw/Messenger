package com.kusmierczyk.wojciech.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.kusmierczyk.wojciech.messenger.model.Constants;
import com.kusmierczyk.wojciech.messenger.model.Message;
import com.kusmierczyk.wojciech.messenger.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by wojciech on 29.06.2017.
 */

public class ConversationMessagesActivity extends MainActivity{
    private final String TAG = "ConversationMessagesActivity";

    private String messageID;
    private String conversationName;

    private ListView mMessagesList;
    private EditText mMessageToSendField;
    private ImageButton mSendButton;

    private DatabaseReference mUsersDatabaseReference;
    private DatabaseReference mMessagesDatabaseReference;

    private FirebaseListAdapter<Message> mMessagesListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_messages);

        Intent intent = this.getIntent();

        messageID = intent.getStringExtra(Constants.MESSAGE_ID);
        conversationName = intent.getStringExtra(Constants.CONVERSATION_NAME);

        if(messageID == null)
            finish();

        initialization();
        showMessages();
        addListeners();
    }

    private void initialization() {
        mMessagesList = (ListView) findViewById(R.id.activity_conversation_messages_messages_list);
        mMessageToSendField = (EditText) findViewById(R.id.activity_conversation_messages_message_field);
        mSendButton = (ImageButton) findViewById(R.id.activity_conversation_messages_send_button);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        mUsersDatabaseReference = mDatabase.getReference().child(Constants.USERS_LOCATION);
        mMessagesDatabaseReference = mDatabase.getReference().child(Constants.MESSAGES_LOCATION + "/" + messageID);

        setTitle(conversationName);
    }

    public void addListeners(){
        mMessageToSendField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void sendMessage(){
        final DatabaseReference pushRef = mMessagesDatabaseReference.push();
        final String pushKey = pushRef.getKey();

        String messageString = mMessageToSendField.getText().toString();

        //Block before send empty message
        if(!messageString.equals("") && !messageString.equals(" ")) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy, HH:mm");
            Date date = new Date();
            String timestamp = dateFormat.format(date);

            //TODO Check if is the other solution of this problem
            Message message = new Message(encryptEmail(mAuth.getCurrentUser().getEmail()), messageString, timestamp);
            HashMap<String, Object> messageItemMap = new HashMap<>();
            HashMap<String, Object> messageObj = (HashMap<String, Object>) new ObjectMapper().convertValue(message, Map.class);
            messageItemMap.put("/" + pushKey, messageObj);
            mMessagesDatabaseReference.updateChildren(messageItemMap).addOnCompleteListener(this, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    mMessageToSendField.setText("");
                }
            });
        }
    }

    private void showMessages(){
        mMessagesListAdapter = new FirebaseListAdapter<Message>(this, Message.class, R.layout.message_item, mMessagesDatabaseReference) {
            @Override
            protected void populateView(final View v, Message model, int position) {
                LinearLayout messageItem = v.findViewById(R.id.message_item);
                TextView message = v.findViewById(R.id.message_item_message);
                final TextView sendTime = v.findViewById(R.id.message_item_send_time);
                LinearLayout messageCloud = v.findViewById(R.id.message_item_message_cloud);

                final ImageView friendAvatar = v.findViewById(R.id.message_item_friend_avatar);

                message.setText(model.getMessage());
                sendTime.setText(model.getTimeStamp());

                String messageSender = encryptEmail(model.getSender());

                //If the user of app then message item do right else the message was sent from friend
                if (encryptEmail(messageSender).equals(encryptEmail(mUser.getEmail()))) {
                    messageItem.setGravity(Gravity.RIGHT);
                    friendAvatar.setVisibility(View.GONE);
                    messageCloud.setBackgroundResource(R.drawable.message_cloud_user);
                } else {
                    messageItem.setGravity(Gravity.LEFT);
                    friendAvatar.setVisibility(View.VISIBLE);
                    messageCloud.setBackgroundResource(R.drawable.message_cloud_friend);

                    mUsersDatabaseReference.child(messageSender).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            try {
                                User user = dataSnapshot.getValue(User.class);
                                if (user != null && user.getAvatarURL() != null) {
                                    StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(user.getAvatarURL());
                                    Glide.with(v.getContext()).using(new FirebaseImageLoader()).load(storageReference).bitmapTransform(new CropCircleTransformation(v.getContext())).into(friendAvatar);
                                }
                            }catch (Exception e){
                                Log.e("Err", e.toString());
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
                }
            }
        };
        mMessagesList.setAdapter(mMessagesListAdapter);
    }
}
