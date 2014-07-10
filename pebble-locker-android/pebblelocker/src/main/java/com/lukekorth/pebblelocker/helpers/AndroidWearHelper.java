package com.lukekorth.pebblelocker.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.lukekorth.pebblelocker.BuildConfig;
import com.lukekorth.pebblelocker.logging.Logger;

import java.util.HashMap;
import java.util.Map;

public class AndroidWearHelper {

    private Context mContext;
    private Logger mLogger;

    public AndroidWearHelper(Context context) {
        mContext = context;
        mLogger = new Logger(context, "[ANDROID-WEAR-HELPER]");
    }

    public AndroidWearHelper(Context context, Logger logger) {
        mContext = context;
        mLogger = logger;
    }

    public Map<String, String> getKnownDevices() {
        SQLiteDatabase db = new AndroidWearDatabase(mContext).getReadableDatabase();
        Cursor cursor = db.query(AndroidWearDatabase.ANDROID_WEAR_DEVICES, new String[] { "name" },
                null, null, null, null, null);

        Map<String, String> knownDevices = new HashMap<String, String>();
        while(cursor.moveToNext()) {
            knownDevices.put(cursor.getString(cursor.getColumnIndex("device_id")),
                    cursor.getString(cursor.getColumnIndex("name")));
        }

        cursor.close();
        db.close();

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(mContext).build();
        NodeApi.GetConnectedNodesResult connectedNodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : connectedNodes.getNodes()) {
            knownDevices.put(node.getId(), node.getDisplayName());
        }

        return knownDevices;
    }

    public void addDevice(Node node) {
        ContentValues cv = new ContentValues();
        cv.put("name", node.getDisplayName());
        cv.put("device_id", node.getId());

        SQLiteDatabase db = new AndroidWearDatabase(mContext).getWritableDatabase();
        db.insertWithOnConflict(AndroidWearDatabase.ANDROID_WEAR_DEVICES, null, cv,
                SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    private class AndroidWearDatabase extends SQLiteOpenHelper {

        private static final String ANDROID_WEAR_DEVICES = "androidWearDevices";

        public AndroidWearDatabase(Context context) {
            super(context, "pebble-locker", null, BuildConfig.VERSION_CODE);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + ANDROID_WEAR_DEVICES + " (name TEXT PRIMARY KEY, device_id TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onCreate(db);
        }
    }
}
