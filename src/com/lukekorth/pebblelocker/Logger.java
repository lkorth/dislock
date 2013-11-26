package com.lukekorth.pebblelocker;

import java.sql.Timestamp;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Logger extends SQLiteOpenHelper {
	
	private static final int VERSION = 11;
	
    public Logger(Context context) {
    	super(context, "pebble-locker", null, VERSION);  
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
		long timestamp = System.currentTimeMillis();
		
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("timestamp", timestamp);
        cv.put("message", new Timestamp(new Date().getTime()) + " : " + message);
        db.insert("log", null, cv);
        
        Log.d("pebble-locker", message);
        
        db.delete("log","timestamp < ?", new String[] { Long.toString(System.currentTimeMillis() - 604800000) });
        db.close();
    }
	
	public String getLog() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query("log", new String[] {"timestamp", "message" }, "", null, null, null, "timestamp ASC");
        
        StringBuffer response = new StringBuffer();
        
        while(cursor.moveToNext()) {
        	response.append(cursor.getString(cursor.getColumnIndex("message")) + "\n");
        }
        
        cursor.close();
        db.close();

        return response.toString();
    }
}
