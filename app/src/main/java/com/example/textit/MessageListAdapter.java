package com.example.textit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for SendMessageActivity that inherits from {@link FirestoreRecyclerOptions} to listen to
 * changes in the database and update the UI accordingly.
 *
 * @see SendMessageActivity
 */
public class MessageListAdapter extends FirestoreRecyclerAdapter<UserMessage,
        RecyclerView.ViewHolder> {
    // Constants for the two message layout files (right and left)
    private static final int VIEW_TYPE_MESSAGE_RIGHT = 1;
    private static final int VIEW_TYPE_MESSAGE_LEFT = 2;

    public MessageListAdapter(@NonNull FirestoreRecyclerOptions<UserMessage> options) {
        super(options);
    }

    // Check every model class object in the model list and return the appropriate ViewType based
    // on who the sender of the message is.
    @Override
    public int getItemViewType(int position) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        UserMessage message = getItem(position);
        String sender = message.getSender();
        String nameOfUser = user.getDisplayName();

        if ((sender != null && nameOfUser != null) && sender.equals(nameOfUser)) {
            // If current user is the sender of the message
            return VIEW_TYPE_MESSAGE_RIGHT;
        } else {
            // Sender of the message is not the current user
            return VIEW_TYPE_MESSAGE_LEFT;
        }
    }

    // Get the ViewType for the current model object and use the appropriate ViewHolder
    @Override
    protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                    @NonNull UserMessage model) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_RIGHT:
                ((RightMessagesHolder) holder).bind(model);
                break;
            case VIEW_TYPE_MESSAGE_LEFT:
                ((LeftMessagesHolder) holder).bind(model);
        }
    }

    // Inflate the appropriate layout based on the ViewType
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_RIGHT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_right, parent, false);

            return new RightMessagesHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_LEFT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_left, parent, false);

            return new LeftMessagesHolder(view);
        }

        return null;
    }

    /**
     * ViewHolder class to set the left messages view
     */
    public static class LeftMessagesHolder extends RecyclerView.ViewHolder {
        TextView messageTextView, timeTextView;

        public LeftMessagesHolder(@NonNull View itemView) {
            super(itemView);

            messageTextView = itemView.findViewById(R.id.message_txt_view_msg_left);
            timeTextView = itemView.findViewById(R.id.message_time_txt_view_msg_left);

            // Show the message timestamp if User clicks on a message
            messageTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (timeTextView.getVisibility() == View.INVISIBLE) {
                        timeTextView.setVisibility(View.VISIBLE);
                    } else {
                        timeTextView.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }

        // Get the message information from the object and display it in the views
        void bind(UserMessage message) {
            // Get the message timestamp and format it based on the message time and the current time
            // a User is looking at the message.
            if (message.getSent() != null) {
                String time;
                long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1);
                long MILLIS_PER_WEEK = TimeUnit.DAYS.toMillis(7);
                Date messageDate = message.getSent();
                Date currentTime = new Date(System.currentTimeMillis());

                // Message is less than a day old
                if (Math.abs(messageDate.getTime() - currentTime.getTime()) < MILLIS_PER_DAY) {
                    time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                            .format(message.getSent());
                    timeTextView.setText(time);
                }
                // Message is more than a day old but less than a week old
                else if (Math.abs(messageDate.getTime() - currentTime.getTime()) > MILLIS_PER_DAY
                        && Math.abs(messageDate.getTime() - currentTime.getTime()) < MILLIS_PER_WEEK) {
                    time = new SimpleDateFormat("EE 'at' hh:mm a", Locale.getDefault())
                            .format(message.getSent());
                    timeTextView.setText(time);
                }
                // Message is more than a week old
                else {
                    time = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a",
                            Locale.getDefault()).format(message.getSent());
                    timeTextView.setText(time);
                }
            } else {
                timeTextView.setText(" ");
            }

            // If receiver uid is null, then it is not a group message
            if (message.getReceiverUid() != null) {
                if (message.getMessage() != null) {
                    messageTextView.setText(message.getMessage());
                } else {
                    messageTextView.setText(" ");
                }
            }
            // For group message, show message sender's name above the message
            else {
                if (message.getMessage() != null) {
                    String newMessage = message.getSender() + "\t\t\t\n\n" + message.getMessage();
                    messageTextView.setText(newMessage);
                } else {
                    messageTextView.setText(" ");
                }
            }
        }
    }

    /**
     * ViewHolder class to set the right messages view
     */
    public static class RightMessagesHolder extends RecyclerView.ViewHolder {
        TextView messageTextView, timeTextView;

        public RightMessagesHolder(@NonNull View itemView) {
            super(itemView);

            messageTextView = itemView.findViewById(R.id.message_txt_view_msg_right);
            timeTextView = itemView.findViewById(R.id.time_txt_view_msg_right);

            // Show the message timestamp if User clicks on a message
            messageTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (timeTextView.getVisibility() == View.INVISIBLE) {
                        timeTextView.setVisibility(View.VISIBLE);
                    } else {
                        timeTextView.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }

        // Get the message information from the object and display it in the views
        void bind(UserMessage message) {
            // Get the message timestamp and format it based on the message time and the current time
            // a User is looking at the message.
            if (message.getSent() != null) {
                String time;
                long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1);
                long MILLIS_PER_WEEK = TimeUnit.DAYS.toMillis(7);
                Date messageDate = message.getSent();
                Date currentTime = new Date(System.currentTimeMillis());

                // Message is less than a day old
                if (Math.abs(messageDate.getTime() - currentTime.getTime()) < MILLIS_PER_DAY) {
                    time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                            .format(message.getSent());
                    timeTextView.setText(time);
                }
                // Message is more than a day old but less than a week old
                else if (Math.abs(messageDate.getTime() - currentTime.getTime()) > MILLIS_PER_DAY
                        && Math.abs(messageDate.getTime() - currentTime.getTime()) < MILLIS_PER_WEEK) {
                    time = new SimpleDateFormat("EE 'at' hh:mm a", Locale.getDefault())
                            .format(message.getSent());
                    timeTextView.setText(time);
                }
                // Message is more than a week old
                else {
                    time = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a",
                            Locale.getDefault()).format(message.getSent());
                    timeTextView.setText(time);
                }
            } else {
                timeTextView.setText(" ");
            }

            // Get the message
            if (message.getMessage() != null) {
                messageTextView.setText(message.getMessage());
            } else {
                messageTextView.setText(" ");
            }
        }
    }
}
