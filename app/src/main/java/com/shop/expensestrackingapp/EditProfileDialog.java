package com.shop.expensestrackingapp;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment; // Generated from dialog_edit_profile.xml

import com.shop.expensestrackingapp.databinding.EditProfileDialogBinding;

public class EditProfileDialog extends DialogFragment {

    // Listener to send data back to ProfileFragment
    public interface EditProfileDialogListener {
        void onProfileUpdated(String newFirstName, String newLastName, String newEmail);
        // Add a new callback for password changes
        void onPasswordChanged(String currentPassword, String newPassword); // Pass current for verification
    }

    private EditProfileDialogListener listener;
    private EditProfileDialogBinding binding; // ViewBinding

    private static final String TAG = "EditProfileDialog";
    private static final String ARG_FIRST_NAME = "firstName";
    private static final String ARG_LAST_NAME = "lastName";
    private static final String ARG_EMAIL = "email";

    // Factory method to pass current profile data
    public static EditProfileDialog newInstance(String firstName, String lastName, String email) {
        EditProfileDialog dialog = new EditProfileDialog();
        Bundle args = new Bundle();
        args.putString(ARG_FIRST_NAME, firstName);
        args.putString(ARG_LAST_NAME, lastName);
        args.putString(ARG_EMAIL, email);
        dialog.setArguments(args);
        return dialog;
    }

    public void setEditProfileDialogListener(EditProfileDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setStyle(DialogFragment.STYLE_NO_TITLE, R.style.YourDialogStyle); // Optional styling
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = EditProfileDialogBinding.inflate(inflater, container, false);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupToolbar();
        populateInitialData();
        setupButtonListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarEditProfile;
        toolbar.inflateMenu(R.menu.toolbar_menu); // Re-use menu with R.id.btnClose
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.btnClose) {
                dismiss();
                return true;
            }
            return false;
        });
    }

    private void populateInitialData() {
        if (getArguments() != null) {
            binding.etEditFirstName.setText(getArguments().getString(ARG_FIRST_NAME, ""));
            binding.etEditLastName.setText(getArguments().getString(ARG_LAST_NAME, ""));
            binding.etEditEmail.setText(getArguments().getString(ARG_EMAIL, ""));
        }
    }

    private void setupButtonListeners() {
        binding.btnDialogSaveChanges.setOnClickListener(v -> processSaveChanges());
        binding.btnDialogCancelEdit.setOnClickListener(v -> dismiss());
    }

    private void processSaveChanges() {
        String firstName = binding.etEditFirstName.getText().toString().trim();
        String lastName = binding.etEditLastName.getText().toString().trim();
        String email = binding.etEditEmail.getText().toString().trim();
        String currentPassword = binding.etEditCurrentPassword.getText().toString(); // No trim for passwords
        String newPassword = binding.etEditNewPassword.getText().toString();
        String confirmPassword = binding.etEditConfirmPassword.getText().toString();

        boolean basicInfoValid = true;

        if (TextUtils.isEmpty(firstName)) {
            binding.tilEditFirstName.setError("First name cannot be empty");
            basicInfoValid = false;
        } else {
            binding.tilEditFirstName.setError(null);
        }

        // Basic Password Change Validation (can be improved)
        if (TextUtils.isEmpty(email)) {
            binding.tilEditEmail.setError("Email cannot be empty");
            basicInfoValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEditEmail.setError("Invalid email format");
            basicInfoValid = false;
        } else {
            binding.tilEditEmail.setError(null);
        }

        if (!basicInfoValid) {
            return; // Stop if basic info is not valid
        }

        // --- Password Change Logic ---
        boolean wantsToChangePassword = !TextUtils.isEmpty(currentPassword) ||
                !TextUtils.isEmpty(newPassword) ||
                !TextUtils.isEmpty(confirmPassword);
        boolean passwordChangeValid = true;

        if (wantsToChangePassword) {
            Log.d(TAG, "User wants to change password.");
            if (TextUtils.isEmpty(currentPassword)) {
                binding.tilEditCurrentPassword.setError("Enter current password to change");
                passwordChangeValid = false;
            } else {
                binding.tilEditCurrentPassword.setError(null);
            }

            if (TextUtils.isEmpty(newPassword)) {
                binding.tilEditNewPassword.setError("New password cannot be empty");
                passwordChangeValid = false;
            } else if (newPassword.length() < 6) { // Example: min 6 chars
                binding.tilEditNewPassword.setError("New password too short (min 6 chars)");
                passwordChangeValid = false;
            } else {
                binding.tilEditNewPassword.setError(null);
            }

            if (!newPassword.equals(confirmPassword)) {
                binding.tilEditConfirmPassword.setError("Passwords do not match");
                passwordChangeValid = false;
            } else {
                binding.tilEditConfirmPassword.setError(null);
            }

            if (!passwordChangeValid) {
                return; // Stop if password change fields are invalid
            }
        } else {
            Log.d(TAG, "User does not want to change password (all password fields empty or some empty).");
        }



        if (listener != null) {
            // Always callback for name/email updates
            listener.onProfileUpdated(firstName, lastName, email);

            // If user attempted a valid password change, callback for that too
            if (wantsToChangePassword && passwordChangeValid) {
                listener.onPasswordChanged(currentPassword, newPassword);
            }
        }
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT); // Height wraps content
            window.setGravity(Gravity.CENTER);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}