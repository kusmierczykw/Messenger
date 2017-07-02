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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignInActivity extends MainActivity {
    private static final String TAG = "SignInActivity";

    /** Activity elements **/
    private EditText mEmailField;
    private EditText mPasswordField;
    private Button mSignInButton;
    private Button mSignUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        if(mUser != null){
            Log.d(TAG, "signIn:User is signed in");
            startActivity(new Intent(SignInActivity.this, MainProfileActivity.class));
            finish();
        }

        initialization();
    }

    protected void initialization(){
        mSignInButton = (Button) findViewById(R.id.activity_sign_in_sign_in_button);
        mSignInButton.setOnClickListener(listener);
        mSignUpButton = (Button) findViewById(R.id.activity_sign_in_sign_up_button);
        mSignUpButton.setOnClickListener(listener);

        mEmailField = (EditText) findViewById(R.id.activity_sign_in_email_field);
        mPasswordField = (EditText) findViewById(R.id.activity_sign_in_password_field);
    }

    protected View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.activity_sign_in_sign_in_button:
                    signIn(mEmailField.getText().toString(), mPasswordField.getText().toString());
                    break;

                case R.id.activity_sign_in_sign_up_button:
                    startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
                    finish();
                    break;
            }
        }
    };

    private void signIn(String email, String password) {
        if (!validateForm()) {
            return;
        }
        showProgressDialog();
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
                Log.d(TAG, "signInWithEmail:success");
                mUser = mAuth.getCurrentUser();
                startActivity(new Intent(SignInActivity.this, MainProfileActivity.class));
                finish();
            } else {
                Log.e(TAG, "signInWithEmail:failure", task.getException());
                Toast.makeText(SignInActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();

            }
            hideProgressDialog();
            }
        });

    }

    private boolean validateForm() {
        boolean valid = true;

        String email = mEmailField.getText().toString();
        String password = mPasswordField.getText().toString();

        if(password.length() < 6){
            mPasswordField.setError(getString(R.string.password_is_too_short));
            valid = false;
        }

        String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
        Pattern emailPattern = Pattern.compile(regex);
        Matcher emailMatcher = emailPattern.matcher(email);

        if(!emailMatcher.matches()){
            mEmailField.setError(getString(R.string.email_address_is_incorrect));
            valid = false;
        }
        return valid;
    }
}
