package com.example.textit;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog box that appears when someone in a group wants to add a member.
 */
public class AddNewGroupMemberDialog extends DialogFragment {
    // Constants
    private static final String ADD_MEMBER_HINT = "Enter a User Email";
    private static final String EMPTY_FIELD_ERROR = "Field cannot be empty";

    // Views
    private EditText mUserEmailEditText;
    private TextView mTitleTextView;

    // Listener
    private GroupMemberDialogListener mListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                R.style.alert_dialog_title);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflater
                .inflate(R.layout.group_dialog, null);

        builder.setView(view)
                .setCustomTitle(mTitleTextView)
                .setPositiveButton(getResources().getString(R.string.string_add), null)
                .setNegativeButton(getResources().getString(R.string.string_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });

        mUserEmailEditText = view.findViewById(R.id.group_name_edit_txt_dialog);
        mTitleTextView = view.findViewById(R.id.title_text_view_dialog);

        mUserEmailEditText.setHint(ADD_MEMBER_HINT);
        mTitleTextView.setText(getResources().getString(R.string.string_add_member));

        return builder.create();
    }

    /**
     * Makes it so that dialog box doesn't automatically close after pressing the positive button.
     * Override it in onResume() so that dialog shows even after configuration change.
     */
    @Override
    public void onResume() {
        super.onResume();
        final AlertDialog dialog = (AlertDialog) getDialog();

        if (dialog != null) {
            Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newMember = mUserEmailEditText.getText().toString().trim();

                    // Get the user input if its not empty and close the dialog box
                    if (newMember == null || newMember.isEmpty()) {
                        mUserEmailEditText.setError(EMPTY_FIELD_ERROR);
                        return;
                    }

                    mUserEmailEditText.setError(null);
                    mListener.getNewGroupMember(newMember);
                    dialog.dismiss();
                }
            });
        }
    }

    /**
     * Attach a listener to the dialog passing in the activity it was called from. Throw exception
     * if the calling activity doesn't implement the listener.
     *
     * @param context The activity that opens the dialog.
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mListener = (GroupMemberDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    "must implement GroupMemberDialogListener");
        }
    }

    /**
     * Listener interface class to get the User input.
     */
    public interface GroupMemberDialogListener {
        void getNewGroupMember(String email);
    }
}
