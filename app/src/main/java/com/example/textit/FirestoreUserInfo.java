package com.example.textit;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

/**
 * Model class for the data in a document in the {@link FirestoreReferences#USERS_PATH} collection.
 */
public class FirestoreUserInfo {
    @ServerTimestamp
    private Date accountCreatedOn, profilePicUpdated;
    private String firstName, lastName, dateOfBirth, uid, email, profilePictureUrl;
    private int age;
    private List<String> tokenIds;

    // Required empty constructor
    public FirestoreUserInfo() { }

    public FirestoreUserInfo(String firstName, String lastName, int age, String dateOfBirth,
                             String uid, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.dateOfBirth = dateOfBirth;
        this.uid = uid;
        this.email = email;
    }

    @ServerTimestamp
    public Date getAccountCreatedOn() {
        return accountCreatedOn;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getAge() {
        return age;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    @ServerTimestamp
    public Date getProfilePicUpdated() {
        return profilePicUpdated;
    }

    public List<String> getTokenIds() {
        return tokenIds;
    }
}
