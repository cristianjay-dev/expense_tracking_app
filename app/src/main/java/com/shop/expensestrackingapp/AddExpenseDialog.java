package com.shop.expensestrackingapp;

import android.app.Dialog;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import com.shop.expensestrackingapp.databinding.ActivityAddExpenseBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddExpenseDialog extends DialogFragment {
    public interface AddExpenseDialogListener {
        void onExpenseAddedSuccess();
        // Add other methods if needed
    }
    private AddExpenseDialogListener listener;

    // Call this from the calling Fragment/Activity to set the listener
    // Make sure the calling Fragment/Activity implements AddExpenseDialogListener
    public void setAddExpenseDialogListener(AddExpenseDialogListener listener) {
        this.listener = listener;
    }
    private ActivityAddExpenseBinding binding; // Use ViewBinding for the layout
    private DatabaseGateway dbGateway;
    private SessionManager sessionManager;
    private List<CategoryItem> categoryList;
    private ArrayAdapter<CategoryItem> categoryAdapter;
    private CategoryItem selectedCategory;

    private int currentUserId = -1;

    private static final String TAG = "AddExpenseDialog";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Optional: Apply a style that defines window properties like no title, background
        // setStyle(DialogFragment.STYLE_NO_TITLE, R.style.YourCustomDialogStyle); // Example
        // If your layout's background is already set, STYLE_NORMAL might be fine.

        dbGateway = new DatabaseGateway(requireContext());
        sessionManager = new SessionManager(requireContext());

        currentUserId = sessionManager.getUserId();
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Error: User not logged in.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "User ID not found in session. Cannot add expense.");
            dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityAddExpenseBinding.inflate(inflater, container, false);
        // If you want a transparent background for the dialog window itself, so only your layout's background shows:
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = binding.toolbarExpense;
        // toolbar.setTitle(""); // Not needed if using TextView and getSupportActionBar().setDisplayShowTitleEnabled(false)
        if (getActivity() instanceof AppCompatActivity) { // Ensure activity is AppCompatActivity
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            // Hide default title if using a custom TextView in toolbar
            if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
                // No need to setDisplayHomeAsUpEnabled(true) for DialogFragment's toolbar usually,
                // as the menu item handles closing.
            }
        }

        toolbar.inflateMenu(R.menu.toolbar_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.btnClose) {
                dismiss();
                return true;
            }
            return false;
        });

        loadCategoriesIntoDropdown();
        binding.btnSave.setOnClickListener(v -> saveExpense());
    }

    private void loadCategoriesIntoDropdown() {
        Log.d(TAG, "loadCategoriesIntoDropdown: Starting to load categories.");
        categoryList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = dbGateway.getAllCategories();

            if (cursor != null) {
                Log.d(TAG, "Cursor count for categories: " + cursor.getCount());
                if (cursor.getCount() == 0) {
                    Log.w(TAG, "No categories found in the database.");
                    Toast.makeText(requireContext(), "No categories available.", Toast.LENGTH_LONG).show();
                }

                int idColIndex = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_CATEGORY_ID);
                int nameColIndex = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_CATEGORY_NAME);
                int iconColIndex = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_CATEGORY_ICON_IDENTIFIER);

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(idColIndex);
                    String name = cursor.getString(nameColIndex);
                    String iconId = cursor.getString(iconColIndex);
                    categoryList.add(new CategoryItem(id, name, iconId));
                    Log.v(TAG, "Loaded category: Name=" + name + ", ID=" + id);
                }
            } else {
                Log.e(TAG, "getAllCategories() returned a null cursor.");
                Toast.makeText(requireContext(), "Could not load categories (null cursor).", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading categories: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "An error occurred while loading categories.", Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d(TAG, "Number of categories loaded into list: " + categoryList.size());

        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList);
        binding.autoComplete.setAdapter(categoryAdapter);

        binding.autoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = (CategoryItem) parent.getItemAtPosition(position);
                if (selectedCategory != null) {
                    Log.d(TAG, "Selected category: " + selectedCategory.getName() + " (ID: " + selectedCategory.getId() + ")");
                    if (binding != null && binding.txtDropdown != null) { // Null check for binding
                        binding.txtDropdown.setError(null);
                    }
                }
            }
        });
    }

    // --- PASTE THE saveExpense METHOD HERE ---
    private void saveExpense() {
        if (binding == null) { // Check if binding is null (e.g., if view is destroyed)
            Log.e(TAG, "saveExpense called when binding is null.");
            return;
        }
        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "Cannot save expense: User not identified.", Toast.LENGTH_LONG).show();
            return;
        }

        String amountStr = binding.txtAmount.getText().toString().trim();
        String note = binding.txtNote.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            binding.txtAmount.setError("Amount cannot be empty");
            binding.txtAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                binding.txtAmount.setError("Amount must be positive");
                binding.txtAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            binding.txtAmount.setError("Invalid amount format");
            binding.txtAmount.requestFocus();
            return;
        }

        if (selectedCategory == null) {
            binding.txtDropdown.setError("Please select a category");
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        } else {
            binding.txtDropdown.setError(null);
        }

        SimpleDateFormat dbSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = dbSdf.format(new Date());

        Log.d(TAG, "Attempting to save expense: UserID=" + currentUserId +
                ", CategoryID=" + selectedCategory.getId() + ", Amount=" + amount +
                ", Note='" + note + "', Timestamp=" + timestamp);

        boolean success = dbGateway.addExpense(currentUserId, selectedCategory.getId(), amount, note, timestamp);

        if (success) {
            Toast.makeText(requireContext(), "Expense added successfully!", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onExpenseAddedSuccess();
            }
            dismiss();
        } else {
            Toast.makeText(requireContext(), "Failed to add expense. Please try again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to insert expense into database.");
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
                window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
                window.setGravity(Gravity.CENTER);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called, setting binding to null.");
        binding = null; // Crucial to prevent memory leaks
    }
}
