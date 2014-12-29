package com.lukekorth.pebblelocker;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.lukekorth.mailable_log.MailableLog;
import com.lukekorth.pebblelocker.helpers.ThreadBus;
import com.squareup.otto.Bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.UUID;

public class PebbleLockerApplication extends com.activeandroid.app.Application implements Thread.UncaughtExceptionHandler {

    private static final String VERSION = "version";

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
            // update old version prefs
            if (previousVersion <= 35) {
                String password = prefs.getString("key_password", "");
                if (TextUtils.isEmpty(password)) {
                    editor.putString(ScreenLockType.SCREEN_LOCK_TYPE_KEY, ScreenLockType.SLIDE.getType());
                } else if (password.matches("[0-9]+")) {
                    editor.putString(ScreenLockType.SCREEN_LOCK_TYPE_KEY, ScreenLockType.PIN.getType());
                } else {
                    editor.putString(ScreenLockType.SCREEN_LOCK_TYPE_KEY, ScreenLockType.PASSWORD.getType());
                }
                editor.remove("key_enable_locker");
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

    public static Bus getBus() {
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
