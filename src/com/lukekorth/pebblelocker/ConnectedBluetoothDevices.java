package com.lukekorth.pebblelocker;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ConnectedBluetoothDevices extends SQLiteOpenHelper {

	public ConnectedBluetoothDevices(Context context) {
		super(context, "pebble-locker-bluetooth-devices", null, PebbleLocker.VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE devices (address TEXT PRIMARY KEY, connected INTEGER)");
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
		db.insertWithOnConflict("devices", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
		db.close();
	}
	
	public ArrayList<String> connectedDevices() {
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = 
				db.query("devices", new String[] { "address" }, "connected == ?", new String[] { "1" }, null, null, null);
		
		ArrayList<String> connectedDevices = new ArrayList<String>();
		while(cursor.moveToNext()) {
			connectedDevices.add(cursor.getString(cursor.getColumnIndex("address")));
		}
		
		cursor.close();
		db.close();
		
		return connectedDevices;
	}
}