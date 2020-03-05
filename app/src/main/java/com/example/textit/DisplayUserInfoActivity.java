package com.example.textit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Activity which shows the profile picture, name, and email of a User. Logged in Users can either
 * send a message to this User if they are friends, or send them a friend request if they are not
 * friends.
 */
public class DisplayUserInfoActivity extends AppCompatActivity {
    // Constants
    private static final String TAG = DisplayUserInfoActivity.class.getName();
    public static final String DOCUMENT_ID_EXTRA = TAG + ".DOCUMENT_ID";
    private static final String SAME_EMAIL_ERROR =
            "Search email address cannot be the same as yours.";
    private static final String VALID_EMAIL_ERROR_MESSAGE = "Please enter a valid email address";
    public static final String CHAT_PARTNER_EXTRA = TAG + ".CHAT_PARTNER";
    public static final String FRIEND_PROFILE_URL_EXTRA = TAG + ".FRIEND_PROFILE_URL";
    public static final String COLLECTION_PATH_EXTRA = TAG + ".COLLECTION_PATH";
    public static final String FRIEND_UID_EXTRA = TAG + ".FRIEND_UID";
    public static final String GROUP_ROOM_NAME_EXTRA = TAG + ".GROUP_ROOM_NAME";
    public static final String GROUP_ROOM_PIC_EXTRA = TAG + ".GROUP_ROOM_PIC";

    // Views
    private EditText mSearchEditText;
    private CircleImageView mProfilePicCirImgView;
    private TextView mNameTextView, mEmailTextView;
    private Button mMsgOrAddFriendButton, mGroupMessageButton;
    private ProgressBar mProgressBar;

    // Firebase related member variables
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private CollectionReference friendListCollRef =
            db.collection(FirestoreReferences.FRIEND_LIST_PATH);
    private CollectionReference usersCollRef =
            db.collection(FirestoreReferences.USERS_PATH);

    // Member variables
    private List<String> mFriendList = new ArrayList<>();
    private TextView mNoUserFoundTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_user_info_activity);

        // Set views
        mSearchEditText = findViewById(R.id.search_edit_text_info_activity);
        mProfilePicCirImgView = findViewById(R.id.profile_img_view_display_info_activity);
        mNameTextView = findViewById(R.id.name_txt_view_info_activity);
        mEmailTextView = findViewById(R.id.email_txt_view_info_activity);
        mMsgOrAddFriendButton = findViewById(R.id.msg_or_add_button_info_activity);
        mGroupMessageButton = findViewById(R.id.group_message_button_info_activity);
        mProgressBar = findViewById(R.id.progress_bar_display_info);
        mNoUserFoundTextView = findViewById(R.id.no_user_found_info_activity);

        // Logged in User can Search for another User by entering that person's email address
        mSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                        actionId == EditorInfo.IME_ACTION_DONE) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    showViews(false);
                    searchForUser(mSearchEditText.getText().toString().trim());

                    return true;
                }

                return false;
            }
        });
    }

    /**
     * Get the list of all the User's friends and adds it to {@link #mFriendList}.
     */
    @Override
    protected void onStart() {
        super.onStart();

        if (user != null) {
            friendListCollRef.document(user.getUid())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            FriendsModel model = documentSnapshot
                                    .toObject(FriendsModel.class);

                            if (model.getFriends() != null) {
                                mFriendList.addAll(model.getFriends());
                            }

                            Log.d(TAG, "User's friend list: " + mFriendList);
                            checkIntent();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Failed to get User's friend list " +
                                    "(from onStart()): " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Uses the intent data to decide how the UI is set. Sets all the views to invisible if User
     * wants to search for a new friend. Or sets all the views with the friend's info that the User
     * clicked on in the {@link FriendsFragment} RecyclerView.
     */
    private void checkIntent() {
        String documentID = getIntent().getStringExtra(FriendsFragment.DOCUMENT_ID_EXTRA);

        if (documentID != null) {
            usersCollRef.document(documentID)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();

                                if (document.exists()) {
                                    FirestoreUserInfo data = document
                                            .toObject(FirestoreUserInfo.class);
                                    String firstName = data.getFirstName();
                                    String lastName = data.getLastName();
                                    String profileUrl = data.getProfilePictureUrl();
                                    String email = data.getEmail();

                                    showFirebaseUserInfo(email, document.getId(), firstName,
                                            lastName, profileUrl);
                                } else {
                                    Log.d(TAG, "No such document (from checkIntent())");
                                }
                            } else {
                                Log.d(TAG, "get failed with (from checkIntent())",
                                        task.getException());
                            }
                        }
                    });
        } else {
            showViews(false);
        }
    }

    /**
     * Shows the information of a searched User. Buttons get attached with onClickListeners which
     * either have the "Add Friend" or "Message" functionality depending on if the two Users are
     * friends or not.
     *
     * @param email Email of the searched User
     * @param uid   Searched User's uid to use as the document ID
     * @param fName First name of searched User
     * @param lName Last name of searched User
     */
    private void showFirebaseUserInfo(final String email, final String uid, final String fName,
                                      final String lName, final String url) {
        if (user != null) {
            final String name = fName + " " + lName;
            Drawable icon;

            // When both users are friends
            if (mFriendList != null && !mFriendList.isEmpty() && mFriendList.contains(uid)) {
                icon = getResources().getDrawable(R.drawable.message_icon);
                icon.setBounds(0, 0, 48, 48);
                mGroupMessageButton.setVisibility(View.VISIBLE);
                mMsgOrAddFriendButton.setCompoundDrawables(icon, null, null, null);
                mMsgOrAddFriendButton.setText(getResources().getString(R.string.string_message));
                mMsgOrAddFriendButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkForExistingRoom(uid, fName, lName, url);
                    }
                });
                mGroupMessageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        createNewRoom(FirestoreReferences.GROUP_MESSAGES_PATH, uid, fName, lName, url);
                    }
                });
            } else {
                // When both users are not friends
                icon = getResources().getDrawable(R.drawable.add_friend_icon);
                icon.setBounds(0, 0, 48, 48);
                mGroupMessageButton.setVisibility(View.INVISIBLE);
                mMsgOrAddFriendButton.setText(getResources().getString(R.string.string_add_friend));
                mMsgOrAddFriendButton.setCompoundDrawables(icon, null, null, null);
                mMsgOrAddFriendButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendFriendRequest(uid);
                    }
                });
            }

            showViews(true);

            // Load the FirebaseUser's profile picture into the View
            if (url != null) {
                Picasso
                        .get()
                        .load(url)
                        .into(mProfilePicCirImgView);
            }

            mNameTextView.setText(name);
            mEmailTextView.setText(email);
        }
    }

    /**
     * Shows All the views in the activity or hides all of them except the search bar
     *
     * @param show True to show views, false to hide.
     */
    private void showViews(boolean show) {
        if (show) {
            mProfilePicCirImgView.setVisibility(View.VISIBLE);
            mNameTextView.setVisibility(View.VISIBLE);
            mMsgOrAddFriendButton.setVisibility(View.VISIBLE);
            mEmailTextView.setVisibility(View.VISIBLE);
        } else {
            mProfilePicCirImgView.setVisibility(View.INVISIBLE);
            mNameTextView.setVisibility(View.INVISIBLE);
            mMsgOrAddFriendButton.setVisibility(View.INVISIBLE);
            mEmailTextView.setVisibility(View.INVISIBLE);
            mGroupMessageButton.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Hides the keyboard after the User clicked Done or Send and calls {@link #getDocument(String)}
     * to get the info of the FirebaseUser.
     */
    private void searchForUser(String input) {
        Log.d(TAG, "User search input in searchForUser(): " + input);

        // Get the input method and the current focus
        InputMethodManager imm = (InputMethodManager) DisplayUserInfoActivity.this
                .getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = DisplayUserInfoActivity.this.getCurrentFocus();

        //If no view currently has focus, create a new one so we can grab a window token from it
        if (view == null) {
            view = new View(DisplayUserInfoActivity.this);
        }

        // Hide the keyboard
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        getDocument(input);
    }

    /**
     * Set error if User entered incorrect email format, continue otherwise.
     *
     * @return True if email format is good, false otherwise
     */
    private boolean validateEmail() {
        CharSequence emailCharSeq = mSearchEditText.getText();

        if (emailCharSeq != null) {
            String emailInput = emailCharSeq.toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                mSearchEditText.setError(VALID_EMAIL_ERROR_MESSAGE);

                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * If the entered email is the same as the logged in User or a badly formatted email, set
     * error to the EditText and exit out of function. Else, query the database for the user enter
     * information. Show the 'No User found' TextView if search doesn't find any User with the
     * provided email.
     *
     * @param email The email address of the FirebaseUser to search for in the database
     */
    private void getDocument(String email) {
        if (!validateEmail()) {
            mProgressBar.setVisibility(View.INVISIBLE);
            mNoUserFoundTextView.setVisibility(View.INVISIBLE);
            return;
        }

        if (user != null) {
            if (user.getEmail().equals(email)) {
                mSearchEditText.setError(SAME_EMAIL_ERROR);
                mProgressBar.setVisibility(View.INVISIBLE);
                mNoUserFoundTextView.setVisibility(View.INVISIBLE);
                return;
            }
        }

        usersCollRef.whereEqualTo(FirestoreReferences.EMAIL_FIELD, email).limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.exists()) {
                                    mProgressBar.setVisibility(View.INVISIBLE);
                                    mNoUserFoundTextView.setVisibility(View.INVISIBLE);
                                    FirestoreUserInfo data = document.toObject(FirestoreUserInfo.class);
                                    String firstName = data.getFirstName();
                                    String lastName = data.getLastName();
                                    String profileUrl = data.getProfilePictureUrl();
                                    String email = data.getEmail();

                                    showFirebaseUserInfo(email, document.getId(), firstName, lastName,
                                            profileUrl);
                                }
                            }

                            if (task.getResult().isEmpty()) {
                                Log.d(TAG, "No such document (from getDocument()): ");

                                mProgressBar.setVisibility(View.INVISIBLE);
                                mNoUserFoundTextView.setVisibility(View.VISIBLE);
                                showViews(false);
                            }
                        } else {
                            Log.d(TAG, "No such document (from getDocument()): "
                                    + task.getException());

                            mProgressBar.setVisibility(View.INVISIBLE);
                            mNoUserFoundTextView.setVisibility(View.VISIBLE);
                            showViews(false);
                        }
                    }
                });
    }

    /**
     * Sends the searched User a friend request from the current logged in User, if they are not
     * friends already.
     *
     * @param searchedUserUid Selected User's uid
     */
    private void sendFriendRequest(String searchedUserUid) {
        if (user != null) {
            String userPhotoUrl = null;

            if (user.getPhotoUrl() != null) {
                userPhotoUrl = user.getPhotoUrl().toString();
            }

            FriendsModel data = new FriendsModel(user.getDisplayName(), userPhotoUrl, user.getUid(),
                    user.getEmail(), searchedUserUid);

            db.collection(FirestoreReferences.FRIEND_LIST_PATH).document(searchedUserUid)
                    .collection(FirestoreReferences.FRIEND_REQUESTS_PATH)
                    .add(data)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "Friend request sent: " + documentReference.getId());

                            Toast.makeText(DisplayUserInfoActivity.this,
                                    "Friend request sent!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error sending friend request: ", e);
                            Toast.makeText(DisplayUserInfoActivity.this,
                                    "Something went wrong, please try again later.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    /**
     * Checks if the logged in User has an existing chat room with the searched user in
     * {@link FirestoreReferences#TWO_PERSON_ROOMS_PATH} collection. If room the exists, start the
     * message in that room or create a new room.
     *
     * @param friendUid   Uid of the friend
     * @param friendFName First name of friend
     * @param friendLName Last name of friend
     * @param friendUrl   Profile URL of friend
     */
    private void checkForExistingRoom(final String friendUid, final String friendFName,
                                      final String friendLName, final String friendUrl) {
        if (user != null) {
            String userUid = user.getUid();
            final String fullName = friendFName + " " + friendLName;

            db.collection(FirestoreReferences.TWO_PERSON_ROOMS_PATH)
                    .whereEqualTo(FirestoreReferences.MEMBERS_FIELD + "." + userUid, true)
                    .whereEqualTo(FirestoreReferences.MEMBERS_FIELD + "." + friendUid, true)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, "Existing chat room found :" + document.getId());
                                    startChat(document.getId(),
                                            FirestoreReferences.TWO_PERSON_ROOMS_PATH, fullName,
                                            friendUrl, friendUid);
                                }

                                if (task.getResult().isEmpty()) {
                                    Log.d(TAG, "Existing chat room not found.");
                                    createNewRoom(FirestoreReferences.TWO_PERSON_ROOMS_PATH,
                                            friendUid, friendFName, friendLName, friendUrl);
                                }
                            } else {
                                Log.d(TAG, "Error getting existing chat room " +
                                        "(from checkForExistingRoom()): ", task.getException());
                            }
                        }
                    });
        }
    }

    /**
     * Creates a new chat room in the {@link FirestoreReferences#TWO_PERSON_ROOMS_PATH} or
     * {@link FirestoreReferences#GROUP_MESSAGES_PATH} collection.
     *
     * @param collectionPath The room type to create (group or two person)
     * @param friendUid      Uid of the Friend
     * @param friendFName    First name of friend
     * @param friendLName    Last name of friend
     * @param friendUrl      Profile URL of friend
     */
    private void createNewRoom(final String collectionPath, final String friendUid,
                               String friendFName, String friendLName, final String friendUrl) {
        if (user != null) {
            // Create objects for the new room information to pass into constructor
            String userUid = user.getUid();
            String userPhotoUrl = null;
            List<String> membersList = new ArrayList<>(Arrays.asList(userUid, friendUid));
            Map<String, Object> profileUrls = new HashMap<>();
            Map<String, Object> display_names = new HashMap<>();
            Map<String, Object> members = new HashMap<>();

            final String friendDisplayName = friendFName + " " + friendLName;
            String userDisplayName = user.getDisplayName();
            String roomName = userDisplayName + ", " + friendDisplayName;

            if (user.getPhotoUrl() != null) {
                userPhotoUrl = user.getPhotoUrl().toString();
            }

            profileUrls.put(userUid, userPhotoUrl);
            profileUrls.put(friendUid, friendUrl);
            display_names.put(userUid, userDisplayName);
            display_names.put(friendUid, friendDisplayName);
            members.put(userUid, true);
            members.put(friendUid, true);

            if (collectionPath.equals(FirestoreReferences.TWO_PERSON_ROOMS_PATH)) {
                // Create the new Two Person Chat Room with all the info
                TwoPersonRoomModel twoPersonRoomModel = new TwoPersonRoomModel(" ",
                        userDisplayName, profileUrls, display_names, members, membersList);

                db.collection(collectionPath)
                        .add(twoPersonRoomModel)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, "New Two Person Chat Room created " +
                                        "(from createNewRoom()): " + documentReference.getId());
                                startChat(documentReference.getId(), collectionPath,
                                        friendDisplayName, friendUrl, friendUid);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error creating new Two Person Chat Room " +
                                        "(from createNewRoom()): ", e);
                            }
                        });
            } else {
                // Create the new Group Chat Room with all the info
                Map<String, Object> membersAdded = new HashMap<>();
                membersAdded.put(userUid, FieldValue.serverTimestamp());
                membersAdded.put(friendUid, FieldValue.serverTimestamp());

                GroupRoomModel groupRoomModel = new GroupRoomModel(" ", userDisplayName,
                        roomName, membersList, membersAdded, display_names);

                db.collection(collectionPath)
                        .add(groupRoomModel)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, "New Group Room created " +
                                        "(from createNewRoom()): " + documentReference.getId());
                                startChat(documentReference.getId(), collectionPath,
                                        friendDisplayName, friendUrl, friendUid);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error creating new Group Room " +
                                        "(from createNewRoom()): ", e);
                            }
                        });
            }
        }
    }

    /**
     * Start the activity to send messages to the searched User.
     *
     * @param id         ID of the chat room
     * @param collPath   Room type the message is for (group or two person)
     * @param friendName Name of the friend
     * @param friendUrl  Profile Url of the friend
     */
    private void startChat(String id, String collPath, String friendName, String friendUrl,
                           String friendUid) {
        Log.d(TAG, "startChat: Room ID - " + id + "\tCollection Path - " + collPath
                + "\tName - " + friendName + "\tfriendUrl - " + friendUrl);

        Intent intent = new Intent
                (DisplayUserInfoActivity.this, SendMessageActivity.class);

        if (collPath.equals(FirestoreReferences.TWO_PERSON_ROOMS_PATH)) {
            intent.putExtra(CHAT_PARTNER_EXTRA, friendName);
            intent.putExtra(FRIEND_PROFILE_URL_EXTRA, friendUrl);
        } else if (collPath.equals(FirestoreReferences.GROUP_MESSAGES_PATH)) {
            intent.putExtra(GROUP_ROOM_NAME_EXTRA, friendName);
            intent.putExtra(GROUP_ROOM_PIC_EXTRA, friendUrl);
        }

        intent.putExtra(DOCUMENT_ID_EXTRA, id);
        intent.putExtra(COLLECTION_PATH_EXTRA, collPath);
        intent.putExtra(FRIEND_UID_EXTRA, friendUid);
        intent.putExtra(ActivityConstants.CALLING_ACTIVITY_EXTRA, ActivityConstants.DISPLAY_INFO);

        startActivity(intent);
        finish();
    }
}
