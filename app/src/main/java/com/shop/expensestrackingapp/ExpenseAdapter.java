package com.shop.expensestrackingapp;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shop.expensestrackingapp.databinding.ItemExpenseBinding; // Ensure this is your generated ViewBinding class

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends
        RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<ExpenseModel> expenseList;
    private boolean isManageModeEnabled = false;
    private OnExpenseActionsListener actionsListener;
    private NumberFormat phpCurrencyFormatter;
    private Context context; // For getResources in ViewHolder

    public interface OnExpenseActionsListener {
        void onDeleteClicked(ExpenseModel expense, int position);
    }

    public ExpenseAdapter(Context context, List<ExpenseModel> expenseList, OnExpenseActionsListener listener) {
        this.context = context; // Store context from constructor
        this.expenseList = expenseList;
        this.actionsListener = listener;

        // Initialize PHP currency formatter
        Locale phLocale = new Locale("fil", "PH");
        this.phpCurrencyFormatter = NumberFormat.getCurrencyInstance(phLocale);
        this.phpCurrencyFormatter.setMinimumFractionDigits(2);
        this.phpCurrencyFormatter.setMaximumFractionDigits(2);
    }

    public void setManageMode(boolean enabled) {
        this.isManageModeEnabled = enabled;
        notifyDataSetChanged(); // Re-bind all visible items to reflect mode change
    }

    public boolean isManageModeEnabled() {
        return isManageModeEnabled;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemExpenseBinding binding = ItemExpenseBinding.inflate(inflater, parent, false);
        return new ExpenseViewHolder(binding, context, phpCurrencyFormatter);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Log.d("ExpenseAdapter", "onBindViewHolder: Binding data for position: " + position);
        if (expenseList == null || position >= expenseList.size()) {
            Log.e("ExpenseAdapter", "onBindViewHolder: Invalid position or null/empty expenseList. Pos: " + position + ", Size: " + (expenseList != null ? expenseList.size() : "null"));
            return;
        }
        ExpenseModel expense = expenseList.get(position);
        if (expense == null) {
            Log.e("ExpenseAdapter", "onBindViewHolder: ExpenseModel at position " + position + " is NULL.");
            return;
        }
        holder.bind(expense);

        if (isManageModeEnabled) {
            holder.binding.btnDeleteExpense.setVisibility(View.VISIBLE);
            holder.binding.btnDeleteExpense.setOnClickListener(v -> {
                if (actionsListener != null) {
                    int currentClickedPosition = holder.getBindingAdapterPosition();
                    if (currentClickedPosition != RecyclerView.NO_POSITION) {
                        actionsListener.onDeleteClicked(expenseList.get(currentClickedPosition), currentClickedPosition);
                    }
                }
            });
        } else {
            holder.binding.btnDeleteExpense.setVisibility(View.GONE);
            holder.binding.btnDeleteExpense.setOnClickListener(null);
        }

    }

    @Override
    public int getItemCount() {
        int itemCount = expenseList != null ? expenseList.size() : 0;
        Log.d("ExpenseAdapter", "getItemCount() called, returning: " + itemCount);
        return itemCount;
    }

    // ViewHolder class - Make it a static nested class
    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private ItemExpenseBinding binding; // ViewBinding instance
        private Context holderContext;      // Context for getResources().getIdentifier()
        private NumberFormat formatter;     // To format currency

        public ExpenseViewHolder(ItemExpenseBinding binding, Context context, NumberFormat formatter) {
            super(binding.getRoot());
            this.binding = binding;
            this.holderContext = context;
            this.formatter = formatter;
        }

        // Bind method now only takes the ExpenseModel item
        void bind(final ExpenseModel expense) {
            // ... (bind logic is fine as previously provided) ...
            Log.d("ExpenseViewHolder", "bind: Binding expense - Category: " + expense.getCategoryName());
            try {
                binding.tvCategoryName.setText(expense.getCategoryName());
                binding.tvExpenseNote.setText(expense.getNote());
                binding.tvExpenseDate.setText(expense.getDisplayDate());
                binding.tvExpenseAmount.setText("- " + formatter.format(expense.getAmount()));

                if (expense.getCategoryIconIdentifier() != null && !expense.getCategoryIconIdentifier().isEmpty()) {
                    int iconResId = holderContext.getResources().getIdentifier(
                            expense.getCategoryIconIdentifier(),
                            "drawable",
                            holderContext.getPackageName()
                    );
                    if (iconResId != 0) {
                        binding.ivExpenseCategoryIcon.setImageResource(iconResId);
                    } else {
                        binding.ivExpenseCategoryIcon.setImageResource(R.drawable.ic_default_category);
                    }
                } else {
                    binding.ivExpenseCategoryIcon.setImageResource(R.drawable.ic_default_category);
                }
                // Log.d("ExpenseViewHolder", "bind: Expense bound successfully - Category: " + expense.getCategoryName());
            } catch (Exception e) {
                Log.e("ExpenseViewHolder", "bind: CRITICAL ERROR setting data in ViewHolder", e);
            }
        }
    }
}
