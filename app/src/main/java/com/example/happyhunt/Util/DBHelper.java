package com.example.happyhunt.Util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.happyhunt.Model.Favorite;
import com.example.happyhunt.Model.Profile;

public class DBHelper extends SQLiteOpenHelper {
    static String DBNAME = "HappyHunt.dp";
    static int VERSION = 1;
    static String FAVORITE_TABLE_NAME = "Favorite";
    static String PROFILE_TABLE_NAME = "Profile";
    static String COL1 = "id";
    static String COL2 = "placeName";
    static String COL3 = "placeAddres";
    static String COL4 = "type";
    static String PCOL1 = "id";
    static String PCOL2 = "username";
    static String PCOL3 = "email";
    static String PCOL4 = "password";
    static final String CREATE_FAVORITE_TABLE = "create table " + FAVORITE_TABLE_NAME + " (" + COL1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL2 + " TEXT NOT NULL, "
            + COL3 + " TEXT, "  + COL4 + " TEXT); ";
    static final String DROP_FAVORITE_TABLE = "DROP TABLE IF EXISTS " + FAVORITE_TABLE_NAME;
    static final String CREATE_PROFILE_TABLE = "create table " + PROFILE_TABLE_NAME + " (" + PCOL1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " + PCOL2 + " TEXT NOT NULL, "
            + PCOL3 + " TEXT NOT NULL, "  + PCOL4 + " TEXT NOT NULL); ";
    static final String DROP_PROFILE_TABLE = "DROP TABLE IF EXISTS " + PROFILE_TABLE_NAME;


    public DBHelper(Context context) {
        super(context, DBNAME, null, VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FAVORITE_TABLE);
        db.execSQL(CREATE_PROFILE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_FAVORITE_TABLE);
        db.execSQL(DROP_PROFILE_TABLE);
        onCreate(db);
    }

    public void InsertFavorite(Favorite objFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL2, objFavorite.getPlaceName());
        cv.put(COL3, objFavorite.getPlaceAddress());
        cv.put(COL4, objFavorite.getType());
        db.insert(FAVORITE_TABLE_NAME, null, cv);
    }

    public void DeleteFavorite(Favorite objFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = "placeName=?";
        String[] whereArgs = new String[]{objFavorite.getPlaceName()};
        db.delete(FAVORITE_TABLE_NAME, whereClause, whereArgs);
    }

    public Cursor readFavorites() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursorObj;
        cursorObj = db.rawQuery("select * from " + FAVORITE_TABLE_NAME, null);
        if (cursorObj != null) {
            cursorObj.moveToFirst();
        }
        return cursorObj;
    }

    public void InsertProfileAccount(Profile objProfile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(PCOL2, objProfile.getUsername());
        cv.put(PCOL3, objProfile.getEmail());
        cv.put(PCOL4, objProfile.getPassword());
        db.insert(PROFILE_TABLE_NAME, null, cv);
    }

    public Cursor readProfile() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursorObj;
        cursorObj = db.rawQuery("select * from " + PROFILE_TABLE_NAME, null);
        if (cursorObj != null) {
            cursorObj.moveToFirst();
        }
        return cursorObj;
    }

    public int deleteProfile(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(PROFILE_TABLE_NAME, "id="+id, null);
    }
}
