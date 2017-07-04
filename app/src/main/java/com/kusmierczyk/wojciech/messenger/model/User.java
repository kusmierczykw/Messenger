package com.kusmierczyk.wojciech.messenger.model;

import static com.google.android.gms.internal.zznu.is;

/**
 * Created by wojciech on 29.06.2017.
 */

public class User {
    private String username;
    private String email;
    private String avatarURL;
    private boolean status;

    public User(){}
    public User(String username, String email, String avatarURL){
        this.username = username;
        this.email = email;
        this.avatarURL = avatarURL;
        status = false;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAvatarURL(String avatarURL) {
        this.avatarURL = avatarURL;
    }

    public void setStatus(boolean status){
        status = status;
    }

    public boolean getStatus(){
        return status;
    }



    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarURL() {
        return avatarURL;
    }


    @Override
    public String toString() {
        return "{name="+username + ":email=" + email + ":avatarURL=" + avatarURL+"}";
    }
}
