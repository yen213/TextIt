package com.example.textit;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
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
import com.google.firebase.firestore.WriteBatch;

/**
 * Activity where Users can see a list of all their friend requests, if any, and choose
 * to either accept or decline the request.
 */
public class FriendRequestsActivity extends AppCompatActivity {
    // Log tag
    private static final String TAG = FriendRequestsActivity.class.getName();

    // Firebase  and Firestore variables
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private CollectionReference friendListCollRef =
            db.collection(FirestoreReferences.FRIEND_LIST_PATH);

    // Views
    private RecyclerView mFriendReqRecyclerView;
    private TextView mNoFriendReqsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_requests_activity);

        // Set views
        mFriendReqRecyclerView = findViewById(R.id.recycler_view_friend_req_activity);
        mNoFriendReqsTextView = findViewById(R.id.no_friend_reqs_friend_req_fragment);

        setReqsToRecyclerView();
    }

    /**
     * Queries the database for the logged in User's friend requests and puts them in the
     * RecyclerView. RecyclerView automatically updates UI whenever any of the queried data changes,
     * if that change is needed to be applied to the UI.
     */
    private void setReqsToRecyclerView() {
        if (user != null) {
            String userUid = user.getUid();
            Query query = friendListCollRef.document(userUid)
                    .collection(FirestoreReferences.FRIEND_REQUESTS_PATH)
                    .orderBy(FirestoreReferences.SENT_FIELD, Query.Direction.ASCENDING);

            FirestoreRecyclerOptions<FriendsModel> options =
                    new FirestoreRecyclerOptions.Builder<FriendsModel>()
                            .setQuery(query, FriendsModel.class)
                            .setLifecycleOwner(FriendRequestsActivity.this)
                            .build();

            FriendRequestAdapter mFriendRequestAdapter = new FriendRequestAdapter(options) {
                @Override
                public void onDataChanged() {
                    super.onDataChanged();

                    // Show the 'No friend reqs' TextView if the User doesn't have any friend reqs
                    if (getItemCount() < 1) {
                        mNoFriendReqsTextView.setVisibility(View.VISIBLE);
                    } else {
                        mNoFriendReqsTextView.setVisibility(View.INVISIBLE);
                    }
                }
            };

            mFriendReqRecyclerView.setHasFixedSize(true);
            mFriendReqRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mFriendReqRecyclerView.setAdapter(mFriendRequestAdapter);

            // Set onClickListener to the Accept and Decline buttons
            mFriendRequestAdapter.setOnItemClickListener(new FriendRequestAdapter.OnItemClickListener() {
                @Override
                public void onAcceptClick(DocumentSnapshot snapshot) {
                    FriendsModel data = snapshot.toObject(FriendsModel.class);

                    if (user != null && data.getEmail() != null && data.getUid() != null) {
                        acceptFriendRequest(data.getEmail(), data.getUid(), snapshot.getId());
                    }
                }

                @Override
                public void onDeclineClick(DocumentSnapshot snapshot) {
                    deleteFriendRequest(snapshot.getId(), false);
                }
            });
        }
    }

    /**
     * Accepts the selected friend request and adds both Users' data in the appropriate documents
     * in the {@link FirestoreReferences#FRIEND_LIST_PATH} collection and calls
     * to {@link #deleteFriendRequest(String, boolean)} to delete the request document afterwards.
     *
     * @param friendEmail Email of the friend requester
     * @param friendUid   Uid of the friend requester
     * @param reqDocID    Document ID of the friend request.
     */
    private void acceptFriendRequest(final String friendEmail, final String friendUid,
                                     final String reqDocID) {
        if (user != null) {
            String userUid = user.getUid();
            WriteBatch batch = db.batch();
            DocumentReference docRef;

            // Add data to the User's friend list
            docRef = friendListCollRef.document(userUid);
            batch.update(docRef, FirestoreReferences.FRIENDS_FIELD, FieldValue.arrayUnion(friendUid));
            batch.update(docRef, FirestoreReferences.FRIEND_EMAILS_FIELD,
                    FieldValue.arrayUnion(friendEmail));
            batch.update(docRef, FirestoreReferences.FRIENDS_SINCE_FIELD + "." + friendUid,
                    FieldValue.serverTimestamp());

            // Add data to the Requester's friend list
            docRef = friendListCollRef.document(friendUid);
            batch.update(docRef, FirestoreReferences.FRIENDS_FIELD, FieldValue.arrayUnion(userUid));
            batch.update(docRef, FirestoreReferences.FRIEND_EMAILS_FIELD,
                    FieldValue.arrayUnion(user.getEmail()));
            batch.update(docRef, FirestoreReferences.FRIENDS_SINCE_FIELD + "." + userUid,
                    FieldValue.serverTimestamp());

            // Commit the batch
            batch.commit()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(FriendRequestsActivity.this,
                                    "Added new friend to friend's list!",
                                    Toast.LENGTH_LONG).show();

                            Log.d(TAG, "Successfully added friends to each other's documents: "
                                    + "UID - " + friendUid + "\tEmail - " + friendEmail);

                            deleteFriendRequest(reqDocID, true);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Unable to accept friend request: " + e.getMessage());

                            Toast.makeText(FriendRequestsActivity.this,
                                    "Something went wrong, please try again later.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    /**
     * Deletes the selected friend request document from the
     * {@link FirestoreReferences#FRIEND_REQUESTS_PATH} collection.
     *
     * @param docID           ID of the document to delete from the collection
     * @param requestAccepted Checks if the call is
     *                        from {@link #acceptFriendRequest(String, String, String)} and doesn't
     *                        show the toast message since User accepted friend request and the
     *                        document is just being deleted then.
     */
    private void deleteFriendRequest(String docID, final boolean requestAccepted) {
        if (user != null) {
            friendListCollRef.document(user.getUid())
                    .collection(FirestoreReferences.FRIEND_REQUESTS_PATH).document(docID)
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (!requestAccepted) {
                                Toast.makeText(FriendRequestsActivity.this,
                                        "Friend request declined!", Toast.LENGTH_LONG).show();
                            }

                            Log.d(TAG, "Friend Request successfully deleted");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error deleting friend request: ", e);
                        }
                    });
        }
    }
}
