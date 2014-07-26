package com.lukekorth.pebblelocker.receivers;

import android.content.Intent;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.lukekorth.pebblelocker.helpers.AndroidWearHelper;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.logging.Logger;

import java.util.UUID;

public class AndroidWearReceiver extends WearableListenerService {

    private Logger mLogger;

    private void init() {
        mLogger = new Logger(this, "[" + UUID.randomUUID().toString().split("-")[1] + "]");
    }

    @Override
    public void onPeerConnected(Node peer) {
        init();
        mLogger.log("Android Wear " + peer.getDisplayName() + " : " + peer.getId() + " connected");
        new AndroidWearHelper(this).addDevice(peer);

        DeviceHelper.sendLockStatusChangedBroadcast(this);
        sendBroadcast(new Intent(ConnectionReceiver.ANDROID_WEAR_CONNECTED));
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        init();
        mLogger.log("Android Wear " + peer.getDisplayName() + " : " + peer.getId() + " disconnected");

        DeviceHelper.sendLockStatusChangedBroadcast(this);
        sendBroadcast(new Intent(ConnectionReceiver.ANDROID_WEAR_DISCONNECTED));
    }

}
