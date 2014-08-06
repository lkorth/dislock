package com.lukekorth.pebblelocker.receivers;

import android.content.Intent;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;

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
        AndroidWearDevices.setDeviceConnected(peer, true);

        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
        sendBroadcast(new Intent(ConnectionReceiver.ANDROID_WEAR_CONNECTED));
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        init();
        mLogger.log("Android Wear " + peer.getDisplayName() + " : " + peer.getId() + " disconnected");
        AndroidWearDevices.setDeviceConnected(peer, false);

        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
        sendBroadcast(new Intent(ConnectionReceiver.ANDROID_WEAR_DISCONNECTED));
    }

}
