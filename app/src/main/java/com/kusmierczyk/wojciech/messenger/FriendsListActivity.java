package com.kusmierczyk.wojciech.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.kusmierczyk.wojciech.messenger.model.Constants;
import com.kusmierczyk.wojciech.messenger.model.Conversation;
import com.kusmierczyk.wojciech.messenger.model.Friend;
import com.kusmierczyk.wojciech.messenger.model.User;

import java.util.HashMap;
import java.util.Map;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by wojciech on 02.07.2017.
 */

public class FriendsListActivity extends MainActivity {
    private final String TAG = "FriendsListActivity";

    private ListView friendsListView;
    private FirebaseListAdapter mFriendListAdapter;
    private DatabaseReference mFriendsDatabaseReference;

    private DatabaseReference mCurrentUserDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        initialization();
        showUsersList();

        setTitle(getString(R.string.friends_list));
    }

    private void showUsersList(){
        mFriendListAdapter = new FirebaseListAdapter<Friend>(this, Friend.class, R.layout.user_item, mFriendsDatabaseReference) {
            @Override
            protected void populateView(final View v, final Friend model, int position) {
                //Reset of avatar after buffering image
                ((ImageView) v.findViewById(R.id.user_item_user_avatar)).setImageResource(R.drawable.user);

                DatabaseReference userReference = mDatabase.getReference(Constants.USERS_LOCATION + "/" + encryptEmail(model.getEmail()));

                userReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String avatarURL = dataSnapshot.child("avatarURL").getValue().toString();
                        if(avatarURL != null && avatarURL.length() > 0){
                            StorageReference mAvatarReference = FirebaseStorage.getInstance().getReference().child(avatarURL);
                            Glide.with(v.getContext()).using(new FirebaseImageLoader()).load(mAvatarReference).bitmapTransform(new CropCircleTransformation(v.getContext())).into((ImageView) v.findViewById(R.id.user_item_user_avatar));
                        }


                        ImageView status = ((ImageView) v.findViewById(R.id.user_item_status));

                        try {
                            boolean userStatus = (Boolean) dataSnapshot.child("status").getValue();
                            if (!userStatus){
                                status.setVisibility(View.GONE);
                                status.setImageResource(R.drawable.presence_offline);
                                status.setVisibility(View.VISIBLE);
                            }else{
                                status.setVisibility(View.GONE);
                                status.setImageResource(R.drawable.presence_online);
                                status.setVisibility(View.VISIBLE);
                            }
                        }catch (NullPointerException e){
                            status.setVisibility(View.GONE);
                            status.setImageResource(R.drawable.presence_offline);
                            status.setVisibility(View.VISIBLE);
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
                ((TextView)v.findViewById(R.id.user_item_username)).setText(model.getUsername());
                ((TextView)v.findViewById(R.id.user_item_email)).setText(model.getEmail());
            }
        };
        friendsListView.setAdapter(mFriendListAdapter);
        friendsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                //Creating new conversation between current user and his friend
                Log.e(TAG, position+" clicked");
                Conversation conversation = new Conversation();
                User chatCreator = new User(mUser.getDisplayName(), mUser.getEmail(), null);
                Friend user = ((Friend) (adapterView.getItemAtPosition(position)));

                conversation.setChatCreator(chatCreator);
                conversation.setUser(new User(user.getUsername(), user.getEmail(), null));

                createOfConversation(conversation, view);
            }
        });
    }

    private void createOfConversation(final Conversation conversation, final View view){
        DatabaseReference mConversationReference = mDatabase.getReference(Constants.CONVERSATIONS_LOCATION);

        mConversationReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean flag = false;
                String pushKey = null;

                for(DataSnapshot concreteConversation : dataSnapshot.getChildren()){
                    Conversation conv = concreteConversation.getValue(Conversation.class);

                    if(conversation.equalsTo(conv)){
                        pushKey = conv.getConversationID();
                        flag = true;
                        break;
                    }
                }

                if(!flag){
                    DatabaseReference conversationReference = mDatabase.getReference(Constants.CONVERSATIONS_LOCATION);
                    DatabaseReference pushReference = conversationReference.push();
                    pushKey = pushReference.getKey();

                    conversation.setConversationID(pushKey);
                    HashMap<String, Object> conversationItemMap = new HashMap<>();
                    HashMap<String, Object> conversationObject = (HashMap<String, Object>) new ObjectMapper().convertValue(conversation, Map.class);

                    conversationItemMap.put("/"+pushKey, conversationObject);
                    conversationReference.updateChildren(conversationItemMap);

                    conversationItemMap = new HashMap<>();
                    conversationItemMap.put("/"+Constants.CONVERSATIONS_LOCATION + "/" + pushKey, conversationObject);
                    mCurrentUserDatabaseReference.updateChildren(conversationItemMap);

                    mFriendsDatabaseReference = mDatabase.getReference().child(Constants.USERS_LOCATION + "/" + encryptEmail(conversation.getUser().getEmail()));
                    mFriendsDatabaseReference.updateChildren(conversationItemMap);

                    conversationItemMap = new HashMap<>();
                    conversationItemMap.put("/chats/" + pushKey, conversationObject);
                }

                Intent intent = new Intent(view.getContext(), ConversationMessagesActivity.class);
                intent.putExtra(Constants.MESSAGE_ID, pushKey);
                intent.putExtra(Constants.CONVERSATION_NAME, conversation.getUser().getUsername());
                startActivity(intent);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void initialization(){
        friendsListView = (ListView) findViewById(R.id.activity_friends_find_result);
        mFriendsDatabaseReference = mDatabase.getReference().child(Constants.FRIENDS_LOCATION).child(encryptEmail(mUser.getEmail()));
        mCurrentUserDatabaseReference = mDatabase.getReference().child(Constants.USERS_LOCATION + "/" + encryptEmail(mAuth.getCurrentUser().getEmail()));
    }
}
