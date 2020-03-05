package com.example.textit;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Activity screen where a Users update their basic information. Additional database collections
 * are created here if a new User has navigated to this screen for the first time.
 */
public class UpdateInfoActivity extends AppCompatActivity implements View.OnClickListener {
    // Constants
    private static final String TAG = UpdateInfoActivity.class.getName();
    private static final String ERROR_MESSAGE = "Field cannot be empty";
    private static final int PICK_IMAGE_REQUEST = 1;

    // Firebase and Firestore variables
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private CollectionReference usersCollRef = db.collection(FirestoreReferences.USERS_PATH);
    private CollectionReference friendCollRef = db.collection(FirestoreReferences.FRIEND_LIST_PATH);
    private CollectionReference groupCollRef = db.collection(FirestoreReferences.GROUP_MESSAGES_PATH);
    private CollectionReference twoPersonCollRef =
            db.collection(FirestoreReferences.TWO_PERSON_ROOMS_PATH);
    private StorageReference mStorageRef;

    // Views
    private TextInputEditText mFNameTextInEditText, mLNameTextInEditText, mAgeTextInEditText;
    private TextInputLayout mFNameTextInLayout, mLNameTextInLayout, mAgeTextInLayout;
    private TextView mDateOfBirthTxtView;
    private CircleImageView mProfileCirImgView;
    private Button mSaveOrCreateButton;

    // User entered information to be put into the database and boolean for checking if Users who
    // opened this activity is a new User.
    private String mDateOfBirth;
    private int mAge;
    private Uri mUserChosenImageUri;
    private boolean mNewUser;

    // Date Picker Listener
    private DatePickerDialog.OnDateSetListener mDateSetListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_info_activity);

        setViews();

        // Set listeners
        mSaveOrCreateButton.setOnClickListener(this);
        mDateOfBirthTxtView.setOnClickListener(this);
        mProfileCirImgView.setOnClickListener(this);
        mFNameTextInEditText.addTextChangedListener(editTextWatcher);
        mLNameTextInEditText.addTextChangedListener(editTextWatcher);
        mAgeTextInEditText.addTextChangedListener(editTextWatcher);

        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month += 1;

                Log.d(TAG, month + "/" + dayOfMonth + "/" + year);

                mDateOfBirth = month + "/" + dayOfMonth + "/" + year;
                mDateOfBirthTxtView.setText(mDateOfBirth);
            }
        };

        mNewUser = getIntent().getBooleanExtra(ActivityConstants.NEW_USER_EXTRA, false);
        mStorageRef = FirebaseStorage.getInstance()
                .getReference(FirestoreReferences.USER_PROFILE_PICS_PATH);

        if (mNewUser) {
            mSaveOrCreateButton.setText(getResources().getString(R.string.string_create_account));
        }
    }

    /**
     * Called when a view with a click listener attached to it is clicked.
     *
     * @param v The view that was clicked on
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // Save the user information
            case R.id.save_button_update_info_screen:
                if (setInfo()) {
                    mFNameTextInLayout.setError(null);
                    mLNameTextInLayout.setError(null);
                    mAgeTextInLayout.setError(null);

                    if (mUserChosenImageUri == null && !mNewUser) {
                        finish();
                    } else if (mUserChosenImageUri != null && !mNewUser) {
                        uploadPictureToStorage();
                    } else if (mUserChosenImageUri == null && mNewUser) {
                        startActivity(new Intent(UpdateInfoActivity.this,
                                HomePageActivity.class));
                    } else if (mUserChosenImageUri != null && mNewUser) {
                        uploadPictureToStorage();
                    }
                }

                hideKeyboard();

                break;

            // Open the Date Picker Dialog
            case R.id.dob_txt_view_update_info_screen:
                displayDate();

                break;

            // Open file chooser so that Users can upload a picture
            case R.id.profile_img_view_update_info_screen:
                openFileChooser();

                break;
        }
    }

    /**
     * Function called when Users have selected a picture from their device's File Chooser and
     * puts that image inside the CircleImageView and saves the image Uri to
     * {@link #mUserChosenImageUri}.
     *
     * @param requestCode The picture upload request
     * @param resultCode  Result of the Intent
     * @param data        The picture data that is returned
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            mUserChosenImageUri = data.getData();

            Picasso
                    .get()
                    .load(mUserChosenImageUri)
                    .into(mProfileCirImgView);
        }
    }

    /**
     * Sets the Views from the XML layout to the member variables
     */
    private void setViews() {
        mFNameTextInEditText = findViewById(R.id.f_name_txt_in_edit_txt_update_info_screen);
        mFNameTextInLayout = findViewById(R.id.f_name_txt_in_layout_update_info_screen);
        mLNameTextInEditText = findViewById(R.id.l_name_txt_in_edit_txt_update_info_screen);
        mLNameTextInLayout = findViewById(R.id.l_name_txt_in_layout_update_info_screen);
        mAgeTextInEditText = findViewById(R.id.age_txt_in_edit_txt_update_info_screen);
        mAgeTextInLayout = findViewById(R.id.age_txt_in_layout_update_info_screen);
        mDateOfBirthTxtView = findViewById(R.id.dob_txt_view_update_info_screen);
        mProfileCirImgView = findViewById(R.id.profile_img_view_update_info_screen);
        mSaveOrCreateButton = findViewById(R.id.save_button_update_info_screen);

        // Load the User's picture if they have one
        if (user != null && user.getPhotoUrl() != null) {
            Picasso
                    .get()
                    .load(user.getPhotoUrl())
                    .into(mProfileCirImgView);
        }

        // Hide the keyboard when the DONE/SEND button is pressed
        mAgeTextInEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                        actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard();

                    return true;
                }

                return false;
            }
        });
    }

    /**
     * Hides the keyboard
     */
    private void hideKeyboard() {
        // Get the input method and the current focus
        InputMethodManager imm = (InputMethodManager) UpdateInfoActivity.this
                .getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = UpdateInfoActivity.this.getCurrentFocus();

        //If no view currently has focus, create a new one so we can grab a window token from it
        if (view == null) {
            view = new View(UpdateInfoActivity.this);
        }

        // Hide the keyboard
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    /**
     * Custom TextWatcher for the TextInputEditText fields. Used to check that all fields are not
     * empty before enabling the create account or save button.
     */
    private TextWatcher editTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String firstName = mFNameTextInEditText.getText().toString().trim();
            String lastName = mLNameTextInEditText.getText().toString().trim();
            String age = mAgeTextInEditText.getText().toString().trim();

            mSaveOrCreateButton.setEnabled(!firstName.isEmpty() && !lastName.isEmpty()
                    && !age.isEmpty());

            mFNameTextInLayout.setError(null);
            mLNameTextInLayout.setError(null);
            mAgeTextInLayout.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    /**
     * Checks that Users have entered all the required information. Sets ERROR_MESSAGES on views if
     * user leaves field(s) empty. Otherwise, updates User document in the
     * {@link FirestoreReferences#USERS_PATH} collection with the new information or creates a new
     * document in this collection if it is a new User.
     *
     * @return True if all required information was entered, false otherwise
     */
    private boolean setInfo() {
        String firstName = mFNameTextInEditText.getText().toString().trim();
        String lastName = mLNameTextInEditText.getText().toString().trim();
        String age = mAgeTextInEditText.getText().toString().trim();
        String fullName = firstName + " " + lastName;

        if (age != null && !age.isEmpty()) {
            mAge = Integer.parseInt(mAgeTextInEditText.getText().toString().trim());
        }

        if (firstName != null && firstName.isEmpty()) {
            mFNameTextInLayout.setError(ERROR_MESSAGE);

            return false;
        } else if (lastName != null && lastName.isEmpty()) {
            mLNameTextInLayout.setError(ERROR_MESSAGE);

            return false;
        } else if (age != null && age.isEmpty()) {
            mAgeTextInLayout.setError(ERROR_MESSAGE);

            return false;
        } else if (mDateOfBirth == null) {
            Toast.makeText(
                    UpdateInfoActivity.this,
                    "Please choose a Date of Birth", Toast.LENGTH_LONG).show();

            return false;
        }

        if (mNewUser) {
            addNewUserDocument(firstName, lastName);
        } else {
            updateExistingUserDocument(firstName, lastName);
        }

        updateUserDisplayName(fullName);

        return true;
    }

    /**
     * Adds the new User's information to a new document in the
     * {@link FirestoreReferences#USERS_PATH} collection.
     *
     * @param firstName First name of the new User
     * @param lastName  Last name of the new User
     */
    private void addNewUserDocument(String firstName, String lastName) {
        if (user != null) {
            String fullName = firstName + " " + lastName;
            String uid = user.getUid();
            String email = user.getEmail();
            FirestoreUserInfo newUserInfo = new FirestoreUserInfo(firstName, lastName,
                    mAge, mDateOfBirth, uid, email);

            usersCollRef.document(uid)
                    .set(newUserInfo)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "New User basic information doc added successfully!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error writing New User basic information doc: ", e);
                        }
                    });

            createFriendListPath(email, uid, fullName);
        }
    }

    /**
     * Create the {@link FirestoreReferences#FRIEND_LIST_PATH} collection in for new Users.
     *
     * @param uid   Collection's document ID.
     * @param name  Display name of the new User
     * @param email Email of the new User
     */
    private void createFriendListPath(String email, String uid, String name) {
        Map<String, String> data = new HashMap<>();

        data.put(FirestoreReferences.UID_FIELD, uid);
        data.put(FirestoreReferences.NAME_FIELD, name);
        data.put(FirestoreReferences.EMAIL_FIELD, email);
        data.put(FirestoreReferences.PROFILE_PIC_URL_FIELD, null);

        friendCollRef.document(uid)
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Created User's Friend List document");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Failed to create User's Friend List document: ", e);
                    }
                });
    }

    /**
     * Updates the existing User information in their document in the
     * {@link FirestoreReferences#USERS_PATH} collection.
     *
     * @param firstName Updated first name of the User
     * @param lastName  Updated last name of the User
     */
    private void updateExistingUserDocument(String firstName, String lastName) {
        if (user != null) {
            Map<String, Object> updatedInfo = new HashMap<>();

            updatedInfo.put(FirestoreReferences.FIRST_NAME_FIELD, firstName);
            updatedInfo.put(FirestoreReferences.LAST_NAME_FIELD, lastName);
            updatedInfo.put(FirestoreReferences.AGE_FIELD, mAge);
            updatedInfo.put(FirestoreReferences.DOB_FIELD, mDateOfBirth);

            usersCollRef.document(user.getUid())
                    .update(updatedInfo)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Existing User basic information updated successfully!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating existing User basic information: ", e);
                        }
                    });
        }
    }

    /**
     * Updates the display names of Users in Firebase and calls functions to update documents in
     * relevant collections.
     *
     * @param name New display name
     */
    private void updateUserDisplayName(final String name) {
        if (name != null) {
            Log.d(TAG, "User's new display name: " + name);

            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                if (!mNewUser) {
                                    updateFriendListName(name);
                                    getGroupChatRoomDocuments(name);
                                    getTwoPersonRoomDocuments(name);
                                }

                                Log.d(TAG, "User's profile display name updated.");
                            } else {
                                Log.d(TAG, "Failed to update User's profile display name: " +
                                        task.getException());
                            }
                        }
                    });
        }
    }

    /**
     * Update User display names in the {@link FirestoreReferences#NAME_FIELD} field in their
     * document in the {@link FirestoreReferences#FRIEND_LIST_PATH} collection.
     *
     * @param name New display name
     */
    private void updateFriendListName(String name) {
        if (user != null) {
            friendCollRef.document(user.getUid())
                    .update(FirestoreReferences.NAME_FIELD, name)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "User's display name updated in the Friend List doc");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Failed to update User's display name in the Friend " +
                                    "List doc: ", e);
                        }
                    });
        }
    }

    /**
     * Gets the document ID of all the group chats a User is a part of and calls
     * {@link #updateDisplayNameForGroups(List, String)} function to change their display name in
     * all those chats.
     *
     * @param name New display name
     */
    private void getGroupChatRoomDocuments(final String name) {
        if (user != null) {
            groupCollRef.whereArrayContains(FirestoreReferences.MEMBERS_FIELD, user.getUid())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<String> groupRoomIDs = new ArrayList<>();

                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    groupRoomIDs.add(document.getId());
                                }

                                Log.d(TAG, "User's groups: " + groupRoomIDs);

                                updateDisplayNameForGroups(groupRoomIDs, name);
                            } else {
                                Log.d(TAG, "Error getting User's group rooms: ",
                                        task.getException());
                            }
                        }
                    });
        }
    }

    /**
     * Gets the document ID of all the two person chat rooms a User is a part of and calls
     * {@link #updateDisplayNameForChatRooms(List, String)} function to change their display name
     * in all those chats.
     *
     * @param name New display name
     */
    private void getTwoPersonRoomDocuments(final String name) {
        if (user != null) {
            twoPersonCollRef.whereArrayContains(FirestoreReferences.MEMBER_LIST_FIELD, user.getUid())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<String> chatRooms = new ArrayList<>();

                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    chatRooms.add(document.getId());
                                }

                                Log.d(TAG, "User's chat rooms: " + chatRooms);

                                updateDisplayNameForChatRooms(chatRooms, name);
                            } else {
                                Log.d(TAG, "Error getting User's chat rooms: ",
                                        task.getException());
                            }
                        }
                    });
        }
    }

    /**
     * Updates User display name in all the groups they are a part of using batch writes.
     *
     * @param roomIDs Document ID of all the group rooms of User
     * @param name    New display name
     */
    private void updateDisplayNameForGroups(List<String> roomIDs, String name) {
        if (user != null && roomIDs != null && !roomIDs.isEmpty()) {
            WriteBatch batch = db.batch();
            String field = FirestoreReferences.DISPLAY_NAMES_FIELD + "." + user.getUid();

            for (String id : roomIDs) {
                DocumentReference docRef = groupCollRef.document(id);
                batch.update(docRef, field, name);
            }

            // Commit the batch
            batch.commit()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "User's display name in all their groups is updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Error updating the User's display name in all their " +
                                    "groups: " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Updates User display name in all the two person chat rooms they are in using batch writes.
     *
     * @param roomIDs Document ID of all the two person chat rooms of User
     * @param name    New display name
     */
    private void updateDisplayNameForChatRooms(List<String> roomIDs, String name) {
        if (user != null && roomIDs != null && !roomIDs.isEmpty()) {
            WriteBatch batch = db.batch();
            String field = FirestoreReferences.DISPLAY_NAMES_FIELD + "." + user.getUid();

            for (String id : roomIDs) {
                DocumentReference docRef = twoPersonCollRef.document(id);
                batch.update(docRef, field, name);
            }

            // Commit the batch
            batch.commit()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "User's display name in all their chat rooms is updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Error updating the User's display name in all their " +
                                    "chat rooms: " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Display's date picker to User so that they can select a date of birth.
     */
    private void displayDate() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                UpdateInfoActivity.this,
                android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                mDateSetListener,
                year, month, dayOfMonth);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    /**
     * Opens the File Chooser on User's. They can select a picture from there to upload into the app.
     */
    private void openFileChooser() {
        Intent intent = new Intent();

        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * Returns the extension of the image file User chose from their device.
     */
    private String getFileExtension(Uri uri) {
        ContentResolver cResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        return mime.getExtensionFromMimeType(cResolver.getType(uri));
    }

    /**
     * If User chose to upload a new picture, then upload it to the Firebase Storage. Then
     * update the {@link FirestoreReferences#PROFILE_PIC_URL_FIELD} fields in all the appropriate
     * documents in Firestore.
     */
    private void uploadPictureToStorage() {
        if (mUserChosenImageUri != null && user != null) {
            final StorageReference fileRef = mStorageRef.child(user.getUid()).child(
                    System.currentTimeMillis() + "." + getFileExtension(mUserChosenImageUri));

            fileRef.putFile(mUserChosenImageUri)
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

                        updateUserDocProfileFields(downloadUri.toString());
                        updateProfilePhoto(downloadUri);
                        updateFriendListPhoto(downloadUri.toString());

                        if (!mNewUser) {
                            getUserChatRooms(downloadUri.toString());
                        }

                        if (task.isComplete()) {
                            if (!mNewUser) {
                                finish();
                            } else {
                                // Open the home page after new User has uploaded their picture
                                startActivity(new Intent(UpdateInfoActivity.this,
                                        HomePageActivity.class));
                            }
                        }

                        Log.e(TAG, "Download Uri (from uploadPictureToStorage()): " +
                                downloadUri.toString());
                    } else {
                        Toast.makeText(UpdateInfoActivity.this, "Profile upload failed!",
                                Toast.LENGTH_SHORT).show();

                        Log.d(TAG, "Profile picture upload failed (from " +
                                "uploadPictureToStorage()): " + task.getException().getMessage());
                    }
                }
            });
        }
    }


    /**
     * Update User photo URI in Firebase.
     *
     * @param uri Download link to User profile picture
     */
    private void updateProfilePhoto(Uri uri) {
        if (uri != null) {
            Log.d(TAG, "updateProfilePhoto Uri: " + uri);

            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(uri)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User's profile photo Uri updated in Firebase.");
                            } else {
                                Log.d(TAG, "Failed to update User's profile photo Uri in " +
                                        "Firebase: " + task.getException());
                            }
                        }
                    });
        }
    }

    /**
     * Get the images download Uri and update the {@link FirestoreReferences#PROFILE_PIC_UPDATED_FIELD}
     * and {@link FirestoreReferences#PROFILE_PIC_URL_FIELD} for a User's document in the
     * {@link FirestoreReferences#USERS_PATH}} collection.
     *
     * @param uri Download Uri of the profile photo.
     */
    private void updateUserDocProfileFields(String uri) {
        if (user != null && uri != null) {
            Map<String, Object> update = new HashMap<>();

            update.put(FirestoreReferences.PROFILE_PIC_URL_FIELD, uri);
            update.put(FirestoreReferences.PROFILE_PIC_UPDATED_FIELD, FieldValue.serverTimestamp());

            usersCollRef.document(user.getUid())
                    .update(update)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "User's profile pic url updated " +
                                    "(from updateUserDocProfileFields())");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating User's profile pic: " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Get the images download Uri and update the {@link FirestoreReferences#PROFILE_PIC_UPDATED_FIELD}
     * and {@link FirestoreReferences#PROFILE_PIC_URL_FIELD} for a User's document in the
     * {@link FirestoreReferences#FRIEND_LIST_PATH}} collection.
     *
     * @param uri Download Uri of the image
     */
    private void updateFriendListPhoto(String uri) {
        if (user != null && uri != null) {
            Map<String, Object> update = new HashMap<>();

            update.put(FirestoreReferences.PROFILE_PIC_URL_FIELD, uri);
            update.put(FirestoreReferences.PROFILE_PIC_UPDATED_FIELD, FieldValue.serverTimestamp());

            friendCollRef.document(user.getUid())
                    .update(update)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "User's profile pic url updated in friend list" +
                                    " (from updateFriendListPhoto())");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating User's profile pic url in friend list: "
                                    + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Get a list of all the two person chat rooms of a User from the
     * {@link FirestoreReferences#TWO_PERSON_ROOMS_PATH} collection and pass the document IDs to the
     * {@link #updateUserChatRoomPhotos(List, String)} function so that their
     * profile picture is updated for all their chat rooms as well.
     *
     * @param uri Download Uri for the new profile photo of a User
     */
    private void getUserChatRooms(final String uri) {
        if (user != null) {
            twoPersonCollRef.whereArrayContains(FirestoreReferences.MEMBER_LIST_FIELD, user.getUid())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<String> rooms = new ArrayList<>();

                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    rooms.add(document.getId());
                                }

                                Log.d(TAG, "List of User's chat rooms: " + rooms);

                                updateUserChatRoomPhotos(rooms, uri);
                            } else {
                                Log.d(TAG, "Error getting User's chat rooms: " +
                                        task.getException());
                            }
                        }
                    });
        }
    }

    /**
     * Update the {@link FirestoreReferences#PROFILE_URLS_FIELD} field in all the two person chat
     * rooms User is a part of using batch writes.
     *
     * @param uri         The new profile photo download Uri
     * @param chatRoomIDs The list of all the two person chat rooms
     */
    private void updateUserChatRoomPhotos(List<String> chatRoomIDs, String uri) {
        if (user != null && chatRoomIDs != null && !chatRoomIDs.isEmpty()) {
            WriteBatch batch = db.batch();
            String field = FirestoreReferences.PROFILE_URLS_FIELD + "." + user.getUid();

            for (String id : chatRoomIDs) {
                DocumentReference docRef = twoPersonCollRef.document(id);
                batch.update(docRef, field, uri);
            }

            // Commit the batch
            batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "All User chat rooms updated with new profile photo" +
                                "(from updateUserChatRoomPhotos())");
                    } else {
                        Log.d(TAG, "Failed to update User chat rooms with new profile photo" +
                                " (from updateUserChatRoomPhotos()): " + task.getException());
                    }
                }
            });
        }
    }
}
