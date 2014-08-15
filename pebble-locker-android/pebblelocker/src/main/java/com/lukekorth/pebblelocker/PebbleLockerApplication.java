package com.lukekorth.pebblelocker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.helpers.ThreadBus;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.services.AndroidWearDetectionService;
import com.squareup.otto.Bus;

import java.util.Date;
import java.util.UUID;

public class PebbleLockerApplication extends com.activeandroid.app.Application implements Thread.UncaughtExceptionHandler {

    private static SQLiteDatabase sLogDatabase;
    private static ThreadBus sBus;

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(this);
        sLogDatabase = new Logger(this).getWritableDatabase();
        sBus = new ThreadBus();
        migrate();

        startService(new Intent(this, AndroidWearDetectionService.class));
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

    public static String getUniqueTag() {
        return "[" + UUID.randomUUID().toString().split("-")[1] + "]";
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
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

        throw new RuntimeException(ex);
    }
}
