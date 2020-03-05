package com.example.textit;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

/**
 * Activity to reset User passwords if they forgot it. The password reset email link is sent to
 * the account they signed up with.
 */
public class ForgotPasswordActivity extends AppCompatActivity {
    // Log tag and error message constants
    private static final String TAG = ForgotPasswordActivity.class.getName();
    private static final String INVALID_USER_ERROR_MESSAGE =
            "No user corresponding to this email address found. The user may have been deleted.";
    private static final String BAD_FORMAT_ERROR_MESSAGE = "This email address is badly formatted.";

    // Views
    private TextInputEditText mEmailEditText;
    private TextInputLayout mEmailTextInLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password_activity);

        // Set views
        mEmailEditText = findViewById(R.id.email_txt_in_edit_txt_forgot_activity);
        mEmailTextInLayout = findViewById(R.id.email_txt_in_layout_forgot_activity);
        Button mSendButton = findViewById(R.id.send_button_forgot_activity);

        // Set listener
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPasswordResetEmail();
            }
        });
    }

    /**
     * Sends a password reset email to the email address User provided, else handle the different
     * errors/exceptions thrown.
     */
    private void sendPasswordResetEmail() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        final String email = mEmailEditText.getText().toString().trim();

        if (email != null && !email.isEmpty()) {
            auth.useAppLanguage();
            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Email sent (from sendPasswordResetEmail()).");

                                Toast.makeText(ForgotPasswordActivity.this,
                                        "Password reset link is being sent to your email",
                                        Toast.LENGTH_LONG).show();

                                finish();
                            }
                            // If a User account with this email has been deleted or hasn't been
                            // registered with.
                            else if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                Log.d(TAG, "Failed to send verification link " +
                                        "(from sendPasswordResetEmail()): " + task.getException());

                                mEmailTextInLayout.setError(INVALID_USER_ERROR_MESSAGE);
                            }
                            // If the email is badly formatted
                            else if (task.getException() instanceof
                                    FirebaseAuthInvalidCredentialsException) {
                                Log.d(TAG, "Failed to send verification link " +
                                        "(from sendPasswordResetEmail()): " + task.getException());

                                mEmailTextInLayout.setError(BAD_FORMAT_ERROR_MESSAGE);
                            } else {
                                Log.d(TAG, "Error sending email (from sendPasswordResetEmail()): "
                                        + task.getException());
                                Toast.makeText(ForgotPasswordActivity.this,
                                        "Something went wrong, please try again later.",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }
}
