package com.lukekorth.pebblelocker.helpers;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Bus;

public class ThreadBus extends Bus {

    private final Handler mainThread = new Handler(Looper.getMainLooper());

    @Override
    public void post(final Object event) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            super.post(event);
        } else {
            mainThread.post(new Runnable() {
                @Override
                public void run() {
                    ThreadBus.super.post(event);
                }
            });
        }
    }
}
