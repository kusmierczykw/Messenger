package com.kusmierczyk.wojciech.messenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.kusmierczyk.wojciech.messenger.model.Constants;
import com.kusmierczyk.wojciech.messenger.model.User;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wojciech on 29.06.2017.
 */

public class SignUpActivity extends MainActivity {
    private static final String TAG = "SignUpActivity";

    /** Activity elements **/
    private EditText mEmailField;
    private EditText mPasswordField;
    private EditText mRepeatPasswordField;
    private EditText mUsernameField;
    private Button mSignInButton;
    private Button mSignUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        initialization();
    }

    protected void initialization(){
        mSignInButton = (Button) findViewById(R.id.activity_sign_up_sign_in_button);
        mSignInButton.setOnClickListener(listener);
        mSignUpButton = (Button) findViewById(R.id.activity_sign_up_sign_up_button);
        mSignUpButton.setOnClickListener(listener);

        mUsernameField = (EditText) findViewById(R.id.activity_sign_up_username_field);
        mEmailField = (EditText) findViewById(R.id.activity_sign_up_email_field);
        mPasswordField = (EditText) findViewById(R.id.activity_sign_up_password_field);
        mRepeatPasswordField = (EditText) findViewById(R.id.activity_sign_up_repeat_password_field);
    }

    protected View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.activity_sign_up_sign_up_button:
                    signUp(mUsernameField.getText().toString(), mEmailField.getText().toString(), mPasswordField.getText().toString());
                    break;

                case R.id.activity_sign_up_sign_in_button:
                    startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                    finish();
                    break;
            }
        }
    };

    private void signUp(final String username, String email, String password){
        Log.d(TAG, "createAccount:" + email);
        if (!validateForm()) {
            return;
        }
        showProgressDialog(R.string.registering);
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "createUserWithEmail:success");
                    mUser = mAuth.getCurrentUser();
                    mUser.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(username).build()).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            createUser();
                            startActivity(new Intent(SignUpActivity.this, MainProfileActivity.class));
                            finish();
                        }
                    });
                } else {
                    Log.e(TAG, "createUserWithEmail:failure", task.getException());
                    Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
                hideProgressDialog();
            }
        });

    }

    private boolean validateForm() {
        boolean valid = true;

        String userName = mUsernameField.getText().toString();
        String email = mEmailField.getText().toString();
        String password = mPasswordField.getText().toString();
        String repeatPassword = mRepeatPasswordField.getText().toString();

        if(userName.isEmpty()){
            mUsernameField.setError(getString(R.string.username_is_required));
            valid = false;
        }

        String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
        Pattern emailPattern = Pattern.compile(regex);
        Matcher emailMatcher = emailPattern.matcher(email);

        if(!emailMatcher.matches()){
            mEmailField.setError(getString(R.string.email_address_is_incorrect));
            valid = false;
        }

        if(password.length() < 6){
            mPasswordField.setError(getString(R.string.password_is_too_short));
            valid = false;
        }

        if(!repeatPassword.equals(password)){
            mRepeatPasswordField.setError(getString(R.string.passwords_are_different));
            valid = false;
        }
        return valid;
    }

    private void createUser(){
        final DatabaseReference usersReference = mDatabase.getReference(Constants.USERS_LOCATION);
        final String email = mUser.getEmail();
        final String username = mUser.getDisplayName();
        final DatabaseReference userReference = usersReference.child(encryptEmail(email));

        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() == null){
                    User user = new User(username, email, null);
                    userReference.setValue(user);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

}
