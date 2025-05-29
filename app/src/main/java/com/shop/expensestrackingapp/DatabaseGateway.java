package com.shop.expensestrackingapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DatabaseGateway extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SmartSpend.db";
    private static final int DATABASE_VERSION = 3;

    // User Table
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_FIRSTNAME = "firstname";
    public static final String COL_LASTNAME = "lastname";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";
    public static final String COL_PROFILE_IMAGE = "profile_image";

    //Categories Table
    public static final String TABLE_CATEGORIES = "categories";
    public static final String COL_CATEGORY_ID = "category_id";
    public static final String COL_CATEGORY_NAME = "name";
    public static final String COL_CATEGORY_ICON_IDENTIFIER = "icon_identifier";
    public static final String COL_CATEGORY_IS_PREDEFINED = "is_predefined";// Blob (optional)

    // Budget Table
    public static final String TABLE_BUDGETS = "budgets";
    public static final String COL_BUDGET_ID = "budget_id";
    public static final String COL_BUDGET_USER_ID = "user_id";
    public static final String COL_BUDGET_AMOUNT = "budget_amount";
    public static final String COL_BUDGET_PERIOD_TYPE = "period_type"; // "WEEKLY", "MONTHLY"
    public static final String COL_BUDGET_START_DATE = "start_date";   // Format: "YYYY-MM-DD"
    public static final String COL_BUDGET_END_DATE = "end_date";     // Format: "YYYY-MM-DD"
    public static final String COL_BUDGET_CREATED_AT = "created_at";
    // Expenses Table
    public static final String TABLE_EXPENSES = "expenses";
    public static final String COL_EXPENSE_ID = "expense_id";
    public static final String COL_EXPENSE_USER_ID = "user_id";
    public static final String COL_EXPENSE_CATEGORY_ID_FK = "category_id";
    public static final String COL_EXPENSE_AMOUNT = "amount";
    public static final String COL_EXPENSE_NOTE = "note";
    public static final String COL_EXPENSE_TIMESTAMP = "timestamp";
    // Format: "YYYY-MM-DD HH:MM:SS"

    public static final String TABLE_BUDGET_HISTORY = "budget_history";
    public static final String COL_HISTORY_ID = "history_id";
    public static final String COL_HISTORY_USER_ID = "user_id";
    public static final String COL_HISTORY_PERIOD_TYPE = "period_type"; // "WEEKLY", "MONTHLY"
    public static final String COL_HISTORY_BUDGET_AMOUNT_SET = "budget_amount_set";
    public static final String COL_HISTORY_TOTAL_SPENT = "total_spent_in_period";
    public static final String COL_HISTORY_PERIOD_START_DATE = "period_start_date"; // YYYY-MM-DD
    public static final String COL_HISTORY_PERIOD_END_DATE = "period_end_date";   // YYYY-MM-DD
    public static final String COL_HISTORY_SUMMARY_CREATED_AT = "summary_created_at"; // YYYY-MM-DD HH:MM:SS


    public DatabaseGateway(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FIRSTNAME + " TEXT, " +
                COL_LASTNAME + " TEXT, " +
                COL_EMAIL + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT, " +
                COL_PROFILE_IMAGE + " BLOB" +
                ");";

        String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COL_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CATEGORY_NAME + " TEXT UNIQUE NOT NULL, " +
                COL_CATEGORY_ICON_IDENTIFIER + " TEXT, " +
                COL_CATEGORY_IS_PREDEFINED + " INTEGER DEFAULT 1" +
                ");";

        String createExpenseTable = "CREATE TABLE " + TABLE_EXPENSES + " (" +
                COL_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EXPENSE_USER_ID + " INTEGER, " +
                COL_EXPENSE_CATEGORY_ID_FK + " INTEGER, " +
                COL_EXPENSE_AMOUNT + " REAL NOT NULL, " +
                COL_EXPENSE_NOTE + " TEXT, " +
                COL_EXPENSE_TIMESTAMP + " TEXT NOT NULL, " +
                "FOREIGN KEY (" + COL_EXPENSE_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "), " +
                "FOREIGN KEY (" + COL_EXPENSE_CATEGORY_ID_FK + ") REFERENCES " + TABLE_CATEGORIES + "(" + COL_CATEGORY_ID + ")" +
                ");";

        String createBudgetTable = "CREATE TABLE " + TABLE_BUDGETS + " (" +
                COL_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_BUDGET_USER_ID + " INTEGER, " +
                COL_BUDGET_AMOUNT + " REAL NOT NULL, " +
                COL_BUDGET_PERIOD_TYPE + " TEXT NOT NULL, " +
                COL_BUDGET_START_DATE + " TEXT NOT NULL, " +
                COL_BUDGET_END_DATE + " TEXT NOT NULL, " +
                COL_BUDGET_CREATED_AT + " TEXT, " +
                "FOREIGN KEY (" + COL_BUDGET_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                ");";
        String createBudgetHistoryTable = "CREATE TABLE " + TABLE_BUDGET_HISTORY + " (" +
                COL_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_HISTORY_USER_ID + " INTEGER, " +
                COL_HISTORY_PERIOD_TYPE + " TEXT NOT NULL, " +
                COL_HISTORY_BUDGET_AMOUNT_SET + " REAL NOT NULL, " +
                COL_HISTORY_TOTAL_SPENT + " REAL NOT NULL, " +
                COL_HISTORY_PERIOD_START_DATE + " TEXT NOT NULL, " +
                COL_HISTORY_PERIOD_END_DATE + " TEXT NOT NULL, " +
                COL_HISTORY_SUMMARY_CREATED_AT + " TEXT NOT NULL, " +
                "FOREIGN KEY (" + COL_HISTORY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                ");";

        db.execSQL(createUserTable);
        db.execSQL(createCategoriesTable);
        db.execSQL(createExpenseTable);
        db.execSQL(createBudgetTable);
        db.execSQL(createBudgetHistoryTable);

        populatePredefinedCategories(db);
    }
    private void populatePredefinedCategories(SQLiteDatabase db) {
        Log.d("DatabaseGateway", "populatePredefinedCategories: Populating categories.");
        String[] predefinedCategories = {
                "Food:ic_category_food",
                "Rent:ic_category_rent",
                "Bills:ic_category_bill",
                "Transport:ic_category_transportation",
                "Entertainment:ic_category_entertainment",
                "Health:ic_category_health",
                "Groceries:ic_category_grocery",
                "Other:ic_category_others"

        };
        for (String catData : predefinedCategories) {
            String[] parts = catData.split(":");
            if (parts.length == 2) {
                ContentValues values = new ContentValues();
                values.put(COL_CATEGORY_NAME, parts[0].trim());
                values.put(COL_CATEGORY_ICON_IDENTIFIER, parts[1].trim());
                values.put(COL_CATEGORY_IS_PREDEFINED, 1);
                long id = db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (id == -1) {
                    Log.w("DatabaseGateway", "Category already exists or failed to insert: " + parts[0]);
                }
            } else {
                Log.e("DatabaseGateway", "Invalid category data format: " + catData);
            }
        }
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("DatabaseGateway", "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Example of a more structured upgrade path:
        if (oldVersion < 2) {
            Log.d("DatabaseGateway", "Applying V2 schema changes: Adding Categories, Budgets, and modifying Expenses table.");
            // Create Categories Table (if it doesn't exist from a previous partial upgrade attempt)
            String createCategoriesTableSQL = "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + " (" +
                    COL_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_CATEGORY_NAME + " TEXT UNIQUE NOT NULL, " +
                    COL_CATEGORY_ICON_IDENTIFIER + " TEXT, " +
                    COL_CATEGORY_IS_PREDEFINED + " INTEGER DEFAULT 1" +
                    ");";
            db.execSQL(createCategoriesTableSQL);
            populatePredefinedCategories(db); // Populate/re-populate, insertWithOnConflict handles existing

            // Create Budgets Table (if it doesn't exist)
            String createBudgetTableSQL = "CREATE TABLE IF NOT EXISTS " + TABLE_BUDGETS + " (" +
                    COL_BUDGET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_BUDGET_USER_ID + " INTEGER, " +
                    COL_BUDGET_AMOUNT + " REAL NOT NULL, " +
                    COL_BUDGET_PERIOD_TYPE + " TEXT NOT NULL, " +
                    COL_BUDGET_START_DATE + " TEXT NOT NULL, " +
                    COL_BUDGET_END_DATE + " TEXT NOT NULL, " +
                    COL_BUDGET_CREATED_AT + " TEXT, " +
                    "FOREIGN KEY (" + COL_BUDGET_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                    ");";
            db.execSQL(createBudgetTableSQL);


            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
            String createExpenseTableSQL = "CREATE TABLE " + TABLE_EXPENSES + " (" +
                    COL_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_EXPENSE_USER_ID + " INTEGER, " +
                    COL_EXPENSE_CATEGORY_ID_FK + " INTEGER, " +
                    COL_EXPENSE_AMOUNT + " REAL NOT NULL, " +
                    COL_EXPENSE_NOTE + " TEXT, " +
                    COL_EXPENSE_TIMESTAMP + " TEXT NOT NULL, " +
                    "FOREIGN KEY (" + COL_EXPENSE_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "), " +
                    "FOREIGN KEY (" + COL_EXPENSE_CATEGORY_ID_FK + ") REFERENCES " + TABLE_CATEGORIES + "(" + COL_CATEGORY_ID + ")" +
                    ");";
            db.execSQL(createExpenseTableSQL);
            Log.i("DatabaseGateway", "TABLE_EXPENSES recreated for V2 schema.");
        }
        if (oldVersion < 3) { // NEW: Upgrade logic for versions less than 3 (e.g., from V2 to V3)
            Log.d("DatabaseGateway", "Applying V3 schema changes (from version " + oldVersion + "): Adding Budget History table.");
            String createBudgetHistoryTable = "CREATE TABLE " + TABLE_BUDGET_HISTORY + " (" +
                    COL_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_HISTORY_USER_ID + " INTEGER, " +
                    COL_HISTORY_PERIOD_TYPE + " TEXT NOT NULL, " +
                    COL_HISTORY_BUDGET_AMOUNT_SET + " REAL NOT NULL, " +
                    COL_HISTORY_TOTAL_SPENT + " REAL NOT NULL, " +
                    COL_HISTORY_PERIOD_START_DATE + " TEXT NOT NULL, " +
                    COL_HISTORY_PERIOD_END_DATE + " TEXT NOT NULL, " +
                    COL_HISTORY_SUMMARY_CREATED_AT + " TEXT NOT NULL, " +
                    "FOREIGN KEY (" + COL_HISTORY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")" +
                    ");";
            db.execSQL(createBudgetHistoryTable);
            Log.i("DatabaseGateway", TABLE_BUDGET_HISTORY + " table created for V3 schema.");
        }

    }
    public boolean addBudgetHistory(int userId, String periodType, double budgetAmountSet,
                                    double totalSpentInPeriod, String periodStartDate, String periodEndDate,
                                    String summaryCreatedAt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_HISTORY_USER_ID, userId);
        values.put(COL_HISTORY_PERIOD_TYPE, periodType);
        values.put(COL_HISTORY_BUDGET_AMOUNT_SET, budgetAmountSet);
        values.put(COL_HISTORY_TOTAL_SPENT, totalSpentInPeriod);
        values.put(COL_HISTORY_PERIOD_START_DATE, periodStartDate);
        values.put(COL_HISTORY_PERIOD_END_DATE, periodEndDate);
        values.put(COL_HISTORY_SUMMARY_CREATED_AT, summaryCreatedAt); // Format: YYYY-MM-DD HH:MM:SS

        long result = db.insert(TABLE_BUDGET_HISTORY, null, values);
        if (result == -1) {
            Log.e("DatabaseGateway", "Failed to insert budget history for user " + userId + " period " + periodStartDate);
        } else {
            Log.d("DatabaseGateway", "Budget history added for user " + userId + " period " + periodStartDate + ", History ID: " + result);
        }
        return result != -1;
    }

    public Cursor getBudgetHistory(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_BUDGET_HISTORY, null,
                COL_HISTORY_USER_ID + " = ?", new String[]{String.valueOf(userId)},
                null, null, COL_HISTORY_PERIOD_START_DATE + " DESC"); // Show newest history first
    }
    public boolean budgetHistoryExists(int userId, String periodType, String periodStartDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BUDGET_HISTORY, new String[]{COL_HISTORY_ID},
                COL_HISTORY_USER_ID + " = ? AND " +
                        COL_HISTORY_PERIOD_TYPE + " = ? AND " +
                        COL_HISTORY_PERIOD_START_DATE + " = ?",
                new String[]{String.valueOf(userId), periodType, periodStartDate},
                null, null, null, "1");
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        Log.d("DatabaseGateway", "Budget history check for user " + userId + ", period " + periodType + ", start " + periodStartDate + ": " + exists);
        return exists;
    }
    public Cursor getEndedBudgetsWithoutHistory(int userId, String todayDateStr_YYYY_MM_DD) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT b.* FROM " + TABLE_BUDGETS + " b " +
                "LEFT JOIN " + TABLE_BUDGET_HISTORY + " bh ON b." + COL_BUDGET_USER_ID + " = bh." + COL_HISTORY_USER_ID +
                " AND b." + COL_BUDGET_PERIOD_TYPE + " = bh." + COL_HISTORY_PERIOD_TYPE +
                " AND b." + COL_BUDGET_START_DATE + " = bh." + COL_HISTORY_PERIOD_START_DATE +
                " WHERE b." + COL_BUDGET_USER_ID + " = ? " +
                " AND b." + COL_BUDGET_END_DATE + " < ? " +   // Budgets whose period has ended
                " AND bh." + COL_HISTORY_ID + " IS NULL";   // And no history record exists for it yet
        Log.d("DatabaseGateway", "Querying ended budgets without history for user " + userId + " before date " + todayDateStr_YYYY_MM_DD);
        return db.rawQuery(query, new String[]{String.valueOf(userId), todayDateStr_YYYY_MM_DD});
    }
    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_USER_ID + " FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + " = ? LIMIT 1", new String[]{email});
        boolean exists = cursor.moveToFirst(); // Changed from getCount() > 0 to moveToFirst() for consistency and slight efficiency
        cursor.close();
        return exists;
    }
    public boolean insertUser(String firstname, String lastname, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FIRSTNAME, firstname);
        values.put(COL_LASTNAME, lastname);
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        values.put(COL_PROFILE_IMAGE, (byte[]) null);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }
    public int getUserIdByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_USER_ID + " FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + " = ?", new String[]{email});
        int userId = -1; // Default if not found
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID));
        }
        cursor.close();
        return userId;
    }
    public boolean checkLogin(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_USER_ID + " FROM " + TABLE_USERS + " WHERE " + COL_EMAIL + " = ? AND " + COL_PASSWORD + " = ?", new String[]{email, password});

        boolean result = cursor.moveToFirst();
        cursor.close();
        return result;
    }

    public String getUserFirstName(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT firstname FROM users WHERE user_id = ?", new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            cursor.close();
            return name;
        }
        cursor.close();
        return null;
    }
    public Cursor getUserById(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT " + COL_FIRSTNAME + ", " + COL_LASTNAME + ", " + COL_EMAIL + ", " + COL_PROFILE_IMAGE +
                        " FROM " + TABLE_USERS + " WHERE " + COL_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
    }
    public boolean updateUserProfileImage(int userId, byte[] imageBytes) {
        SQLiteDatabase db = null; // Initialize to null
        int rowsAffected = 0;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_PROFILE_IMAGE, imageBytes);

            // The 'where' clause for the update query
            String selection = COL_USER_ID + " = ?";
            String[] selectionArgs = {String.valueOf(userId)};

            rowsAffected = db.update(TABLE_USERS, values, selection, selectionArgs);
            Log.d("DatabaseGateway", "updateUserProfileImage: userId=" + userId + ", rowsAffected=" + rowsAffected);

        } catch (Exception e) {
            Log.e("DatabaseGateway", "Error updating user profile image for userId: " + userId, e);
            // rowsAffected will remain 0 or could be set to -1 to indicate error
            return false; // Indicate failure due to exception
        } finally {
            if (db != null && db.isOpen()) {
                 db.close();
             }
        }
        return rowsAffected > 0;
    }
    public boolean updateUserProfile(int userId, String firstName, String lastName, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FIRSTNAME, firstName);
        values.put(COL_LASTNAME, lastName);
        values.put(COL_EMAIL, email);
        int rowsAffected = db.update(TABLE_USERS, values, COL_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        Log.d("DatabaseGateway", "updateUserProfile for userId " + userId + ": rowsAffected=" + rowsAffected);
        return rowsAffected > 0;
    }

    // Category related methods
    public long addCategory(String name, String iconIdentifier, boolean isPredefined) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CATEGORY_NAME, name);
        values.put(COL_CATEGORY_ICON_IDENTIFIER, iconIdentifier);
        values.put(COL_CATEGORY_IS_PREDEFINED, isPredefined ? 1 : 0);
        // Use insertWithOnConflict to avoid crashing if a UNIQUE constraint is violated (e.g., category name)
        long id = db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            Log.w("DatabaseGateway", "Category '" + name + "' already exists or failed to insert.");
        }
        return id;
    }

    public Cursor getAllCategories() {
        SQLiteDatabase db = this.getReadableDatabase();
        // Order by name for consistent dropdown display
        return db.query(TABLE_CATEGORIES, null, null, null, null, null, COL_CATEGORY_NAME + " ASC");
    }

    public int getCategoryIdByName(String categoryName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CATEGORIES, new String[]{COL_CATEGORY_ID},
                COL_CATEGORY_NAME + " = ?", new String[]{categoryName},
                null, null, null, "1");
        int categoryId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CATEGORY_ID));
            cursor.close();
        }
        return categoryId;
    }

    // Expense related methods
    public boolean addExpense(int userId, int categoryId, double amount, String note, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EXPENSE_USER_ID, userId);
        values.put(COL_EXPENSE_CATEGORY_ID_FK, categoryId);
        values.put(COL_EXPENSE_AMOUNT, amount);
        values.put(COL_EXPENSE_NOTE, note);
        values.put(COL_EXPENSE_TIMESTAMP, timestamp);
        long result = db.insert(TABLE_EXPENSES, null, values);
        return result != -1;
    }

    public boolean updateExpense(int expenseId, int categoryId, double amount, String note, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EXPENSE_CATEGORY_ID_FK, categoryId);
        values.put(COL_EXPENSE_AMOUNT, amount);
        values.put(COL_EXPENSE_NOTE, note);
        values.put(COL_EXPENSE_TIMESTAMP, timestamp);
        int rowsAffected = db.update(TABLE_EXPENSES, values, COL_EXPENSE_ID + " = ?", new String[]{String.valueOf(expenseId)});
        return rowsAffected > 0;
    }

    public boolean deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_EXPENSES, COL_EXPENSE_ID + " = ?", new String[]{String.valueOf(expenseId)});
        return result > 0;
    }

    public Cursor getExpensesForUserWithCategory(int userId, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT e." + COL_EXPENSE_ID + ", e." + COL_EXPENSE_AMOUNT + ", e." + COL_EXPENSE_NOTE + ", e." + COL_EXPENSE_TIMESTAMP + ", " +
                "c." + COL_CATEGORY_NAME + ", c." + COL_CATEGORY_ICON_IDENTIFIER +
                " FROM " + TABLE_EXPENSES + " e" +
                " INNER JOIN " + TABLE_CATEGORIES + " c ON e." + COL_EXPENSE_CATEGORY_ID_FK + " = c." + COL_CATEGORY_ID +
                " WHERE e." + COL_EXPENSE_USER_ID + " = ? AND " +
                "e." + COL_EXPENSE_TIMESTAMP + " >= ? AND e." + COL_EXPENSE_TIMESTAMP + " <= ? " +
                "ORDER BY e." + COL_EXPENSE_TIMESTAMP + " DESC";

        String startDateTime = startDate + " 00:00:00";
        String endDateTime = endDate + " 23:59:59";

        return db.rawQuery(query, new String[]{String.valueOf(userId), startDateTime, endDateTime});
    }

    public double getTotalExpensesForPeriod(int userId, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        String startDateTime = startDate + " 00:00:00";
        String endDateTime = endDate + " 23:59:59";
        String query = "SELECT SUM(" + COL_EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                " WHERE " + COL_EXPENSE_USER_ID + " = ? AND " +
                COL_EXPENSE_TIMESTAMP + " >= ? AND " + COL_EXPENSE_TIMESTAMP + " <= ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), startDateTime, endDateTime});
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }
    // Budget related methods
    public boolean setOrUpdateBudget(int userId, double amount, String periodType, String startDate, String endDate, String createdAtTimestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BUDGET_USER_ID, userId);
        values.put(COL_BUDGET_AMOUNT, amount);
        values.put(COL_BUDGET_PERIOD_TYPE, periodType);
        values.put(COL_BUDGET_START_DATE, startDate); // YYYY-MM-DD
        values.put(COL_BUDGET_END_DATE, endDate);     // YYYY-MM-DD
        values.put(COL_BUDGET_CREATED_AT, createdAtTimestamp); // YYYY-MM-DD HH:MM:SS

        int rowsAffected = db.update(TABLE_BUDGETS, values,
                COL_BUDGET_USER_ID + " = ? AND " + COL_BUDGET_PERIOD_TYPE + " = ? AND " + COL_BUDGET_START_DATE + " = ?",
                new String[]{String.valueOf(userId), periodType, startDate});

        if (rowsAffected > 0) {
            return true; // Updated existing budget for that specific start date and period type
        } else {
            // No existing budget found for this exact start date and period type, insert new
            long result = db.insert(TABLE_BUDGETS, null, values);
            return result != -1; // Inserted new budget
        }
    }

    public Cursor getCurrentBudget(int userId, String periodType, String currentDate_YYYY_MM_DD) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_BUDGETS +
                " WHERE " + COL_BUDGET_USER_ID + " = ? AND " +
                COL_BUDGET_PERIOD_TYPE + " = ? AND " +
                "'" + currentDate_YYYY_MM_DD + "' BETWEEN " + COL_BUDGET_START_DATE + " AND " + COL_BUDGET_END_DATE +
                " ORDER BY " + COL_BUDGET_CREATED_AT + " DESC LIMIT 1";
        return db.rawQuery(query, new String[]{String.valueOf(userId), periodType});
    }
    public boolean deleteBudget(int userId, String periodType, String budgetStartDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_BUDGETS,
                COL_BUDGET_USER_ID + " = ? AND " +
                        COL_BUDGET_PERIOD_TYPE + " = ? AND " +
                        COL_BUDGET_START_DATE + " = ?",
                new String[]{String.valueOf(userId), periodType, budgetStartDate});
        Log.d("DatabaseGateway", "Deleting budget for user " + userId + ", period " + periodType + ", start " + budgetStartDate + ". Rows affected: " + result);
        return result > 0;
    }
    public boolean verifyPassword(int userId, String enteredPassword) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String storedPassword = null;
        try {
            cursor = db.query(TABLE_USERS, new String[]{COL_PASSWORD},
                    COL_USER_ID + " = ?", new String[]{String.valueOf(userId)},
                    null, null, null, "1");
            if (cursor != null && cursor.moveToFirst()) {
                storedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD));
            }
        } catch (Exception e) {
            Log.e("DatabaseGateway", "Error verifying password", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        // THIS IS PLAIN TEXT COMPARISON - NOT SECURE FOR PRODUCTION
        // In a real app, 'enteredPassword' would be hashed and compared to 'storedPassword' (which is a hash)
        return storedPassword != null && storedPassword.equals(enteredPassword);
    }

    public boolean updateUserPassword(int userId, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        // In a real app, 'newPassword' should be securely hashed before storing
        values.put(COL_PASSWORD, newPassword);
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(TABLE_USERS, values, COL_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        } catch (Exception e) {
            Log.e("DatabaseGateway", "Error updating password for user " + userId, e);
        }
        Log.d("DatabaseGateway", "updateUserPassword for userId " + userId + ": rowsAffected=" + rowsAffected);
        return rowsAffected > 0;
    }
    public Cursor getCategorySpendingForPeriod(int userId, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        String startDateTime = startDate + " 00:00:00";
        String endDateTime = endDate + " 23:59:59";

        String query = "SELECT c." + COL_CATEGORY_NAME + ", SUM(e." + COL_EXPENSE_AMOUNT + ") as total_spent " +
                "FROM " + TABLE_EXPENSES + " e " +
                "JOIN " + TABLE_CATEGORIES + " c ON e." + COL_EXPENSE_CATEGORY_ID_FK + " = c." + COL_CATEGORY_ID +
                " WHERE e." + COL_EXPENSE_USER_ID + " = ? AND " +
                "e." + COL_EXPENSE_TIMESTAMP + " >= ? AND e." + COL_EXPENSE_TIMESTAMP + " <= ? " +
                "GROUP BY c." + COL_CATEGORY_NAME + " " +
                "HAVING total_spent > 0 " + // Only categories with spending
                "ORDER BY total_spent DESC";

        Log.d("DatabaseGateway", "Querying category spending for user " + userId + " from " + startDateTime + " to " + endDateTime);
        return db.rawQuery(query, new String[]{String.valueOf(userId), startDateTime, endDateTime});
    }

    // Method to get total spending for each of the last N months
    // Returns (MonthYearString e.g., "2023-05", TotalSpent)
    public Cursor getMonthlySpendingTrend(int userId, int numberOfMonths) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Simpler query: Get all expenses from start of (CurrentMonth - N + 1) to end of CurrentMonth
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -(numberOfMonths - 1)); // Go back N-1 months to get the start of the N-month window
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()); // Today

        String startDateTime = startDate + " 00:00:00";
        String endDateTime = endDate + " 23:59:59";


        String query = "SELECT strftime('%Y-%m', " + COL_EXPENSE_TIMESTAMP + ") as month_year, " +
                "SUM(" + COL_EXPENSE_AMOUNT + ") as total_spent " +
                "FROM " + TABLE_EXPENSES + " " +
                "WHERE " + COL_EXPENSE_USER_ID + " = ? AND " +
                COL_EXPENSE_TIMESTAMP + " >= ? AND " + COL_EXPENSE_TIMESTAMP + " <= ? " +
                "GROUP BY month_year " +
                "ORDER BY month_year ASC"; // Oldest month first for trend line

        Log.d("DatabaseGateway", "Querying monthly spending trend for user " + userId + " for last " + numberOfMonths + " months (approx). Start: " + startDate);
        return db.rawQuery(query, new String[]{String.valueOf(userId), startDateTime, endDateTime});
    }

    // Method to get a specific budget for a period (e.g., for "Last Month")
    // This is similar to getCurrentBudget but can look for a budget whose start_date matches exactly.
    public Cursor getBudgetForSpecificPeriod(int userId, String periodType, String periodStartDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_BUDGETS +
                " WHERE " + COL_BUDGET_USER_ID + " = ? AND " +
                COL_BUDGET_PERIOD_TYPE + " = ? AND " +
                COL_BUDGET_START_DATE + " = ? " + // Match exact start date
                "ORDER BY " + COL_BUDGET_ID + " DESC LIMIT 1";
        Log.d("DatabaseGateway", "Querying specific budget for user " + userId + ", type " + periodType + ", start " + periodStartDate);
        return db.rawQuery(query, new String[]{String.valueOf(userId), periodType, periodStartDate});
    }

}
