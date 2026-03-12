package com.example.gomarket.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RecipeDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "gomarket_recipes.db";
    private static final int DATABASE_VERSION = 1;

    // Tên bảng và cột
    public static final String TABLE_RECIPES = "saved_recipes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_RECIPE_NAME = "recipe_name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_INGREDIENTS = "ingredients";
    public static final String COLUMN_STEPS = "steps";
    public static final String COLUMN_WEATHER_CONTEXT = "weather_context";
    public static final String COLUMN_IMAGE_URL = "image_url";
    public static final String COLUMN_IS_FAVORITE = "is_favorite";
    public static final String COLUMN_CREATED_AT = "created_at";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_RECIPES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_RECIPE_NAME + " TEXT NOT NULL, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_INGREDIENTS + " TEXT, " +
                    COLUMN_STEPS + " TEXT, " +
                    COLUMN_WEATHER_CONTEXT + " TEXT, " +
                    COLUMN_IMAGE_URL + " TEXT, " +
                    COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0, " +
                    COLUMN_CREATED_AT + " TEXT DEFAULT (datetime('now','localtime'))" +
                    ")";

    private static final String SQL_DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_RECIPES;

    public RecipeDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_TABLE);
        onCreate(db);
    }
}
