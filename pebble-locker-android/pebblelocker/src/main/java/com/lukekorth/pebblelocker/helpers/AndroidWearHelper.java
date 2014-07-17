package com.lukekorth.pebblelocker.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.lukekorth.pebblelocker.BuildConfig;
import com.lukekorth.pebblelocker.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidWearHelper implements ResultCallback<NodeApi.GetConnectedNodesResult> {

    private Context mContext;
    private Logger mLogger;
    private Listener mListener;
    private boolean mAllDevices;

    public AndroidWearHelper(Context context) {
        mContext = context;
        mLogger = new Logger(context, "[ANDROID-WEAR-HELPER]");
    }

    public AndroidWearHelper(Context context, Logger logger) {
        mContext = context;
        mLogger = logger;
    }

    public boolean isTrustedWearConnected() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        List<Node> wears = getConnectedDevices();

        for(Node node : wears) {
            mLogger.log("Wear " + node.getDisplayName() + " with id: " + node.getId() + " is connected");
            if (prefs.getBoolean(node.getId(), false)) {
                return true;
            }
        }

        return false;
    }

    private GoogleApiClient getGoogleClient() {
        GoogleApiClient client = new GoogleApiClient.Builder(mContext).addApi(Wearable.API).build();
        client.connect();

        return client;
    }

    public List<Node> getConnectedDevices() {
        NodeApi.GetConnectedNodesResult connectedNodes = Wearable.NodeApi
                .getConnectedNodes(getGoogleClient()).await();

        return connectedNodes.getNodes();
    }

    public void getConnectedDevices(Listener listener) {
        mListener = listener;
        mAllDevices = false;

        Wearable.NodeApi.getConnectedNodes(getGoogleClient())
                .setResultCallback(this);
    }

    public void getKnownDevices(Listener listener) {
        mListener = listener;
        mAllDevices = true;
        Wearable.NodeApi.getConnectedNodes(getGoogleClient())
                .setResultCallback(this);
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

    @Override
    public void onResult(NodeApi.GetConnectedNodesResult connectedNodesResult) {
        Map<String, String> devices = new HashMap<String, String>();

        if (mAllDevices) {
            getEncounteredDevices(devices);
        }

        for (Node node : connectedNodesResult.getNodes()) {
            devices.put(node.getId(), node.getDisplayName());
        }

        if (mListener != null) {
            mListener.onKnownDevicesLoaded(devices);
        }
    }

    private void getEncounteredDevices(Map<String, String> devices) {
        SQLiteDatabase db = new AndroidWearDatabase(mContext).getReadableDatabase();
        Cursor cursor = db.query(AndroidWearDatabase.ANDROID_WEAR_DEVICES, new String[] { "name", "device_id" },
                null, null, null, null, null);

        while(cursor.moveToNext()) {
            devices.put(cursor.getString(cursor.getColumnIndex("device_id")),
                    cursor.getString(cursor.getColumnIndex("name")));
        }

        cursor.close();
        db.close();
    }

    public interface Listener {
        public void onKnownDevicesLoaded(Map<String, String> devices);
    }

    private class AndroidWearDatabase extends SQLiteOpenHelper {

        private static final String ANDROID_WEAR_DEVICES = "androidWearDevices";

        public AndroidWearDatabase(Context context) {
            super(context, "pebble-locker-android-wear", null, BuildConfig.VERSION_CODE);
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
