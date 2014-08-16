package com.lukekorth.pebblelocker.receivers;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;

public class AndroidWearReceiver extends WearableListenerService {

    private Logger mLogger;

    private void init() {
        mLogger = new Logger(this, PebbleLockerApplication.getUniqueTag());
    }

    @Override
    public void onPeerConnected(Node peer) {
        init();
        mLogger.log("Android Wear " + peer.getDisplayName() + " : " + peer.getId() + " connected");
        AndroidWearDevices.setDeviceConnected(peer, true);

        BaseBroadcastReceiver.handleLocking(this, mLogger.getTag());
        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        init();
        mLogger.log("Android Wear " + peer.getDisplayName() + " : " + peer.getId() + " disconnected");
        AndroidWearDevices.setDeviceConnected(peer, false);

        BaseBroadcastReceiver.lockWithDelay(this, mLogger);
        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
    }

}
