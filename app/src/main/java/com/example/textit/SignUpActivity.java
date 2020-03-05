package com.example.textit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

/**
 * Sign up screen where new Users can create an account and then start using the app.
 */
public class SignUpActivity extends AppCompatActivity {
    // Log tag, error message constants, and color constants
    private static final String TAG = SignUpActivity.class.getName();
    private static final String EMAIL_IN_USE_ERROR_MESSAGE = "Email in use by another user";
    private static final String VALID_EMAIL_ERROR_MESSAGE = "Please enter a valid email address";
    private static final String COMPLEXITY_ERROR_MESSAGE =
            "Password does not meet complexity requirement";
    private static final String PASS_MATCH_ERROR_MESSAGE =
            "Does not match previously entered password";
    private int WHITE_COLOR;
    private int BLACK_COLOR;

    // Firebase authentication
    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Views
    private TextInputEditText mEmailTxtInEditTxt, mPasswordTxtInEditTxt, mRePassTxtInEditTxt;
    private TextInputLayout mEmailTxtInLayout, mPassTxtInLayout, mRePassTxtInLayout;
    private TextView mPassLenTextView, mPassUpperTextView, mPassLowerTextView, mPassSpecialTextView,
            mPassNumberTextView;
    private Button mCreateAccountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up_activity);

        WHITE_COLOR = getResources().getColor(R.color.white);
        BLACK_COLOR = getResources().getColor(R.color.mostly_black);

        setViewsAndListeners();
    }

    /**
     * Sets all the Views to the screen and attaches listeners to some of them
     */
    private void setViewsAndListeners() {
        // Set views
        mEmailTxtInEditTxt = findViewById(R.id.email_txt_in_edit_txt_sign_up_screen);
        mCreateAccountButton = findViewById(R.id.create_account_button_sign_up_screen);
        mPasswordTxtInEditTxt = findViewById(R.id.password_txt_in_edit_txt_sign_up_screen);
        mRePassTxtInEditTxt = findViewById(R.id.re_pass_txt_in_edit_txt_sign_up_screen);
        mPassLenTextView = findViewById(R.id.length_req_txt_view_sign_up_screen);
        mPassUpperTextView = findViewById(R.id.uppercase_req_txt_view_sign_up_screen);
        mPassLowerTextView = findViewById(R.id.lowercase_req_txt_view_sign_up_screen);
        mPassSpecialTextView = findViewById(R.id.special_req_txt_view_sign_up_screen);
        mPassNumberTextView = findViewById(R.id.number_req_txt_view_sign_up_screen);
        mEmailTxtInLayout = findViewById(R.id.email_txt_in_layout_sign_up_screen);
        mRePassTxtInLayout = findViewById(R.id.re_pass_txt_in_layout_sign_up_screen);
        mPassTxtInLayout = findViewById(R.id.password_txt_in_layout_sign_up_screen);

        // Set listeners
        mPasswordTxtInEditTxt.addTextChangedListener(passwordTextWatcher);
        mEmailTxtInEditTxt.addTextChangedListener(editTextWatcher);
        mRePassTxtInEditTxt.addTextChangedListener(editTextWatcher);

        // Set button listener
        mCreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                createUserAccount();
            }
        });

        // Hide the keyboard when the DONE/SEND button is pressed
        mRePassTxtInEditTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                        actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard();

                    return true;
                }

                return false;
            }
        });
    }

    /**
     * Hides the keyboard
     */
    private void hideKeyboard() {
        // Get the input method and the current focus
        InputMethodManager imm = (InputMethodManager) SignUpActivity.this
                .getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = SignUpActivity.this.getCurrentFocus();

        //If no view currently has focus, create a new one so we can grab a window token from it
        if (view == null) {
            view = new View(SignUpActivity.this);
        }

        // Hide the keyboard
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Custom TextWatcher for the password TextInputEditText field. The password requirement
     * TextViews change color every time one of the password requirement is met.
     */
    private TextWatcher passwordTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String passwordInput = mPasswordTxtInEditTxt.getText().toString().trim();

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

            mPassTxtInLayout.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    /**
     * Custom TextWatcher for the other 2 TextInputEditText fields. Used to check that all
     * fields are not empty before enabling the continue button.
     */
    private TextWatcher editTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String emailInput = mEmailTxtInEditTxt.getText().toString().trim();
            String passwordInput = mPasswordTxtInEditTxt.getText().toString().trim();
            String passwordReInput = mRePassTxtInEditTxt.getText().toString().trim();

            mCreateAccountButton.setEnabled(!emailInput.isEmpty() && !passwordInput.isEmpty()
                    && !passwordReInput.isEmpty());

            mEmailTxtInLayout.setError(null);
            mRePassTxtInLayout.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    /**
     * Check that the person creating the account has entered valid information before creating
     * their user account.
     */
    private void createUserAccount() {
        if (!validateEmail() | !validatePassword()) {
            return;
        }

        String userEmail = mEmailTxtInEditTxt.getText().toString().trim();
        String userPassword = mPasswordTxtInEditTxt.getText().toString();

        mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "New User account created!");

                            // Open the Update Info Screen
                            Intent intent = new Intent(new Intent(SignUpActivity.this,
                                    UpdateInfoActivity.class));

                            intent.putExtra(ActivityConstants.NEW_USER_EXTRA, true);
                            startActivity(intent);
                        }
                        // Notify user if they attempt to make an account with an already
                        // registered email address
                        else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Log.d(TAG, "Email in use by another user");

                            mEmailTxtInLayout.setError(EMAIL_IN_USE_ERROR_MESSAGE);
                        } else {
                            Log.d(TAG, "createUserWithEmail: failed - " + task.getException());

                            Toast.makeText(SignUpActivity.this,
                                    "Error creating account, please try again later.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Set error if the person creating the account has entered an incorrect email format, continue
     * otherwise.
     *
     * @return True if email format is good, false otherwise
     */
    private boolean validateEmail() {
        CharSequence emailCharSeq = mEmailTxtInEditTxt.getText();

        if (emailCharSeq != null) {
            String emailInput = emailCharSeq.toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                mEmailTxtInLayout.setError(null);
                mEmailTxtInLayout.setError(VALID_EMAIL_ERROR_MESSAGE);

                return false;
            } else {
                mEmailTxtInLayout.setError(null);

                return true;
            }
        }

        return false;
    }

    /**
     * Set error(s) if the person creating the account has not met the password requirement
     * complexity and/or the re-entered password doesn't match previously entered password field.
     */
    private boolean validatePassword() {
        CharSequence passwordCharSeq = mPasswordTxtInEditTxt.getText();
        CharSequence reEnterPasswordCharSeq = mRePassTxtInEditTxt.getText();

        if (passwordCharSeq != null && reEnterPasswordCharSeq != null) {
            String passwordInput = passwordCharSeq.toString();
            String reEnterPasswordInput = reEnterPasswordCharSeq.toString();

            if (!passwordInput.matches(".*[A-Za-z0-9 ].*") ||
                    !passwordInput.matches(".*[^A-Za-z0-9 ].*")) {
                mPassTxtInLayout.setError(COMPLEXITY_ERROR_MESSAGE);

                return false;
            } else if (!reEnterPasswordInput.equals(passwordInput)) {
                mRePassTxtInLayout.setError(PASS_MATCH_ERROR_MESSAGE);
                mPassTxtInLayout.setError(null);

                return false;
            } else {
                mRePassTxtInLayout.setError(null);
                mPassTxtInLayout.setError(null);

                return true;
            }
        }

        return false;
    }
}
