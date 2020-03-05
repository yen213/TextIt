package com.example.textit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter for FriendsFragment that inherits from {@link FirestoreRecyclerOptions} to listen to
 * changes in the database and update the UI accordingly.
 *
 * @see FriendsFragment
 */
public class FriendsAdapter extends FirestoreRecyclerAdapter<FriendsModel,
        FriendsAdapter.FriendsHolder> {
    private OnItemClickListener mListener;

    public FriendsAdapter(@NonNull FirestoreRecyclerOptions<FriendsModel> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull FriendsHolder holder, int position,
                                    @NonNull FriendsModel model) {
        // Get the friends name
        if (model.getName() != null) {
            holder.nameTextView.setText(model.getName());
        } else {
            holder.nameTextView.setText(" ");
        }

        // Get the friends email
        if (model.getEmail() != null) {
            holder.emailTextView.setText(model.getEmail());
        } else {
            holder.emailTextView.setText(" ");
        }

        // Get the friends profile picture and put in the CircleImageView, if available
        if (model.getProfilePictureUrl() != null) {
            Picasso
                    .get()
                    .load(model.getProfilePictureUrl())
                    .into(holder.pictureCircleImageView);
        }
    }

    @NonNull
    @Override
    public FriendsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friends_list_item,
                parent, false);

        return new FriendsHolder(view);
    }

    /**
     * Gets the Document Id from the selected item in the adapter
     *
     * @param position Position of Document in the adapter
     * @return Document ID
     */
    public String getDocument(int position) {
        return getSnapshots().getSnapshot(position).getReference().getId();
    }

    /**
     * Gets the Name of the friend from the selected item in the adapter
     *
     * @param position Position of Document in the adapter
     * @return Name of the friend
     */
    public String getFriendName(int position) {
        return getSnapshots().getSnapshot(position).get(FirestoreReferences.NAME_FIELD).toString();
    }

    /**
     * Gets the Email of the friend from the selected item in the adapter
     *
     * @param position Position of Document in the adapter
     * @return Email of the friend
     */
    public String getFriendEmail(int position) {
        return getSnapshots().getSnapshot(position).get(FirestoreReferences.EMAIL_FIELD).toString();
    }

    /**
     * ViewHolder class to set the views and implement onClickListener to the adapter items
     */
    public class FriendsHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, emailTextView;
        CircleImageView pictureCircleImageView;
        RelativeLayout backgroundView, foregroundView;

        public FriendsHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name_txt_view_friend_list);
            emailTextView = itemView.findViewById(R.id.email_txt_view_friend_list);
            pictureCircleImageView = itemView.findViewById(R.id.picture_img_view_friend_list);
            backgroundView = itemView.findViewById(R.id.background_view_friend_list);
            foregroundView = itemView.findViewById(R.id.foreground_view_friend_list);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();

                    if (position != RecyclerView.NO_POSITION && mListener != null) {
                        mListener.onItemClick(getSnapshots().getSnapshot(position), position);
                    }
                }
            });
        }
    }

    /**
     * Interface for adding the onClickListener to the adapter.
     */
    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot snapshot, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }
}
