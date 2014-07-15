package com.lukekorth.pebblelocker.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.lukekorth.pebblelocker.BuildConfig;
import com.lukekorth.pebblelocker.logging.Logger;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class AndroidWearHelper implements ResultCallback<NodeApi.GetConnectedNodesResult> {

    private Context mContext;
    private Logger mLogger;
    private WeakReference<CallbackListener> mListener;

    public AndroidWearHelper(Context context) {
        mContext = context;
        mLogger = new Logger(context, "[ANDROID-WEAR-HELPER]");
    }

    public AndroidWearHelper(Context context, Logger logger) {
        mContext = context;
        mLogger = logger;
    }

    private GoogleApiClient getGoogleClient() {
        return new GoogleApiClient.Builder(mContext).addApi(Wearable.API).build();
    }

    public String[] getConnectedDevices() {
        NodeApi.GetConnectedNodesResult connectedNodes = Wearable.NodeApi
                .getConnectedNodes(getGoogleClient()).await();

        String[] connectedDevices = new String[connectedNodes.getNodes().size()];
        for (int i = 0; i < connectedNodes.getNodes().size(); i++) {
            connectedDevices[i] = connectedNodes.getNodes().get(i).getId();
        }

        return connectedDevices;
    }

    public void getKnownDevices(CallbackListener listener) {
        mListener = new WeakReference<CallbackListener>(listener);

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

        for (Node node : connectedNodesResult.getNodes()) {
            knownDevices.put(node.getId(), node.getDisplayName());
        }

        if (mListener != null && mListener.get() != null) {
            mListener.get().onKnownDevicesLoaded(knownDevices);
        }
    }

    public interface CallbackListener {
        public void onKnownDevicesLoaded(Map<String, String> devices);
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
