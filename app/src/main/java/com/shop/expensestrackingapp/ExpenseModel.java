package com.shop.expensestrackingapp;

public class ExpenseModel {
    private int id;
    private String categoryName;
    private String categoryIconIdentifier;
    private String note;
    private double amount;
    private String displayDate; // Formatted date for display
    private String rawTimestamp;

    public ExpenseModel(int id, String categoryName, String categoryIconIdentifier, String note, double amount, String displayDate, String rawTimestamp) {
        this.id = id;
        this.categoryName = categoryName;
        this.categoryIconIdentifier = categoryIconIdentifier;
        this.note = note;
        this.amount = amount;
        this.displayDate = displayDate;
        this.rawTimestamp = rawTimestamp;
    }

    public int getId() {
        return id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getCategoryIconIdentifier() {
        return categoryIconIdentifier;
    }

    public String getNote() {
        return note;
    }

    public double getAmount() {
        return amount;
    }

    public String getDisplayDate() {
        return displayDate;
    }

    public String getRawTimestamp() {
        return rawTimestamp;
    }


}
