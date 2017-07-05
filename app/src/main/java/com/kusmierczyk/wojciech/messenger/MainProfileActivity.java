package com.kusmierczyk.wojciech.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.util.Pair;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.kusmierczyk.wojciech.messenger.model.Constants;
import com.kusmierczyk.wojciech.messenger.model.Conversation;
import com.kusmierczyk.wojciech.messenger.model.Message;

import java.util.HashMap;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by wojciech on 29.06.2017.
 */

public class MainProfileActivity extends MainActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainProfileActivity";

    /** Firebase **/
    private DatabaseReference mConversationDatabaseReference;
    private DatabaseReference mSenderDatabaseReference;
    private DatabaseReference mReceiverDatabaseReference;

    private FirebaseListAdapter mConversationAdapter;

    private ListView mConversationsListView;
    private ValueEventListener mValueEventListener;
    private DatabaseReference mStatusReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_profile);

        syncProfile();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton writeMessageButton = (FloatingActionButton) findViewById(R.id.write_message_button);
        writeMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), FriendsListActivity.class);
                startActivity(intent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

//        //Access to the Header
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerMainProfile = navigationView.getHeaderView(0);
        updateProfileHeader(headerMainProfile);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;

        switch(id){
            case R.id.nav_friends:
                intent = new Intent(this, FriendsFindActivity.class);
                startActivity(intent);
                break;

            case R.id.nav_create_conversation:
                intent = new Intent(this, FriendsListActivity.class);
                startActivity(intent);
                break;

            case R.id.nav_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;

            case R.id.nav_log_out:
                updateUserStatus(false);
                mAuth.signOut();
                Toast.makeText(MainProfileActivity.this, getString(R.string.signed_out_successfully), Toast.LENGTH_SHORT).show();
                Intent newIntent = new Intent(MainProfileActivity.this, SignInActivity.class);
                startActivity(newIntent);
                finish();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateProfileHeader(final View header){
        TextView username = header.findViewById(R.id.nav_username);
        TextView userEmail = header.findViewById(R.id.nav_user_email);
        username.setText(mUser.getDisplayName());
        userEmail.setText(mUser.getEmail());

        final ImageView profilePhoto = (ImageView) header.findViewById(R.id.imageView);

        mReceiverDatabaseReference.child(encryptEmail(mUser.getEmail())).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String avatarURL = dataSnapshot.child("avatarURL").getValue().toString();
                if(avatarURL.length() > 0) {
                    try {
                        StorageReference storageReferenceLastSender = FirebaseStorage.getInstance().getReference().child(avatarURL);
                        Glide.with(header.getContext()).using(new FirebaseImageLoader()).load(storageReferenceLastSender).bitmapTransform(new CropCircleTransformation(header.getContext())).into(profilePhoto);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

    }

    private void syncProfile(){
        updateUserStatus(true);

        Log.d(TAG, "SyncProfile:start");
        showProgressDialog(R.string.sync_process);
        mConversationDatabaseReference = mDatabase.getReference().child(Constants.USERS_LOCATION + "/"+ encryptEmail(mUser.getEmail()) + "/"+Constants.CONVERSATIONS_LOCATION);
        mSenderDatabaseReference = mDatabase.getReference().child(Constants.USERS_LOCATION);
        mReceiverDatabaseReference = mDatabase.getReference().child(Constants.USERS_LOCATION);

        mConversationsListView = (ListView) findViewById(R.id.conversationsListView);
        mConversationAdapter = new FirebaseListAdapter<Conversation>(this, Conversation.class, R.layout.conversation_item, mConversationDatabaseReference) {
            @Override
            protected void populateView(final View v, final Conversation model, int position) {
                final DatabaseReference mMessageReference = mDatabase.getReference(Constants.MESSAGES_LOCATION + "/" + model.getConversationID());

                final TextView lastMessage = v.findViewById(R.id.conversation_item_last_message);
                final ImageView senderImageView = v.findViewById(R.id.conversation_item_last_sender_avatar);
                final ImageView receiverImageView = v.findViewById(R.id.conversation_item_user_avatar);

                senderImageView.setImageResource(R.drawable.user);
                receiverImageView.setImageResource(R.drawable.user);

                //Change of conversation name
                if(model.getChatCreator().getEmail().equals(mUser.getEmail())){
                    ((TextView) v.findViewById(R.id.conversation_item_username)).setText(model.getUser().getUsername());

                }else{
                    ((TextView) v.findViewById(R.id.conversation_item_username)).setText(model.getChatCreator().getUsername());
                }

                mMessageReference.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Message newMessage = dataSnapshot.getValue(Message.class);
                        lastMessage.setText(newMessage.getMessage());

                        Pair<String, String> whoIsWho = determineWhoIsWho(newMessage.getSender(), model);
                        String sender = whoIsWho.first;
                        String receiver = whoIsWho.second;

                        //Reset of avatar after buffering image
                        senderImageView.setImageResource(R.drawable.user);
                        //Get avatar for sender
                        mSenderDatabaseReference.child(sender).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String avatarURL = dataSnapshot.child("avatarURL").getValue().toString();
                                if(avatarURL.length() > 0) {
                                    try {
                                        StorageReference storageReferenceLastSender = FirebaseStorage.getInstance().getReference().child(avatarURL);
                                        Glide.with(v.getContext()).using(new FirebaseImageLoader()).load(storageReferenceLastSender).bitmapTransform(new CropCircleTransformation(v.getContext())).into(senderImageView);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });

                        //Reset of avatar after buffering image
                        receiverImageView.setImageResource(R.drawable.user);
                        //Get avatar for receiver
                        mReceiverDatabaseReference.child(receiver).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String avatarURL = dataSnapshot.child("avatarURL").getValue().toString();
                                if(avatarURL.length() > 0) {
                                    try {
                                        StorageReference storageReferenceLastSender = FirebaseStorage.getInstance().getReference().child(avatarURL);
                                        Glide.with(v.getContext()).using(new FirebaseImageLoader()).load(storageReferenceLastSender).bitmapTransform(new CropCircleTransformation(v.getContext())).into(receiverImageView);
                                    }catch(Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
                    }
                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {}
                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
                hideProgressDialog();
            }
        };

        mConversationsListView.setAdapter(mConversationAdapter);
        mConversationsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String messageLocation = mConversationAdapter.getRef(i).toString();
                if(messageLocation != null){
                    Intent intent = new Intent(view.getContext(), ConversationMessagesActivity.class);
                    String messageKey = mConversationAdapter.getRef(i).getKey();
                    intent.putExtra(Constants.MESSAGE_ID, messageKey);
                    Conversation conversation = (Conversation) mConversationAdapter.getItem(i);

                    if(conversation.getChatCreator().getEmail().equals(encryptEmail(mUser.getEmail()))){
                        intent.putExtra(Constants.CONVERSATION_NAME, conversation.getUser().getUsername());
                    }else{
                        intent.putExtra(Constants.CONVERSATION_NAME, conversation.getChatCreator().getUsername());
                    }
                    startActivity(intent);
                }
            }
        });

        mValueEventListener = mConversationDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Conversation conversation = dataSnapshot.getValue(Conversation.class);
                if (conversation == null) {
                    hideProgressDialog();
                    return;
                }
                mConversationAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
        Log.d(TAG, "SyncProfile:finish");
    }

    private void updateUserStatus(boolean isOnline){
        mStatusReference = mDatabase.getReference().child(Constants.USERS_LOCATION + "/" + encryptEmail(mUser.getEmail()));

        HashMap<String, Object> status = new HashMap<>();
        status.put("status", isOnline);

        mStatusReference.updateChildren(status);

    }

    private Pair<String, String> determineWhoIsWho(String messageSender, Conversation conversation){
        String email = encryptEmail(conversation.getChatCreator().getEmail());
        if(messageSender.equals(email)){
            return new Pair<>(messageSender, encryptEmail(conversation.getUser().getEmail()));
        }else{
            return new Pair<>(messageSender, email);
        }
    }

}
