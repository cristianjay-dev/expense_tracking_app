package com.shop.expensestrackingapp;

import static android.content.ContentValues.TAG;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.shop.expensestrackingapp.databinding.ActivityDashboardBinding;

import java.util.Objects;

public class DashboardActivity extends AppCompatActivity implements AddExpenseDialog.AddExpenseDialogListener {

    ActivityDashboardBinding binding;
    SessionManager sessionManager;
    private static final String TAG = "DashboardActivity"; // Define TAG for this class
    private static final String HOME_FRAGMENT_TAG = "HomeFragmentInstanceTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;

        });
        sessionManager = new SessionManager(this);

        // Set user's first name to txtUser
        String firstName = sessionManager.getFirstName();
        if (firstName != null) {
            binding.txtUser.setText(firstName);
        } else {
            binding.txtUser.setText("User"); // Fallback
            Log.w(TAG, "First name not found in session for txtUser.");
        }


        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), HOME_FRAGMENT_TAG); // Load with tag
        }

        binding.navHome.setOnClickListener(v -> loadFragment(new HomeFragment(), HOME_FRAGMENT_TAG));
        binding.navHistory.setOnClickListener(v -> loadFragment(new HistoryFragment(), "HistoryFragmentTag"));
        binding.navStats.setOnClickListener(v -> loadFragment(new StatsFragment(), "StatsFragmentTag"));
        binding.navProfile.setOnClickListener(v -> loadFragment(new ProfileFragment(), "ProfileFragmentTag"));

        binding.fabAddExpense.setOnClickListener(v -> showAddExpenseDialog());
    }

    // Modified to accept and use a tag
    private void loadFragment(Fragment fragment, String tag) {
        Log.d(TAG, "Loading fragment: " + fragment.getClass().getSimpleName() + " with tag: " + tag);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.frameLayout.getId(), fragment, tag) // Use the tag
                .commit();
    }

    private void showAddExpenseDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        AddExpenseDialog addExpenseDialog = new AddExpenseDialog();

        // Set DashboardActivity as the listener for the dialog
        addExpenseDialog.setAddExpenseDialogListener(this);

        addExpenseDialog.show(fragmentManager, "AddExpenseDialogTag");
        Log.d(TAG, "Attempting to show AddExpenseDialog.");
    }

    // Implementation of the listener method from AddExpenseDialog
    @Override
    public void onExpenseAddedSuccess() {
        Log.d(TAG, "onExpenseAddedSuccess callback received in DashboardActivity.");

        // Find the HomeFragment instance using the tag
        Fragment homeFragment = getSupportFragmentManager().findFragmentByTag(HOME_FRAGMENT_TAG);

        if (homeFragment instanceof HomeFragment) {
            Log.d(TAG, "HomeFragment found. Calling refreshAllData().");
            ((HomeFragment) homeFragment).refreshAllData(); // Call public method in HomeFragment
        } else {
            Log.w(TAG, "HomeFragment not found with tag: " + HOME_FRAGMENT_TAG +
                    ". Cannot call refreshAllData directly. HomeFragment's onResume should handle refresh if it becomes active.");
            if (homeFragment != null) {
                Log.w(TAG, "Fragment found by tag is of type: " + homeFragment.getClass().getSimpleName());
            }
        }
    }
}