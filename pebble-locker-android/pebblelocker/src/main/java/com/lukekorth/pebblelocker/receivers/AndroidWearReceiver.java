package com.lukekorth.pebblelocker.receivers;

import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.WearableListenerService;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;

import org.slf4j.LoggerFactory;

public class AndroidWearReceiver extends WearableListenerService {

    @Override
    public void onPeerConnected(Node peer) {
        String tag = PebbleLockerApplication.getUniqueTag();
        LoggerFactory.getLogger(tag).debug("Android Wear " + peer.getDisplayName() + " : " + peer.getId() + " connected");
        AndroidWearDevices.setDeviceConnected(peer, true);

        BaseBroadcastReceiver.handleLocking(this, tag);
        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        String tag = PebbleLockerApplication.getUniqueTag();
        LoggerFactory.getLogger(tag).debug("Android Wear " + peer.getDisplayName() + " : " + peer.getId() + " disconnected");
        AndroidWearDevices.setDeviceConnected(peer, false);

        BaseBroadcastReceiver.lockWithDelay(this, tag);
        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
    }

}
