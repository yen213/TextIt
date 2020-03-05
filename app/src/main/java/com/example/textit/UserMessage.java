package com.example.textit;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model class for User messages
 */
public class UserMessage {
    @ServerTimestamp
    private Date sent;
    private String message, sender, receiverUid;

    public UserMessage() { }

    public UserMessage(String message, String sender) {
        this.message = message;
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }

    @ServerTimestamp
    public Date getSent() {
        return sent;
    }

    public String getReceiverUid() {
        return receiverUid;
    }

    public void setReceiverUid(String receiverUid) {
        this.receiverUid = receiverUid;
    }
}
