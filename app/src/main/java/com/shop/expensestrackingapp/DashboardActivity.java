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

public class DashboardActivity extends AppCompatActivity {

    ActivityDashboardBinding binding;
    SessionManager sessionManager;

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
        binding.txtUser.setText(firstName);


        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        binding.navHome.setOnClickListener(v -> loadFragment(new HomeFragment()));
        binding.navHistory.setOnClickListener(v -> loadFragment(new HistoryFragment()));
        binding.navStats.setOnClickListener(v -> loadFragment(new StatsFragment()));
        binding.navProfile.setOnClickListener(v -> loadFragment(new ProfileFragment()));

        binding.fabAddExpense.setOnClickListener( v -> showAddExpenseDialog());
    }
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.frameLayout.getId(), fragment)
                .commit();
    }
    private void showAddExpenseDialog() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        AddExpenseDialog addExpenseDialog = new AddExpenseDialog(); // Create an instance of your DialogFragment

        // You can set the listener later when HomeFragment is ready
        // addExpenseDialog.setAddExpenseDialogListener(this_or_homeFragment_instance);

        addExpenseDialog.show(fragmentManager, "AddExpenseDialogTag"); // Show the dialog
        Log.d(TAG, "Attempting to show AddExpenseDialog.");
    }

}