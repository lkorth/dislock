package com.lukekorth.pebblelocker.helpers;

import android.content.Context;

import com.activeandroid.query.Select;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AndroidWearHelper implements ResultCallback<NodeApi.GetConnectedNodesResult> {

    private Context mContext;
    private Logger mLogger;
    private Listener mListener;
    private boolean mAllDevices;

    public static interface Listener {
        public void onKnownDevicesLoaded(List<AndroidWearDevices> devices);
    }

    public AndroidWearHelper(Context context) {
        mContext = context;
        mLogger = new Logger(context, "[ANDROID-WEAR-HELPER]");
    }

    public AndroidWearHelper(Context context, Logger logger) {
        mContext = context;
        mLogger = logger;
    }

    public AndroidWearDevices addDevice(Node node) {
        AndroidWearDevices device = new Select()
                .from(AndroidWearDevices.class)
                .where("deviceId = ?", node.getId())
                .executeSingle();

        if (device == null) {
            device = new AndroidWearDevices();
            device.name = node.getDisplayName();
            device.deviceId = node.getId();
            device.trusted = false;
            device.save();
        }

        return device;
    }

    public void setDeviceTrusted(String name, String deviceId, boolean trusted) {
        AndroidWearDevices device = new Select()
                .from(AndroidWearDevices.class)
                .where("deviceId = ?", deviceId)
                .executeSingle();

        if (device == null) {
            device = new AndroidWearDevices();
            device.name = name;
            device.deviceId = deviceId;
        }

        device.trusted = trusted;
        device.save();
    }

    public boolean isTrustedDeviceConnected() {
        List<Node> wears = getConnectedDevices();
        for(Node node : wears) {
            mLogger.log("Wear " + node.getDisplayName() + " with id: " + node.getId() + " is connected");
            boolean trusted = new Select()
                    .from(AndroidWearDevices.class)
                    .where("deviceId = ?", node.getId())
                    .where("trusted = ?", true)
                    .exists();
            if (trusted) {
                return true;
            }
        }

        return false;
    }

    public List<AndroidWearDevices> getEncounteredDevices() {
        return new Select()
                .from(AndroidWearDevices.class)
                .execute();
    }

    public List<Node> getConnectedDevices() {
        NodeApi.GetConnectedNodesResult connectedNodes = Wearable.NodeApi
                .getConnectedNodes(getGoogleClient()).await();
        return connectedNodes.getNodes();
    }

    public void getConnectedDevices(Listener listener) {
        mListener = listener;
        Wearable.NodeApi.getConnectedNodes(getGoogleClient())
                .setResultCallback(this);
    }

    public void getConnectedDevices(Listener listener, boolean allDevices) {
        mAllDevices = allDevices;
        getConnectedDevices(listener);
    }

    @Override
    public void onResult(NodeApi.GetConnectedNodesResult connectedNodesResult) {
        Set<AndroidWearDevices> devices = new HashSet<AndroidWearDevices>();
        for (Node node : connectedNodesResult.getNodes()) {
            devices.add(addDevice(node));
        }

        if (mAllDevices) {
            devices.addAll(getEncounteredDevices());
        }

        if (mListener != null) {
            List<AndroidWearDevices> response = new ArrayList<AndroidWearDevices>();
            response.addAll(devices);
            mListener.onKnownDevicesLoaded(response);
        }
    }

    private GoogleApiClient getGoogleClient() {
        GoogleApiClient client = new GoogleApiClient.Builder(mContext).addApi(Wearable.API).build();
        client.connect();

        return client;
    }

}
