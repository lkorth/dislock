package com.lukekorth.pebblelocker;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.helpers.ThreadBus;
import com.squareup.otto.Bus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.UUID;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

public class PebbleLockerApplication extends com.activeandroid.app.Application implements Thread.UncaughtExceptionHandler {

    private static ThreadBus sBus;

    private Thread.UncaughtExceptionHandler mExceptionHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        migrate();
        initLogger();
        sBus = new ThreadBus();
    }

    public String getLogFilePath() {
        return getFileStreamPath("debug.log").getAbsolutePath();
    }

    private void initLogger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%date{MMM dd | HH:mm:ss.SSS} %highlight(%-5level) %-25([%logger{36}]) %msg%n");
        encoder.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setContext(loggerContext);
        fileAppender.setFile(getLogFilePath());
        fileAppender.setAppend(true);
        fileAppender.setEncoder(encoder);
        fileAppender.start();

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(fileAppender);

        if (BuildConfig.DEBUG) {
            LogcatAppender logcatAppender = new LogcatAppender();
            logcatAppender.setContext(loggerContext);
            logcatAppender.setEncoder(encoder);
            logcatAppender.start();

            root.addAppender(logcatAppender);
        }
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

            new File(getLogFilePath()).delete();
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
