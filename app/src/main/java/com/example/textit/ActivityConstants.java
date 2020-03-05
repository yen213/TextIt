package com.example.textit;

/**
 * Interface class defining constants for some of the activities in the application and key
 * constants shared among activities. Int constants are used in
 * @see SendMessageActivity to decide how a particular instance of that activity is started.
 */
public interface ActivityConstants {
    // Intent Key constants
    String CALLING_ACTIVITY_EXTRA = "CALLING_ACTIVITY";
    String NEW_USER_EXTRA = "NEW_USER";

    // Activity constants
    int CHAT_FRAGMENT = 100;
    int GROUP_FRAGMENT = 200;
    int DISPLAY_INFO = 300;
}
