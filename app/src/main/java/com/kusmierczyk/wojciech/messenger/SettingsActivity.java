package com.kusmierczyk.wojciech.messenger;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.kusmierczyk.wojciech.messenger.model.Constants;
import com.kusmierczyk.wojciech.messenger.model.User;

import java.util.UUID;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by wojciech on 03.07.2017.
 */

public class SettingsActivity extends MainActivity {
    private final String TAG = "SettingsActivity";
    private static final int GALLERY = 2;

    private ImageButton uploadAvatarButton;
    private ProgressDialog mUploadProgress;

    private StorageReference mStorageReference;
    private DatabaseReference mCurrentUserDatabaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initialization();
        openGallery();
        updateUserInfo();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY && resultCode == RESULT_OK){
            mUploadProgress.setMessage(getString(R.string.uploading));
            mUploadProgress.show();

            Uri uri = data.getData();
            final String imageLocation = "Photos/profile_picture/" + encryptEmail(mUser.getEmail());
            final String uniqueId = UUID.randomUUID().toString();
            final StorageReference userAvatarPath = mStorageReference.child(imageLocation).child(uniqueId + "/profile_pic");
            final String downloadURL = userAvatarPath.getPath();
            userAvatarPath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    addImageToProfile(downloadURL);
                    mUploadProgress.dismiss();
                }
            });
        }
    }

    public void addImageToProfile(final String imageLocation){
        final ImageView imageView = (ImageView) findViewById(R.id.activity_settings_user_avatar);
        mCurrentUserDatabaseReference.child("avatarURL").setValue(imageLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                try{
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(imageLocation);
                    Glide.with(SettingsActivity.this).using(new FirebaseImageLoader()).load(storageRef).bitmapTransform(new CropCircleTransformation(SettingsActivity.this)).diskCacheStrategy(DiskCacheStrategy.RESULT).into(imageView);
                }catch (Exception e){
                    Log.e("Err", e.toString());
                }
            }
        });
    }

    private void openGallery(){
        uploadAvatarButton = (ImageButton) findViewById(R.id.activity_settings_upload_button);
        mUploadProgress = new ProgressDialog(this);
        uploadAvatarButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, GALLERY);
            }
        });
    }

    private void updateUserInfo(){
        final ImageView imageView = (ImageView) findViewById(R.id.activity_settings_user_avatar);
        mCurrentUserDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    User user = dataSnapshot.getValue(User.class);
                    if (user.getAvatarURL() != null) {
                        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(user.getAvatarURL());
                        Glide.with(SettingsActivity.this).using(new FirebaseImageLoader()).load(storageReference).bitmapTransform(new CropCircleTransformation(SettingsActivity.this)).into(imageView);
                    }
                }catch (Exception e){
                    Log.e("Err", e.toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void initialization() {
        mStorageReference = FirebaseStorage.getInstance().getReference();
        mCurrentUserDatabaseReference = mDatabase.getReference().child(Constants.USERS_LOCATION + "/" +encryptEmail(mAuth.getCurrentUser().getEmail()));
    }
}
