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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter for ChatFragment that inherits from {@link FirestoreRecyclerOptions} to listen to changes
 * in the database and update the UI accordingly.
 *
 * @see ChatFragment
 */
public class ChatAdapter extends FirestoreRecyclerAdapter<TwoPersonRoomModel, ChatAdapter.ChatHolder> {
    // Listener and current time
    private OnItemClickListener mListener;
    Date mCurrentTime = new Date(System.currentTimeMillis());

    public ChatAdapter(@NonNull FirestoreRecyclerOptions<TwoPersonRoomModel> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatHolder holder, int position,
                                    @NonNull TwoPersonRoomModel model) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Get the Timestamp of the latest message
        if (model.getLatestMessageTimestamp() != null && mCurrentTime != null) {
            String time;
            long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1);
            long MILLIS_PER_WEEK = TimeUnit.DAYS.toMillis(7);
            Date messageDate = model.getLatestMessageTimestamp();

            // Message is less than a day old
            if (Math.abs(messageDate.getTime() - mCurrentTime.getTime()) < MILLIS_PER_DAY) {
                time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(model.getLatestMessageTimestamp());
                holder.dateTextView.setText(time);
            }
            // Message is more than a day old but less than a week old
            else if (Math.abs(messageDate.getTime() - mCurrentTime.getTime()) > MILLIS_PER_DAY
                    && Math.abs(messageDate.getTime() - mCurrentTime.getTime()) < MILLIS_PER_WEEK) {
                time = new SimpleDateFormat("EE 'at' hh:mm a", Locale.getDefault())
                        .format(model.getLatestMessageTimestamp());
                holder.dateTextView.setText(time);
            }
            // Message is more than a week old
            else {
                time = new SimpleDateFormat("MMM dd, yyyy",
                        Locale.getDefault()).format(model.getLatestMessageTimestamp());
                holder.dateTextView.setText(time);
            }
        } else {
            holder.dateTextView.setText(" ");
        }

        // Get the latest message and it's sender
        if (user != null) {
            if (model.getLatestMessage() != null && model.getLatestMessageSender() != null) {
                String userUid = user.getUid();
                StringBuilder message = new StringBuilder(model.getLatestMessage());
                String friendName = " ";

                // Get the sender profile picture download urls and put in the CircleImageView
                if (model.getProfile_urls() != null) {
                    for (Map.Entry<String, Object> entry : model.getProfile_urls().entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();



                        if (!key.equals(userUid) && value != null) {
                            Picasso
                                    .get()
                                    .load(value.toString())
                                    .into(holder.pictureCircleImageView);
                            break;
                        }
                    }
                }

                // Get display name
                if (model.getDisplay_names() != null) {
                    for (Map.Entry<String, Object> entry : model.getDisplay_names().entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue().toString();

                        if (!key.equals(userUid) && value != null) {
                            friendName = value;
                            break;
                        }
                    }
                }

                // Get the latest message sender and display the sender name + message in the view.
                String name = user.getDisplayName();
                String sender = model.getLatestMessageSender();

                if (sender.equals(name)) {
                    message.insert(0, "You: ");
                } else {
                    sender = sender.substring(0, sender.indexOf(" "));
                    message.insert(0, sender + ": ");
                }

                // Cut out last message text if it is too long
                if (message.length() > 19) {
                    message = new StringBuilder(message.substring(0, 20) + "...");
                }

                holder.nameTextView.setText(friendName);
                holder.latestMessageTextView.setText(message.toString());
            } else {
                holder.nameTextView.setText(" ");
                holder.latestMessageTextView.setText(" ");
            }
        } else {
            holder.nameTextView.setText(" ");
            holder.latestMessageTextView.setText(" ");
        }
    }

    @NonNull
    @Override
    public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_list_item,
                parent, false);

        return new ChatHolder(view);
    }

    /**
     * Gets the Document Id of an item in the RecyclerView
     *
     * @param position Position of Document in the adapter
     * @return Document ID
     */
    public String getDocument(int position) {
        return getSnapshots().getSnapshot(position).getReference().getId();
    }

    /**
     * Gets the {@link FirestoreReferences#MEMBER_LIST_FIELD} list of members from the selected
     * Document from an item in the RecyclerView.
     *
     * @param position Position of Document in the adapter
     * @return Document ID
     */
    public List<String> getUidList(int position) {
        TwoPersonRoomModel model = getSnapshots().getSnapshot(position)
                .toObject(TwoPersonRoomModel.class);

        if (model.getMember_list() != null) {
            return model.getMember_list();
        }

        return null;
    }

    /**
     * ViewHolder class to set the views and implement onClickListener to the adapter items
     */
    public class ChatHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, latestMessageTextView, dateTextView;
        CircleImageView pictureCircleImageView;
        RelativeLayout backgroundView, foregroundView;

        public ChatHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name_txt_view_chat_list);
            latestMessageTextView = itemView.findViewById(R.id.message_txt_view_chat_list);
            dateTextView = itemView.findViewById(R.id.date_txt_view_chat_list);
            pictureCircleImageView = itemView.findViewById(R.id.picture_img_view_chat_list);
            backgroundView = itemView.findViewById(R.id.background_view_chat_list);
            foregroundView = itemView.findViewById(R.id.foreground_view_chat_list);

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
        // Get the document that was clicked on by the user in the RecyclerView and its position.
        void onItemClick(DocumentSnapshot snapshot, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }
}
