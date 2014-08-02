package com.lukekorth.pebblelocker;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.app.Application;
import com.lukekorth.pebblelocker.logging.Logger;

import java.util.Date;

public class PebbleLockerApplication extends Application implements Thread.UncaughtExceptionHandler {

    private static SQLiteDatabase mLogDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(this);
        ActiveAndroid.initialize(this);
        mLogDatabase = new Logger(this).getWritableDatabase();
        init();
    }

    private void init() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        if (BuildConfig.VERSION_CODE > prefs.getInt("version", 0)) {
            String now = new Date().toString();
            if (prefs.getInt("version", 0) == 0) {
                editor.putString("install_date", now);
            }

            editor.putString("upgrade_date", now);
            editor.putInt("version", BuildConfig.VERSION_CODE);
        }

        editor.apply();
    }

    @Override
    public void onLowMemory() {
        new Logger(this).log("[APPLICATION]", "Memory is low!");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ActiveAndroid.dispose();
        mLogDatabase.close();
    }

    public static SQLiteDatabase getLogDatabase() {
        return mLogDatabase;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Logger logger = new Logger(this, "[EXCEPTION]");

        logger.log("Crashed!");
        logger.log("thread.toString(): " + thread.toString());

        logger.log("Exception string: " + ex.toString());
        logger.log("Exception message: " + ex.getMessage());
        logger.log("Exception stacktrace:");
        for (StackTraceElement trace : ex.getStackTrace()) {
            logger.log(trace.toString());
        }

        System.exit(1);
    }
}
