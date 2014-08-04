package com.lukekorth.pebblelocker.logging;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.lukekorth.pebblelocker.BuildConfig;
import com.lukekorth.pebblelocker.PebbleLockerApplication;

import java.sql.Timestamp;
import java.util.Date;

public class Logger extends SQLiteOpenHelper {

    private String mTag = "[NO_TAG]";
	
    public Logger(Context context) {
    	super(context, "pebble-locker-logger", null, BuildConfig.VERSION_CODE);
    }

    public Logger(Context context, String tag) {
        this(context);
        mTag = tag;
    }

    public String getTag() {
        return mTag;
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE log (pk INTEGER PRIMARY KEY AUTOINCREMENT, timestamp INTEGER, message TEXT)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS log");
        onCreate(db);
	}

    public void log(String message) {
        log(mTag, message);
    }
	
	public void log(String tag, String message) {
		long timestamp = System.currentTimeMillis();
		
        SQLiteDatabase db = PebbleLockerApplication.getLogDatabase();

        ContentValues cv = new ContentValues();
        cv.put("timestamp", timestamp);
        cv.put("message", new Timestamp(new Date().getTime()) + " : " + tag + " " + message);
        db.insert("log", null, cv);
        
        db.delete("log","timestamp < ?", new String[] { Long.toString(System.currentTimeMillis() - 604800000) });

        if(BuildConfig.DEBUG)
            Log.d("pebble-locker", tag + " " + message);
    }
	
	public String getLog() {
        SQLiteDatabase db = PebbleLockerApplication.getLogDatabase();

        Cursor cursor = db.query("log", new String[] {"timestamp", "message" }, "", null, null, null, "timestamp ASC");
        
        StringBuffer response = new StringBuffer();
        String line;
        String currentTag;
        String lastTag = null;
        while(cursor.moveToNext()) {
            line = cursor.getString(cursor.getColumnIndex("message"));
            currentTag = line.substring(line.indexOf("["), line.indexOf("]") + 1);
            if (!currentTag.equals(lastTag)) {
                lastTag = currentTag;
                response.append("\n");
            }
            response.append(line + "\n");
        }
        
        cursor.close();

        return response.toString();
    }
}
