package com.kusmierczyk.wojciech.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.kusmierczyk.wojciech.messenger.model.Constants;
import com.kusmierczyk.wojciech.messenger.model.Friend;
import com.kusmierczyk.wojciech.messenger.model.User;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by wojciech on 29.06.2017.
 */

public class FriendsFindActivity extends MainActivity{
    private static final String TAG = "FriendsFindActivity";

    private ListView friendsListView;
    private FirebaseListAdapter mFriendListAdapter;
    private DatabaseReference mUsersDatabaseReference;
    private ValueEventListener mValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        initialization();
        showUsersList();
    }

    private void showUsersList(){
        mFriendListAdapter = new FirebaseListAdapter<User>(this, User.class, R.layout.user_item, mUsersDatabaseReference) {
            @Override
            protected void populateView(final View v, final User model, int position) {
                //Reset of avatar after buffering image
                ((ImageView) v.findViewById(R.id.user_item_user_avatar)).setImageResource(R.drawable.user);

                ((ImageView) v.findViewById(R.id.user_item_status)).setVisibility(View.GONE);

                final DatabaseReference mFriendsDatabaseReference = mDatabase.getReference().child(Constants.FRIENDS_LOCATION).child(encryptEmail(mUser.getEmail()));
                mFriendsDatabaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child(encryptEmail(model.getEmail())).getValue() != null){
                            v.findViewById(R.id.user_item_remove_button).setVisibility(View.VISIBLE);
                            v.findViewById(R.id.user_item_add_button).setVisibility(View.GONE);
                        }else {
                            v.findViewById(R.id.user_item_remove_button).setVisibility(View.GONE);
                            v.findViewById(R.id.user_item_add_button).setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });

                if(model.getAvatarURL() != null && model.getAvatarURL().length() > 0){
                    StorageReference mAvatarReference = FirebaseStorage.getInstance().getReference().child(model.getAvatarURL());
                    Glide.with(v.getContext()).using(new FirebaseImageLoader()).load(mAvatarReference).bitmapTransform(new CropCircleTransformation(v.getContext())).into((ImageView) v.findViewById(R.id.user_item_user_avatar));
                }
                ((TextView)v.findViewById(R.id.user_item_username)).setText(model.getUsername());
                ((TextView)v.findViewById(R.id.user_item_email)).setText(model.getEmail());

                (v.findViewById(R.id.user_item_remove_button)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Remove user: " + model.getEmail());
                        removeFriend(model.getEmail());
                    }
                });

                (v.findViewById(R.id.user_item_add_button)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Add user: " + model.getEmail());
                        addFriend(model);
                    }
                });
            }
        };

        friendsListView.setAdapter(mFriendListAdapter);
        mValueEventListener = mUsersDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if(user == null){
                    finish();
                    return;
                }
                mFriendListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void removeFriend(String friendEmail){
        final String currentUser = mAuth.getCurrentUser().getEmail();
        final DatabaseReference friendReference = mDatabase.getReference(Constants.FRIENDS_LOCATION + "/" + encryptEmail(currentUser));
        friendReference.child(encryptEmail(friendEmail)).removeValue();
    }

    private void addFriend(User user){
        final String currentUser = mAuth.getCurrentUser().getEmail();
        Friend friend = new Friend(user.getEmail(), user.getUsername());
        final DatabaseReference friendReference = mDatabase.getReference(Constants.FRIENDS_LOCATION + "/" + encryptEmail(currentUser));
        friendReference.child(encryptEmail(user.getEmail())).setValue(friend);
    }

    private void initialization(){
        friendsListView = (ListView) findViewById(R.id.activity_friends_find_result);
        mUsersDatabaseReference = mDatabase.getReference().child(Constants.USERS_LOCATION);
    }
}
