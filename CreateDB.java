package com.softtronic.socisnap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class CreateDB extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "VulgarDB";
    static final String TABLE_NAME = "VulgarTB";
    private static final String KEY_ID = "ID";
    static final String KEY_WORD = "Word";

    public CreateDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_WORD + " TEXT" + ")";
        db.execSQL(CREATE_TABLE);

        // Insert 1000 non-integer words into the table
        String[] nonIntegerWords = {"shit","fuck","Madoadoa", "Operation Linda Kura", "Mende",
                "Kama Mbaya Mbaya", "Mhajira and Fumigation","hatupangwingwi",
                "watu wa kurusha mawe", "kama noma noma", "kwekwe", "chinja kafir", "wakuja", "operation linda kura",
                "Kihii", "Uthamaki ni witu", "mwiji",
                "Kimurkeldet", "Ngetiik"
        };
        for (String nonIntegerWord : nonIntegerWords) {
            db.execSQL("INSERT INTO " + TABLE_NAME + " (" + KEY_WORD + ") VALUES ('" + nonIntegerWord + "')");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // Delete the table if it exists
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        // Delete the database if it exists
        SQLiteDatabase.deleteDatabase(new File(sqLiteDatabase.getPath()));
        // Create the table
        onCreate(sqLiteDatabase);
    }

}
