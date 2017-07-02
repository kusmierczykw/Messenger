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
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.kusmierczyk.wojciech.messenger.model.Constants;
import com.kusmierczyk.wojciech.messenger.model.User;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by wojciech on 02.07.2017.
 */

public class FriendsListActivity extends MainActivity {
    private ListView friendsListView;
    private FirebaseListAdapter mFriendListAdapter;
    private DatabaseReference mFriendsDatabaseReference;

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


                startActivity(new Intent(view.getContext(), ConversationMessagesActivity.class));
            }
        });
    }

    private void initialization(){
        friendsListView = (ListView) findViewById(R.id.activity_friends_find_result);
        mFriendsDatabaseReference = mDatabase.getReference().child(Constants.FRIENDS_LOCATION).child(encryptEmail(mUser.getEmail()));
    }
}
