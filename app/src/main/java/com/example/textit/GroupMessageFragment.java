package com.example.textit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * Fragment shows all the group chats of Users in a RecyclerView. Users can click on an item in the
 * RecyclerView to open that particular group message chat room.
 */
public class GroupMessageFragment extends Fragment {
    // Log tag and intent extras
    private static final String TAG = GroupMessageFragment.class.getName();
    public static final String DOCUMENT_ID_EXTRA = TAG + ".DOCUMENT_ID";
    public static final String GROUP_ROOM_NAME_EXTRA = TAG + ".GROUP_ROOM_NAME";
    public static final String GROUP_ROOM_PIC_EXTRA = TAG + ".GROUP_ROOM_PIC";

    // Firebase and Firestore variables
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    // Views
    private RecyclerView recyclerView;
    private TextView mNoMessagesTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.group_message_fragment, container, false);

        // Set Views
        recyclerView = rootView.findViewById(R.id.group_message_recycler_view);
        mNoMessagesTextView = rootView.findViewById(R.id.no_messages_group_message_fragment);

        setMessagesToRecycler();

        return rootView;
    }

    /**
     * Queries the database for all the group chats the logged in User is part of and puts the chat
     * rooms in the RecyclerView. RecyclerView automatically updates UI whenever any of the queried
     * data changes, if that change is needed to be applied to the UI.
     */
    private void setMessagesToRecycler() {
        if (user != null) {
            String userUid = user.getUid();

            Query query = db.collection(FirestoreReferences.GROUP_MESSAGES_PATH)
                    .whereArrayContains(FirestoreReferences.MEMBERS_FIELD, userUid)
                    .orderBy(FirestoreReferences.LATEST_MSG_SENT_FIELD, Query.Direction.DESCENDING);

            FirestoreRecyclerOptions<GroupRoomModel> options =
                    new FirestoreRecyclerOptions.Builder<GroupRoomModel>()
                            .setQuery(query, GroupRoomModel.class)
                            .setLifecycleOwner(GroupMessageFragment.this)
                            .build();

            GroupMessageAdapter mGroupMessageAdapter = new GroupMessageAdapter(options) {
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
            recyclerView.setAdapter(mGroupMessageAdapter);

            // Set onClickListener to the adapter so Users can open that specific chat activity.
            mGroupMessageAdapter.setOnItemClickListener(new GroupMessageAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(DocumentSnapshot snapshot, int position) {
                    openMessageActivity(snapshot.getId(),
                            snapshot.get(FirestoreReferences.ROOM_NAME_FIELD),
                            snapshot.get(FirestoreReferences.ROOM_PICTURE_FIELD));
                }
            });

        }
    }

    /**
     * Opens the messages activity whenever User clicks on any of the group message items in the
     * RecyclerView.
     *
     * @param Id        The Document ID for the group room
     * @param groupName Name of the group
     * @see SendMessageActivity
     */
    public void openMessageActivity(String Id, Object groupName, Object roomPhotoUrl) {
        Intent intent = new Intent(getActivity(), SendMessageActivity.class);
        String name = null;
        String url = null;

        if (groupName != null) {
            name = groupName.toString();
        }

        if (roomPhotoUrl != null) {
            url = roomPhotoUrl.toString();
        }

        intent.putExtra(DOCUMENT_ID_EXTRA, Id);
        intent.putExtra(GROUP_ROOM_NAME_EXTRA, name);
        intent.putExtra(GROUP_ROOM_PIC_EXTRA, url);
        intent.putExtra(ActivityConstants.CALLING_ACTIVITY_EXTRA, ActivityConstants.GROUP_FRAGMENT);

        startActivity(intent);
    }
}
