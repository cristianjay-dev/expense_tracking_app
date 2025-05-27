package com.shop.expensestrackingapp;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.shop.expensestrackingapp.databinding.ActivityAddExpenseBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {
    ActivityAddExpenseBinding binding; // ViewBinding instance

    private DatabaseGateway dbGateway;
    private SessionManager sessionManager; // For getting current user ID
    private List<CategoryItem> categoryList;
    private ArrayAdapter<CategoryItem> categoryAdapter;
    private int currentUserId = -1;
    private CategoryItem selectedCategory;
    private static final String TAG = "AddExpenseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddExpenseBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbarExpense);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        dbGateway = new DatabaseGateway(this);
        sessionManager = new SessionManager(this);

        currentUserId = sessionManager.getUserId();
        Log.d(TAG, "Current User ID: " + currentUserId);
        if (currentUserId == -1) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "User ID not found in session. Cannot add expense.");
            finish();
            return;
        }

        loadCategoriesIntoDropdown();

        binding.btnSave.setOnClickListener(v -> saveExpense());
    }

    private void loadCategoriesIntoDropdown() {
        Log.d(TAG, "loadCategoriesIntoDropdown: Starting to load categories.");
        categoryList = new ArrayList<>();
        // Initialize to null
        try (Cursor cursor = dbGateway.getAllCategories()) {

            if (cursor != null) {
                Log.d(TAG, "Cursor count for categories: " + cursor.getCount());
                if (cursor.getCount() == 0) {
                    Log.w(TAG, "No categories found in the database.");
                    Toast.makeText(this, "No categories available. Please check database setup.", Toast.LENGTH_LONG).show();
                }

                int idColIndex = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_CATEGORY_ID);
                int nameColIndex = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_CATEGORY_NAME);
                int iconColIndex = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_CATEGORY_ICON_IDENTIFIER);

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(idColIndex);
                    String name = cursor.getString(nameColIndex);
                    String iconId = cursor.getString(iconColIndex);
                    categoryList.add(new CategoryItem(id, name, iconId));
                    Log.v(TAG, "Loaded category: Name=" + name + ", ID=" + id); // Verbose log for each item
                }
            } else {
                Log.e(TAG, "getAllCategories() returned a null cursor.");
                Toast.makeText(this, "Could not load categories (null cursor).", Toast.LENGTH_LONG).show();
            }
        } catch (
                Exception e) { // Catch any potential exception during DB interaction or cursor processing
            Log.e(TAG, "Error loading categories: " + e.getMessage(), e);
            Toast.makeText(this, "An error occurred while loading categories.", Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, "Number of categories loaded into list: " + categoryList.size());

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoryList);
        binding.autoComplete.setAdapter(categoryAdapter);
        // Optionally, to make the dropdown show on focus/touch if it's not already:
        // binding.autoComplete.setOnFocusChangeListener((v, hasFocus) -> {
        //    if (hasFocus && categoryList != null && !categoryList.isEmpty()) {
        //        binding.autoComplete.showDropDown();
        //    }
        // });
        // binding.autoComplete.setOnClickListener(v -> {
        //     if (categoryList != null && !categoryList.isEmpty()) {
        //        binding.autoComplete.showDropDown();
        //    }
        // });


        binding.autoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = (CategoryItem) parent.getItemAtPosition(position);
                if (selectedCategory != null) {
                    Log.d(TAG, "Selected category: " + selectedCategory.getName() + " (ID: " + selectedCategory.getId() + ")");
                    binding.txtDropdown.setError(null);
                }
            }
        });
    }

    private void saveExpense() {
        // ... (saveExpense logic remains the same)
        if (currentUserId == -1) {
            Toast.makeText(this, "Cannot save expense: User not identified.", Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to add expense. Please try again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to insert expense into database.");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity finishing");
        // if (dbGateway != null) {
        //     dbGateway.close();
        // }
    }
         // Close the activity when the close button is clicked
}
