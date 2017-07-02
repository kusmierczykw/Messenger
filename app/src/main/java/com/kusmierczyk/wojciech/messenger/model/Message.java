package com.kusmierczyk.wojciech.messenger.model;

/**
 * Created by wojciech on 29.06.2017.
 */

public class Message {
    private String sender;
    private String message;
    private String timeStamp;

    public Message(){}
    public Message(String sender, String message, String timeStamp){
        this.sender = sender;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeStamp() {
        return timeStamp;
    }
}
