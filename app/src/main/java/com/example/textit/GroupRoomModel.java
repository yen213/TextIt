package com.example.textit;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Model class for Group Chat Rooms.
 */
public class GroupRoomModel {
    @ServerTimestamp
    private Date latestMessageTimestamp, roomPicUpdatedTimestamp, roomCreatedTimestamp;
    private String latestMessage, latestMessageSender, roomPic, roomName;
    private List<String> members;
    private Map<String, Object> member_added, display_names;

    // Required empty constructor
    public GroupRoomModel() {
    }

    public GroupRoomModel(String latestMessage, String latestMessageSender, String roomName,
                          List<String> members, Map<String, Object> member_added,
                          Map<String, Object> display_names) {
        this.latestMessage = latestMessage;
        this.latestMessageSender = latestMessageSender;
        this.roomName = roomName;
        this.members = members;
        this.member_added = member_added;
        this.display_names = display_names;
    }

    @ServerTimestamp
    public Date getLatestMessageTimestamp() {
        return latestMessageTimestamp;
    }

    @ServerTimestamp
    public Date getRoomPicUpdatedTimestamp() {
        return roomPicUpdatedTimestamp;
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

    public String getRoomPic() {
        return roomPic;
    }

    public String getRoomName() {
        return roomName;
    }

    public List<String> getMembers() {
        return members;
    }

    public Map<String, Object> getMember_added() {
        return member_added;
    }

    public Map<String, Object> getDisplay_names() {
        return display_names;
    }
}
