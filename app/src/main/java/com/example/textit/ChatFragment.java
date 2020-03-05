package com.example.textit;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Fragment that shows all the chats (User + another User) of the logged in User in a RecyclerView.
 * Each item in the RecyclerView shows the name of the other person, the last message in the chat,
 * and the date it was sent. User can click on an item in the RecyclerView to open that particular
 * chat message room.
 */
public class ChatFragment extends Fragment {
    // Log tag and intent extras
    private static final String TAG = ChatFragment.class.getName();
    public static final String DOCUMENT_ID_EXTRA = TAG + ".DOCUMENT_ID";
    public static final String CHAT_PARTNER_EXTRA = TAG + ".CHAT_PARTNER";
    public static final String FRIEND_PROFILE_URL_EXTRA = TAG + ".FRIEND_PROFILE_URL";
    public static final String FRIEND_UID_EXTRA = TAG + ".FRIEND_UID";

    // Firebase and Firestore variables
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private CollectionReference twoPersonCollRef =
            db.collection(FirestoreReferences.TWO_PERSON_ROOMS_PATH);

    // Views
    private RecyclerView recyclerView;
    TextView mNoMessagesTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.chat_fragment, container, false);

        // Set views
        recyclerView = rootView.findViewById(R.id.chat_recycler_view);
        mNoMessagesTextView = rootView.findViewById(R.id.no_messages_chat_fragment);
        CircleImageView mProfilePicImgView = rootView
                .findViewById(R.id.profile_img_view_chat_fragment);

        // Open the Settings activity when Users click on the CircleImageView on the top left of
        // the screen.
        mProfilePicImgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), SettingsActivity.class));
            }
        });

        // Load User's profile image into the CircleImageView
        if (user != null && user.getPhotoUrl() != null) {
            Picasso
                    .get()
                    .load(user.getPhotoUrl())
                    .into(mProfilePicImgView);
        }

        setChatsToRecycler();

        return rootView;
    }

    /**
     * Queries the database for all the chat (User + another User) rooms of the logged in User is
     * and puts them in the RecyclerView. RecyclerView automatically updates UI whenever any of the
     * queried data changes, if that change is needed to be applied to the UI.
     */
    private void setChatsToRecycler() {
        if (user != null) {
            String userUid = user.getUid();

            Query query = twoPersonCollRef
                    .whereArrayContains(FirestoreReferences.MEMBER_LIST_FIELD, userUid)
                    .orderBy(FirestoreReferences.LATEST_MSG_SENT_FIELD, Query.Direction.DESCENDING);

            FirestoreRecyclerOptions<TwoPersonRoomModel> options =
                    new FirestoreRecyclerOptions.Builder<TwoPersonRoomModel>()
                            .setQuery(query, TwoPersonRoomModel.class)
                            .setLifecycleOwner(ChatFragment.this)
                            .build();

            final ChatAdapter chatAdapter = new ChatAdapter(options) {
                @Override
                public void onDataChanged() {
                    super.onDataChanged();

                    // Show the 'No messages' TextView if the User doesn't have any messages
                    if (getItemCount() < 1) {
                        mNoMessagesTextView.setVisibility(View.VISIBLE);
                    } else {
                        mNoMessagesTextView.setVisibility(View.INVISIBLE);
                    }
                }
            };

            recyclerView.setHasFixedSize(true);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerView.setAdapter(chatAdapter);

            // Set onClickListener to the adapter so Users can open that specific chat activity.
            chatAdapter.setOnItemClickListener(new ChatAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(DocumentSnapshot snapshot, int position) {
                    TwoPersonRoomModel data = snapshot.toObject(TwoPersonRoomModel.class);

                    if (user != null && data.getDisplay_names() != null) {
                        String userUid = user.getUid();
                        String valueFriendDisplayName = null;
                        String keyFriendUid = null;
                        String friendProfileUrl = null;

                        // Get the friend's uid from the appropriate field
                        for (Map.Entry<String, Object> entry : data.getDisplay_names().entrySet()) {
                            keyFriendUid = entry.getKey();

                            if (!keyFriendUid.equals(userUid)) {
                                valueFriendDisplayName = entry.getValue().toString();
                                break;
                            }
                        }

                        if (data.getProfile_urls().get(keyFriendUid) != null) {
                            friendProfileUrl = data.getProfile_urls().get(keyFriendUid).toString();
                        }

                        openMessageActivity(snapshot.getId(), valueFriendDisplayName,
                                friendProfileUrl, keyFriendUid);
                    }
                }
            });

            // Display dialog box when User swipes left to delete a conversation
            RecyclerItemTouchHelper helper = new RecyclerItemTouchHelper(0,
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                    ChatAdapter.ChatHolder.class.getName(),
                    new RecyclerItemTouchHelper.RecyclerItemTouchHelperListener() {
                        @Override
                        public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction,
                                             int position) {
                            if (direction == ItemTouchHelper.LEFT) {
                                openDialog(chatAdapter.getDocument(position),
                                        chatAdapter.getUidList(position));
                                chatAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                            }
                        }
                    });

            new ItemTouchHelper(helper).attachToRecyclerView(recyclerView);
        }
    }

    /**
     * Opens the dialog when User swipes left to delete a conversation history to get their final
     * confirmation before performing the delete action.
     *
     * @param docID         Document ID of the chat room User clicked on.
     * @param memberUidList List of the two member's uid.
     */
    private void openDialog(final String docID, final List<String> memberUidList) {
        // Setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.string_title_delete_convo);
        builder.setMessage(R.string.string_body_delete_chat);

        // Set negative button
        builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "onClick: cancel - " + docID);
            }
        });

        // Set positive button
        builder.setPositiveButton(R.string.string_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "onClick: delete - " + docID);
                deleteSelectedConversation(docID, memberUidList);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Removes both Users from the Chat Room the User has selected and replaces the rooms data
     * fields with information regarding the room being deleted.
     *
     * @param docID         Chat room Document ID to delete the Users from.
     * @param memberUidList List of the two member's uid.
     */
    private void deleteSelectedConversation(final String docID, final List<String> memberUidList) {
        if (user != null) {
            Map<String, Object> newRoomInfo = new HashMap<>();

            newRoomInfo.put(FirestoreReferences.DELETED_BY_FIELD, user.getUid());
            newRoomInfo.put(FirestoreReferences.ROOM_DELETED_DATE_FIELD, FieldValue.serverTimestamp());
            newRoomInfo.put(FirestoreReferences.PREVIOUS_MEMBERS_FIELD, memberUidList);

            db.collection(FirestoreReferences.TWO_PERSON_ROOMS_PATH).document(docID)
                    .set(newRoomInfo)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Chat Room " + docID + " deleted!");
                            Toast.makeText(getContext(), "Conversation history deleted!",
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error deleting document", e);
                            Toast.makeText(getContext(),
                                    "Something went wrong, please try again later.!",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    /**
     * Opens the messages activity whenever User clicks on any of the chat items in the RecyclerView.
     * The Document ID for the chat room is passed in through an Intent to the
     *
     * @param Id        The chat room ID
     * @param name      Display name of the friend
     * @param url       Profile Url of the friend
     * @param friendUid Uid of the friend
     * @see SendMessageActivity along with the message partner.
     */
    private void openMessageActivity(String Id, String name, String url, String friendUid) {
        Intent intent = new Intent(getActivity(), SendMessageActivity.class);

        Log.d(TAG, "Two Person Chat Rooms ID (from openMessageActivity()): " + Id);

        intent.putExtra(DOCUMENT_ID_EXTRA, Id);
        intent.putExtra(CHAT_PARTNER_EXTRA, name);
        intent.putExtra(FRIEND_PROFILE_URL_EXTRA, url);
        intent.putExtra(FRIEND_UID_EXTRA, friendUid);
        intent.putExtra(ActivityConstants.CALLING_ACTIVITY_EXTRA, ActivityConstants.CHAT_FRAGMENT);

        startActivity(intent);
    }
}
