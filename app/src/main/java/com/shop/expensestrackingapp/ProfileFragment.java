package com.shop.expensestrackingapp;


import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.shop.expensestrackingapp.databinding.FragmentProfileBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;


public class ProfileFragment extends Fragment implements EditProfileDialog.EditProfileDialogListener{
    private FragmentProfileBinding binding;
    private SessionManager sessionManager;
    private DatabaseGateway dbHelper;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Bitmap selectedImageBitmap; // To hold the image selected from gallery
    private int currentUserId;
    private static final long MAX_INITIAL_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB limit for initial selection
    private static final int PROFILE_IMAGE_MAX_DIMENSION = 512;
    private static final int JPEG_COMPRESSION_QUALITY = 85;
    private static final int PNG_COMPRESSION_QUALITY = 100;
    private static final long MAX_PROCESSED_IMAGE_SIZE_BYTES = 1 * 1024 * 1024;
    private static final String TAG = "ProfileFragment";//


    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        dbHelper = new DatabaseGateway(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentUserId = sessionManager.getUserId();
        Log.d(TAG, "Current User ID in onCreate: " + currentUserId);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (binding == null) return; // Fragment view might be destroyed
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            long fileSize = getFileSizeFromUri(imageUri);
                            if (fileSize == -1 && getContext() != null) { // Check getContext() for Toast
                                Toast.makeText(getContext(), "Could not determine image size.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (fileSize > MAX_INITIAL_FILE_SIZE_BYTES) {
                                if (getContext() != null) {
                                    String sizeMB = String.format(Locale.US, "%.1f", MAX_INITIAL_FILE_SIZE_BYTES / (1024.0 * 1024.0));
                                    Toast.makeText(getContext(), "Selected image is too large. Max " + sizeMB + "MB.", Toast.LENGTH_LONG).show();
                                }
                                selectedImageBitmap = null;
                                binding.profileImage.setImageResource(R.drawable.profile);
                                return;
                            }
                            Log.d(TAG, "Selected image file size: " + fileSize / 1024 + "KB");
                            loadBitmapFromUri(imageUri);
                        }
                    }
                });
    }
    private long getFileSizeFromUri(Uri uri) {
        if (getContext() == null) {
            Log.e(TAG, "getFileSizeFromUri: Context is null!");
            return -1;
        }
        ContentResolver contentResolver = getContext().getContentResolver();
        Cursor cursor = null;
        try {
            // Try to query for the size column directly
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex);
                }
            }
            // Fallback for Uris that don't support OpenableColumns.SIZE (e.g., some file Uris)
            // This method is less efficient as it opens the file.
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                if (uri.getPath() != null) {
                    java.io.File file = new java.io.File(uri.getPath());
                    if (file.exists()) {
                        return file.length();
                    }
                }
            }
            // Another fallback: open a ParcelFileDescriptor (can be slow)
            ParcelFileDescriptor pfd = contentResolver.openFileDescriptor(uri, "r");
            if (pfd != null) {
                long size = pfd.getStatSize();
                pfd.close();
                if (size >= 0) return size; // getStatSize returns -1 on error
            }

        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Error getting file size from URI: " + uri, e);
            return -1; // Indicate error
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.w(TAG, "Could not determine file size for URI: " + uri + ". Proceeding without size check for this URI.");
        return 0; // Or -1 to enforce failure if size unknown. 0 allows proceeding. Let's allow to proceed if size unknown.
    }


    private void loadBitmapFromUri(Uri imageUri) {
        if (getContext() == null || getActivity() == null) { // Check context/activity
            Log.e(TAG, "loadBitmapFromUri: Context or Activity is null!");
            return;
        }
        InputStream imageStream = null;
        try {
            imageStream = requireActivity().getContentResolver().openInputStream(imageUri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            // Add inJustDecodeBounds for a preliminary check if needed, but file size check is primary here
            selectedImageBitmap = BitmapFactory.decodeStream(imageStream, null, options);

            if (selectedImageBitmap != null) {
                binding.profileImage.setImageBitmap(selectedImageBitmap);
                Log.d(TAG, "Selected image decoded. Has alpha: " + selectedImageBitmap.hasAlpha());
            } else {
                Toast.makeText(getContext(), "Failed to decode image", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "BitmapFactory.decodeStream returned null for URI: " + imageUri);
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException loading image from URI: " + imageUri, e);
            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            selectedImageBitmap = null;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "OutOfMemoryError loading image from URI: " + imageUri, e);
            Toast.makeText(getContext(), "Image is too large to load into memory despite size check. Try a smaller resolution image.", Toast.LENGTH_LONG).show();
            selectedImageBitmap = null;
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing image input stream.", e);
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Ensure session and user ID are valid before proceeding
        if (!sessionManager.isLoggedIn() || currentUserId == -1) {
            Log.w(TAG, "User not logged in or invalid ID. Redirecting to login.");
            redirectToLogin();
            return view;
        }

        loadUserProfileData(currentUserId);

        binding.editIcon.setOnClickListener(v -> openImagePicker());
        binding.btnSaveProfile.setOnClickListener(v -> processAndSaveProfileImage());
        binding.btnLogOut.setOnClickListener(v -> {
            sessionManager.logout();
            redirectToLogin();
        });
        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        return view;
    }
    private void showEditProfileDialog() {
        if (binding == null || getContext() == null || sessionManager == null) {
            Log.e(TAG, "Cannot show edit dialog, critical components missing.");
            if (getContext() != null) Toast.makeText(getContext(), "Error preparing edit profile.", Toast.LENGTH_SHORT).show();
            return;

        }
        String currentFirstName = "";
        String currentLastName = "";
        String currentEmail = sessionManager.getEmail();

        Cursor userCursor = null;
        try {
            userCursor = dbHelper.getUserById(currentUserId);
            if (userCursor != null && userCursor.moveToFirst()) {
                currentFirstName = userCursor.getString(userCursor.getColumnIndexOrThrow(DatabaseGateway.COL_FIRSTNAME));
                currentLastName = userCursor.getString(userCursor.getColumnIndexOrThrow(DatabaseGateway.COL_LASTNAME));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching user details for edit dialog", e);
        } finally {
            if (userCursor != null) userCursor.close();

        }
        FragmentManager fragmentManager = getParentFragmentManager(); // Use getParentFragmentManager in Fragments
        EditProfileDialog editDialog = EditProfileDialog.newInstance(
                currentFirstName != null ? currentFirstName : "",
                currentLastName != null ? currentLastName : "",
                currentEmail != null ? currentEmail : ""
        );
        editDialog.setEditProfileDialogListener(this); // Set ProfileFragment as the listener
        editDialog.show(fragmentManager, "EditProfileDialogTag");
        Log.d(TAG, "Showing EditProfileDialog with FirstName: " + currentFirstName + ", Email: " + currentEmail);
    }

    private void loadUserProfileData(int userId) {
        if (binding == null || getContext() == null || dbHelper == null) {
            Log.e(TAG, "loadUserProfileData: Pre-requisites not met (binding, context, or dbHelper is null).");
            return;
        }

        Cursor cursor = null;
        try {
            cursor = dbHelper.getUserById(userId);
            if (cursor != null && cursor.moveToFirst()) {
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseGateway.COL_FIRSTNAME));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseGateway.COL_LASTNAME));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseGateway.COL_EMAIL));
                byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(DatabaseGateway.COL_PROFILE_IMAGE));

                String fullName = (firstName != null ? firstName : "") + (lastName != null && !lastName.isEmpty() ? " " + lastName : "");
                binding.textName.setText(fullName.trim().isEmpty() ? "N/A" : fullName.trim());
                binding.textEmail.setText(email != null ? email : "N/A");

                if (imageBytes != null && imageBytes.length > 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    if (bitmap != null) {
                        binding.profileImage.setImageBitmap(bitmap);
                    } else {
                        Log.w(TAG, "Failed to decode profile image from DB for userId: " + userId);
                        binding.profileImage.setImageResource(R.drawable.profile);
                    }
                } else {
                    binding.profileImage.setImageResource(R.drawable.profile);
                }
            } else {
                Log.w(TAG, "No user data found in DB for userId: " + userId);
                // Toast.makeText(getContext(), "User data not found.", Toast.LENGTH_SHORT).show(); // Optional: can be noisy
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user profile data for userId: " + userId, e);
            Toast.makeText(getContext(), "Error loading profile.", Toast.LENGTH_SHORT).show();
            if (e instanceof android.database.sqlite.SQLiteBlobTooBigException) {
                Toast.makeText(getContext(), "Profile image in DB is too large to display.", Toast.LENGTH_LONG).show();
            }
            binding.profileImage.setImageResource(R.drawable.profile);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    private void openImagePicker() {
        // ... (same as before)
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            imagePickerLauncher.launch(intent);
        } else {
            Toast.makeText(getContext(), "No application available to pick images.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "No activity found to handle ACTION_PICK for images.");
        }
    }

    private byte[] resizeAndCompressBitmap(Bitmap originalBitmap) {
        // ... (same as before)
        if (originalBitmap == null) {
            Log.w(TAG, "resizeAndCompressBitmap: originalBitmap is null.");
            return null;
        }

        Bitmap bitmapToProcess = originalBitmap;

        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();
        if (originalWidth > PROFILE_IMAGE_MAX_DIMENSION || originalHeight > PROFILE_IMAGE_MAX_DIMENSION) {
            float ratio = Math.min(
                    (float) PROFILE_IMAGE_MAX_DIMENSION / originalWidth,
                    (float) PROFILE_IMAGE_MAX_DIMENSION / originalHeight);
            int newWidth = Math.round(originalWidth * ratio);
            int newHeight = Math.round(originalHeight * ratio);
            try {
                bitmapToProcess = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
                Log.d(TAG, "Image resized from " + originalWidth + "x" + originalHeight + " to " + newWidth + "x" + newHeight);
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "OutOfMemoryError while resizing bitmap.", e);
                bitmapToProcess = originalBitmap;
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Bitmap.CompressFormat compressFormat;
        int quality;

        if (bitmapToProcess.hasAlpha()) {
            compressFormat = Bitmap.CompressFormat.PNG;
            quality = PNG_COMPRESSION_QUALITY;
            Log.d(TAG, "Compressing as PNG (has alpha).");
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                compressFormat = Bitmap.CompressFormat.WEBP_LOSSY;
                Log.d(TAG, "Compressing as WEBP_LOSSY.");
            } else {
                compressFormat = Bitmap.CompressFormat.JPEG;
                Log.d(TAG, "Compressing as JPEG.");
            }
            quality = JPEG_COMPRESSION_QUALITY;
        }

        try {
            if (bitmapToProcess.compress(compressFormat, quality, outputStream)) {
                byte[] imageBytes = outputStream.toByteArray();
                Log.d(TAG, "Compressed image to " + compressFormat.name() + ", size: " + imageBytes.length / 1024 + "KB");
                return imageBytes;
            } else {
                Log.e(TAG, "Bitmap.compress returned false for format: " + compressFormat.name());
                if (compressFormat != Bitmap.CompressFormat.JPEG) {
                    Log.d(TAG, "Attempting fallback compression to JPEG.");
                    outputStream.reset();
                    if (bitmapToProcess.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, outputStream)) {
                        byte[] imageBytes = outputStream.toByteArray();
                        Log.d(TAG, "Fallback compression to JPEG successful, size: " + imageBytes.length / 1024 + "KB");
                        return imageBytes;
                    } else {
                        Log.e(TAG, "Fallback JPEG compression also failed.");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during bitmap compression.", e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing ByteArrayOutputStream.", e);
            }
        }
        return null;
    }
    public void onProfileUpdated(String newFirstName, String newLastName, String newEmail) {
        Log.d(TAG, "onProfileUpdated callback: Name=" + newFirstName + " " + newLastName + ", Email=" + newEmail);
        if (dbHelper == null || sessionManager == null || currentUserId == -1) {
            if (getContext() != null) Toast.makeText(getContext(), "Error updating profile. Session/DB invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        String oldEmail = sessionManager.getEmail();
        if (!newEmail.equalsIgnoreCase(oldEmail) && dbHelper.checkEmailExists(newEmail)) {
            Toast.makeText(getContext(), "This email is already registered.", Toast.LENGTH_LONG).show();
            return;
        }

        boolean success = dbHelper.updateUserProfile(currentUserId, newFirstName, newLastName, newEmail);
        if (success) {
            Toast.makeText(getContext(), "Profile details updated successfully!", Toast.LENGTH_SHORT).show();
            sessionManager.updateFirstName(newFirstName); // Assuming your SessionManager has this
            sessionManager.updateEmail(newEmail);
            loadUserProfileData(currentUserId); // Refresh UI
        } else {
            Toast.makeText(getContext(), "Failed to update profile details.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPasswordChanged(String currentPassword, String newPassword) {
        Log.d(TAG, "onPasswordChanged callback received.");
        if (dbHelper == null || currentUserId == -1) {
            if (getContext() != null) Toast.makeText(getContext(), "Error changing password. Session/DB invalid.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!dbHelper.verifyPassword(currentUserId, currentPassword)) { // You need to implement verifyPassword
            Toast.makeText(getContext(), "Current password incorrect.", Toast.LENGTH_LONG).show();

            return;
        }
        boolean success = dbHelper.updateUserPassword(currentUserId, newPassword); // You need to implement updateUserPassword
        if (success) {
            Toast.makeText(getContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show();
            // No UI refresh needed here typically, unless you display "password last changed"
        } else {
            Toast.makeText(getContext(), "Failed to change password.", Toast.LENGTH_SHORT).show();
        }
    }


    private void processAndSaveProfileImage() {
        // ... (name changed from MAX_IMAGE_SIZE_BYTES to MAX_PROCESSED_IMAGE_SIZE_BYTES for clarity)
        if (selectedImageBitmap == null) {
            Toast.makeText(getContext(), "No new image selected.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserId == -1) {
            Toast.makeText(getContext(), "User session error. Cannot save.", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] imageBytes = resizeAndCompressBitmap(selectedImageBitmap);

        if (imageBytes == null) {
            Toast.makeText(getContext(), "Failed to process image.", Toast.LENGTH_LONG).show();
            return;
        }

        if (imageBytes.length > MAX_PROCESSED_IMAGE_SIZE_BYTES) {
            String sizeKB = String.format("%.1f", imageBytes.length / 1024.0);
            String maxAllowedKB = String.format("%.0f", MAX_PROCESSED_IMAGE_SIZE_BYTES / 1024.0);
            Toast.makeText(getContext(), "Image is too large after processing (" + sizeKB + "KB). Max allowed: " + maxAllowedKB + "KB.", Toast.LENGTH_LONG).show();
            return;
        }

        boolean updateSuccess = dbHelper.updateUserProfileImage(currentUserId, imageBytes);

        if (updateSuccess) {
            Toast.makeText(getContext(), "Profile image updated successfully!", Toast.LENGTH_SHORT).show();
            Bitmap processedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (processedBitmap != null) {
                binding.profileImage.setImageBitmap(processedBitmap);
            } else {
                Log.e(TAG, "Failed to decode processed image bytes after saving. Reloading profile.");
                loadUserProfileData(currentUserId);
            }
        } else {
            Toast.makeText(getContext(), "Failed to save image to database.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "dbHelper.updateUserProfileImage returned false for userId: " + currentUserId);
        }
        selectedImageBitmap = null;
    }

    private void redirectToLogin() {
        // ... (same as before)
        if (getActivity() != null && !getActivity().isFinishing()) {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        } else {
            Log.w(TAG, "redirectToLogin: Activity is null or finishing.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        Log.d(TAG, "onDestroyView called, binding set to null.");
    }
    private void showDialog() {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.activity_profile_edit);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.popup_modal);
        Toolbar toolbar = dialog.findViewById(R.id.toolbarEdit);

        // Inflate the toolbar menu
        toolbar.inflateMenu(R.menu.toolbar_menu);

        // Handle toolbar menu item click (like the close button)
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.btnClose) {
                dialog.dismiss(); // Close the dialog
                return true;
            }
            return false;
        });

        dialog.show();
    }

}