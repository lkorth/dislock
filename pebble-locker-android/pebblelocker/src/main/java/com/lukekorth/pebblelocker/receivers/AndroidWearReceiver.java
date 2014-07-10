package com.lukekorth.pebblelocker.receivers;

import android.content.Context;
import android.os.PowerManager;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.helpers.AndroidWearHelper;
import com.lukekorth.pebblelocker.logging.Logger;

import java.util.UUID;

public class AndroidWearReceiver extends WearableListenerService {

    private String mTag;
    private Logger mLogger;
    private PowerManager.WakeLock mWakeLock;

    private void init() {
        mTag = "[" + UUID.randomUUID().toString().split("-")[1] + "]";
        mLogger = new Logger(this, mTag);
        acquireWakeLock();
    }

    @Override
    public void onPeerConnected(Node peer) {
        init();
        mLogger.log("Android Wear " + peer.getDisplayName() + " : " + peer.getId() +
                " connected");
        new AndroidWearHelper(this).addDevice(peer);
        new Locker(this, mTag).handleLocking();
        releaseWakeLock();
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        init();
        mLogger.log("Android Wear " + peer.getDisplayName() + " : " + peer.getId() +
                " disconnected");
        new Locker(this, mTag).lockWithDelay();
        releaseWakeLock();
    }

    private void acquireWakeLock() {
        mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PebbleLockerReceiver");

        mLogger.log("Acquiring wakelock");

        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        mLogger.log("Releasing wakelock");
        mWakeLock.release();
    }

}
