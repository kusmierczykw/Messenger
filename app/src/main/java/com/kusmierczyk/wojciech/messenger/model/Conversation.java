package com.kusmierczyk.wojciech.messenger.model;

import java.util.List;

/**
 * Created by wojciech on 29.06.2017.
 */

public class Conversation {
    private String conversationID;
    private List<Message> messageList;
    private User chatCreator;
    private User user;

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

    public User getChatCreator() {
        return chatCreator;
    }
    public User getUser() {
        return user;
    }


    //TODO Methods to remove
    public void setChatCreator(User user){
        this.chatCreator = user;
    }
    public void setUser(User user){
        this.user = user;
    }
}
