package com.kusmierczyk.wojciech.messenger.model;

/**
 * Created by wojciech on 29.06.2017.
 */

public class Friend {
    private String email;
    private String username;

    public Friend(){}
    public Friend(String email, String username){
        this.email = email;
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername(){
        return username;
    }

    public boolean equals(Object o){
        if(o == null){
            return false;
        }
        if(!(o instanceof Friend)){
            return false;
        }
        Friend friend = (Friend) o;
        return this.email == friend.email;
    }

    public int hashCode(){
        return email.hashCode() + username.hashCode();
    }
}
