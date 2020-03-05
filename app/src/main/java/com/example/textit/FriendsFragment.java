package com.example.textit;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
 * Fragment shows all the friends of Users in a RecyclerView. Each item in the RecyclerView shows
 * the name of the friend and their profile picture. User can click on an item in the RecyclerView
 * to open an info screen for that particular friend.
 */
public class FriendsFragment extends Fragment implements View.OnClickListener {
    // Log tag and intent extra
    private static final String TAG = FriendsFragment.class.getName();
    public static final String DOCUMENT_ID_EXTRA = TAG + ".DOCUMENT_ID";

    // Firebase and Firestore variables
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private CollectionReference friendListCollRef =
            db.collection(FirestoreReferences.FRIEND_LIST_PATH);

    // Views
    private RecyclerView mFriendsRecyclerView;
    private FloatingActionButton mMainFAB, mSearchFAB, mRequestsFAB;
    private TextView mSearchTextView, mRequestsTextView, mNoFriendsTextView;

    // Member variables
    private Animation mCloseAnim, mOpenAnim, mCounterAnim, mClockAnim;
    private boolean mOpened = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.friends_fragment, container, false);

        // Set Views and Animations
        mFriendsRecyclerView = rootView.findViewById(R.id.recycler_view_friends_fragment);
        mMainFAB = rootView.findViewById(R.id.fab_start_fab);
        mSearchFAB = rootView.findViewById(R.id.fab_search_fab);
        mRequestsFAB = rootView.findViewById(R.id.fab_friend_req_fab);
        mCloseAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.close_fab);
        mOpenAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.open_fab);
        mCounterAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_fab_counter_clock);
        mClockAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_fab_clock);
        mSearchTextView = rootView.findViewById(R.id.search_txt_view_fab);
        mRequestsTextView = rootView.findViewById(R.id.friend_req_txt_view_fab);
        mNoFriendsTextView = rootView.findViewById(R.id.no_friends_friends_fragment);

        // Set listeners
        mMainFAB.setOnClickListener(this);
        mSearchFAB.setOnClickListener(this);
        mRequestsFAB.setOnClickListener(this);

        setFriendsToRecycler();

        return rootView;
    }

    /**
     * Queries the database for logged in User's friends and puts them in the RecyclerView.
     * RecyclerView automatically updates UI whenever any of the queried data changes, if that
     * change is needed to be applied to the UI.
     */
    private void setFriendsToRecycler() {
        if (user != null) {
            Query query = friendListCollRef
                    .whereArrayContains(FirestoreReferences.FRIENDS_FIELD, user.getUid())
                    .orderBy(FirestoreReferences.NAME_FIELD, Query.Direction.ASCENDING);

            FirestoreRecyclerOptions<FriendsModel> options =
                    new FirestoreRecyclerOptions.Builder<FriendsModel>()
                            .setQuery(query, FriendsModel.class)
                            .setLifecycleOwner(FriendsFragment.this)
                            .build();

            final FriendsAdapter mFriendsAdapter = new FriendsAdapter(options) {
                @Override
                public void onDataChanged() {
                    super.onDataChanged();

                    // Show the 'No friends' TextView if the User doesn't have any friends
                    if (getItemCount() < 1) {
                        mNoFriendsTextView.setVisibility(View.VISIBLE);
                    } else {
                        mNoFriendsTextView.setVisibility(View.INVISIBLE);
                    }
                }
            };

            mFriendsRecyclerView.setHasFixedSize(true);
            mFriendsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mFriendsRecyclerView.setAdapter(mFriendsAdapter);

            // Set onClickListener to the adapter so Users can open that specific friends info screen.
            mFriendsAdapter.setOnItemClickListener(new FriendsAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(DocumentSnapshot snapshot, int position) {
                    openUserInfoActivity(snapshot.getId());
                }
            });

            // Display dialog box when User swipes left to delete a friend
            RecyclerItemTouchHelper helper = new RecyclerItemTouchHelper(0,
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                    FriendsAdapter.FriendsHolder.class.getName(),
                    new RecyclerItemTouchHelper.RecyclerItemTouchHelperListener() {
                        @Override
                        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction,
                                             int position) {
                            if (direction == ItemTouchHelper.LEFT) {
                                openDialog(mFriendsAdapter.getDocument(position),
                                        mFriendsAdapter.getFriendName(position),
                                        mFriendsAdapter.getFriendEmail(position));
                                mFriendsAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                            }
                        }
                    });

            new ItemTouchHelper(helper).attachToRecyclerView(mFriendsRecyclerView);
        }
    }

    /**
     * Opens a dialog box when Users swipe left to delete a friend from their friend list. Gets
     * Users' final confirmation before performing the delete action.
     *
     * @param docID       Document ID of the chat room Users clicked on.
     * @param friendName  Name of the friend
     * @param friendEmail Email of the friend
     */
    private void openDialog(final String docID, final String friendName, final String friendEmail) {
        // Setup the alert builder
        String message = "Are you sure you want to delete " + friendName + " from your friends list?";
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.string_title_delete_friend);
        builder.setMessage(message);

        // Set negative button
        builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "onClick: cancel - " + docID + "\t" + friendName);
            }
        });

        // Set positive button
        builder.setPositiveButton(R.string.string_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "onClick: delete - " + docID + "\t" + friendName);
                deleteFriend(friendName, friendEmail, docID);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Deletes the appropriate data from both Users' documents in the
     * {@link FirestoreReferences#FRIEND_LIST_PATH} collection.
     *
     * @param friendName  Name of the friend
     * @param friendEmail Email of the friend.
     * @param friendUid   The uid of the friend.
     */
    private void deleteFriend(final String friendName, String friendEmail, String friendUid) {
        if (user != null) {
            String userUid = user.getUid();
            String userEmail = user.getEmail();
            WriteBatch batch = db.batch();

            // Delete friend from the User's friend list
            DocumentReference docRef;
            docRef = friendListCollRef.document(userUid);
            batch.update(docRef, FirestoreReferences.FRIEND_EMAILS_FIELD,
                    FieldValue.arrayRemove(friendEmail));
            batch.update(docRef, FirestoreReferences.FRIENDS_FIELD,
                    FieldValue.arrayRemove(friendUid));
            batch.update(docRef, FirestoreReferences.FRIENDS_SINCE_FIELD + "." + friendUid,
                    FieldValue.delete());

            // Delete the User from the friend's friend list
            docRef = friendListCollRef.document(friendUid);
            batch.update(docRef, FirestoreReferences.FRIEND_EMAILS_FIELD,
                    FieldValue.arrayRemove(userEmail));
            batch.update(docRef, FirestoreReferences.FRIENDS_FIELD,
                    FieldValue.arrayRemove(userUid));
            batch.update(docRef, FirestoreReferences.FRIENDS_SINCE_FIELD + "." + userUid,
                    FieldValue.delete());

            // Commit the batch
            batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Both Users deleted from each others' friend list.");
                        Toast.makeText(getContext(),
                                "You and " + friendName + " are no longer friends.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Log.d(TAG, "Unable to delete User's from friend list: ",
                                task.getException());
                        Toast.makeText(getContext(),
                                "Something went wrong, please try again.",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    /**
     * Opens the User info screen whenever Users click on any of their friends in the RecyclerView.
     * The DocumentSnapshot ID is passed in through an Intent to the User info screen.
     *
     * @param friendUid The friend's uid
     * @see DisplayUserInfoActivity
     */
    private void openUserInfoActivity(String friendUid) {
        Intent intent = new Intent(getContext(), DisplayUserInfoActivity.class);

        Log.d(TAG, "Document ID from FriendsFragment onClick Adapter:" + friendUid);

        intent.putExtra(DOCUMENT_ID_EXTRA, friendUid);

        startActivity(intent);
    }

    /**
     * OnClick triggered when one of the 3 FABs are clicked on by the User
     *
     * @param v FAB that was clicked on
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            // Opens or closes the other two FABs
            case R.id.fab_start_fab:
                activateFABs();
                break;

            // Opens the activity where the Users can look up other Users
            case R.id.fab_search_fab:
                startActivity(new Intent(getContext(), DisplayUserInfoActivity.class));
                break;

            // Opens the activity with the friend requests of the Users
            case R.id.fab_friend_req_fab:
                startActivity(new Intent(getContext(), FriendRequestsActivity.class));
                break;
        }
    }

    /**
     * Set up the FAB buttons
     */
    private void activateFABs() {
        if (mOpened) {
            mSearchTextView.setVisibility(View.INVISIBLE);
            mRequestsTextView.setVisibility(View.INVISIBLE);
            mSearchFAB.startAnimation(mCloseAnim);
            mRequestsFAB.startAnimation(mCloseAnim);
            mMainFAB.startAnimation(mCounterAnim);
            mSearchFAB.setClickable(false);
            mRequestsFAB.setClickable(false);
            mOpened = false;
        } else {
            mSearchTextView.setVisibility(View.VISIBLE);
            mRequestsTextView.setVisibility(View.VISIBLE);
            mSearchFAB.startAnimation(mOpenAnim);
            mRequestsFAB.startAnimation(mOpenAnim);
            mMainFAB.startAnimation(mClockAnim);
            mSearchFAB.setClickable(true);
            mRequestsFAB.setClickable(true);
            mOpened = true;
        }
    }
}
