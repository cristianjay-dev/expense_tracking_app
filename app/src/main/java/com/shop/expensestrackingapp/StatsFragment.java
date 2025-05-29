package com.shop.expensestrackingapp;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.shop.expensestrackingapp.databinding.FragmentStatsBinding;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private FragmentStatsBinding binding;
    private DatabaseGateway dbGateway;
    private SessionManager sessionManager;
    private int currentUserId = -1;

    private String selectedStatsPeriodKey = "CURRENT_MONTH"; // Default
    private String statsStartDate, statsEndDate;

    private final SimpleDateFormat sdfDbDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat sdfMonthYearDisplay = new SimpleDateFormat("MMM yyyy", Locale.getDefault()); // For display
    private final SimpleDateFormat sdfYearMonthDb = new SimpleDateFormat("yyyy-MM", Locale.getDefault()); // For DB grouping


    private NumberFormat phpCurrencyFormatter;

    private static final String TAG = "StatsFragment";

    public StatsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            dbGateway = new DatabaseGateway(requireContext());
            sessionManager = new SessionManager(requireContext());
            currentUserId = sessionManager.getUserId();

            Locale phLocale = new Locale("fil", "PH");
            phpCurrencyFormatter = NumberFormat.getCurrencyInstance(phLocale);
            phpCurrencyFormatter.setMinimumFractionDigits(2);
            phpCurrencyFormatter.setMaximumFractionDigits(2);
        }
        Log.d(TAG, "Current User ID in onCreate: " + currentUserId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (binding == null) { Log.e(TAG, "Binding is null!"); return; }

        if (currentUserId == -1) {
            binding.tvNoStatsData.setVisibility(View.VISIBLE);
            binding.tvNoStatsData.setText("User not logged in.");
            hideChartsAndSummary();
            return;
        }
        setupPeriodSelector();
        // Initial load based on default selectedStatsPeriodKey
        updateDateRangeForStats();
        loadAndDisplayStats();
    }

    private void hideChartsAndSummary(){
        if(binding == null) return;
        binding.cardStatsSummary.setVisibility(View.GONE);
        binding.pieChartCategorySpending.setVisibility(View.GONE);
        binding.barChartSpendingTrend.setVisibility(View.GONE);
    }
    private void showChartsAndSummary(){
        if(binding == null) return;
        binding.cardStatsSummary.setVisibility(View.VISIBLE);
        binding.pieChartCategorySpending.setVisibility(View.VISIBLE);
        binding.barChartSpendingTrend.setVisibility(View.VISIBLE);
        binding.tvTitleSpendingByCategory.setVisibility(View.GONE);
    }


    private void setupPeriodSelector() {
        // Define periods for stats
        // Using a Map for display name -> key
        final Map<String, String> periodMap = new HashMap<>();
        periodMap.put("This Week", "CURRENT_WEEK");
        periodMap.put("This Month", "CURRENT_MONTH");
        periodMap.put("Last Week", "LAST_WEEK");
        periodMap.put("Last Month", "LAST_MONTH");
        periodMap.put("Last 3 Months", "LAST_3_MONTHS");
        periodMap.put("All Time", "ALL_TIME");

        List<String> periodDisplayNames = new ArrayList<>(periodMap.keySet());
        // Ensure a specific order if needed, e.g., by sorting or defining in order
        // For now, using HashMap's potential order

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, periodDisplayNames);
        binding.actvStatsPeriod.setAdapter(adapter);

        // Set default selection
        String defaultDisplay = "This Month"; // Or get key from selectedStatsPeriodKey
        for(Map.Entry<String, String> entry : periodMap.entrySet()){
            if(entry.getValue().equals(selectedStatsPeriodKey)){
                defaultDisplay = entry.getKey();
                break;
            }
        }
        binding.actvStatsPeriod.setText(defaultDisplay, false);


        binding.actvStatsPeriod.setOnItemClickListener((parent, Lview, position, id) -> {
            String selectedText = (String) parent.getItemAtPosition(position);
            selectedStatsPeriodKey = periodMap.get(selectedText); // Get the key
            Log.d(TAG, "Stats period selected: " + selectedText + " (Key: " + selectedStatsPeriodKey + ")");
            updateDateRangeForStats();
            loadAndDisplayStats();
        });
    }

    private void updateDateRangeForStats() {
        Calendar cal = Calendar.getInstance();
        Date today = new Date();

        switch (selectedStatsPeriodKey) {
            case "CURRENT_WEEK":
                cal.setTime(today);
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                statsStartDate = sdfDbDateOnly.format(cal.getTime());
                cal.add(Calendar.DAY_OF_WEEK, 6);
                statsEndDate = sdfDbDateOnly.format(cal.getTime());
                break;
            case "LAST_WEEK":
                cal.setTime(today);
                cal.add(Calendar.WEEK_OF_YEAR, -1); // Go to previous week
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                statsStartDate = sdfDbDateOnly.format(cal.getTime());
                cal.add(Calendar.DAY_OF_WEEK, 6);
                statsEndDate = sdfDbDateOnly.format(cal.getTime());
                break;
            case "CURRENT_MONTH":
                cal.setTime(today);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                statsStartDate = sdfDbDateOnly.format(cal.getTime());
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                statsEndDate = sdfDbDateOnly.format(cal.getTime());
                break;
            case "LAST_MONTH":
                cal.setTime(today);
                cal.add(Calendar.MONTH, -1); // Go to previous month
                cal.set(Calendar.DAY_OF_MONTH, 1);
                statsStartDate = sdfDbDateOnly.format(cal.getTime());
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                statsEndDate = sdfDbDateOnly.format(cal.getTime());
                break;
            case "LAST_3_MONTHS":
                // End date is end of last month
                Calendar endCal = Calendar.getInstance();
                endCal.setTime(today);
                endCal.add(Calendar.MONTH, -1); // Go to previous month
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                statsEndDate = sdfDbDateOnly.format(endCal.getTime());

                // Start date is start of month 2 months before last month (i.e., 3 months ago from current)
                Calendar startCal = Calendar.getInstance();
                startCal.setTime(today);
                startCal.add(Calendar.MONTH, -3); // Go back 3 months
                startCal.set(Calendar.DAY_OF_MONTH, 1);
                statsStartDate = sdfDbDateOnly.format(startCal.getTime());
                break;
            case "ALL_TIME":
            default:
                statsStartDate = "1970-01-01";
                statsEndDate = "2999-12-31";
                break;
        }
        Log.d(TAG, "Date range for STATS updated for " + selectedStatsPeriodKey + ": [" + statsStartDate + "] to [" + statsEndDate + "]");
    }

    private void loadAndDisplayStats() {
        if (binding == null || dbGateway == null || currentUserId == -1 || statsStartDate == null || statsEndDate == null) {
            Log.w(TAG, "Cannot load stats, pre-conditions not met.");
            if (binding != null && binding.tvNoStatsData != null) { // Check tvNoStatsData as well
                binding.tvNoStatsData.setVisibility(View.VISIBLE);
            }
            hideChartsAndSummary();
            return;
        }
        Log.d(TAG, "Loading stats for period: " + statsStartDate + " to " + statsEndDate);
        // 1. Overall Summary Card
        updateOverallSummaryCard();

        // 2. Pie Chart for Category Spending
        setupCategoryPieChart();

        // 3. Bar Chart for Monthly Spending Trend (e.g., last 6 months)
        setupMonthlySpendingBarChart(6);

        // Check if any data was actually loaded for charts
        boolean hasCategoryData = false;
        if (binding.pieChartCategorySpending.getData() != null &&
                binding.pieChartCategorySpending.getData().getDataSetCount() > 0 && // Check if there's at least one dataset
                binding.pieChartCategorySpending.getData().getEntryCount() > 0) {    // Check if that dataset has entries
            hasCategoryData = true;
        }
        Log.d(TAG, "PieChart has data: " + hasCategoryData);


        boolean hasTrendData = false;
        if (binding.barChartSpendingTrend.getData() != null &&
                binding.barChartSpendingTrend.getData().getDataSetCount() > 0 &&
                binding.barChartSpendingTrend.getData().getEntryCount() > 0) {
            hasTrendData = true;
        }
        Log.d(TAG, "BarChart has data: " + hasTrendData);


        double totalSpentForPeriod = dbGateway.getTotalExpensesForPeriod(currentUserId, statsStartDate, statsEndDate);
        boolean hasSummaryDataToShow = totalSpentForPeriod > 0;

        if (hasCategoryData || hasTrendData || hasSummaryDataToShow) {
        binding.tvNoStatsData.setVisibility(View.GONE);
        showChartsAndSummary();
        } else {
            binding.tvNoStatsData.setVisibility(View.VISIBLE);
            hideChartsAndSummary();
        }
    }

    private void updateOverallSummaryCard() {
        if (binding == null || dbGateway == null) return;

        String periodTitle = binding.actvStatsPeriod.getText().toString() + " Performance";
        binding.tvSummaryPeriodTitle.setText(periodTitle);

        double totalSpent = dbGateway.getTotalExpensesForPeriod(currentUserId, statsStartDate, statsEndDate);
        binding.tvSummarySpentAmount.setText(phpCurrencyFormatter.format(totalSpent));

        // For "Budgeted", we need to find a budget that matches the selectedStatsPeriodKey
        // This is tricky if selectedStatsPeriodKey is "Last Month" etc.
        // For simplicity, let's try to find a budget for the *current* month/week if applicable
        // Or show "N/A"
        double budgetAmount = 0;
        String budgetPeriodTypeForQuery = null;
        String budgetStartDateForQuery = null;

        if("CURRENT_WEEK".equals(selectedStatsPeriodKey)) {
            budgetPeriodTypeForQuery = "WEEKLY";
            budgetStartDateForQuery = statsStartDate; // Start of current week
        } else if ("CURRENT_MONTH".equals(selectedStatsPeriodKey)) {
            budgetPeriodTypeForQuery = "MONTHLY";
            budgetStartDateForQuery = statsStartDate; // Start of current month
        }
        // For other periods like "LAST_MONTH", "ALL_TIME", finding a single "budgeted" amount
        // for the summary card is less straightforward without more complex budget history logic.
        // For now, we only show budget comparison if viewing current week/month stats.

        if (budgetPeriodTypeForQuery != null && budgetStartDateForQuery != null) {
            Cursor budgetCursor = dbGateway.getBudgetForSpecificPeriod(currentUserId, budgetPeriodTypeForQuery, budgetStartDateForQuery);
            if (budgetCursor != null && budgetCursor.moveToFirst()) {
                budgetAmount = budgetCursor.getDouble(budgetCursor.getColumnIndexOrThrow(DatabaseGateway.COL_BUDGET_AMOUNT));
            }
            if (budgetCursor != null) budgetCursor.close();
        }

        if (budgetAmount > 0) {
            binding.tvSummaryBudgetedAmount.setText(phpCurrencyFormatter.format(budgetAmount));
            int progress = (int) ((totalSpent / budgetAmount) * 100.0);
            binding.progressSummary.setProgress(Math.max(0, Math.min(100, progress)));
            binding.tvSummaryPercentage.setText(Math.max(0, Math.min(100, progress)) + "% of budget spent");
            binding.progressSummary.setVisibility(View.VISIBLE);
            binding.tvSummaryPercentage.setVisibility(View.VISIBLE);

            double difference = budgetAmount - totalSpent;
            if (difference >= 0) {
                binding.tvSummaryDifferenceLabel.setText("Remaining: ");
                binding.tvSummaryDifferenceAmount.setText(phpCurrencyFormatter.format(difference));
                binding.tvSummaryDifferenceAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnPositive));
            } else {
                binding.tvSummaryDifferenceLabel.setText("Overspent: ");
                binding.tvSummaryDifferenceAmount.setText(phpCurrencyFormatter.format(Math.abs(difference)));
                binding.tvSummaryDifferenceAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnNegative));
            }
        } else {
            binding.tvSummaryBudgetedAmount.setText("N/A");
            binding.progressSummary.setVisibility(View.INVISIBLE);
            binding.tvSummaryPercentage.setVisibility(View.INVISIBLE);
            binding.tvSummaryDifferenceLabel.setText("Balance: ");
            binding.tvSummaryDifferenceAmount.setText(phpCurrencyFormatter.format(-totalSpent)); // Show total spent as negative balance
            binding.tvSummaryDifferenceAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnBackground));
        }
    }


    private void setupCategoryPieChart() {
        if (binding == null || dbGateway == null) return;
        PieChart pieChart = binding.pieChartCategorySpending;
        pieChart.clear();

        Cursor categoryCursor = dbGateway.getCategorySpendingForPeriod(currentUserId, statsStartDate, statsEndDate);
        ArrayList<PieEntry> entries = new ArrayList<>();
        float totalSpendingForPie = 0f;

        if (categoryCursor != null && categoryCursor.moveToFirst()) {
            // ... (populate entries and totalSpendingForPie as before) ...
            int categoryNameCol = categoryCursor.getColumnIndexOrThrow(DatabaseGateway.COL_CATEGORY_NAME);
            int totalSpentCol = categoryCursor.getColumnIndexOrThrow("total_spent");
            do {
                String categoryName = categoryCursor.getString(categoryNameCol);
                float spent = categoryCursor.getFloat(totalSpentCol);
                entries.add(new PieEntry(spent, categoryName));
                totalSpendingForPie += spent;
            } while (categoryCursor.moveToNext());
        }
        if (categoryCursor != null) categoryCursor.close();

        if (entries.isEmpty()) {
            Log.d(TAG, "No category spending data for pie chart in this period.");
            pieChart.setVisibility(View.GONE);
            // Consider hiding the "Spending by Category" title TextView as well
            // Example: binding.yourPieChartTitleTextView.setVisibility(View.GONE);
        } else {
            pieChart.setVisibility(View.VISIBLE);
            // Example: binding.yourPieChartTitleTextView.setVisibility(View.VISIBLE);
            PieDataSet dataSet = new PieDataSet(entries, ""); // Empty label for dataset if legend is enough
            dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            dataSet.setValueTextColor(Color.WHITE); // Or a theme-aware color
            dataSet.setValueTextSize(10f); // Adjusted size
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);
            dataSet.setValueFormatter(new PercentFormatter(pieChart));

            PieData pieData = new PieData(dataSet);
            pieChart.setData(pieData);
            pieChart.setUsePercentValues(true);
            pieChart.getDescription().setEnabled(false);
            pieChart.setExtraOffsets(5, 10, 5, 5);
            pieChart.setDragDecelerationFrictionCoef(0.95f);
            pieChart.setDrawHoleEnabled(true);
            pieChart.setHoleColor(Color.WHITE);
            pieChart.setHoleRadius(50f); // Adjust hole size
            pieChart.setTransparentCircleRadius(55f); // Adjust transparent circle
            pieChart.setTransparentCircleAlpha(110);
            pieChart.setCenterText(totalSpendingForPie > 0 ? phpCurrencyFormatter.format(totalSpendingForPie) : "No Spending");
            pieChart.setCenterTextSize(16f);
            pieChart.setEntryLabelColor(Color.WHITE); // Color for labels on slices
            pieChart.setEntryLabelTextSize(12f);
            // pieChart.setDrawEntryLabels(false); // Optionally hide labels on slices if too cluttered
            pieChart.getLegend().setEnabled(true); // Enable legend
            pieChart.animateY(1000, Easing.EaseInOutCubic);
            Legend l = pieChart.getLegend();
            l.setEnabled(false);
            pieChart.setDrawEntryLabels(true); // Make sure this is true
            pieChart.setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.colorOnBackground));
            pieChart.setEntryLabelTextSize(11f);
        }
        pieChart.invalidate();
    }

    private void setupMonthlySpendingBarChart(int numberOfMonths) {
        if (binding == null || dbGateway == null) return;
        BarChart barChart = binding.barChartSpendingTrend;
        barChart.clear();

        Cursor trendCursor = dbGateway.getMonthlySpendingTrend(currentUserId, numberOfMonths);
        ArrayList<BarEntry> entries = new ArrayList<>();
        final ArrayList<String> xAxisLabels = new ArrayList<>();
        int i = 0;

        Map<String, Float> monthlyTotals = new HashMap<>();
        if (trendCursor != null && trendCursor.moveToFirst()) {
            int monthYearCol = trendCursor.getColumnIndexOrThrow("month_year"); // Alias from query
            int totalSpentCol = trendCursor.getColumnIndexOrThrow("total_spent");

            do {
                String monthYear = trendCursor.getString(monthYearCol); // YYYY-MM
                float spent = trendCursor.getFloat(totalSpentCol);
                monthlyTotals.put(monthYear, spent);
            } while (trendCursor.moveToNext());
            trendCursor.close();
        }

        if (monthlyTotals.isEmpty() && numberOfMonths > 0) { // Fill with zeros if no data but we want to show months
            Calendar cal = Calendar.getInstance();
            for (int j = 0; j < numberOfMonths; j++) {
                String monthYearLabel = sdfYearMonthDb.format(cal.getTime()); // YYYY-MM
                try { // Attempt to format for display
                    Date monthDate = sdfYearMonthDb.parse(monthYearLabel);
                    xAxisLabels.add(0, sdfMonthYearDisplay.format(monthDate != null ? monthDate : new Date())); // Add to beginning to reverse order for display
                } catch (ParseException e) {
                    xAxisLabels.add(0, monthYearLabel);
                }
                entries.add(0, new BarEntry(numberOfMonths - 1 - j, 0f)); // X value from right to left
                cal.add(Calendar.MONTH, -1);
            }
        } else {
            // Generate labels and entries for the last N months, filling missing ones with 0
            Calendar cal = Calendar.getInstance(); // Current month
            for (int k = 0; k < numberOfMonths; k++) {
                String monthYearKey = sdfYearMonthDb.format(cal.getTime()); // YYYY-MM
                String displayMonth = "";
                try {
                    Date monthDate = sdfYearMonthDb.parse(monthYearKey);
                    displayMonth = sdfMonthYearDisplay.format(monthDate != null ? monthDate : new Date());
                } catch (ParseException e) {
                    displayMonth = monthYearKey; // Fallback
                }

                xAxisLabels.add(0, displayMonth); // Add to beginning for correct order on X-axis
                float spent = monthlyTotals.containsKey(monthYearKey) ? monthlyTotals.get(monthYearKey) : 0f;
                entries.add(0, new BarEntry(numberOfMonths - 1 - k, spent)); // X value represents index from right to left
                cal.add(Calendar.MONTH, -1); // Go to previous month
            }
        }


        if (entries.isEmpty()) {
            Log.d(TAG, "No monthly trend data for bar chart.");
            barChart.setVisibility(View.GONE); // Hide if no data
            return;
        }
        barChart.setVisibility(View.VISIBLE);


        BarDataSet dataSet = new BarDataSet(entries, "Monthly Spending");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setFitBars(true); // make the x-axis fit exactly all bars

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnBackground)); // Set X-axis label color
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(-45); // Rotate labels if they overlap

        barChart.getAxisRight().setEnabled(false); // Disable right Y-axis
        barChart.animateY(1000);
        barChart.getLegend().setEnabled(false);
        barChart.invalidate(); // Refresh
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorOnBackground)); // Set Y-axis label color
        leftAxis.setDrawGridLines(true); // Keep horizontal grid lines
        leftAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.colorOnBackground)); // Style grid lines

        leftAxis.setValueFormatter(new ValueFormatter() { // Format Y-axis with currency
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                return phpCurrencyFormatter.format(value);
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: StatsFragment resumed.");
        if (currentUserId != -1 && isAdded()) {
            if (statsStartDate == null) { // Initial load or if cleared
                updateDateRangeForStats();
            }
            loadAndDisplayStats();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important for Fragments
    }
}