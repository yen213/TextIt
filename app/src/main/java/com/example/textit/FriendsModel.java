package com.example.textit;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

/**
 * Model class for friends.
 */
public class FriendsModel {
    @ServerTimestamp
    private Date sent;
    private String name, profilePictureUrl, uid, email, receiverUid;
    private List<String> friends;

    public FriendsModel() {
    }

    public FriendsModel(String name, String profilePictureUrl, String uid, String email,
                        String receiverUid) {
        this.name = name;
        this.profilePictureUrl = profilePictureUrl;
        this.uid = uid;
        this.email = email;
        this.receiverUid = receiverUid;
    }

    public String getName() {
        return name;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public List<String> getFriends() {
        return friends;
    }

    @ServerTimestamp
    public Date getSent() {
        return sent;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getReceiverUid() {
        return receiverUid;
    }
}
