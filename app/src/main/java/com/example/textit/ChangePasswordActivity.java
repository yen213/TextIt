package com.example.textit;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;

/**
 * Activity where Users can change their account passwords.
 */
public class ChangePasswordActivity extends AppCompatActivity {
    // Constants
    private static final String TAG = ChangePasswordActivity.class.getName();
    private static final String COMPLEXITY_ERROR_MESSAGE =
            "Password does not meet complexity requirement";
    private static final String PASS_MATCH_ERROR_MESSAGE =
            "Does not match previously entered password";
    private int WHITE_COLOR, BLACK_COLOR;

    // Views
    private TextInputLayout mPasswordLayout, mReEnterLayout;
    private TextInputEditText mCurrentPassEditText, mPasswordEditText, mReEnterEditText;
    private TextView mPassLenTextView, mPassUpperTextView, mPassLowerTextView, mPassSpecialTextView,
            mPassNumberTextView;
    private Button mChangePasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_password_activity);

        // Get the colors
        WHITE_COLOR = getResources().getColor(R.color.white);
        BLACK_COLOR = getResources().getColor(R.color.mostly_black);

        setViews();
    }

    /**
     * Sets all the Views to the screen and attaches listeners to some of them
     */
    private void setViews() {
        mCurrentPassEditText = findViewById(R.id.current_password_edit_text_change_pass_activity);
        mPasswordLayout = findViewById(R.id.password_txt_in_layout_change_pass_activity);
        mPasswordEditText = findViewById(R.id.password_edit_text_change_pass_activity);
        mReEnterLayout = findViewById(R.id.re_password_txt_in_layout_change_pass_activity);
        mReEnterEditText = findViewById(R.id.re_password_txt_in_edit_txt_change_pass_activity);
        mPassLenTextView = findViewById(R.id.length_req_txt_view_change_pass_activity);
        mPassUpperTextView = findViewById(R.id.uppercase_req_txt_view_change_pass_activity);
        mPassLowerTextView = findViewById(R.id.lowercase_req_txt_view_change_pass_activity);
        mPassSpecialTextView = findViewById(R.id.special_req_txt_view_change_pass_activity);
        mPassNumberTextView = findViewById(R.id.number_req_txt_view_change_pass_activity);
        mChangePasswordButton = findViewById(R.id.change_button_change_pass_activity);

        // Set listeners
        mCurrentPassEditText.addTextChangedListener(editTextWatcher);
        mPasswordEditText.addTextChangedListener(passwordTextWatcher);
        mReEnterEditText.addTextChangedListener(editTextWatcher);
    }

    /**
     * Custom TextWatcher variable for the password TextInputEditText field. The password requirement
     * TextViews change color every time a User finishes one of the requirements.
     */
    private TextWatcher passwordTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String passwordInput = mPasswordEditText.getText().toString().trim();

            // Check required length
            if (passwordInput.length() > 7) {
                mPassLenTextView.setTextColor(BLACK_COLOR);
            } else {
                mPassLenTextView.setTextColor(WHITE_COLOR);
            }

            // Check Uppercase, Lowercase, Number, and Special Character reqs (respectively)
            mPassUpperTextView.setTextColor(
                    passwordInput.matches(".*[A-Z].*") ? BLACK_COLOR : WHITE_COLOR);
            mPassLowerTextView.setTextColor(
                    passwordInput.matches(".*[a-z].*") ? BLACK_COLOR : WHITE_COLOR);
            mPassNumberTextView.setTextColor(
                    passwordInput.matches(".*[0-9].*") ? BLACK_COLOR : WHITE_COLOR);
            mPassSpecialTextView.setTextColor(
                    passwordInput.matches(".*[^A-Za-z0-9 ].*") ? BLACK_COLOR : WHITE_COLOR);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    /**
     * Custom TextWatcher variable for the other 2 TextInputEditText fields. Used to check that all
     * fields are not empty before enabling the create account button.
     */
    private TextWatcher editTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String passwordInput = mPasswordEditText.getText().toString().trim();
            String passwordReInput = mReEnterEditText.getText().toString().trim();

            mChangePasswordButton.setEnabled(!passwordInput.isEmpty() && !passwordReInput.isEmpty());

            changePassword();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    /**
     * Update the User's password
     */
    private void changePassword() {
        mChangePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check that passwords are valid before continuing
                if (!validatePassword()) {
                    return;
                }

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                final String currentPassword = mCurrentPassEditText.getText().toString();
                final String password = mPasswordEditText.getText().toString();

                user.updatePassword(password)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "User password updated.");
                                    Toast.makeText(ChangePasswordActivity.this,
                                            "Successfully changed password!",
                                            Toast.LENGTH_LONG).show();
                                    finish();
                                }
                                // If the User account needs to be Re-authenticated
                                else if (task.getException() instanceof
                                        FirebaseAuthRecentLoginRequiredException) {
                                    Log.d(TAG, "Failed to send verification link " +
                                            "(from sendPasswordResetEmail()): " + task.getException());
                                    reAuthenticateUser(currentPassword, password);
                                } else {
                                    Log.d(TAG, "Error updating User password: "
                                            + task.getException());
                                    Toast.makeText(ChangePasswordActivity.this,
                                            "Something went wrong, please try again later.",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }

    /**
     * Set error(s) if User has not met the password requirement complexity and/or the re-entered
     * password doesn't match previously entered password field.
     */
    private boolean validatePassword() {
        CharSequence passwordCharSeq = mPasswordEditText.getText();
        CharSequence reEnterPasswordCharSeq = mReEnterEditText.getText();

        if (passwordCharSeq != null && reEnterPasswordCharSeq != null) {
            String passwordInput = passwordCharSeq.toString().trim();
            String reEnterPasswordInput = reEnterPasswordCharSeq.toString().trim();

            if (!passwordInput.matches(".*[A-Za-z0-9 ].*") ||
                    !passwordInput.matches(".*[^A-Za-z0-9 ].*")) {
                mPasswordLayout.setError(COMPLEXITY_ERROR_MESSAGE);

                return false;
            } else if (!reEnterPasswordInput.equals(passwordInput)) {
                mReEnterLayout.setError(PASS_MATCH_ERROR_MESSAGE);
                mPasswordLayout.setError(null);

                return false;
            } else {
                mReEnterLayout.setError(null);
                mPasswordLayout.setError(null);

                return true;
            }
        }

        return false;
    }

    /**
     * If the User signed into the account too long ago, they need to enter their account info in
     * order to re-authenticate them before changing their password.
     *
     * @param userEmail    The account email address
     * @param userPassword The last know password
     */
    private void reAuthenticateUser(String userEmail, String userPassword) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        AuthCredential credential = EmailAuthProvider.getCredential(userEmail, userPassword);

        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User re-authenticated (from reAuthenticateUser()).");
                            changePassword();
                        } else {
                            Log.d(TAG, "User re-authenticated failed " +
                                    "(from reAuthenticateUser()): " + task.getException());
                            Toast.makeText(ChangePasswordActivity.this,
                                    "Something went wrong, please try again later.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
