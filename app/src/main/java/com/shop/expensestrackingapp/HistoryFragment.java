package com.shop.expensestrackingapp;

import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.shop.expensestrackingapp.databinding.FragmentHistoryBinding;

import java.util.ArrayList;
import java.util.List;


public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding; // ViewBinding for fragment_history.xml
    private DatabaseGateway dbGateway;
    private SessionManager sessionManager;
    private BudgetHistoryAdapter historyAdapter;
    private List<BudgetHistoryModel> budgetHistoryList;
    private int currentUserId = -1;

    private static final String TAG = "HistoryFragment";

    public HistoryFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Initializing...");
        if (getContext() != null) {
            dbGateway = new DatabaseGateway(requireContext());
            sessionManager = new SessionManager(requireContext());
            currentUserId = sessionManager.getUserId();
        } else {
            Log.e(TAG, "onCreate: Context is null, cannot initialize DB or SessionManager.");
        }
        Log.d(TAG, "Current User ID in onCreate: " + currentUserId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Inflating layout.");
        // Inflate the layout for this fragment
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Setting up UI components for User ID: " + currentUserId);

        if (binding == null) {
            Log.e(TAG, "Binding is null in onViewCreated!");
            return;
        }
        // Re-initialize if context was null during onCreate
        if (dbGateway == null && getContext() != null) dbGateway = new DatabaseGateway(requireContext());
        if (sessionManager == null && getContext() != null) sessionManager = new SessionManager(requireContext());
        if (currentUserId == -1 && sessionManager != null) currentUserId = sessionManager.getUserId();


        if (currentUserId == -1) {
            Log.e(TAG, "User ID is -1 in onViewCreated. Cannot display history.");
            binding.tvNoHistoryMessage.setText("User not logged in. Cannot display history.");
            binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
            binding.recyclerViewBudgetHistory.setVisibility(View.GONE);
            return;
        }

        setupRecyclerView();
        // loadBudgetHistory() will be called in onResume
    }

    private void setupRecyclerView() {
        if (binding == null || getContext() == null) {
            Log.e(TAG, "setupRecyclerView: Binding or context is null.");
            return;
        }
        budgetHistoryList = new ArrayList<>();
        historyAdapter = new BudgetHistoryAdapter(requireContext(), budgetHistoryList);
        binding.recyclerViewBudgetHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewBudgetHistory.setAdapter(historyAdapter);
        Log.d(TAG, "RecyclerView and BudgetHistoryAdapter setup complete.");
    }

    private void loadBudgetHistory() {
        if (binding == null || dbGateway == null || currentUserId == -1 || !isAdded()) {
            Log.w(TAG, "loadBudgetHistory: Pre-conditions not met (binding, dbGateway, userId, or fragment not added).");
            if (binding != null && binding.tvNoHistoryMessage != null) { // Check tvNoHistoryMessage as well
                binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
                binding.tvNoHistoryMessage.setText(currentUserId == -1 ? "User not logged in." : "Could not load history.");
            }
            if (binding != null && binding.recyclerViewBudgetHistory != null) {
                binding.recyclerViewBudgetHistory.setVisibility(View.GONE);
            }
            return;
        }

        Log.d(TAG, "Loading budget history for user: " + currentUserId);
        if (budgetHistoryList == null) budgetHistoryList = new ArrayList<>(); // Should be initialized in setupRecyclerView
        budgetHistoryList.clear();
        Cursor cursor = null;

        try {
            cursor = dbGateway.getBudgetHistory(currentUserId);
            if (cursor != null) {
                Log.d(TAG, "Budget history cursor count: " + cursor.getCount());
                if (cursor.getCount() == 0) {
                    binding.tvNoHistoryMessage.setText("No budget history recorded yet.");
                    binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
                    binding.recyclerViewBudgetHistory.setVisibility(View.GONE);
                } else {
                    binding.tvNoHistoryMessage.setVisibility(View.GONE);
                    binding.recyclerViewBudgetHistory.setVisibility(View.VISIBLE);

                    int idCol = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_HISTORY_ID);
                    int typeCol = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_HISTORY_PERIOD_TYPE);
                    int budgetSetCol = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_HISTORY_BUDGET_AMOUNT_SET);
                    int totalSpentCol = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_HISTORY_TOTAL_SPENT);
                    int startDateCol = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_HISTORY_PERIOD_START_DATE);
                    int endDateCol = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_HISTORY_PERIOD_END_DATE);
                    int createdAtCol = cursor.getColumnIndexOrThrow(DatabaseGateway.COL_HISTORY_SUMMARY_CREATED_AT);

                    while (cursor.moveToNext()) {
                        budgetHistoryList.add(new BudgetHistoryModel(
                                cursor.getInt(idCol),
                                cursor.getString(typeCol),
                                cursor.getDouble(budgetSetCol),
                                cursor.getDouble(totalSpentCol),
                                cursor.getString(startDateCol),
                                cursor.getString(endDateCol),
                                cursor.getString(createdAtCol)
                        ));
                        Log.v(TAG, "Loaded history item: " + cursor.getString(typeCol) + " for " + cursor.getString(startDateCol));
                    }
                }
                if (historyAdapter != null) {
                    // historyAdapter.updateHistoryList(budgetHistoryList); // If you use this method in adapter
                    historyAdapter.notifyDataSetChanged(); // If adapter uses the same list instance
                } else {
                    Log.e(TAG, "historyAdapter is null, cannot notify.");
                }
            } else {
                Log.w(TAG, "getBudgetHistory returned null cursor.");
                binding.tvNoHistoryMessage.setText("Could not load history (null cursor).");
                binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
                binding.recyclerViewBudgetHistory.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading or processing budget history cursor", e);
            if (getContext() != null) Toast.makeText(getContext(), "Error loading history data.", Toast.LENGTH_SHORT).show();
            binding.tvNoHistoryMessage.setText("Error loading history.");
            binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
            binding.recyclerViewBudgetHistory.setVisibility(View.GONE);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "Budget history loaded. List size: " + budgetHistoryList.size());
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: HistoryFragment is becoming visible/resumed.");
        // Refresh history every time the fragment is resumed,
        // as HomeFragment might have archived new items.
        if (currentUserId != -1 && isAdded()) { // isAdded() checks if fragment is attached
            loadBudgetHistory();
        } else if (currentUserId == -1 && binding != null) {
            binding.tvNoHistoryMessage.setText("User not logged in.");
            binding.tvNoHistoryMessage.setVisibility(View.VISIBLE);
            binding.recyclerViewBudgetHistory.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Important for preventing memory leaks
        Log.d(TAG, "onDestroyView: HistoryFragment binding nulled.");
    }
}