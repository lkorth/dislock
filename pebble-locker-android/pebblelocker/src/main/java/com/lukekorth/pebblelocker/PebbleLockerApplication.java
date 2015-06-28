package com.lukekorth.pebblelocker;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lukekorth.mailable_log.MailableLog;
import com.lukekorth.pebblelocker.helpers.ThreadBus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.UUID;

public class PebbleLockerApplication extends com.activeandroid.app.Application implements Thread.UncaughtExceptionHandler {

    private static final String VERSION = "version";

    public static boolean sIsRunningInTestHarness = false;

    private static ThreadBus sBus;

    private Thread.UncaughtExceptionHandler mExceptionHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        migrate();
        MailableLog.init(this, BuildConfig.DEBUG);
        sBus = new ThreadBus();
    }

    private void migrate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        int previousVersion = prefs.getInt(VERSION, 0);
        if (previousVersion < BuildConfig.VERSION_CODE) {
            if (previousVersion <= 38 && !sIsRunningInTestHarness) {
                prefs.edit().clear().apply();
                deleteDatabase("pebble_locker.db");
            }

            String now = new Date().toString();
            if (prefs.getInt(VERSION, 0) == 0) {
                editor.putString("install_date", now);
            }

            editor.putString("upgrade_date", now);
            editor.putInt(VERSION, BuildConfig.VERSION_CODE);
            editor.apply();

            MailableLog.clearLog(this);
        }
    }

    public static ThreadBus getBus() {
        return sBus;
    }

    public static String getUniqueTag() {
        return UUID.randomUUID().toString().split("-")[1];
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Logger logger = LoggerFactory.getLogger("Exception");

        logger.error("thread.toString(): " + thread.toString());
        logger.error("Exception: " + ex.toString());
        logger.error("Exception stacktrace:");
        for (StackTraceElement trace : ex.getStackTrace()) {
            logger.error(trace.toString());
        }

        logger.error("");

        logger.error("cause.toString(): " + ex.getCause().toString());
        logger.error("Cause: " + ex.getCause().toString());
        logger.error("Cause stacktrace:");
        for (StackTraceElement trace : ex.getCause().getStackTrace()) {
            logger.error(trace.toString());
        }

        mExceptionHandler.uncaughtException(thread, ex);
    }
}
