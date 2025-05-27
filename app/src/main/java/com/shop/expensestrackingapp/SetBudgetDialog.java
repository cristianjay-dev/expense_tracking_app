package com.shop.expensestrackingapp;

import android.app.Dialog;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import com.shop.expensestrackingapp.databinding.SetBudgetDialogBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class SetBudgetDialog extends DialogFragment {

    public interface SetBudgetDialogListener {
        void onBudgetSet(double amount, String periodType, String startDate, String endDate, String createdAt);
    }
    private SetBudgetDialogListener listener;
    private SetBudgetDialogBinding binding;

    private String selectedPeriodType = "WEEKLY";
    private String currentBudgetStartDate, currentBudgetEndDate;
    private final SimpleDateFormat sdfDbDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat sdfDbTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private static final String TAG = "SetBudgetDialog";

    private static final String ARG_EXISTING_AMOUNT = "existing_amount";
    private static final String ARG_EXISTING_PERIOD_TYPE = "existing_period_type";

    public static SetBudgetDialog newInstance(double existingAmount, String existingPeriodType) {
        SetBudgetDialog dialog = new SetBudgetDialog();
        Bundle args = new Bundle();
        args.putDouble(ARG_EXISTING_AMOUNT, existingAmount);
        args.putString(ARG_EXISTING_PERIOD_TYPE, existingPeriodType);
        dialog.setArguments(args);
        return dialog;
    }
    public static SetBudgetDialog newInstance() { // For setting a brand new budget
        SetBudgetDialog dialog = new SetBudgetDialog();
        return dialog;
    }
    public void setSetBudgetDialogListener(SetBudgetDialogListener listener) {
        this.listener = listener;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: DialogFragment created.");
    }

    // 6. onCreateView (Inflate the layout using ViewBinding)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Inflating layout dialog_set_budget.xml");
        binding = SetBudgetDialogBinding.inflate(inflater, container, false);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Setting up UI listeners and pre-filling data if any.");

        setupToolbar();
        prefillDataIfEditing(); // Handle arguments passed via newInstance
        setupRadioGroupListener();
        setupButtonListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbarBudget;
        toolbar.inflateMenu(R.menu.toolbar_menu); // Ensure R.menu.toolbar_menu with R.id.btnClose exists
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.btnClose) { // Ensure R.id.btnClose is defined in your menu XML
                Log.d(TAG, "Toolbar close button clicked.");
                dismiss(); // Close the dialog
                return true;
            }
            return false;
        });
    }

    private void prefillDataIfEditing() {
        if (getArguments() != null) {
            double existingAmount = getArguments().getDouble(ARG_EXISTING_AMOUNT, 0.0);
            // Provide a default for existingPeriod if it's not passed or is null
            String existingPeriod = getArguments().getString(ARG_EXISTING_PERIOD_TYPE, "WEEKLY"); // Default to WEEKLY

            Log.d(TAG, "Prefilling data from arguments: Amount=" + existingAmount + ", Period=" + existingPeriod);

            if (existingAmount > 0) {
                binding.etBudgetAmount.setText(String.valueOf(existingAmount));
            }

            if ("MONTHLY".equals(existingPeriod)) {
                binding.rbThisMonth.setChecked(true);
                selectedPeriodType = "MONTHLY"; // Sync internal state
            } else { // Default to WEEKLY
                binding.rbThisWeek.setChecked(true);
                selectedPeriodType = "WEEKLY"; // Sync internal state
            }
        } else {
            // Default selection for a new budget if no arguments
            binding.rbThisWeek.setChecked(true);
            selectedPeriodType = "WEEKLY"; // Ensure this is set
            Log.d(TAG, "No arguments provided, defaulting to WEEKLY period.");
        }
    }

    private void setupRadioGroupListener() {
        binding.rgBudgetPeriod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbThisWeek) {
                selectedPeriodType = "WEEKLY";
            } else if (checkedId == R.id.rbThisMonth) {
                selectedPeriodType = "MONTHLY";
            }
            Log.d(TAG, "RadioGroup: User selected period type: " + selectedPeriodType);
        });
    }

    private void setupButtonListeners() {
        binding.btnDialogSetBudget.setOnClickListener(v -> processSetBudget());
        binding.btnDialogCancelBudget.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked.");
            dismiss();
        });
    }


    // 8. Logic to process budget input and call the listener
    private void processSetBudget() {
        String amountStr = binding.etBudgetAmount.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            binding.tilBudgetAmount.setError("Amount cannot be empty");
            binding.etBudgetAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                binding.tilBudgetAmount.setError("Amount must be positive");
                binding.etBudgetAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            binding.tilBudgetAmount.setError("Invalid amount format");
            binding.etBudgetAmount.requestFocus();
            return;
        }
        binding.tilBudgetAmount.setError(null); // Clear error if input is valid

        // Determine start and end dates for the selected period (relative to the current date)
        Calendar calendar = Calendar.getInstance(); // Using android.icu.util.Calendar
        Date today = new Date(); // Current date to base week/month calculations
        calendar.setTime(today);

        if ("WEEKLY".equals(selectedPeriodType)) {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
            currentBudgetStartDate = sdfDbDateOnly.format(calendar.getTime());
            calendar.add(Calendar.DAY_OF_WEEK, 6); // End of the week
            currentBudgetEndDate = sdfDbDateOnly.format(calendar.getTime());
        } else { // MONTHLY
            calendar.set(Calendar.DAY_OF_MONTH, 1); // Start of the month
            currentBudgetStartDate = sdfDbDateOnly.format(calendar.getTime());
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)); // End of the month
            currentBudgetEndDate = sdfDbDateOnly.format(calendar.getTime());
        }
        String createdAt = sdfDbTimestamp.format(new Date()); // Current timestamp for budget creation/update

        Log.d(TAG, "Processing budget: Amount=" + amount + ", PeriodType=" + selectedPeriodType +
                ", StartDate=" + currentBudgetStartDate + ", EndDate=" + currentBudgetEndDate +
                ", CreatedAt=" + createdAt);

        if (listener != null) {
            listener.onBudgetSet(amount, selectedPeriodType, currentBudgetStartDate, currentBudgetEndDate, createdAt);
        } else {
            Log.w(TAG, "SetBudgetDialogListener is null. Cannot callback to host.");
        }
        dismiss(); // Close the dialog
    }

    // 9. onStart (Control dialog size and position)
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            // Set width, height will be wrap_content due to the layout's root LinearLayout
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90); // Example: 90% of screen width
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER); // Center the dialog on screen
            Log.d(TAG, "Dialog window size and gravity set.");
        }
    }

    // 10. onDestroyView (Clean up ViewBinding reference)
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Crucial to prevent memory leaks with ViewBinding in Fragments
        Log.d(TAG, "onDestroyView: SetBudgetDialog binding nulled.");
    }





}
