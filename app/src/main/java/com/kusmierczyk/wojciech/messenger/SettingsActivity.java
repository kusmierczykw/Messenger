package com.kusmierczyk.wojciech.messenger;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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

import java.io.File;
import java.util.UUID;

import jp.wasabeef.glide.transformations.CropCircleTransformation;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import top.zibin.luban.Luban;

/**
 * Created by wojciech on 03.07.2017.
 */

public class SettingsActivity extends MainActivity {
    private final String TAG = "SettingsActivity";
    private static final int GALLERY = 2;

    //Gallery Runtime Permissions
    private boolean permissionToReadAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private String [] permissions = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};


    private ImageButton uploadAvatarButton;
    private ProgressDialog mUploadProgress;

    private StorageReference mStorageReference;
    private DatabaseReference mCurrentUserDatabaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Check Permissions at runtime
        int requestCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode);
        }

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

            final Uri uri = data.getData();
            final String imageLocation = "Photos/profile_picture/" + encryptEmail(mUser.getEmail());
            final String uniqueId = UUID.randomUUID().toString();
            final StorageReference userAvatarPath = mStorageReference.child(imageLocation).child(uniqueId + "/profile_pic");
            final String downloadURL = userAvatarPath.getPath();

            Luban.get(this).load(new File(getPath(uri))).putGear(Luban.THIRD_GEAR).asObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    })
                    .onErrorResumeNext(new Func1<Throwable, Observable<? extends File>>() {
                        @Override
                        public Observable<? extends File> call(Throwable throwable) {
                            return Observable.empty();
                        }
                    })
                    .subscribe(new Action1<File>() {
                        @Override
                        public void call(File file) {
                            Log.e(TAG, file.getPath());
                            userAvatarPath.putFile(Uri.fromFile(file)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    addImageToProfile(downloadURL);
                                    mUploadProgress.dismiss();
                                }
                            });
                            // TODO called when compression finishes successfully, provides compressed image
                        }
                    });



        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String s=cursor.getString(column_index);
        cursor.close();
        return s;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 200:
                permissionToReadAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteAccepted  = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToReadAccepted ) this.finish();
        if (!permissionToWriteAccepted ) this.finish();
    }

    private void initialization() {
        mStorageReference = FirebaseStorage.getInstance().getReference();
        mCurrentUserDatabaseReference = mDatabase.getReference().child(Constants.USERS_LOCATION + "/" +encryptEmail(mAuth.getCurrentUser().getEmail()));
    }
}
