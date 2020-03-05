package com.example.textit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Settings activity where Users can update their profile information, log out of the app, or
 * change their password.
 *
 * @see DisplayUserInfoActivity
 * @see ChangePasswordActivity
 */
public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        // Set views
        TextView mSignOutTextView = findViewById(R.id.sign_out_txt_view_settings_activity);
        TextView mUpdateInfoTextView = findViewById(R.id.update_info_txt_view_settings_activity);
        TextView mChangePasswordTextView =
                findViewById(R.id.change_password_txt_view_settings_activity);

        // Set listeners
        mSignOutTextView.setOnClickListener(this);
        mUpdateInfoTextView.setOnClickListener(this);
        mChangePasswordTextView.setOnClickListener(this);
    }

    /**
     * Called when one of the views that has a listener attached to it gets clicked.
     *
     * @param v The view that was clicked on
     */
    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()) {
            // Signs Users out of the app and takes them to the log in screen.
            case R.id.sign_out_txt_view_settings_activity:
                intent = new Intent(SettingsActivity.this, LogInPageActivity.class);

                FirebaseAuth.getInstance().signOut();
                startActivity(intent);

                break;

            // Opens the Activity where Users can update their profile information from.
            case R.id.update_info_txt_view_settings_activity:
                intent = new Intent(SettingsActivity.this, UpdateInfoActivity.class);
                startActivity(intent);

                break;

            // Opens the Activity where Users can change their password from.
            case R.id.change_password_txt_view_settings_activity:
                intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
                startActivity(intent);

                break;
        }
    }
}
