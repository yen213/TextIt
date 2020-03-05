package com.example.textit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Adapter for GroupMessageFragment that inherits from {@link FirestoreRecyclerOptions} to listen
 * to changes in the database and update the UI accordingly.
 *
 * @see GroupMessageFragment
 */
public class GroupMessageAdapter extends FirestoreRecyclerAdapter<GroupRoomModel, GroupMessageAdapter.GroupHolder> {
    // Listener and current time
    private OnItemClickListener mListener;
    Date mCurrentTime = new Date(System.currentTimeMillis());

    public GroupMessageAdapter(@NonNull FirestoreRecyclerOptions<GroupRoomModel> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull GroupHolder holder, int position,
                                    @NonNull GroupRoomModel model) {
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

        // Get the message
        if (model.getLatestMessage() != null) {
            String message = model.getLatestMessage();

            // Ellipsize the message preview if it is too long
            if (message.length() > 19) {
                message = message.substring(0, 20) + "...";
            }

            holder.messageTextView.setText(message);
        } else {
            holder.messageTextView.setText(" ");
        }

        // Get the room picture and put it in the CircleImageView
        if (model.getRoomPic() != null) {
            Picasso
                    .get()
                    .load(model.getRoomPic())
                    .into(holder.pictureCircleImageView);
        }

        // Get the room name
        if (model.getRoomName() != null) {
            holder.nameTextView.setText(model.getRoomName());
        } else {
            holder.nameTextView.setText(" ");
        }
    }

    @NonNull
    @Override
    public GroupHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_list_item,
                parent, false);

        return new GroupHolder(view);
    }

    /**
     * ViewHolder class to set the views and implement onClickListener to each item in the adapter
     */
    public class GroupHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, messageTextView, dateTextView;
        CircleImageView pictureCircleImageView;

        public GroupHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name_txt_view_chat_list);
            messageTextView = itemView.findViewById(R.id.message_txt_view_chat_list);
            dateTextView = itemView.findViewById(R.id.date_txt_view_chat_list);
            pictureCircleImageView = itemView.findViewById(R.id.picture_img_view_chat_list);

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
