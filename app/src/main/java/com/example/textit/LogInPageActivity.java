package com.example.textit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

/**
 * Log in page of the App. Users can either go to the home page if they are registered, sign up page,
 * or forgot password page from here.
 *
 * @see HomePageActivity
 * @see SignUpActivity
 * @see ForgotPasswordActivity
 */
public class LogInPageActivity extends AppCompatActivity {
    // Log tag and error message constants
    private static final String TAG = LogInPageActivity.class.getName();
    private static final String EMPTY_FIELD_ERROR_MESSAGE = "Field cannot be empty";
    private static final String INVALID_INFO_ERROR_MESSAGE =
            "Please check that information is entered correctly";
    private static final String INVALID_USER_ERROR_MESSAGE =
            "There is no user record corresponding to this email address";

    // Firebase Authentication
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Views
    private TextInputEditText mEmailTxtInEditTxt, mPasswordTxtInEditTxt;
    private TextInputLayout mEmailTxtInLayout, mPasswordTxtInLayout;
    private TextView mSignUpTextView;
    private Button mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page_activity);

        // Set views
        mEmailTxtInEditTxt = findViewById(R.id.email_txt_in_edit_txt_login_screen);
        mEmailTxtInLayout = findViewById(R.id.email_txt_in_layout_login_screen);
        mPasswordTxtInLayout = findViewById(R.id.password_txt_in_layout_login_screen);
        mPasswordTxtInEditTxt = findViewById(R.id.password_txt_in_edit_txt_login_screen);
        mSignUpTextView = findViewById(R.id.sign_up_txt_view_login_screen);
        mLoginButton = findViewById(R.id.login_button_login_screen);
        TextView mForgotPasswordTextView = findViewById(R.id.forgot_password_txt_view_login_screen);

        // Set listeners
        mEmailTxtInEditTxt.addTextChangedListener(editTextWatcher);
        mPasswordTxtInEditTxt.addTextChangedListener(editTextWatcher);

        mForgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the forgot password activity
                startActivity(new Intent(
                        LogInPageActivity.this, ForgotPasswordActivity.class));
            }
        });

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();

                if (!validateLoginFields()) {
                    return;
                }

                loginUser();
            }
        });

        // Hide the keyboard when the DONE/SEND button is pressed
        mPasswordTxtInEditTxt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

        setSignUp();
    }

    /**
     * Hides the keyboard
     */
    private void hideKeyboard() {
        // Get the input method and the current focus
        InputMethodManager imm = (InputMethodManager) LogInPageActivity.this
                .getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = LogInPageActivity.this.getCurrentFocus();

        //If no view currently has focus, create a new one so we can grab a window token from it
        if (view == null) {
            view = new View(LogInPageActivity.this);
        }

        // Hide the keyboard
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Open the home page screen if Users are already signed in and has updated their basic
     * information during their sign up process. Otherwise, direct them to the update info screen so
     * they can update their information first before being directed to the home page.
     *
     * @see UpdateInfoActivity
     */
    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            if (user.getDisplayName() == null || user.getDisplayName().isEmpty()) {
                Intent intent = (new Intent
                        (LogInPageActivity.this, UpdateInfoActivity.class));

                intent.putExtra(ActivityConstants.NEW_USER_EXTRA, true);

                startActivity(intent);
            } else {
                startActivity(new Intent(LogInPageActivity.this, HomePageActivity.class));
            }
        }
    }

    /**
     * Custom TextWatcher Object for the 2 TextInputEditText fields. Used to check that all fields
     * are not empty before enabling the log in button.
     */
    private TextWatcher editTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String emailInput = mEmailTxtInEditTxt.getText().toString().trim();
            String passwordInput = mPasswordTxtInEditTxt.getText().toString().trim();

            mLoginButton.setEnabled(!emailInput.isEmpty() && !passwordInput.isEmpty());
            mEmailTxtInLayout.setError(null);
            mPasswordTxtInLayout.setError(null);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    /**
     * Makes the 'Sign Up' portion of the sign up TextView clickable and open the sign up activity
     * when it is clicked.
     *
     * @see SignUpActivity
     */
    private void setSignUp() {
        final String signUpText = getResources().getString(R.string.string_sign_up);

        SpannableString spannableString = new SpannableString(signUpText);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(LogInPageActivity.this, SignUpActivity.class));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);

                // Change appearance on the 'Sign Up' text
                ds.setColor(getResources().getColor(R.color.light_blue));
                ds.setUnderlineText(false);
            }
        };

        spannableString.setSpan(clickableSpan, 22, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mSignUpTextView.setText(spannableString);
        mSignUpTextView.setMovementMethod(LinkMovementMethod.getInstance());
        mSignUpTextView.setHighlightColor(Color.TRANSPARENT);
    }

    /**
     * Checks that User has enter valid login information before attempting to begin the sign in
     * process.
     */
    private void loginUser() {
        String emailInput = mEmailTxtInEditTxt.getText().toString().trim();
        String passwordInput = mPasswordTxtInEditTxt.getText().toString().trim();

        mAuth.signInWithEmailAndPassword(emailInput, passwordInput)
                .addOnCompleteListener(LogInPageActivity.this,
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "signInWithEmail:success (from loginUser())");

                                    // Open the HomePageActivity
                                    startActivity(new Intent(LogInPageActivity.this,
                                            HomePageActivity.class));
                                }
                                // Handle exception when user entered the wrong login information
                                // for an existing account
                                else if (task.getException() instanceof
                                        FirebaseAuthInvalidCredentialsException) {
                                    Log.d(TAG, "signInWithEmail:failure (from loginUser()) - "
                                            + task.getException());

                                    mEmailTxtInLayout.setError(INVALID_INFO_ERROR_MESSAGE);
                                    mPasswordTxtInLayout.setError(INVALID_INFO_ERROR_MESSAGE);
                                }
                                // Handle exception when user entered information for a deleted
                                // account or any other reason for this particular exception
                                else if (task.getException() instanceof
                                        FirebaseAuthInvalidUserException) {
                                    Log.d(TAG, "signInWithEmail:failure (from loginUser()) - "
                                            + task.getException());

                                    mEmailTxtInLayout.setError(INVALID_USER_ERROR_MESSAGE);
                                }
                                // If sign in fails for any other reason, display a message to the
                                // User.
                                else {
                                    Log.d(TAG, "signInWithEmail:failure (from loginUser()): ",
                                            task.getException());

                                    Toast.makeText(LogInPageActivity.this,
                                            "Login failed, please try again",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
    }

    /**
     * Checks and sets errors to the login fields if Users leave them empty.
     *
     * @return True if all the fields are not empty, false otherwise
     */
    private boolean validateLoginFields() {
        String emailInput = mEmailTxtInEditTxt.getText().toString().trim();
        String passwordInput = mPasswordTxtInEditTxt.getText().toString().trim();

        if (emailInput.isEmpty() && passwordInput.isEmpty()) {
            mEmailTxtInLayout.setError(EMPTY_FIELD_ERROR_MESSAGE);
            mPasswordTxtInLayout.setError(EMPTY_FIELD_ERROR_MESSAGE);

            return false;
        }

        if (emailInput.isEmpty()) {
            mEmailTxtInLayout.setError(EMPTY_FIELD_ERROR_MESSAGE);
            mPasswordTxtInLayout.setError(null);

            return false;
        } else if (passwordInput.isEmpty()) {
            mEmailTxtInLayout.setError(null);
            mPasswordTxtInLayout.setError(EMPTY_FIELD_ERROR_MESSAGE);

            return false;
        } else {
            mEmailTxtInLayout.setError(null);
            mPasswordTxtInLayout.setError(null);

            return true;
        }
    }
}
