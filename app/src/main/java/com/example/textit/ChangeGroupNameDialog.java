package com.example.textit;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog box that appears when someone in a group wants to change the group name.
 */
public class ChangeGroupNameDialog extends DialogFragment {
    // View
    private EditText mNameEditText;
    private TextView mTitleTextView;

    // Listener
    private GroupDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                R.style.alert_dialog_title);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.group_dialog, null);

        builder.setView(view)
                .setCustomTitle(mTitleTextView)
                .setNegativeButton(getResources().getString(R.string.string_cancel), null)
                .setPositiveButton(getResources().getString(R.string.string_save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String groupName = mNameEditText.getText().toString().trim();
                        listener.getNewGroupName(groupName);
                    }
                });

        mNameEditText = view.findViewById(R.id.group_name_edit_txt_dialog);
        mTitleTextView = view.findViewById(R.id.title_text_view_dialog);

        mTitleTextView.setText(getResources().getString(R.string.string_edit_chat_name));

        return builder.create();
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
            listener = (GroupDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    "must implement GroupMemberDialogListener");
        }
    }

    /**
     * Listener interface class to get the User input.
     */
    public interface GroupDialogListener {
        void getNewGroupName(String name);
    }
}
