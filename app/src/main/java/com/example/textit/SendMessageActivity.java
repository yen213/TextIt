package com.example.textit;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Continuation;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Activity from where Users can send a message to other Users or groups. Checks the intents
 * received and starts the chat room with the right type of chat room with the appropriate views
 * and variables initialized.
 */
public class SendMessageActivity extends AppCompatActivity
        implements ChangeGroupNameDialog.GroupDialogListener,
        AddNewGroupMemberDialog.GroupMemberDialogListener {
    // Constants
    public static final String TAG = SendMessageActivity.class.getName();
    private static final int PICK_IMAGE_REQUEST = 1;
    public static final int QUERY_LIMIT = 50;

    // Firebase and Firestore variables
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private CollectionReference groupMessagesCollRef =
            db.collection(FirestoreReferences.GROUP_MESSAGES_PATH);
    private StorageReference mStorageRef;

    // Views
    private RecyclerView mMessagesRecyclerView;
    private EditText mMessageEditText;
    private LinearLayoutManager mLinearLayoutManager;
    private TextView mActivityTitleTxtView;
    private CircleImageView mChatPicCirImgView;
    private MessageListAdapter mMessageListAdapter;
    private Toolbar mToolBar;

    // Member variables
    private String mDocumentID, mCollRefMessageRoom, mFriendUid, mLatestMessage, mLatestMessageSender;
    private boolean mOptionsMenu;
    private Uri mImageUri;
    private Bundle mBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_message_activity);

        // Set views
        mMessagesRecyclerView = findViewById(R.id.messages_recycler_view_send_msg_activity);
        mMessageEditText = findViewById(R.id.type_msg_edit_txt_send_msg_activity);
        mToolBar = findViewById(R.id.toolbar_send_msg_activity);
        mActivityTitleTxtView = findViewById(R.id.chat_txt_view_toolbar);
        mChatPicCirImgView = findViewById(R.id.chat_img_view_toolbar);
        ImageButton mSendImageButton = findViewById(R.id.send_button_send_msg_activity);

        mLinearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mStorageRef = FirebaseStorage.getInstance()
                .getReference(FirestoreReferences.GROUP_ROOM_PICS_PATH);

        // Get intent extras
        mBundle = getIntent().getExtras();

        // Set ToolBar and get intent constants
        setSupportActionBar(mToolBar);
        checkIntentActivity();

        // Set listener to the Send Button
        mSendImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMessageToDatabase();
            }
        });

        // Set listeners to the ToolBar CircleImageView and TextView if this activity is a group
        // message type. Allows group members to click them and change group picture/name.
        if (mCollRefMessageRoom != null && mCollRefMessageRoom.equals(FirestoreReferences.GROUP_MESSAGES_PATH)) {
            mChatPicCirImgView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFileChooser();
                }
            });

            mActivityTitleTxtView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ChangeGroupNameDialog nameDialog = new ChangeGroupNameDialog();
                    nameDialog.show(getSupportFragmentManager(), "group name dialog");
                }
            });
        }
    }

    /**
     * Write User's latest message to the correct message room and upload any picture to storage, if
     * needed.
     */
    @Override
    protected void onPause() {
        super.onPause();
        uploadPictureToStorage();

        if (mCollRefMessageRoom != null && mDocumentID != null && mLatestMessage != null
                && mLatestMessageSender != null) {
            db.collection(mCollRefMessageRoom).document(mDocumentID)
                    .update(FirestoreReferences.LATEST_MSG_FIELD, mLatestMessage,
                            FirestoreReferences.LATEST_MSG_SENDER_FIELD, mLatestMessageSender,
                            FirestoreReferences.LATEST_MSG_SENT_FIELD,
                            FieldValue.serverTimestamp())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Latest message updated in room " + mCollRefMessageRoom +
                                        " (from onPause())");
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Failed to update latest message in " + mCollRefMessageRoom +
                                    " (from onPause()): " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Create a options menu for this activity if it is a group message activity.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mOptionsMenu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.group_options_menu, menu);

            return true;
        }

        return false;
    }

    /**
     * Handle User click events for the options menu items.
     *
     * @param item The item in the options menu that was clicked on
     * @return True if an option is selected else just return the options menu.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            // Show the dialog box for adding a group member
            case R.id.add_group_member_options_menu:
                AddNewGroupMemberDialog memberDialog = new AddNewGroupMemberDialog();
                memberDialog.show(getSupportFragmentManager(), "MEMBER_ADD_DIALOG");

                return true;

            // Show the dialog box for views the list of group members
            case R.id.group_members_options_menu:
                getMembers();

                return true;

            // Takes Users out of the current group they are in
            case R.id.leave_group_options_menu:
                leaveGroup();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Function called when Users have selected a picture from the File Chooser on their device and
     * puts that image inside {@link #mChatPicCirImgView}.
     *
     * @param requestCode The picture upload request
     * @param resultCode  Result of the upload request
     * @param data        The picture that is uploaded
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            mImageUri = data.getData();

            Picasso
                    .get()
                    .load(mImageUri)
                    .into(mChatPicCirImgView);
        }
    }

    /**
     * Override method from {@link ChangeGroupNameDialog} dialog. Gets the new group name input from
     * the dialog box that the User has entered and sets it to the ToolBar and updates the group
     * name field in Firestore.
     *
     * @param name New group name User entered.
     * @see #changeGroupName(String)
     */
    @Override
    public void getNewGroupName(String name) {
        if (name != null && !name.isEmpty()) {
            mActivityTitleTxtView.setText(name);
            changeGroupName(name);
        }
    }

    /**
     * Override method from {@link ChangeGroupNameDialog} dialog. Gets the email of the person the
     * User wants to add to the group from the dialog box and checks to see if that member already
     * exists in the group before adding them to the group.
     *
     * @param email Email of the person User entered.
     * @see #checkMemberExists(String)
     */
    @Override
    public void getNewGroupMember(String email) {
        if (email != null && !email.isEmpty()) {
            Log.d(TAG, "getNewGroupMember: " + email);
            checkMemberExists(email);
        }
    }

    /**
     * Get the activity that created the intent to call this activity and update the screen.
     */
    private void checkIntentActivity() {
        int callingActivity = getIntent()
                .getIntExtra(ActivityConstants.CALLING_ACTIVITY_EXTRA, 0);

        Log.d(TAG, "Calling activity: " + callingActivity);

        switch (callingActivity) {
            case ActivityConstants.CHAT_FRAGMENT:
                updateView(ActivityConstants.CHAT_FRAGMENT,
                        FirestoreReferences.TWO_PERSON_ROOMS_PATH,
                        ChatFragment.DOCUMENT_ID_EXTRA, false);

                break;

            case ActivityConstants.GROUP_FRAGMENT:
                updateView(ActivityConstants.GROUP_FRAGMENT,
                        FirestoreReferences.GROUP_MESSAGES_PATH,
                        GroupMessageFragment.DOCUMENT_ID_EXTRA, true);

                break;

            case ActivityConstants.DISPLAY_INFO:
                String room = getIntent().getStringExtra(DisplayUserInfoActivity.COLLECTION_PATH_EXTRA);
                updateView(ActivityConstants.DISPLAY_INFO, room,
                        DisplayUserInfoActivity.DOCUMENT_ID_EXTRA, false);

                break;
        }
    }

    /**
     * Retrieve information from the intents and {@link #checkIntentActivity()} function and set the
     * Toolbar views and message views.
     *
     * @param activity           The calling activity
     * @param messageRoomCollRef Collection Reference for the chat room
     *                           {@link FirestoreReferences#GROUP_MESSAGES_PATH} or
     *                           {@link FirestoreReferences#TWO_PERSON_ROOMS_PATH}
     * @param activityDocKey     Intent key to retrieve the Document ID of a chat room from the
     *                           correct activity
     * @param options            Show the options menu if chat room is a group message room.
     */
    private void updateView(int activity, String messageRoomCollRef, String activityDocKey,
                            boolean options) {
        if (mBundle != null) {
            mDocumentID = mBundle.getString(activityDocKey);
            mCollRefMessageRoom = messageRoomCollRef;
            mOptionsMenu = options;

            if (activity == ActivityConstants.CHAT_FRAGMENT) {
                mFriendUid = mBundle.getString(ChatFragment.FRIEND_UID_EXTRA);
            } else if (activity == ActivityConstants.DISPLAY_INFO) {
                mFriendUid = mBundle.getString(DisplayUserInfoActivity.FRIEND_UID_EXTRA);
            }

            Log.d(TAG, "Document ID, Message Room, Friend Uid: " + mDocumentID + "\t" +
                    mCollRefMessageRoom + "\t" + mFriendUid);

            setAppToolBar(activity);
            setMessagesToRecycler();
        }
    }

    /**
     * Sets the ToolBar title {@link #mActivityTitleTxtView} and image {@link #mChatPicCirImgView}
     * depending on if this activity is a group message or type or two person chat room type.
     *
     * @param callingActivity The activity where to get the intent information from
     */
    private void setAppToolBar(int callingActivity) {
        if (user != null && mCollRefMessageRoom != null && mBundle != null) {
            // Get friend's name and profile picture url for two person chat rooms
            if (mCollRefMessageRoom.equals(FirestoreReferences.TWO_PERSON_ROOMS_PATH)) {
                String chatPartner = " ";
                String profileUrl = " ";

                // Chat fragment
                if (callingActivity == ActivityConstants.CHAT_FRAGMENT) {
                    chatPartner = mBundle.getString(ChatFragment.CHAT_PARTNER_EXTRA);
                    profileUrl = mBundle.getString(ChatFragment.FRIEND_PROFILE_URL_EXTRA);
                }
                // Display info
                else if (callingActivity == ActivityConstants.DISPLAY_INFO) {
                    chatPartner = mBundle.getString(DisplayUserInfoActivity.CHAT_PARTNER_EXTRA);
                    profileUrl = mBundle.getString(DisplayUserInfoActivity.FRIEND_PROFILE_URL_EXTRA);
                }

                mActivityTitleTxtView.setText(chatPartner);

                if (profileUrl != null) {
                    Picasso
                            .get()
                            .load(profileUrl)
                            .into(mChatPicCirImgView);
                }

                Log.d(TAG, "Chat partner and profile URL: " + chatPartner + "\t" + profileUrl);
            }
            // Get group's name and profile picture url for group message rooms
            else if (mCollRefMessageRoom.equals(FirestoreReferences.GROUP_MESSAGES_PATH)) {
                mOptionsMenu = true;
                String groupName, groupPhotoUrl;

                setSupportActionBar(mToolBar);

                if (callingActivity == ActivityConstants.DISPLAY_INFO) {
                    groupName = user.getDisplayName() + ", " +
                            mBundle.getString(DisplayUserInfoActivity.GROUP_ROOM_NAME_EXTRA);
                    groupPhotoUrl = mBundle.getString(DisplayUserInfoActivity.GROUP_ROOM_PIC_EXTRA);
                } else {
                    groupName = user.getDisplayName() + ", " +
                            mBundle.getString(GroupMessageFragment.GROUP_ROOM_NAME_EXTRA);
                    groupPhotoUrl = mBundle.getString(GroupMessageFragment.GROUP_ROOM_PIC_EXTRA);
                }

                mActivityTitleTxtView.setText(groupName);

                if (groupPhotoUrl != null) {
                    Picasso
                            .get()
                            .load(groupPhotoUrl)
                            .into(mChatPicCirImgView);
                }

                Log.d(TAG, "Group name and photo URL (from setAppToolBar()): " + groupName +
                        "\t" + groupPhotoUrl);
            }
        }
    }

    /**
     * Load the last {@link #QUERY_LIMIT} amount of messages from the database and display it in the
     * RecyclerView.
     */
    private void setMessagesToRecycler() {
        if (user != null && mCollRefMessageRoom != null && mDocumentID != null) {
            Log.d(TAG, "setMessagesToRecycler: " + mCollRefMessageRoom + "\t" + mDocumentID);
            Query query = db.collection(mCollRefMessageRoom)
                    .document(mDocumentID).collection(FirestoreReferences.MESSAGES_PATH)
                    .orderBy(FirestoreReferences.SENT_FIELD, Query.Direction.ASCENDING)
                    .limitToLast(QUERY_LIMIT);

            FirestoreRecyclerOptions<UserMessage> options =
                    new FirestoreRecyclerOptions.Builder<UserMessage>()
                            .setQuery(query, UserMessage.class)
                            .setLifecycleOwner(SendMessageActivity.this)
                            .build();

            mMessageListAdapter = new MessageListAdapter(options);
            mMessagesRecyclerView.setHasFixedSize(true);
            mMessagesRecyclerView.setLayoutManager(mLinearLayoutManager);
            mMessagesRecyclerView.setAdapter(mMessageListAdapter);

            setUpRecyclerView();
        }
    }

    /**
     * Sets up the RecyclerView to scroll to the bottom of the messages (latest message) after the
     * messages initially load in and when keyboard is opened.
     */
    private void setUpRecyclerView() {
        // Scroll to the bottom of the RecyclerView when messages load in
        mMessageListAdapter.registerAdapterDataObserver(new AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mMessageListAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();

                // If the recycler view is initially being loaded or the user is at the bottom
                // of the list, scroll to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessagesRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        // Scroll to the bottom of the RecyclerView when users bring up the keyboard
        mMessagesRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, final int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    mMessagesRecyclerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mMessagesRecyclerView.smoothScrollToPosition(bottom);
                        }
                    }, 100);
                }
            }
        });
    }

    /**
     * Deletes Users from the current group and they can no longer see the messages in that group,
     * unless added to the group again.
     */
    private void leaveGroup() {
        if (mDocumentID != null && user != null) {
            String userUid = user.getUid();
            Map<String, Object> deletes = new HashMap<>();

            deletes.put(FirestoreReferences.DISPLAY_NAMES_FIELD + "." + userUid, FieldValue.delete());
            deletes.put(FirestoreReferences.MEMBER_ADDED_FIELD + "." + userUid, FieldValue.delete());
            deletes.put(FirestoreReferences.MEMBERS_FIELD, FieldValue.arrayRemove(userUid));

            groupMessagesCollRef.document(mDocumentID)
                    .update(deletes)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "User has left the group: " + mDocumentID);

                            if (mActivityTitleTxtView.getText() != null) {
                                finish();
                                Toast.makeText(SendMessageActivity.this,
                                        "You have left the group: " + mActivityTitleTxtView.getText(),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                finish();
                                Toast.makeText(SendMessageActivity.this,
                                        "You have left the group.", Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Unable to delete User from the group: ", e);
                            Toast.makeText(SendMessageActivity.this,
                                    "Something went wrong, please try again later.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    /**
     * Opens User's File Chooser from where they can select a picture to upload into the app.
     */
    private void openFileChooser() {
        Intent intent = new Intent();

        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * Returns the extension of the image file Users picked.
     */
    private String getFileExtension(Uri uri) {
        ContentResolver cResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        return mime.getExtensionFromMimeType(cResolver.getType(uri));
    }

    /**
     * Uploads the User's selected picture to the Firebase Storage. If multiple group pictures were
     * added to uploaded during the activity lifecycle, only the last picture is uploaded to the
     * database.
     */
    private void uploadPictureToStorage() {
        if (mImageUri != null && mDocumentID != null) {
            final StorageReference fileRef = mStorageRef
                    .child(FirestoreReferences.GROUP_ROOM_PICS_PATH)
                    .child(mDocumentID)
                    .child(System.currentTimeMillis() + "." + getFileExtension(mImageUri));

            fileRef.putFile(mImageUri)
                    .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return fileRef.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        putImgInFirestore(downloadUri.toString());

                        Log.d(TAG, "Download Uri (from uploadPictureToStorage()): " +
                                downloadUri.toString());
                    } else {
                        Log.d(TAG, "Picture upload failed (from uploadPictureToStorage()): " +
                                task.getException().getMessage());
                    }
                }
            });
        }
    }

    /**
     * Get the download link for the last group picture uploaded by the User and put it in the
     * {@link FirestoreReferences#ROOM_PICTURE_FIELD} field and update the
     * {@link FirestoreReferences#ROOM_PIC_UPDATED_FIELD} field in group room document in the
     * {@link FirestoreReferences#GROUP_MESSAGES_PATH} collection.
     *
     * @param url Download URL of the new group photo
     */
    private void putImgInFirestore(String url) {
        if (user != null && mDocumentID != null) {
            Map<String, Object> updates = new HashMap<>();

            updates.put(FirestoreReferences.ROOM_PICTURE_FIELD, url);
            updates.put(FirestoreReferences.ROOM_PIC_UPDATED_FIELD, FieldValue.serverTimestamp());

            groupMessagesCollRef.document(mDocumentID)
                    .update(updates)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Group picture field updated in Firestore " +
                                    "(from putImgInFirestore())");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating group picture field in Firestore " +
                                    "(from putImgInFirestore()): " + e.getMessage());

                            Toast.makeText(SendMessageActivity.this,
                                    "Failed to change group photo.", Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    /**
     * Retrieves the {@link #mDocumentID} document from the
     * {@link FirestoreReferences#GROUP_MESSAGES_PATH} collection to display a list of all the group's
     * members.
     */
    private void getMembers() {
        if (mDocumentID != null) {
            groupMessagesCollRef.document(mDocumentID)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            GroupRoomModel data = documentSnapshot.toObject(GroupRoomModel.class);

                            if (data.getDisplay_names() != null) {
                                List<String> displayNames = new ArrayList<>();

                                for (Object names : data.getDisplay_names().values()) {
                                    displayNames.add(names.toString());
                                }

                                Log.d(TAG, "Group members: " + displayNames);
                                showMembers(displayNames);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Failed to get a list of group members " +
                                    "(from getMembers()): " + e.getMessage());

                            Toast.makeText(SendMessageActivity.this,
                                    "Something went wrong, please try again later",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    /**
     * Shows the names of all the group members retrieved from the query {@link #getMembers()} in a
     * dialog box.
     */
    private void showMembers(List<String> names) {
        if (names != null && !names.isEmpty()) {
            // Setup the alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(SendMessageActivity.this,
                    R.style.alert_dialog_title);

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>
                    (SendMessageActivity.this, android.R.layout.simple_list_item_1,
                            names);

            // Set the list items
            builder.setAdapter(arrayAdapter, null);

            // Set negative button
            builder.setNegativeButton(getResources().getString(R.string.string_cancel), null);

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Checks that the new member Users enter, exist in the database before adding them to the group.
     *
     * @param email Email of the person Users want to add to group
     */
    private void checkMemberExists(final String email) {
        if (mDocumentID != null && user != null) {
            if (user.getEmail().equals(email)) {
                return;
            }

            db.collection(FirestoreReferences.USERS_PATH)
                    .whereEqualTo(FirestoreReferences.EMAIL_FIELD, email)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, document.getId() + " => " + document.getData());

                                    FirestoreUserInfo info = document
                                            .toObject(FirestoreUserInfo.class);
                                    addNewGroupMember(info.getUid(), info.getFirstName(),
                                            info.getLastName());
                                }

                                if (task.getResult().isEmpty() || task.getResult() == null) {
                                    Toast.makeText(SendMessageActivity.this,
                                            "No User found matching that email address",
                                            Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());

                                Toast.makeText(SendMessageActivity.this,
                                        "No User found matching that email address",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    /**
     * After successfully finding and retrieving the document of the search User, adds their
     * information to the group's document in the {@link FirestoreReferences#GROUP_MESSAGES_PATH}
     * collection.
     *
     * @param newMemberUid   UID of the person Users want to add to group
     * @param newMemberLName Last name of the new member
     * @param newMemberFName First name of the new member
     */
    private void addNewGroupMember(final String newMemberUid, String newMemberFName,
                                   String newMemberLName) {
        if (mDocumentID != null) {
            final String fullName = newMemberFName + " " + newMemberLName;
            Map<String, Object> updates = new HashMap<>();

            updates.put(FirestoreReferences.DISPLAY_NAMES_FIELD + "." + newMemberUid, fullName);
            updates.put(FirestoreReferences.MEMBER_ADDED_FIELD + "." + newMemberUid,
                    FieldValue.serverTimestamp());
            updates.put(FirestoreReferences.MEMBERS_FIELD, FieldValue.arrayUnion(newMemberUid));

            groupMessagesCollRef.document(mDocumentID)
                    .update(updates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "New member added to the group " +
                                    "(from addNewGroupMember())");

                            Toast.makeText(SendMessageActivity.this,
                                    fullName + " has been added to the group!",
                                    Toast.LENGTH_LONG).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Failed to add new member to the group " +
                            "(from addNewGroupMember())", e);

                    Toast.makeText(SendMessageActivity.this,
                            "Could not add " + fullName + " to the group, please try again.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Changes the group name for the the {@link #mDocumentID} document in the
     * {@link FirestoreReferences#GROUP_MESSAGES_PATH} collection.
     *
     * @param name The new name of the group
     */
    private void changeGroupName(String name) {
        if (mDocumentID != null) {
            groupMessagesCollRef.document(mDocumentID)
                    .update(FirestoreReferences.ROOM_NAME_FIELD, name)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Group name successfully updated " +
                                    "(from changeGroupName())");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating group name " +
                                    "(from changeGroupName()): " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Get the input Users enter when they click on the send Button and put it in the
     * {@link FirestoreReferences#MESSAGES_PATH} sub-collection of the
     * {@link FirestoreReferences#GROUP_MESSAGES_PATH} collection or
     * {@link FirestoreReferences#TWO_PERSON_ROOMS_PATH} collection.
     */
    private void addMessageToDatabase() {
        String message = mMessageEditText.getText().toString().trim();

        if (message != null && message.isEmpty()) {
            return;
        }

        if (user != null && mDocumentID != null && mCollRefMessageRoom != null) {
            Log.d(TAG, "User input (from addMessageToDatabase()): " + message);

            String sender = user.getDisplayName();
            UserMessage data = new UserMessage(message, sender);

            mLatestMessage = message;
            mLatestMessageSender = sender;

            // Two person room messages
            if (mCollRefMessageRoom.equals(FirestoreReferences.TWO_PERSON_ROOMS_PATH)
                    && mFriendUid != null) {
                data.setReceiverUid(mFriendUid);
                db.collection(FirestoreReferences.TWO_PERSON_ROOMS_PATH)
                        .document(mDocumentID)
                        .collection(FirestoreReferences.MESSAGES_PATH)
                        .add(data)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, "New Two Person Message added " +
                                        "(from addMessageToDatabase()): " +
                                        documentReference.getId());
                                mMessageEditText.getText().clear();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding new Two Person Message " +
                                        "(from addMessageToDatabase()): ", e);
                            }
                        });
            }
            // Group room messages
            else if (mCollRefMessageRoom.equals(FirestoreReferences.GROUP_MESSAGES_PATH)) {
                groupMessagesCollRef.document(mDocumentID)
                        .collection(FirestoreReferences.MESSAGES_PATH)
                        .add(data)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, "New Group Message added: " +
                                        "(from addMessageToDatabase()): " +
                                        documentReference.getId());
                                mMessageEditText.getText().clear();

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding new Group Message " +
                                        "(from addMessageToDatabase()): ", e);
                            }
                        });
            }
        }
    }
}
