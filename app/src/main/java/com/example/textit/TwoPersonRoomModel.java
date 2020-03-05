package com.example.textit;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Model class for Two Person Chat Rooms.
 */
public class TwoPersonRoomModel {
    @ServerTimestamp
    private Date latestMessageTimestamp, roomCreatedTimestamp;
    private String latestMessage, latestMessageSender;
    private Map<String, Object> profile_urls, display_names, members;
    private List<String> member_list;

    public TwoPersonRoomModel() { }

    public TwoPersonRoomModel(String latestMessage, String latestMessageSender,
                              Map<String, Object> profile_urls, Map<String, Object> display_names,
                              Map<String, Object> members, List<String> member_list) {
        this.latestMessage = latestMessage;
        this.latestMessageSender = latestMessageSender;
        this.profile_urls = profile_urls;
        this.display_names = display_names;
        this.members = members;
        this.member_list = member_list;
    }

    @ServerTimestamp
    public Date getLatestMessageTimestamp() {
        return latestMessageTimestamp;
    }

    @ServerTimestamp
    public Date getRoomCreatedTimestamp() {
        return roomCreatedTimestamp;
    }

    public String getLatestMessage() {
        return latestMessage;
    }

    public String getLatestMessageSender() {
        return latestMessageSender;
    }

    public Map<String, Object> getProfile_urls() {
        return profile_urls;
    }

    public Map<String, Object> getDisplay_names() {
        return display_names;
    }

    public Map<String, Object> getMembers() {
        return members;
    }

    public List<String> getMember_list() {
        return member_list;
    }
}
