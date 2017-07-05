package com.kusmierczyk.wojciech.messenger;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.kusmierczyk.wojciech.messenger.model.Constants;

import java.util.HashMap;

/**
 * Created by wojciech on 28.06.2017.
 */

public class MainActivity extends AppCompatActivity {

    /** Firebase **/
    protected FirebaseAuth mAuth;
    protected FirebaseUser mUser;
    protected FirebaseDatabase mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance();
    }


    @VisibleForTesting
    public ProgressDialog mProgressDialog;

    public void showProgressDialog(@StringRes int stringRes) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(stringRes));
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
    }

    protected void updateUserStatus(boolean isOnline){
        DatabaseReference mStatusReference = mDatabase.getReference().child(Constants.USERS_LOCATION + "/" + encryptEmail(mUser.getEmail()));

        HashMap<String, Object> status = new HashMap<>();
        status.put("status", isOnline);

        mStatusReference.updateChildren(status);

    }

    protected String encryptEmail(String email){
        return email.replace(".", "::");
    }
}
