package com.lukekorth.pebblelocker;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.app.Application;
import com.lukekorth.pebblelocker.logging.Logger;
import com.squareup.otto.Bus;

import java.util.Date;

public class PebbleLockerApplication extends Application implements Thread.UncaughtExceptionHandler {

    private static SQLiteDatabase sLogDatabase;
    private static Bus sBus;

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(this);
        ActiveAndroid.initialize(this);
        sLogDatabase = new Logger(this).getWritableDatabase();
        sBus = new Bus();
        migrate();
    }

    private void migrate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        if (BuildConfig.VERSION_CODE > prefs.getInt("version", 0)) {
            String now = new Date().toString();
            if (prefs.getInt("version", 0) == 0) {
                editor.putString("install_date", now);
            }

            editor.putString("upgrade_date", now);
            editor.putInt("version", BuildConfig.VERSION_CODE);
            editor.apply();
        }
    }

    public static SQLiteDatabase getLogDatabase() {
        return sLogDatabase;
    }

    public static Bus getBus() {
        return sBus;
    }

    @Override
    public void onLowMemory() {
        new Logger(this).log("[APPLICATION]", "Memory is low!");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ActiveAndroid.dispose();
        sLogDatabase.close();
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
