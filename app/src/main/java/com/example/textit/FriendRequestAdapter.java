package com.example.textit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter for FriendRequestActivity that inherits from {@link FirestoreRecyclerOptions} to listen
 * to changes in the database and updates the UI accordingly.
 *
 * @see FriendRequestsActivity
 */
public class FriendRequestAdapter extends FirestoreRecyclerAdapter<FriendsModel,
        FriendRequestAdapter.FriendRequestHolder> {
    private OnItemClickListener mListener;

    public FriendRequestAdapter(@NonNull FirestoreRecyclerOptions<FriendsModel> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull FriendRequestAdapter.FriendRequestHolder holder,
                                    int position, @NonNull FriendsModel model) {
        // Get the friend request sent date
        if (model.getSent() != null) {
            String sentDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(model.getSent());
            sentDate = "Sent on " + sentDate;

            holder.sentTextView.setText(sentDate);
        } else {
            holder.sentTextView.setText(" ");
        }

        // Get the name of the User sending the friend request
        if (model.getName() != null) {
            holder.nameTextView.setText(model.getName());
        } else {
            holder.nameTextView.setText(model.getName());
        }

        // Load the User's profile picture into the CircleImageView, if available
        if (model.getProfilePictureUrl() != null) {
            Picasso
                    .get()
                    .load(model.getProfilePictureUrl())
                    .into(holder.pictureCircleImageView);
        }
    }

    @NonNull
    @Override
    public FriendRequestHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_req_list_item,
                parent, false);

        return new FriendRequestHolder(view);
    }

    /**
     * ViewHolder class to set the views and implement onClickListener to the accept and decline
     * buttons
     */
    public class FriendRequestHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, sentTextView;
        CircleImageView pictureCircleImageView;
        Button acceptButton, declineButton;

        public FriendRequestHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name_txt_view_friend_req_list);
            pictureCircleImageView = itemView.findViewById(R.id.picture_img_view_friend_req_list);
            sentTextView = itemView.findViewById(R.id.sent_txt_view_friend_req_list);
            acceptButton = itemView.findViewById(R.id.accept_button_friend_req_list);
            declineButton = itemView.findViewById(R.id.decline_button_friend_req_list);

            // Set onClick listener to the accept button
            acceptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();

                    if (position != RecyclerView.NO_POSITION && mListener != null) {
                        mListener.onAcceptClick(getSnapshots().getSnapshot(position));
                    }
                }
            });

            // Set onClick listener to the decline button
            declineButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();

                    if (position != RecyclerView.NO_POSITION && mListener != null) {
                        mListener.onDeclineClick(getSnapshots().getSnapshot(position));
                    }
                }
            });
        }
    }


    /**
     * Interface for adding the onClickListener to the accept and decline buttons.
     */
    public interface OnItemClickListener {
        void onAcceptClick(DocumentSnapshot snapshot);

        void onDeclineClick(DocumentSnapshot snapshot);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }
}
