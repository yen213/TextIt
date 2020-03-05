package com.example.textit;

/**
 * Constants for the projects Firestore Data Fields and Collection Paths.
 */
public interface FirestoreReferences {
    // Paths
    String TWO_PERSON_ROOMS_PATH = "twoPersonRooms";
    String USERS_PATH = "users";
    String GROUP_MESSAGES_PATH = "groupMessages";
    String MESSAGES_PATH = "messages";
    String USER_PROFILE_PICS_PATH = "userProfilePictures";
    String GROUP_ROOM_PICS_PATH = "groupRoomPictures";
    String FRIEND_LIST_PATH = "friendList";
    String FRIEND_REQUESTS_PATH = "friendRequests";

    // Fields
    String UID_FIELD = "uid";
    String EMAIL_FIELD = "email";
    String DISPLAY_NAMES_FIELD = "display_names";
    String ROOM_NAME_FIELD = "roomName";
    String ROOM_PICTURE_FIELD = "roomPic";
    String ROOM_PIC_UPDATED_FIELD = "roomPicUpdatedTimestamp";
    String SENT_FIELD = "sent";
    String MEMBERS_FIELD = "members";
    String MEMBER_LIST_FIELD = "member_list";
    String NAME_FIELD = "name";
    String AGE_FIELD = "age";
    String DOB_FIELD = "dateOfBirth";
    String FIRST_NAME_FIELD = "firstName";
    String LAST_NAME_FIELD = "lastName";
    String PROFILE_PIC_URL_FIELD = "profilePictureUrl";
    String PROFILE_PIC_UPDATED_FIELD = "profilePicUpdated";
    String PROFILE_URLS_FIELD = "profile_urls";
    String FRIENDS_FIELD = "friends";
    String FRIENDS_SINCE_FIELD = "friends_since";
    String LATEST_MSG_FIELD = "latestMessage";
    String LATEST_MSG_SENDER_FIELD = "latestMessageSender";
    String LATEST_MSG_SENT_FIELD = "latestMessageTimestamp";
    String MEMBER_ADDED_FIELD = "member_added";
    String FRIEND_EMAILS_FIELD = "friend_emails";
    String TOKEN_ID_FIELD = "tokenIds";

    // Field constants, put in fields where a data has been deleted
    String DELETED_BY_FIELD = "deletedBy";
    String ROOM_DELETED_DATE_FIELD = "roomDeletedTimestamp";
    String PREVIOUS_MEMBERS_FIELD = "previous_member";
}
