package com.shop.expensestrackingapp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.shop.expensestrackingapp.databinding.ActivityProfileEditBinding;

public class ProfileEditActivity extends AppCompatActivity {

    private static final String TAG = "ProfileEditActivity";
    // Keys for Intent extras and results
    public static final String EXTRA_USER_ID = "com.shop.expensestrackingapp.USER_ID";
    public static final String RESULT_PROFILE_UPDATED = "com.shop.expensestrackingapp.PROFILE_UPDATED";

    private ActivityProfileEditBinding binding;
    private DatabaseGateway dbHelper;
    private SessionManager sessionManager;
    private int currentUserId = -1;
    private String originalFirstName = "";
    private String originalLastName = "";
    private String originalEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileEditBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        currentUserId = getIntent().getIntExtra(EXTRA_USER_ID, -1);
        Log.d(TAG, "ProfileEditActivity - Received User ID: " + currentUserId);

        if (currentUserId == -1) {
            Toast.makeText(this, "Error: User information not found.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "No USER_ID passed to ProfileEditActivity or USER_ID was -1.");
            finish();
            return;
        }

        dbHelper = new DatabaseGateway(this);
        sessionManager = new SessionManager(this);

        setSupportActionBar(binding.toolbarEdit);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        loadUserProfileData();

        binding.btnSaveChanges.setOnClickListener(v -> attemptSaveChanges());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadUserProfileData() {
        if (currentUserId == -1) {
            Log.w(TAG, "loadUserProfileData called with invalid userId (-1).");
            return;
        }
        Log.d(TAG, "loadUserProfileData - Attempting to load for userId: " + currentUserId);

        Cursor cursor = null;
        try {
            cursor = dbHelper.getUserById(currentUserId);
            if (cursor != null) {
                Log.d(TAG, "loadUserProfileData - Cursor received. Row count: " + cursor.getCount());
                if (cursor.moveToFirst()) {
                    Log.d(TAG, "loadUserProfileData - Cursor moved to first. Populating fields.");
                    originalFirstName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseGateway.COL_FIRSTNAME));
                    originalLastName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseGateway.COL_LASTNAME));
                    originalEmail = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseGateway.COL_EMAIL));

                    Log.d(TAG, "loadUserProfileData - Fetched from DB: FN=" + originalFirstName + ", LN=" + originalLastName + ", Email=" + originalEmail);

                    if (binding != null) {
                        // Make sure your XML has android:id="@+id/edtFirstName"
                        binding.edtFirstname.setText(originalFirstName != null ? originalFirstName : "");
                        binding.edtLastname.setText(originalLastName != null ? originalLastName : "");
                        binding.edtMail.setText(originalEmail != null ? originalEmail : "");
                        Log.d(TAG, "loadUserProfileData - setText called on EditTexts.");
                    } else {
                        Log.e(TAG, "loadUserProfileData - Binding is null, cannot set text.");
                    }

                } else {
                    Log.w(TAG, "loadUserProfileData - Cursor is empty for userId: " + currentUserId);
                    Toast.makeText(this, "Could not load profile data (user not found).", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "loadUserProfileData - dbHelper.getUserById returned a NULL cursor for userId: " + currentUserId);
                Toast.makeText(this, "Could not load profile data (database error).", Toast.LENGTH_SHORT).show();
            }
        } catch (IllegalArgumentException iae) {
            Log.e(TAG, "loadUserProfileData - Error finding column '" + iae.getMessage() + "' in cursor for userId: " + currentUserId, iae);
            Toast.makeText(this, "Error: Profile data structure mismatch.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "loadUserProfileData - Generic error loading user data for userId: " + currentUserId, e);
            Toast.makeText(this, "Error loading profile data.", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void attemptSaveChanges() {
        // Make sure your XML has android:id="@+id/edtFirstName"
        String newFirstName = binding.edtFirstname.getText().toString().trim();
        String newLastName = binding.edtLastname.getText().toString().trim();
        String newEmail = binding.edtMail.getText().toString().trim();

        if (TextUtils.isEmpty(newFirstName)) {
            binding.edtFirstname.setError("First name cannot be empty");
            binding.edtFirstname.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(newEmail)) {
            binding.edtMail.setError("Email cannot be empty");
            binding.edtMail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            binding.edtMail.setError("Invalid email format");
            binding.edtMail.requestFocus();
            return;
        }

        boolean firstNameChanged = !newFirstName.equals(originalFirstName == null ? "" : originalFirstName);
        boolean lastNameChanged = !newLastName.equals(originalLastName == null ? "" : originalLastName);
        boolean emailChanged = !newEmail.equalsIgnoreCase(originalEmail == null ? "" : originalEmail);

        if (!firstNameChanged && !lastNameChanged && !emailChanged) {
            Toast.makeText(this, "No changes made.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (emailChanged) {
            if (dbHelper.checkEmailExists(newEmail)) {
                binding.edtMail.setError("Email already registered");
                binding.edtMail.requestFocus();
                Toast.makeText(this, "This email address is already in use.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        boolean updateSuccessful = dbHelper.updateUserProfile(currentUserId, newFirstName, newLastName, newEmail);

        if (updateSuccessful) {
            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();

            sessionManager.updateFirstName(newFirstName);
            if (emailChanged) {
                sessionManager.updateEmail(newEmail);
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra(RESULT_PROFILE_UPDATED, true);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Failed to update profile. Please try again.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "dbHelper.updateUserProfile returned false for userId: " + currentUserId);
        }
    }

}