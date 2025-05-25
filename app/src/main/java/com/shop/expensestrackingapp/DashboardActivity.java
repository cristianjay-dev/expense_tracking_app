package com.shop.expensestrackingapp;

import android.app.Dialog;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.shop.expensestrackingapp.databinding.ActivityDashboardBinding;

import java.util.Objects;

public class DashboardActivity extends AppCompatActivity {

    ActivityDashboardBinding binding;

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
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment()); // Assuming you have a HomeFragment
        }

        binding.navHome.setOnClickListener(v -> loadFragment(new HomeFragment()));
        binding.navHistory.setOnClickListener(v -> loadFragment(new HistoryFragment()));
        binding.navStats.setOnClickListener(v -> loadFragment(new StatsFragment()));
        binding.navProfile.setOnClickListener(v -> loadFragment(new ProfileFragment()));

        binding.fabAddExpense.setOnClickListener( v -> showDialog());
    }
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.frameLayout.getId(), fragment)
                .commit();
    }
    private void showDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_add_expense);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.popup_modal);
        Toolbar toolbar = dialog.findViewById(R.id.toolbarExpense);

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