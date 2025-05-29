package com.shop.expensestrackingapp;

public class BudgetHistoryModel {
    private int id;
    private String periodType; // "DAILY", "WEEKLY", "MONTHLY"
    private double budgetAmountSet;
    private double totalSpentInPeriod;
    private String periodStartDate; // Format: "YYYY-MM-DD"
    private String periodEndDate;   // Format: "YYYY-MM-DD"
    private String summaryCreatedAt; // Format: "YYYY-MM-DD HH:MM:SS" - When this history record was made

    public BudgetHistoryModel(int id, String periodType, double budgetAmountSet, double totalSpentInPeriod,
                              String periodStartDate, String periodEndDate, String summaryCreatedAt) {
        this.id = id;
        this.periodType = periodType;
        this.budgetAmountSet = budgetAmountSet;
        this.totalSpentInPeriod = totalSpentInPeriod;
        this.periodStartDate = periodStartDate;
        this.periodEndDate = periodEndDate;
        this.summaryCreatedAt = summaryCreatedAt;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getPeriodType() {
        return periodType;
    }

    public double getBudgetAmountSet() {
        return budgetAmountSet;
    }

    public double getTotalSpentInPeriod() {
        return totalSpentInPeriod;
    }

    public String getPeriodStartDate() {
        return periodStartDate;
    }

    public String getPeriodEndDate() {
        return periodEndDate;
    }

    public String getSummaryCreatedAt() {
        return summaryCreatedAt;
    }
}
