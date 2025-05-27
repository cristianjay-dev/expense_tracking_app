package com.shop.expensestrackingapp;

import androidx.annotation.NonNull;

public class CategoryItem {
    private int id;
    private String name;
    private String iconIdentifier;

    public CategoryItem(int id, String name, String iconIdentifier) {
        this.id = id;
        this.name = name;
        this.iconIdentifier = iconIdentifier;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIconIdentifier() {
        return iconIdentifier;
    }

    @Override
    public String toString() {
        return name; // Crucial for ArrayAdapter to display the name
    }
}
