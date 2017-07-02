package com.kusmierczyk.wojciech.messenger.model;

import java.util.List;

/**
 * Created by wojciech on 29.06.2017.
 */

public class Conversation {
    private String conversationID;
    private List<Message> messageList;
    private Friend friend;

    public Conversation(){}
    public Conversation(String conversationID) {
        this.conversationID = conversationID;
    }

    public String getConversationID() {
        return conversationID;
    }

    public void setConversationID(String conversationID) {
        this.conversationID = conversationID;
    }

    public List<Message> getMessageList() {
        return messageList;
    }

    public Friend getFriend() {
        return friend;
    }


    //TODO Methods to remove
    public void setFriend(Friend friend){
        this.friend = friend;
    }

    public void removeFriend(){
        this.friend = null;
    }
}
