package com.lukekorth.pebblelocker.services;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AndroidWearDetectionService extends IntentService {

    public AndroidWearDetectionService() {
        super("AndroidWearDetectionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GoogleApiClient client = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
        client.connect();
        NodeApi.GetConnectedNodesResult connectedNodes = Wearable.NodeApi.getConnectedNodes(client)
                .await();

        Logger logger = LoggerFactory.getLogger("Android_Wear_Detection_Service");
        List<Node> devices = connectedNodes.getNodes();
        for (Node device : devices) {
            logger.debug("Android Wear " + device.getId() + " connected");
            AndroidWearDevices.setDeviceConnected(device, true);
        }
        PebbleLockerApplication.getBus().post(new StatusChangedEvent());
    }
}
