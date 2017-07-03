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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.kusmierczyk.wojciech.messenger.model.Constants;
import com.kusmierczyk.wojciech.messenger.model.Conversation;
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
        mFriendListAdapter = new FirebaseListAdapter<User>(this, User.class, R.layout.user_item, mFriendsDatabaseReference) {
            @Override
            protected void populateView(final View v, final User model, int position) {
                if(model.getAvatarURL() != null && model.getAvatarURL().length() > 0){
                    StorageReference mAvatarReference = FirebaseStorage.getInstance().getReference().child(model.getAvatarURL());
                    Glide.with(v.getContext()).using(new FirebaseImageLoader()).load(mAvatarReference).bitmapTransform(new CropCircleTransformation(v.getContext())).into((ImageView) findViewById(R.id.user_item_user_avatar));
                }
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

                conversation.setChatCreator(chatCreator);
                conversation.setUser(((User)(adapterView.getItemAtPosition(position))));

                DatabaseReference conversationReference = mDatabase.getReference(Constants.CONVERSATIONS_LOCATION);
                DatabaseReference pushReference = conversationReference.push();
                String pushKey = pushReference.getKey();

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

                Intent intent = new Intent(view.getContext(), ConversationMessagesActivity.class);
                intent.putExtra(Constants.MESSAGE_ID, pushKey);
                intent.putExtra(Constants.CONVERSATION_NAME, conversation.getUser().getUsername());

                startActivity(intent);
            }
        });
    }

    private void initialization(){
        friendsListView = (ListView) findViewById(R.id.activity_friends_find_result);
        mFriendsDatabaseReference = mDatabase.getReference().child(Constants.FRIENDS_LOCATION).child(encryptEmail(mUser.getEmail()));
        mCurrentUserDatabaseReference = mDatabase.getReference().child(Constants.USERS_LOCATION + "/" + encryptEmail(mAuth.getCurrentUser().getEmail()));
    }
}
