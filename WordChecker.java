package com.softtronic.socisnap;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordChecker {
    public static boolean hasVulgarWords(Context context, String statement) {

        CreateDB createDB = new CreateDB(context);
        SQLiteDatabase db = createDB.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + CreateDB.TABLE_NAME, null);

        boolean hasVulgarWords = false;

        while (cursor.moveToNext()) {
            String word = cursor.getString(cursor.getColumnIndexOrThrow(CreateDB.KEY_WORD));
            String[] wordArray = word.split(" ");
            for (String singleWord : wordArray) {
                Pattern pattern = Pattern.compile("\\b" + singleWord + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(statement);
                if (matcher.find()) {
                    hasVulgarWords = true;
                    break;
                }
            }
            if (hasVulgarWords) {
                break;
            }
        }

        cursor.close();
        db.close();

        return hasVulgarWords;
    }
}
