package com.lukekorth.pebblelocker;

import android.database.sqlite.SQLiteDatabase;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.app.Application;
import com.lukekorth.pebblelocker.logging.Logger;

public class PebbleLockerApplication extends Application implements Thread.UncaughtExceptionHandler {

    private static SQLiteDatabase mLogDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(this);
        ActiveAndroid.initialize(this);
        mLogDatabase = new Logger(this).getWritableDatabase();
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
