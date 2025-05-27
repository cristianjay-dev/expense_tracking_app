package com.shop.expensestrackingapp;


import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.shop.expensestrackingapp.databinding.FragmentHomeBinding;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements ExpenseAdapter.OnExpenseActionsListener {

    private FragmentHomeBinding binding; // ViewBinding for fragment_home.xml
    private DatabaseGateway dbGateway;
    private SessionManager sessionManager;
    private ExpenseAdapter expenseAdapter;
    private List<ExpenseModel> expenseList;

    private int currentUserId = -1;
    private String selectedPeriodType = "WEEKLY"; // Default period
    private String currentPeriodStartDate, currentPeriodEndDate;

    // Date formatters
    private final SimpleDateFormat sdfDisplayExpense = new SimpleDateFormat("dd MMM - hh:mma", Locale.getDefault()); // e.g., 16 Dec - 02:34PM
    private final SimpleDateFormat sdfDbTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat sdfDbDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat sdfDisplayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private double currentBudgetAmount = 0.0;
    private double currentTotalSpent = 0.0;

    private static final String TAG = "HomeFragment";

    public HomeFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Initializing...");
        dbGateway = new DatabaseGateway(requireContext());
        sessionManager = new SessionManager(requireContext());
        currentUserId = sessionManager.getUserId();
        Log.d(TAG, "Current User ID in onCreate: " + currentUserId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: Inflating layout.");
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Setting up UI components.");

        if (currentUserId == -1) {
            Toast.makeText(getContext(), "User session error. Please log in again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "User ID is -1 in onViewCreated. Cannot proceed.");
            binding.tvRecentExpensesLabel.setText("Error: User not logged in");
            if (binding.recyclerViewExpenses != null) binding.recyclerViewExpenses.setVisibility(View.GONE);
            return;
        }

        setupBudgetPeriodDropdown();
        setupRecyclerView(); // Initializes expenseList and sets up adapter with this list
        setupActionButtons();

        updateDateRangeForSelectedPeriod();
        // refreshAllData() will be called in onResume for the initial load
    }

    private void setupBudgetPeriodDropdown() {
        String[] periods = new String[]{"This Week", "This Month", "All Time"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line, // Or a custom dropdown item layout
                periods
        );
        binding.actvBudgetPeriod.setAdapter(adapter);
        binding.actvBudgetPeriod.setText(periods[0], false); // Default to "This Week"

        binding.actvBudgetPeriod.setOnItemClickListener((parent, Lview, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            Log.d(TAG, "Budget period selected: " + selected);
            if ("This Week".equals(selected)) {
                selectedPeriodType = "WEEKLY";
            } else if ("This Month".equals(selected)) {
                selectedPeriodType = "MONTHLY";
            } else if ("All Time".equals(selected)) {
                selectedPeriodType = "ALL_TIME";
            }
            updateDateRangeForSelectedPeriod();
            refreshAllData();
        });
    }

    private void updateDateRangeForSelectedPeriod() {
        Calendar calendar = Calendar.getInstance(); // Use android.icu.util.Calendar
        if ("WEEKLY".equals(selectedPeriodType)) {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            currentPeriodStartDate = sdfDbDateOnly.format(calendar.getTime());
            calendar.add(Calendar.DAY_OF_WEEK, 6);
            currentPeriodEndDate = sdfDbDateOnly.format(calendar.getTime());
        } else if ("MONTHLY".equals(selectedPeriodType)) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            currentPeriodStartDate = sdfDbDateOnly.format(calendar.getTime());
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            currentPeriodEndDate = sdfDbDateOnly.format(calendar.getTime());
        } else { // ALL_TIME
            currentPeriodStartDate = "1970-01-01"; // A very early date
            currentPeriodEndDate = "2999-12-31";   // A very far future date
        }
        Log.d(TAG, "Date range updated for " + selectedPeriodType + ": " + currentPeriodStartDate + " to " + currentPeriodEndDate);
    }

    private void setupRecyclerView() {
        expenseList = new ArrayList<>();
        expenseAdapter = new ExpenseAdapter(requireContext(), expenseList, this);
        binding.recyclerViewExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewExpenses.setAdapter(expenseAdapter);
        binding.recyclerViewExpenses.setNestedScrollingEnabled(false);
        Log.d(TAG, "RecyclerView and Adapter setup complete. Adapter is using fragment's expenseList instance.");
    }

    private void setupActionButtons() {
        // The FAB in DashboardActivity should handle launching AddExpenseDialog.
        // This button might be for a different action or can be removed if redundant.
        binding.btnAddExpenseFromActions.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Use the floating button to add expenses.", Toast.LENGTH_SHORT).show();
            // If HomeFragment were to launch it directly (less ideal if FAB is global):
            // showAddExpenseDialog(); // You would need this method or an ActivityResultLauncher
        });

        binding.tvManageExpenses.setOnClickListener(v -> {
            if (expenseAdapter != null) {
                boolean newMode = !expenseAdapter.isManageModeEnabled();
                expenseAdapter.setManageMode(newMode);
                binding.tvManageExpenses.setText(newMode ? "Done" : "Manage");
                binding.tvManageExpenses.setTextColor(ContextCompat.getColor(requireContext(),
                        newMode ? android.R.color.white : R.color.colorPrimary)); // Example color change
                Log.d(TAG, "Manage mode toggled to: " + newMode);
            }
        });

        binding.btnSetBudget.setOnClickListener(v -> {
            // TODO: Implement showSetBudgetDialog()
            Toast.makeText(getContext(), "Set Budget functionality to be implemented.", Toast.LENGTH_SHORT).show();
        });
    }

    // Public method to be called by DashboardActivity after an expense is added
    public void refreshAllData() {
        Log.d(TAG, "refreshAllData called for user ID: " + currentUserId);
        if (!isAdded() || currentUserId == -1 || currentPeriodStartDate == null || currentPeriodEndDate == null) {
            Log.w(TAG, "Cannot refresh data: Fragment not added, User ID invalid, or date range not set.");
            if (this.expenseList != null) this.expenseList.clear(); // Clear the fragment's list
            if (expenseAdapter != null) expenseAdapter.notifyDataSetChanged(); // Notify adapter
            updateUIWithNoData();
            return;
        }
        loadBudgetData();
        loadExpensesData(); // This will populate the fragment's expenseList and notify the adapter
        updateBudgetUI();  // Update the budget summary part
    }

    private void loadBudgetData() {
        Log.d(TAG, "Loading budget data for period: " + selectedPeriodType);
        // Using current date to find the relevant budget period (week/month)
        Cursor budgetCursor = dbGateway.getCurrentBudget(currentUserId, selectedPeriodType, sdfDbDateOnly.format(new Date()));
        currentBudgetAmount = 0.0;
        String budgetSetDateStr = "Budget not set for this period";

        if (budgetCursor != null) {
            if (budgetCursor.moveToFirst()) {
                currentBudgetAmount = budgetCursor.getDouble(budgetCursor.getColumnIndexOrThrow(DatabaseGateway.COL_BUDGET_AMOUNT));
                String rawSetDate = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow(DatabaseGateway.COL_BUDGET_CREATED_AT));
                try {
                    if (rawSetDate != null) {
                        Date date = sdfDbTimestamp.parse(rawSetDate);
                        budgetSetDateStr = "Budget set on " + sdfDisplayDate.format(date);
                    }
                } catch (ParseException e) {
                    budgetSetDateStr = "Set on " + (rawSetDate != null ? rawSetDate.substring(0, Math.min(rawSetDate.length(), 10)) : "N/A");
                    Log.e(TAG, "Error parsing budget set date: " + rawSetDate, e);
                }
            }
            budgetCursor.close();
        } else {
            Log.w(TAG, "getCurrentBudget returned a null cursor.");
        }
        Log.d(TAG, "Budget Amount: " + currentBudgetAmount + ", Set Date: " + budgetSetDateStr);
        binding.tvBudgetUpdatedDate.setText(budgetSetDateStr);
    }

    private void loadExpensesData() {
        Log.d(TAG, "--- loadExpensesData START ---");
        Log.d(TAG, "User ID: " + currentUserId + ", Period: " + selectedPeriodType +
                ", StartDate: " + currentPeriodStartDate + ", EndDate: " + currentPeriodEndDate);

        if (dbGateway == null) {
            Log.e(TAG, "dbGateway is NULL in loadExpensesData!");
            if (this.expenseList != null) this.expenseList.clear();
            if (expenseAdapter != null) expenseAdapter.notifyDataSetChanged();
            return;
        }
        currentTotalSpent = dbGateway.getTotalExpensesForPeriod(currentUserId, currentPeriodStartDate, currentPeriodEndDate);
        Log.d(TAG, "Total spent in period: " + currentTotalSpent);

        Cursor expenseCursor = null;
        // Ensure this.expenseList is the one being modified. It was initialized in setupRecyclerView.
        this.expenseList.clear(); // Clear the list that the adapter is referencing

        try {
            expenseCursor = dbGateway.getExpensesForUserWithCategory(currentUserId, currentPeriodStartDate, currentPeriodEndDate);

            if (expenseCursor != null) {
                Log.d(TAG, "Expense cursor count: " + expenseCursor.getCount());
                if (expenseCursor.getCount() == 0) {
                    Log.i(TAG, "No expenses found for this period in the database.");
                }

                int idCol = expenseCursor.getColumnIndexOrThrow(DatabaseGateway.COL_EXPENSE_ID);
                int categoryNameCol = expenseCursor.getColumnIndexOrThrow(DatabaseGateway.COL_CATEGORY_NAME);
                int iconIdCol = expenseCursor.getColumnIndexOrThrow(DatabaseGateway.COL_CATEGORY_ICON_IDENTIFIER);
                int noteCol = expenseCursor.getColumnIndexOrThrow(DatabaseGateway.COL_EXPENSE_NOTE);
                int amountCol = expenseCursor.getColumnIndexOrThrow(DatabaseGateway.COL_EXPENSE_AMOUNT);
                int timestampCol = expenseCursor.getColumnIndexOrThrow(DatabaseGateway.COL_EXPENSE_TIMESTAMP);

                while (expenseCursor.moveToNext()) {
                    int id = expenseCursor.getInt(idCol);
                    String categoryName = expenseCursor.getString(categoryNameCol);
                    String iconIdentifier = expenseCursor.getString(iconIdCol);
                    String note = expenseCursor.getString(noteCol);
                    double amount = expenseCursor.getDouble(amountCol);
                    String rawTimestamp = expenseCursor.getString(timestampCol);
                    String displayDate = rawTimestamp;
                    try {
                        if (rawTimestamp != null) {
                            Date date = sdfDbTimestamp.parse(rawTimestamp);
                            displayDate = sdfDisplayExpense.format(date);
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing expense timestamp: " + rawTimestamp, e);
                    }
                    ExpenseModel model = new ExpenseModel(id, categoryName, iconIdentifier, note, amount, displayDate, rawTimestamp);
                    this.expenseList.add(model); // Add to the fragment's list
                    Log.v(TAG, "Added to expenseList: " + categoryName + " - " + amount);
                }
            } else {
                Log.w(TAG, "Expense cursor is null for getExpensesForUserWithCategory.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during database query or cursor processing in loadExpensesData", e);
            if(getContext() != null) Toast.makeText(getContext(), "Error loading expenses.", Toast.LENGTH_SHORT).show();
        } finally {
            if (expenseCursor != null) {
                expenseCursor.close();
            }
        }

        if (expenseAdapter != null) {
            expenseAdapter.notifyDataSetChanged(); // Notify adapter that the underlying data in expenseList has changed
        } else {
            Log.e(TAG, "expenseAdapter is null in loadExpensesData. Cannot update RecyclerView.");
        }
        Log.d(TAG, "Expenses loaded. Fragment's expenseList final size: " + this.expenseList.size());
        Log.d(TAG, "--- loadExpensesData END ---");
    }

    private void updateBudgetUI() {
        Log.d(TAG, "Updating Budget UI. Total Spent: " + currentTotalSpent + ", Budget Amount: " + currentBudgetAmount);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("fil", "PH")); // PHP Currency
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);


        binding.tvTotalSpentAmount.setText("- " + currencyFormat.format(currentTotalSpent)); // Expenses are positive in DB

        if (currentBudgetAmount > 0) {
            double remainingBudget = currentBudgetAmount - currentTotalSpent;
            binding.tvRemainingBudgetAmount.setText(currencyFormat.format(remainingBudget));
            binding.tvTotalBudgetAmount.setText("Total: " + currencyFormat.format(currentBudgetAmount));

            int progress = 0;
            if (currentBudgetAmount > 0) { // Avoid division by zero
                progress = (int) ((currentTotalSpent / currentBudgetAmount) * 100);
            }
            binding.progressBarBudget.setProgress(Math.max(0, Math.min(progress, 100)));
            binding.tvBudgetPercentage.setText(Math.max(0, Math.min(progress, 100)) + "% spent");

            if (remainingBudget < 0) {
                binding.tvRemainingBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            } else {
                binding.tvRemainingBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary)); // Make sure R.color.colorPrimary exists
            }
        } else {
            updateUIWithNoData(); // Handles case where budget is 0 or not set
        }
    }

    private void updateUIWithNoData() {
        if (binding == null) {
            Log.w(TAG, "updateUIWithNoData: binding is null, cannot update UI.");
            return;
        }
        Log.d(TAG, "Updating UI with no budget data or zero budget.");
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("fil", "PH"));
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);

        binding.tvRemainingBudgetAmount.setText(currencyFormat.format(0 - currentTotalSpent));
        binding.tvTotalBudgetAmount.setText("Total: Not Set");
        binding.progressBarBudget.setProgress(0);
        binding.tvBudgetPercentage.setText(currentBudgetAmount > 0 ? "0% spent" : "--% spent");
    }

    @Override
    public void onDeleteClicked(ExpenseModel expense, int position) {
        // ... (this method is fine as is) ...
        Log.d(TAG, "onDeleteClicked for expense ID: " + expense.getId() + " at position: " + position);
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete '" + (expense.getNote() != null && !expense.getNote().isEmpty() ? expense.getNote() : expense.getCategoryName()) + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = dbGateway.deleteExpense(expense.getId());
                    if (deleted) {
                        Toast.makeText(getContext(), "Expense deleted", Toast.LENGTH_SHORT).show();
                        if (position >= 0 && position < expenseList.size()) {
                            expenseList.remove(position);
                            if (expenseAdapter != null) expenseAdapter.notifyItemRemoved(position);
                        } else {
                            Log.w(TAG, "Invalid position for deletion: " + position + ", list size: " + expenseList.size());
                        }
                        refreshAllData(); // Refresh all data to update totals
                    } else {
                        Toast.makeText(getContext(), "Failed to delete expense", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to delete expense ID: " + expense.getId() + " from database.");
                    }
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment is becoming visible/resumed.");
        if (currentUserId != -1) {
            if (currentPeriodStartDate == null || currentPeriodEndDate == null) {
                updateDateRangeForSelectedPeriod();
            }
            refreshAllData();
        } else {
            Log.w(TAG, "onResume: User ID is -1, skipping data refresh.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Clearing binding.");
        binding = null;
    }
}