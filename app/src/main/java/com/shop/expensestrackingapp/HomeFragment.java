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
    private String selectedExpenseViewPeriodType = "DAILY"; // Default period
    private String currentExpenseViewStartDate, currentExpenseViewEndDate;

    // Date formatters
    private final SimpleDateFormat sdfDisplayExpense = new SimpleDateFormat("dd MMM - hh:mma", Locale.getDefault());
    private final SimpleDateFormat sdfDbTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat sdfDbDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat sdfDisplayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private double activeBudgetAmount = 0.0;
    private String activeBudgetPeriodType;
    private String activeBudgetSetDateText = "No active budget set";
    private String activeBudget_RecordStartDate; // Actual start_date of the active budget record
    private String activeBudget_RecordEndDate;       // Actual end date of the loaded active budget


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

        setupExpenseViewPeriodDropdown(); // Will now include "Today"
        setupRecyclerView();
        setupActionButtons();

        if (currentExpenseViewStartDate == null || currentExpenseViewEndDate == null) {
            updateDateRangeForExpenseViewPeriod(); // Initialize with default "DAILY"
        }
    }

    private void setupExpenseViewPeriodDropdown() {
        if (binding == null) return;
        String[] periods = new String[]{"Today", "This Week", "This Month", "All Time"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, periods);
        binding.actvBudgetPeriod.setAdapter(adapter);

        if ("DAILY".equals(selectedExpenseViewPeriodType)) binding.actvBudgetPeriod.setText(periods[0], false);
        else if ("WEEKLY".equals(selectedExpenseViewPeriodType)) binding.actvBudgetPeriod.setText(periods[1], false);
        else if ("MONTHLY".equals(selectedExpenseViewPeriodType)) binding.actvBudgetPeriod.setText(periods[2], false);
        else binding.actvBudgetPeriod.setText(periods[3], false);

        // Set default text based on selectedExpenseViewPeriodType
        binding.actvBudgetPeriod.setOnItemClickListener((parent, Lview, position, id) -> {
            String selectedText = (String) parent.getItemAtPosition(position);
            Log.d(TAG, "Expense view period selected from dropdown: " + selectedText);
            // RE-ADD "Today" -> "DAILY" MAPPING
            if ("Today".equals(selectedText)) selectedExpenseViewPeriodType = "DAILY";
            else if ("This Week".equals(selectedText)) selectedExpenseViewPeriodType = "WEEKLY";
            else if ("This Month".equals(selectedText)) selectedExpenseViewPeriodType = "MONTHLY";
            else if ("All Time".equals(selectedText)) selectedExpenseViewPeriodType = "ALL_TIME";

            updateDateRangeForExpenseViewPeriod();
            loadExpensesForViewPeriod();  // Load expenses for the new view period
            updateBudgetDisplay();
        });
    }

    private void updateDateRangeForExpenseViewPeriod() {
        Calendar calendar = Calendar.getInstance();
        Date today = new Date();

        if ("DAILY".equals(selectedExpenseViewPeriodType)) {
            currentExpenseViewStartDate = sdfDbDateOnly.format(today);
            currentExpenseViewEndDate = sdfDbDateOnly.format(today);
        } else if ("WEEKLY".equals(selectedExpenseViewPeriodType)) {
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
        } else { // ALL_TIME
            currentExpenseViewStartDate = "1970-01-01";
            currentExpenseViewEndDate = "2999-12-31";
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
            Log.d(TAG, "btnSetBudget clicked. Active budget: " + activeBudgetAmount + ", Type: " + activeBudgetPeriodType);
            SetBudgetDialog dialog;
            // If an active budget (Daily, Weekly, or Monthly) is displayed, pass its details
            if (activeBudgetAmount > 0 && activeBudgetPeriodType != null) {
                dialog = SetBudgetDialog.newInstance(activeBudgetAmount, activeBudgetPeriodType);
            } else {
                dialog = SetBudgetDialog.newInstance(); // For new budget
            }
            dialog.setSetBudgetDialogListener(this);
            dialog.show(getParentFragmentManager(), "SetBudgetDialogTag");
        });

        View deleteBudgetButton = binding.cardBudgetStatus.findViewById(R.id.btnDeleteBudget);
        if (deleteBudgetButton != null) {
            deleteBudgetButton.setOnClickListener(v -> showDeleteBudgetConfirmation());
        } else { /* Log error */ }
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
        if (binding == null || dbGateway == null) { Log.e(TAG, "loadActiveBudget: binding or dbGateway is null!");
            return;
        }
        Log.d(TAG, "loadActiveBudget: Checking for active D/W/M budget for today.");


        this.activeBudgetAmount = 0.0;
        this.activeBudgetPeriodType = null;
        this.activeBudgetSetDateText = "No active budget set";
        this.activeBudget_RecordStartDate = null;
        this.activeBudget_RecordEndDate = null;


        View deleteButtonInCard = binding.cardBudgetStatus.findViewById(R.id.btnDeleteBudget);
        if(deleteButtonInCard != null) deleteButtonInCard.setVisibility(View.GONE);

        String todayStr = sdfDbDateOnly.format(new Date());
        Cursor budgetCursor = null;
        String foundPeriodType = null;

        Log.d(TAG, "loadActiveBudget: Checking for DAILY budget active on " + todayStr);
        budgetCursor = dbGateway.getCurrentBudget(currentUserId, "DAILY", todayStr);
        if (budgetCursor != null && budgetCursor.moveToFirst()) {
            foundPeriodType = "DAILY";
            Log.d(TAG, "loadActiveBudget: Found active DAILY budget.");
        } else {
            if (budgetCursor != null) budgetCursor.close();
            Log.d(TAG, "loadActiveBudget: No DAILY. Checking for WEEKLY budget active on " + todayStr);
            budgetCursor = dbGateway.getCurrentBudget(currentUserId, "WEEKLY", todayStr);
            if (budgetCursor != null && budgetCursor.moveToFirst()) {
                foundPeriodType = "WEEKLY";
                Log.d(TAG, "loadActiveBudget: Found active WEEKLY budget.");
            } else {
                if (budgetCursor != null) budgetCursor.close();
                Log.d(TAG, "loadActiveBudget: No WEEKLY. Checking for MONTHLY budget active on " + todayStr);
                budgetCursor = dbGateway.getCurrentBudget(currentUserId, "MONTHLY", todayStr);
                if (budgetCursor != null && budgetCursor.moveToFirst()) {
                    foundPeriodType = "MONTHLY";
                    Log.d(TAG, "loadActiveBudget: Found active MONTHLY budget.");
                }
            }
        }

        if (foundPeriodType != null && budgetCursor != null && !budgetCursor.isClosed() && budgetCursor.moveToFirst()) {
            this.activeBudgetPeriodType = foundPeriodType;
            this.activeBudgetAmount = budgetCursor.getDouble(budgetCursor.getColumnIndexOrThrow(DatabaseGateway.COL_BUDGET_AMOUNT));
            this.activeBudget_RecordStartDate = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow(DatabaseGateway.COL_BUDGET_START_DATE));
            this.activeBudget_RecordEndDate = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow(DatabaseGateway.COL_BUDGET_END_DATE));
            String rawSetDate = budgetCursor.getString(budgetCursor.getColumnIndexOrThrow(DatabaseGateway.COL_BUDGET_CREATED_AT));
            try {
                if (rawSetDate != null) {
                    Date date = sdfDbTimestamp.parse(rawSetDate);
                    this.activeBudgetSetDateText = "Budget set on " + sdfDisplayDate.format(date);
                } else { this.activeBudgetSetDateText = "Budget set recently"; }
            } catch (ParseException e) { /* ... */ }
            if (deleteButtonInCard != null) deleteButtonInCard.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "loadActiveBudget: No active Daily, Weekly, or Monthly budget found for today.");
        }

        if (budgetCursor != null && !budgetCursor.isClosed()) {
            budgetCursor.close();
        }
        Log.d(TAG, "Active Budget Loaded: Type=" + this.activeBudgetPeriodType + ", Amount=" + this.activeBudgetAmount + ", RecordStart=" + this.activeBudget_RecordStartDate);
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
            binding.tvBudgetUpdatedDate.setText(activeBudgetSetDateText);
            binding.tvTotalBudgetAmount.setText("Total Budget: " + currencyFormat.format(activeBudgetAmount));
            binding.tvTotalBudgetAmount.setVisibility(View.VISIBLE);
            binding.progressBarBudget.setVisibility(View.VISIBLE);
            binding.tvBudgetPercentage.setVisibility(View.VISIBLE);
            View deleteBtn = binding.cardBudgetStatus.findViewById(R.id.btnDeleteBudget);
            if(deleteBtn!=null) deleteBtn.setVisibility(View.VISIBLE);

                // For progress, calculate expenses WITHIN the active budget's own date range
            double spentForProgressCalculation;
            // Determine which "spent" amount to use for progress against the active budget
            if ("DAILY".equals(activeBudgetPeriodType)) {
                // If the active budget is DAILY, then progress is today's spending against today's budget
                spentForProgressCalculation = totalSpentInSelectedViewPeriod; // Assuming selectedExpenseViewPeriodType is also DAILY
                Log.d(TAG, "Calculating DAILY budget progress using totalSpentInSelectedViewPeriod: " + spentForProgressCalculation);
            } else {
                // If active budget is WEEKLY or MONTHLY, calculate spending within *its* specific period
                if (activeBudget_RecordStartDate != null && activeBudget_RecordEndDate != null) {
                    spentForProgressCalculation = dbGateway.getTotalExpensesForPeriod(currentUserId, activeBudget_RecordStartDate, activeBudget_RecordEndDate);
                    Log.d(TAG, "Calculating " + activeBudgetPeriodType + " budget progress using expenses from " + activeBudget_RecordStartDate + " to " + activeBudget_RecordEndDate + ": " + spentForProgressCalculation);
                } else {
                    spentForProgressCalculation = 0; // Should not happen if active budget is loaded correctly
                    Log.w(TAG, "Active budget (" + activeBudgetPeriodType + ") dates are null, progress spent is 0.");
                }
            }

            double remainingForActiveBudget = activeBudgetAmount - spentForProgressCalculation;
            binding.tvRemainingBudgetAmount.setText(currencyFormat.format(remainingForActiveBudget));

            int progress = 0;
            if (activeBudgetAmount > 0) {
                progress = (int) ((spentForProgressCalculation * 100.0) / activeBudgetAmount);
                progress = (int) Math.round(progress); // Round to nearest int
            }
            binding.progressBarBudget.setProgress(Math.max(0, Math.min(progress, 100)));
            binding.tvBudgetPercentage.setText(Math.max(0, Math.min(progress, 100)) + "% spent");

            if (remainingForActiveBudget < 0) {
                binding.tvRemainingBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            } else {
                binding.tvRemainingBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
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
        binding.tvRemainingBudgetAmount.setText("No active budget set");
        binding.tvBudgetUpdatedDate.setText("Set a budget to track progress");
        binding.tvRemainingBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnBackground)); // Default text color

        binding.tvTotalBudgetAmount.setVisibility(View.GONE);
        binding.tvBudgetUpdatedDate.setText(activeBudgetSetDateText); // Shows "No active budget set"
        binding.progressBarBudget.setVisibility(View.INVISIBLE);
        binding.tvBudgetPercentage.setVisibility(View.INVISIBLE);
        View deleteBtn = binding.cardBudgetStatus.findViewById(R.id.btnDeleteBudget);
        if(deleteBtn!=null) deleteBtn.setVisibility(View.GONE);
    }
    @Override
    public void onBudgetSet(double amount, String periodTypeDialog, String startDate, String endDate, String createdAt) {
        Log.d(TAG, "HomeFragment.onBudgetSet: Amount=" + amount + ", Period=" + periodTypeDialog + ", Start=" + startDate);
        if (dbGateway == null) { Log.e(TAG, "dbGateway null in onBudgetSet"); return; }
        boolean success = dbGateway.setOrUpdateBudget(currentUserId, amount, periodTypeDialog, startDate, endDate, createdAt);
        if (success) {
            Toast.makeText(getContext(), "Budget updated successfully!", Toast.LENGTH_SHORT).show();

            // --- CRITICAL UPDATE ---
            // Immediately update HomeFragment's active budget state to what was just set.
            this.activeBudgetAmount = amount;
            this.activeBudgetPeriodType = periodTypeDialog; // Now can be DAILY, WEEKLY, or MONTHLY
            this.activeBudget_RecordStartDate = startDate;
            this.activeBudget_RecordEndDate = endDate;
            try {
                this.activeBudgetSetDateText = "Budget set on " + sdfDisplayDate.format(sdfDbTimestamp.parse(createdAt));
            } catch (ParseException e) {
                this.activeBudgetSetDateText = "Budget set recently";
            }

            Log.d(TAG, "HomeFragment's active budget state MANUALLY updated to: Type=" + this.activeBudgetPeriodType);
            refreshAllData();
        } else {
            Toast.makeText(getContext(), "Failed to set/update budget.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteBudgetConfirmation() {
        if (activeBudget_RecordStartDate == null || activeBudgetPeriodType == null) {
            Toast.makeText(getContext(), "No active budget to delete.", Toast.LENGTH_SHORT).show();
            return;
        }
        String periodText = "current " + activeBudgetPeriodType.toLowerCase();
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Budget")
                .setMessage("Are you sure you want to delete the " + periodText + " budget?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dbGateway == null) { return; }
                    boolean deleted = dbGateway.deleteBudget(currentUserId, activeBudgetPeriodType, activeBudget_RecordStartDate);
                    if (deleted) {
                        Toast.makeText(getContext(), "Budget deleted.", Toast.LENGTH_SHORT).show();
                        // Clear the active budget fields as it's now deleted
                        activeBudgetAmount = 0.0;
                        activeBudgetPeriodType = null;
                        activeBudget_RecordStartDate = null; // Clear this
                        activeBudget_RecordEndDate = null;
                        activeBudgetSetDateText = "No active budget set";
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
            if (binding == null)
        {
            Log.e(TAG, "Binding is null in onResume!");
            return;
        }

            // If current view period is not initialized or dropdown is empty, default to WEEKLY
        if (currentExpenseViewStartDate == null || currentExpenseViewEndDate == null ||
                (binding.actvBudgetPeriod != null && binding.actvBudgetPeriod.getText().toString().isEmpty())) {
            Log.d(TAG, "onResume: Expense view period not fully set, defaulting to DAILY.");
            selectedExpenseViewPeriodType = "DAILY";
            if(binding.actvBudgetPeriod != null) binding.actvBudgetPeriod.setText("Today", false);
            updateDateRangeForExpenseViewPeriod();
        }
        refreshAllData();
        } else {
            Log.w(TAG, "onResume: No user logged in, cannot refresh data.");
            if (binding != null) {
                updateUIWithNoActiveBudget(); // Update UI to reflect no data state
                if (expenseList != null) expenseList.clear();
                if (expenseAdapter != null) expenseAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Clearing binding.");
        binding = null;
    }
}