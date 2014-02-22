package com.lukekorth.pebblelocker;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
	private static final String BLUETOOTH_DEVICES = "connectedBluetoothDevices";

	public DatabaseHelper(Context context) {
		super(context, "pebble-locker", null, DB_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + BLUETOOTH_DEVICES + " (address TEXT PRIMARY KEY, connected INTEGER)");
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// noop
	}
	
	public void setStatus(String address, boolean connected) {
		ContentValues cv = new ContentValues();
		cv.put("address", address);
		cv.put("connected", connected ? 1 : 0);
		
		SQLiteDatabase db = getWritableDatabase();
		db.insertWithOnConflict(BLUETOOTH_DEVICES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
		db.close();
	}
	
	public ArrayList<String> connectedDevices() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = 
				db.query(BLUETOOTH_DEVICES, new String[] { "address" }, "connected == ?", new String[] { "1" }, null, null, null);
		
		ArrayList<String> connectedDevices = new ArrayList<String>();
		while(cursor.moveToNext()) {
			connectedDevices.add(cursor.getString(cursor.getColumnIndex("address")));
		}
		
		cursor.close();
		db.close();
		
		return connectedDevices;
	}
}