package com.example.gomarket.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gomarket.database.RecipeDbHelper;

public class RecipeContentProvider extends ContentProvider {

    private static final String TAG = "RecipeContentProvider";

    public static final String AUTHORITY = "com.gomarket.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/recipes");

    private static final int RECIPES = 1;
    private static final int RECIPE_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "recipes", RECIPES);
        uriMatcher.addURI(AUTHORITY, "recipes/#", RECIPE_ID);
    }

    private RecipeDbHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new RecipeDbHelper(getContext());
        Log.d(TAG, "RecipeContentProvider created");
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        switch (uriMatcher.match(uri)) {
            case RECIPES:
                cursor = db.query(
                        RecipeDbHelper.TABLE_RECIPES,
                        projection, selection, selectionArgs,
                        null, null,
                        sortOrder != null ? sortOrder : RecipeDbHelper.COLUMN_CREATED_AT + " DESC"
                );
                break;

            case RECIPE_ID:
                String id = uri.getLastPathSegment();
                cursor = db.query(
                        RecipeDbHelper.TABLE_RECIPES,
                        projection,
                        RecipeDbHelper.COLUMN_ID + " = ?",
                        new String[]{id},
                        null, null, null
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (uriMatcher.match(uri) != RECIPES) {
            throw new IllegalArgumentException("Invalid URI for insert: " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(RecipeDbHelper.TABLE_RECIPES, null, values);

        if (id > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, id);
            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(newUri, null);
            }
            Log.d(TAG, "Inserted recipe with id: " + id);
            return newUri;
        }

        return null;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsUpdated;

        switch (uriMatcher.match(uri)) {
            case RECIPES:
                rowsUpdated = db.update(RecipeDbHelper.TABLE_RECIPES, values, selection, selectionArgs);
                break;

            case RECIPE_ID:
                String id = uri.getLastPathSegment();
                rowsUpdated = db.update(
                        RecipeDbHelper.TABLE_RECIPES, values,
                        RecipeDbHelper.COLUMN_ID + " = ?",
                        new String[]{id}
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (rowsUpdated > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted;

        switch (uriMatcher.match(uri)) {
            case RECIPES:
                rowsDeleted = db.delete(RecipeDbHelper.TABLE_RECIPES, selection, selectionArgs);
                break;

            case RECIPE_ID:
                String id = uri.getLastPathSegment();
                rowsDeleted = db.delete(
                        RecipeDbHelper.TABLE_RECIPES,
                        RecipeDbHelper.COLUMN_ID + " = ?",
                        new String[]{id}
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (rowsDeleted > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case RECIPES:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + ".recipes";
            case RECIPE_ID:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + ".recipes";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }
}
