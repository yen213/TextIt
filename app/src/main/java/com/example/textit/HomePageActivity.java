package com.example.textit;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

/**
 * The home page that Users see when they are logged in. Activity has the navigation for the three
 * fragments in the application.
 *
 * @see ChatFragment
 * @see GroupMessageFragment
 * @see FriendsFragment
 */
public class HomePageActivity extends AppCompatActivity {
    // Log tag
    private static final String TAG = HomePageActivity.class.getName();

    // Firebase and Firestore variables
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private CollectionReference usersCollRef = db.collection(FirestoreReferences.USERS_PATH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page_activity);

        // Set the BottomNavigationView and attach a navigation listener to it
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_home_activity);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Get User's device registration token
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed: ", task.getException());
                            return;
                        }

                        String token = task.getResult().getToken();

                        Log.d(TAG, "Token: " + token);

                        checkRegistrationToken(token);
                    }
                });

        // Make the ChatFragment the home fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_home_activity, new ChatFragment())
                .commit();
    }

    /**
     * Do nothing when the back button is pressed from any one of the 3 fragments.
     */
    @Override
    public void onBackPressed() { }

    /**
     * Listener for the BottomNavigationView. Listens to the navigation item that is clicked on and
     * opens the appropriate Fragment Activity.
     */
    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    Fragment selectedFrag = null;

                    switch (menuItem.getItemId()) {
                        case R.id.chat_home:
                            selectedFrag = new ChatFragment();

                            break;

                        case R.id.group_message_home:
                            selectedFrag = new GroupMessageFragment();

                            break;

                        case R.id.friends_home:
                            selectedFrag = new FriendsFragment();

                            break;
                    }

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container_home_activity, selectedFrag)
                            .commit();

                    return true;
                }
            };

    /**
     * Checks Users' device registration token {@link FirestoreReferences#TOKEN_ID_FIELD} field in
     * their document in the {@link FirestoreReferences#USERS_PATH} collection to see if the current
     * registration token is in there, if not, call {@link #addDeviceRegistrationToken(String)} and
     * add the unsaved token to their document.
     */
    private void checkRegistrationToken(final String token) {
        if (user != null) {
            usersCollRef.document(user.getUid())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();

                                if (document.exists()) {
                                    FirestoreUserInfo data = document
                                            .toObject(FirestoreUserInfo.class);

                                    Log.d(TAG, "Tokens: " + data.getTokenIds());

                                    if (data.getTokenIds() != null && !data.getTokenIds().isEmpty()
                                            && !data.getTokenIds().contains(token)) {
                                        addDeviceRegistrationToken(token);
                                    } else if (data.getTokenIds() == null) {
                                        addDeviceRegistrationToken(token);
                                    }
                                } else {
                                    Log.d(TAG, "User doc doesn't exist");
                                }
                            } else {
                                Log.d(TAG, "Failed to get User doc: ", task.getException());
                            }
                        }
                    });
        }
    }

    /**
     * Adds the new registration token to the User's {@link FirestoreReferences#TOKEN_ID_FIELD} field
     * in their document in the {@link FirestoreReferences#USERS_PATH} collection.
     *
     * @param token User's new device registration token.
     */
    private void addDeviceRegistrationToken(String token) {
        if (user != null) {
            usersCollRef.document(user.getUid())
                    .update(FirestoreReferences.TOKEN_ID_FIELD, FieldValue.arrayUnion(token))
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "New token successfully added");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding new token: ", e);
                        }
                    });
        }
    }
}
