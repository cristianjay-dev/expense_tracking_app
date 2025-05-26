package com.shop.expensestrackingapp;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "SmartSpendSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_EMAIL = "email";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void createLoginSession(int userId, String email, String firstName) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_FIRST_NAME, firstName);
        editor.apply();
    }

    public int getUserId() {
        return sharedPreferences.getInt(KEY_USER_ID, -1);
    }

    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    public String getFirstName() {
        return sharedPreferences.getString(KEY_FIRST_NAME, null);
    }

    public boolean isLoggedIn() {
        return sharedPreferences.contains(KEY_USER_ID);
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
    public void clearSession() {
        editor.clear();
        editor.apply();
    }
    public void updateFirstName(String newFirstName) {
        editor.putString(KEY_FIRST_NAME, newFirstName); // Ensure KEY_FIRST_NAME is defined
        editor.apply();
    }

    public void updateEmail(String newEmail) {
        editor.putString(KEY_EMAIL, newEmail);
        editor.apply();
    }


}

