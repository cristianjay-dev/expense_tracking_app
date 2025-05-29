package com.shop.expensestrackingapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.shop.expensestrackingapp.databinding.ItemBudgetHistoryBinding; // Generated from item_budget_history.xml

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar; // Use java.util.Calendar
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BudgetHistoryAdapter extends RecyclerView.Adapter<BudgetHistoryAdapter.BudgetHistoryViewHolder> {

    private List<BudgetHistoryModel> historyList;
    private Context context; // Needed for resources like colors
    private NumberFormat phpCurrencyFormatter;

    // Date formatters for display
    private final SimpleDateFormat sdfDisplayMonthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat sdfDisplayDayMonthYear = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()); // For week of
    private final SimpleDateFormat sdfDisplayFullDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat sdfDbDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // To parse DB dates

    private static final String TAG_ADAPTER = "BudgetHistoryAdapter";

    public BudgetHistoryAdapter(Context context, List<BudgetHistoryModel> historyList) {
        this.context = context;
        this.historyList = historyList;

        // Initialize PHP currency formatter
        Locale phLocale = new Locale("fil", "PH"); // "fil" for Filipino language, "PH" for Philippines
        this.phpCurrencyFormatter = NumberFormat.getCurrencyInstance(phLocale);
        this.phpCurrencyFormatter.setMinimumFractionDigits(2);
        this.phpCurrencyFormatter.setMaximumFractionDigits(2);
    }

    public void updateHistoryList(List<BudgetHistoryModel> newHistoryList) {
        Log.d(TAG_ADAPTER, "updateHistoryList called. New list size: " + (newHistoryList != null ? newHistoryList.size() : "null"));
        if (this.historyList == null) {
            this.historyList = new ArrayList<>();
        }
        this.historyList.clear();
        if (newHistoryList != null) {
            this.historyList.addAll(newHistoryList);
        }
        notifyDataSetChanged();
        Log.d(TAG_ADAPTER, "notifyDataSetChanged called. Final list size in adapter: " + this.historyList.size());
    }

    @NonNull
    @Override
    public BudgetHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemBudgetHistoryBinding binding = ItemBudgetHistoryBinding.inflate(inflater, parent, false);
        return new BudgetHistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetHistoryViewHolder holder, int position) {
        if (historyList == null || position >= historyList.size() || historyList.get(position) == null) {
            Log.e(TAG_ADAPTER, "Invalid item or position in onBindViewHolder. Position: " + position);
            return;
        }
        BudgetHistoryModel historyItem = historyList.get(position);
        holder.bind(historyItem);
    }

    @Override
    public int getItemCount() {
        int count = historyList != null ? historyList.size() : 0;
        Log.d(TAG_ADAPTER, "getItemCount returning: " + count);
        return count;
    }

    // Inner ViewHolder class
    class BudgetHistoryViewHolder extends RecyclerView.ViewHolder {
        private ItemBudgetHistoryBinding binding; // ViewBinding instance

        public BudgetHistoryViewHolder(ItemBudgetHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(BudgetHistoryModel item) {
            String periodTitle = "Budget Period"; // Default
            String dateRangeText = "N/A";

            try {
                Date startDate = sdfDbDate.parse(item.getPeriodStartDate());
                Date endDate = sdfDbDate.parse(item.getPeriodEndDate());

                if (startDate != null && endDate != null) {
                    dateRangeText = sdfDisplayFullDate.format(startDate) + " - " + sdfDisplayFullDate.format(endDate);

                    if ("DAILY".equals(item.getPeriodType())) {
                        periodTitle = sdfDisplayDayMonthYear.format(startDate) + " (Daily)";
                    } else if ("WEEKLY".equals(item.getPeriodType())) {
                        periodTitle = "Week of " + sdfDisplayDayMonthYear.format(startDate) + " (Weekly)";
                    } else if ("MONTHLY".equals(item.getPeriodType())) {
                        periodTitle = sdfDisplayMonthYear.format(startDate) + " (Monthly)";
                    } else {
                        periodTitle = item.getPeriodType() + " Budget"; // Fallback
                    }
                }
            } catch (ParseException e) {
                Log.e(TAG_ADAPTER, "Error parsing dates for history item: " + item.getPeriodStartDate(), e);
                periodTitle = item.getPeriodType() + " (Error)"; // Indicate parsing error
                dateRangeText = item.getPeriodStartDate() + " to " + item.getPeriodEndDate();
            } catch (NullPointerException e){
                Log.e(TAG_ADAPTER, "Date string was null for history item: " + item.getPeriodStartDate(), e);
            }


            binding.tvHistoryPeriod.setText(periodTitle);
            binding.tvHistoryDateRange.setText(dateRangeText);
            binding.tvHistoryBudgetAmount.setText(phpCurrencyFormatter.format(item.getBudgetAmountSet()));
            binding.tvHistoryTotalSpent.setText(phpCurrencyFormatter.format(item.getTotalSpentInPeriod()));

            double difference = item.getBudgetAmountSet() - item.getTotalSpentInPeriod();

            if (difference >= 0) {
                binding.tvHistoryDifferenceLabel.setText("Saved:");
                binding.tvHistoryDifferenceAmount.setText(phpCurrencyFormatter.format(difference));
                // Ensure colors are defined in your colors.xml
                binding.tvHistoryDifferenceAmount.setTextColor(ContextCompat.getColor(context, R.color.colorOnPositive));
            } else {
                binding.tvHistoryDifferenceLabel.setText("Overspent:");
                binding.tvHistoryDifferenceAmount.setText(phpCurrencyFormatter.format(Math.abs(difference)));
                binding.tvHistoryDifferenceAmount.setTextColor(ContextCompat.getColor(context, R.color.colorOnNegative));
            }
        }
    }
}