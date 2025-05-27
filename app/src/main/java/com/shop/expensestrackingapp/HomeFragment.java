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

public class HomeFragment extends Fragment implements
        ExpenseAdapter.OnExpenseActionsListener, SetBudgetDialog.SetBudgetDialogListener {

    private FragmentHomeBinding binding; // ViewBinding for fragment_home.xml
    private DatabaseGateway dbGateway;
    private SessionManager sessionManager;
    private ExpenseAdapter expenseAdapter;
    private List<ExpenseModel> expenseList;

    private int currentUserId = -1;
    private String selectedExpenseViewPeriodType = "WEEKLY"; // Default period
    private String currentExpenseViewStartDate, currentExpenseViewEndDate;

    // Date formatters
    private final SimpleDateFormat sdfDisplayExpense = new SimpleDateFormat("dd MMM - hh:mma", Locale.getDefault());
    private final SimpleDateFormat sdfDbTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat sdfDbDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat sdfDisplayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private double activeBudgetAmount = 0.0;
    private String activeBudgetPeriodType; // "WEEKLY" or "MONTHLY" or null if none active
    private String activeBudgetSetDateStr = "No active budget set";
    private String activeBudget_StartDateForDeletion; // Start date of the active budget (for deletion key)
    private String activeBudget_ActualStartDate;    // Actual start date of the loaded active budget
    private String activeBudget_ActualEndDate;      // Actual end date of the loaded active budget


    // For total spent within the SELECTED EXPENSE VIEWING PERIOD
    private double totalSpentInSelectedViewPeriod = 0.0;
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
        Log.d(TAG, "onViewCreated: Setting up UI for User ID: " + currentUserId);
        if (binding == null) { Log.e(TAG, "Binding is null in onViewCreated!"); return; }
        if (currentUserId == -1) { /* ... error handling ... */ return; }

        setupExpenseViewPeriodDropdown(); // Will now exclude "Today"
        setupRecyclerView();
        setupActionButtons();

        if (currentExpenseViewStartDate == null || currentExpenseViewEndDate == null) {
            updateDateRangeForExpenseViewPeriod();
        }
    }

    private void setupExpenseViewPeriodDropdown() {
        if (binding == null) return;
        String[] periods = new String[]{"This Week", "This Month", "All Time"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, periods);
        binding.actvBudgetPeriod.setAdapter(adapter);

        binding.actvBudgetPeriod.setText(periods[0], false);
        selectedExpenseViewPeriodType = "WEEKLY";

        // Set default text based on selectedExpenseViewPeriodType
        binding.actvBudgetPeriod.setOnItemClickListener((parent, Lview, position, id) -> {
            String selectedText = (String) parent.getItemAtPosition(position);
            Log.d(TAG, "Expense view period selected from dropdown: " + selectedText);
            // Adjusted conditions since "Today" is removed
            if ("This Week".equals(selectedText)) selectedExpenseViewPeriodType = "WEEKLY";
            else if ("This Month".equals(selectedText)) selectedExpenseViewPeriodType = "MONTHLY";
            else if ("All Time".equals(selectedText)) selectedExpenseViewPeriodType = "ALL_TIME";

            updateDateRangeForExpenseViewPeriod();
            loadExpensesForViewPeriod();
            updateBudgetDisplay();
        });
    }

    private void updateDateRangeForExpenseViewPeriod() {
        Calendar calendar = Calendar.getInstance();
        Date today = new Date(); // Still need today for reference

        // "DAILY" case is removed
        if ("WEEKLY".equals(selectedExpenseViewPeriodType)) {
            calendar.setTime(today);
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            currentExpenseViewStartDate = sdfDbDateOnly.format(calendar.getTime());
            calendar.add(Calendar.DAY_OF_WEEK, 6);
            currentExpenseViewEndDate = sdfDbDateOnly.format(calendar.getTime());
        } else if ("MONTHLY".equals(selectedExpenseViewPeriodType)) {
            calendar.setTime(today);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            currentExpenseViewStartDate = sdfDbDateOnly.format(calendar.getTime());
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            currentExpenseViewEndDate = sdfDbDateOnly.format(calendar.getTime());
        } else { // ALL_TIME (or if selectedExpenseViewPeriodType is somehow invalid, default to All Time)
            currentExpenseViewStartDate = "1970-01-01";
            currentExpenseViewEndDate = "2999-12-31";
            if (!"ALL_TIME".equals(selectedExpenseViewPeriodType)) { // Log if it falls here unexpectedly
                Log.w(TAG, "updateDateRangeForExpenseViewPeriod: Unexpected period type '" + selectedExpenseViewPeriodType + "', defaulting to ALL_TIME.");
                selectedExpenseViewPeriodType = "ALL_TIME"; // Correct the state
            }
        }
        Log.d(TAG, "Date range for EXPENSE VIEW updated for " + selectedExpenseViewPeriodType + ": [" + currentExpenseViewStartDate + "] to [" + currentExpenseViewEndDate + "]");
    }

    private void setupRecyclerView() {
        if (binding == null) return;
        expenseList = new ArrayList<>();
        expenseAdapter = new ExpenseAdapter(requireContext(), expenseList, this);
        binding.recyclerViewExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewExpenses.setAdapter(expenseAdapter);
        binding.recyclerViewExpenses.setNestedScrollingEnabled(false);
        Log.d(TAG, "RecyclerView and Adapter setup complete.");
    }

    private void setupActionButtons() {
        if (binding == null)
            return;
        binding.btnAddExpenseFromActions.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Use the floating (+) button to add expenses.", Toast.LENGTH_SHORT).show();
        });

        binding.tvManageExpenses.setOnClickListener(v -> {
            if (expenseAdapter != null) {
                boolean newMode = !expenseAdapter.isManageModeEnabled();
                expenseAdapter.setManageMode(newMode);
                binding.tvManageExpenses.setText(newMode ? "Done" : "Manage");
                binding.tvManageExpenses.setTextColor(ContextCompat.getColor(requireContext(),
                        newMode ? android.R.color.white : R.color.colorPrimary));
                Log.d(TAG, "Manage mode toggled to: " + newMode);
            }
        });

        binding.btnSetBudget.setOnClickListener(v -> {
            Log.d(TAG, "btnSetBudget clicked. Active budget: " + activeBudgetAmount + ", Active Period Type: " + activeBudgetPeriodType);
            SetBudgetDialog dialog;
            // Pass active budget details if a Weekly or Monthly budget is currently active for editing
            if (activeBudgetAmount > 0 && ("WEEKLY".equals(activeBudgetPeriodType) || "MONTHLY".equals(activeBudgetPeriodType))) {
                dialog = SetBudgetDialog.newInstance(activeBudgetAmount, activeBudgetPeriodType);
            } else {
                dialog = SetBudgetDialog.newInstance(); // For setting a new budget
            }
            dialog.setSetBudgetDialogListener(this);
            dialog.show(getParentFragmentManager(), "SetBudgetDialogTag");
        });

        View deleteBudgetButton = binding.cardBudgetStatus.findViewById(R.id.btnDeleteBudget);
        if (deleteBudgetButton != null) {
            deleteBudgetButton.setOnClickListener(v -> showDeleteBudgetConfirmation());
        } else {
            Log.e(TAG, "btnDeleteBudget View ID not found in cardBudgetStatus! Ensure it exists in fragment_home.xml within the card.");
        }
    }

    // Public method to be called by DashboardActivity after an expense is added
    public void refreshAllData() {
        Log.d(TAG, "refreshAllData called. User ID: " + currentUserId);
        if (!isAdded() || binding == null || currentUserId == -1 || currentExpenseViewStartDate == null || currentExpenseViewEndDate == null) {
            Log.w(TAG, "Cannot refresh data: Conditions not met.");
            if (binding != null) updateUIWithNoActiveBudget(); // Update UI to reflect no data state
            if (expenseList != null) expenseList.clear();
            if (expenseAdapter != null) expenseAdapter.notifyDataSetChanged();
            return;
        }
        loadActiveBudget();           // 1. Load the active (Weekly/Monthly) budget
        loadExpensesForViewPeriod();  // 2. Load expenses for the currently selected view period (Today, Week, Month, All Time)
        updateBudgetDisplay();        // 3. Update the entire budget card display using active budget and view period expenses

    }

    private void loadActiveBudget() {
        if (binding == null || dbGateway == null) { Log.e(TAG, "loadActiveBudget: binding or dbGateway is null!"); return; }
        Log.d(TAG, "loadActiveBudget: Checking for active Weekly or Monthly budget.");

        this.activeBudgetAmount = 0.0;
        this.activeBudgetPeriodType = null;
        this.activeBudgetSetDateStr = "No active budget set";
        this.activeBudget_ActualStartDate = null;
        this.activeBudget_ActualEndDate = null;

        View deleteButtonInCard = binding.cardBudgetStatus.findViewById(R.id.btnDeleteBudget);
        if(deleteButtonInCard != null) deleteButtonInCard.setVisibility(View.GONE);

        String todayStr = sdfDbDateOnly.format(new Date());
        Cursor budgetCursor = null;
        String tempPeriodType = null;

        budgetCursor = dbGateway.getCurrentBudget(currentUserId, "WEEKLY", todayStr);
        if (budgetCursor != null && budgetCursor.moveToFirst()) {
            tempPeriodType = "WEEKLY";
            Log.d(TAG, "loadActiveBudget: Found active WEEKLY budget.");
        } else {
            if (budgetCursor != null) budgetCursor.close();
            budgetCursor = dbGateway.getCurrentBudget(currentUserId, "MONTHLY", todayStr);
            if (budgetCursor != null && budgetCursor.moveToFirst()) {
                tempPeriodType = "MONTHLY";
                Log.d(TAG, "loadActiveBudget: Found active MONTHLY budget.");
            }
        }

        if (tempPeriodType != null && budgetCursor != null && !budgetCursor.isClosed() && budgetCursor.moveToFirst()) {
            this.activeBudgetPeriodType = tempPeriodType;
            this.activeBudgetAmount = budgetCursor.getDouble(budgetCursor.getColumnIndexOrThrow(DatabaseGateway.COL_BUDGET_AMOUNT));
            this.activeBudget_ActualStartDate = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow(DatabaseGateway.COL_BUDGET_START_DATE));
            this.activeBudget_ActualEndDate = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow(DatabaseGateway.COL_BUDGET_END_DATE));
            // this.activeBudget_StartDateForDeletion = this.activeBudget_RecordStartDate; // Not needed, use activeBudget_RecordStartDate directly

            String rawSetDate = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow(DatabaseGateway.COL_BUDGET_CREATED_AT));
            try {
                if (rawSetDate != null) {
                    Date date = sdfDbTimestamp.parse(rawSetDate);
                    this.activeBudgetSetDateStr = "Budget set on " + sdfDisplayDate.format(date);
                } else { this.activeBudgetSetDateStr = "Budget set recently"; }
            } catch (ParseException e) { /* ... */ }
            if (deleteButtonInCard != null) deleteButtonInCard.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "loadActiveBudget: No active Weekly or Monthly budget found for today.");
        }

        if (budgetCursor != null && !budgetCursor.isClosed()) {
            budgetCursor.close();
        }
        Log.d(TAG, "Active Budget Loaded: Type=" + this.activeBudgetPeriodType + ", Amount=" + this.activeBudgetAmount);
    }
    private void loadExpensesForViewPeriod() {
        if (binding == null || dbGateway == null) { Log.e(TAG, "Binding or DBGateway null in loadExpensesForViewPeriod"); return; }
        Log.d(TAG, "--- loadExpensesForViewPeriod START ---");
        Log.d(TAG, "View Period: " + selectedExpenseViewPeriodType + ", Start: " + currentExpenseViewStartDate + ", End: " + currentExpenseViewEndDate);

        totalSpentInSelectedViewPeriod = dbGateway.getTotalExpensesForPeriod(currentUserId, currentExpenseViewStartDate, currentExpenseViewEndDate);
        Log.d(TAG, "Total spent in current view period (" + selectedExpenseViewPeriodType + "): " + totalSpentInSelectedViewPeriod);

        Cursor expenseCursor = null;
        if (this.expenseList == null) this.expenseList = new ArrayList<>();
        this.expenseList.clear();

        try {
            expenseCursor = dbGateway.getExpensesForUserWithCategory(currentUserId, currentExpenseViewStartDate, currentExpenseViewEndDate);
            if (expenseCursor != null) {
                Log.d(TAG, "Expense cursor count for view period: " + expenseCursor.getCount());
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
                    } catch (ParseException e) { Log.e(TAG, "Error parsing expense timestamp: " + rawTimestamp, e); }
                    this.expenseList.add(new ExpenseModel(id, categoryName, iconIdentifier, note, amount, displayDate, rawTimestamp));
                }
            } else { Log.w(TAG, "Expense cursor is null."); }
        } catch (Exception e) { Log.e(TAG, "Error in loadExpensesForViewPeriod cursor processing", e); }
        finally { if (expenseCursor != null) expenseCursor.close(); }

        if (expenseAdapter != null) {
            expenseAdapter.notifyDataSetChanged();
        } else { Log.e(TAG, "expenseAdapter is null."); }
        Log.d(TAG, "Expenses loaded for view period. List size: " + this.expenseList.size());
        Log.d(TAG, "--- loadExpensesForViewPeriod END ---");
    }


    private void updateBudgetDisplay() { // Renamed from updateBudgetUI
        if (binding == null) return;
        Log.d(TAG, "Updating Budget Display. ActiveBudget: " + activeBudgetAmount + " (" + activeBudgetPeriodType + ")" +
                ", SpentInViewPeriod ("+ selectedExpenseViewPeriodType +"): " + totalSpentInSelectedViewPeriod);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("fil", "PH"));
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);

            // Card for "Total Spent (This Period)" - uses totalSpentInViewPeriod
        binding.tvTotalSpentAmount.setText("- " + currencyFormat.format(totalSpentInSelectedViewPeriod));


        // Budget Status Card - uses activeBudgetAmount and activeBudgetSetDateStr
        if (activeBudgetAmount > 0 && activeBudgetPeriodType != null) {
            binding.tvRemainingBudgetLabel.setText("Budget Remaining (" + activeBudgetPeriodType.substring(0,1).toUpperCase() + activeBudgetPeriodType.substring(1).toLowerCase() + ")");
            binding.tvBudgetUpdatedDate.setText(activeBudgetSetDateStr);
            binding.tvTotalBudgetAmount.setText("Total Budget: " + currencyFormat.format(activeBudgetAmount));
            binding.tvTotalBudgetAmount.setVisibility(View.VISIBLE);
            binding.progressBarBudget.setVisibility(View.VISIBLE);
            binding.tvBudgetPercentage.setVisibility(View.VISIBLE);
            View deleteBtn = binding.cardBudgetStatus.findViewById(R.id.btnDeleteBudget);
            if(deleteBtn!=null) deleteBtn.setVisibility(View.VISIBLE);

                // For progress, calculate expenses WITHIN the active budget's own date range
            double spentAgainstActiveBudget = 0.0;
            if (activeBudget_ActualStartDate != null && activeBudget_ActualEndDate != null) {
                spentAgainstActiveBudget = dbGateway.getTotalExpensesForPeriod(currentUserId, activeBudget_ActualStartDate, activeBudget_ActualEndDate);
                Log.d(TAG, "Spent against active " + activeBudgetPeriodType + " budget (Range: "+activeBudget_ActualStartDate+" to "+activeBudget_ActualEndDate+"): " + spentAgainstActiveBudget);
            } else {
                Log.w(TAG, "Active budget date range is null, cannot calculate spentAgainstActiveBudget.");
            }

            double remainingForActiveBudget = activeBudgetAmount - spentAgainstActiveBudget;
            binding.tvRemainingBudgetAmount.setText(currencyFormat.format(remainingForActiveBudget));

            int progress = 0;
            if (activeBudgetAmount > 0) { // Avoid division by zero
                progress = (int) ((spentAgainstActiveBudget / activeBudgetAmount) * 100);
            }
            binding.progressBarBudget.setProgress(Math.max(0, Math.min(progress, 100)));
            binding.tvBudgetPercentage.setText(Math.max(0, Math.min(progress, 100)) + "% spent");

            if (remainingForActiveBudget < 0) {
                binding.tvRemainingBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            } else {
                binding.tvRemainingBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary)); // Ensure R.color.colorPrimary exists
            }
        } else {
            updateUIWithNoActiveBudget();
        }
    }

    private void updateUIWithNoActiveBudget() { // When no specific (Weekly/Monthly) budget is displayed
        if (binding == null) return;
        Log.d(TAG, "updateUIWithNoActiveBudget. Total Spent in view period (" + selectedExpenseViewPeriodType + "): " + totalSpentInSelectedViewPeriod);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("fil", "PH"));
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);

        // Budget card shows a general message
        binding.tvRemainingBudgetLabel.setText("Budget Overview");
        binding.tvRemainingBudgetAmount.setText("No active W/M budget"); // W/M for Weekly/Monthly
        binding.tvRemainingBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnBackground)); // Default text color

        binding.tvTotalBudgetAmount.setVisibility(View.GONE);
        binding.tvBudgetUpdatedDate.setText(activeBudgetSetDateStr); // Shows "No active budget set"
        binding.progressBarBudget.setVisibility(View.INVISIBLE);
        binding.tvBudgetPercentage.setVisibility(View.INVISIBLE);
        View deleteBtn = binding.cardBudgetStatus.findViewById(R.id.btnDeleteBudget);
        if(deleteBtn!=null) deleteBtn.setVisibility(View.GONE);
    }
    @Override
    public void onBudgetSet(double amount, String periodTypeDialog, String startDate, String endDate, String createdAt) {
        Log.d(TAG, "HomeFragment.onBudgetSet: Amount=" + amount + ", Period=" + periodTypeDialog + ", Start=" + startDate);
        boolean success = dbGateway.setOrUpdateBudget(currentUserId, amount, periodTypeDialog, startDate, endDate, createdAt);
        if (success) {
            Toast.makeText(getContext(), "Budget " + (activeBudgetAmount > 0 ? "updated" : "set") + " successfully!", Toast.LENGTH_SHORT).show();
            refreshAllData();
        } else {
            Toast.makeText(getContext(), "Failed to set/update budget.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteBudgetConfirmation() {
        if (activeBudget_ActualStartDate == null || activeBudgetPeriodType == null) { // Use activeBudget_RecordStartDate
            Toast.makeText(getContext(), "No active Weekly/Monthly budget to delete.", Toast.LENGTH_SHORT).show();
            return;
        }
        // ... rest of the method ...
        String periodText = activeBudgetPeriodType.equals("WEEKLY") ? "the current week's" : "the current month's";
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Budget")
                .setMessage("Are you sure you want to delete " + periodText + " budget?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dbGateway == null) { return; }
                    // Use the specific start date of the active budget for deletion
                    boolean deleted = dbGateway.deleteBudget(currentUserId, activeBudgetPeriodType, activeBudget_ActualStartDate); // Use activeBudget_RecordStartDate
                    if (deleted) {
                        Toast.makeText(getContext(), "Budget deleted.", Toast.LENGTH_SHORT).show();
                        // Clear the active budget fields as it's now deleted
                        activeBudgetAmount = 0.0;
                        activeBudgetPeriodType = null;
                        activeBudget_ActualStartDate = null; // Clear this
                        activeBudget_ActualEndDate = null;
                        activeBudgetSetDateStr = "No active budget set";
                        refreshAllData(); // Refresh UI
                    } else {
                        Toast.makeText(getContext(), "Failed to delete budget.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
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
        Log.d(TAG, "onResume: Resuming. Current expense view period: " + selectedExpenseViewPeriodType);
        if (currentUserId != -1) {
            if (binding == null) { /* ... error handling ... */ return; }

            // If current view period is not initialized or dropdown is empty, default to WEEKLY
            if (currentExpenseViewStartDate == null || currentExpenseViewEndDate == null ||
                    (binding.actvBudgetPeriod != null && binding.actvBudgetPeriod.getText().toString().isEmpty())) {
                Log.d(TAG, "onResume: Expense view period not fully set, defaulting to WEEKLY.");
                selectedExpenseViewPeriodType = "WEEKLY"; // Default to "This Week"
                if(binding.actvBudgetPeriod != null) binding.actvBudgetPeriod.setText("This Week", false);
                updateDateRangeForExpenseViewPeriod();
            }
            refreshAllData();
        } else { /* ... */ }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Clearing binding.");
        binding = null;
    }
}