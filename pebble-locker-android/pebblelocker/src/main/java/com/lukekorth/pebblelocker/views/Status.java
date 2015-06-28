package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.R;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.helpers.Settings;
import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;
import com.lukekorth.pebblelocker.models.BluetoothDevices;
import com.lukekorth.pebblelocker.models.WifiNetworks;
import com.squareup.otto.Subscribe;

import org.slf4j.LoggerFactory;

public class Status extends Preference {

    private static final String TAG = "Status_Preference";

    public Status(Context context) {
        super(context);
        init();
    }

    public Status(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Status(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        PebbleLockerApplication.getBus().register(this);
        update(new StatusChangedEvent());
    }

    @Override
    public void onPrepareForRemoval() {
        PebbleLockerApplication.getBus().unregister(this);
    }

    @Subscribe
    public void update(StatusChangedEvent event) {
        if (hasTrustedDevices()) {
            String connectedDevices = "";

            connectedDevices = conditionallyAddNewLine(connectedDevices,
                    new PebbleHelper(getContext()).getConnectionStatus());
            connectedDevices = conditionallyAddNewLine(connectedDevices,
                    new WifiHelper(getContext()).getConnectionStatus());
            connectedDevices = conditionallyAddNewLine(connectedDevices,
                    AndroidWearDevices.getConnectionStatus());
            connectedDevices = conditionallyAddNewLine(connectedDevices,
                    BluetoothDevices.getConnectionStatus());

            if (TextUtils.isEmpty(connectedDevices)) {
                connectedDevices = getContext().getString(R.string.no_trusted_devices_connected);
            }

            setSummary(connectedDevices);
        } else {
            LoggerFactory.getLogger(TAG).debug("No trusted devices configured");
            setSummary(R.string.no_trusted_devices_configured);
        }
    }

    private boolean hasTrustedDevices() {
        return (Settings.isPebbleEnabled(getContext()) ||
                BluetoothDevices.countTrustedDevices() > 0 ||
                AndroidWearDevices.countTrustedDevices() > 0 ||
                WifiNetworks.countTrustedDevices() > 0);
    }

    private String conditionallyAddNewLine(String base, String addition) {
        if (!TextUtils.isEmpty(base) && !TextUtils.isEmpty(addition)) {
            return (base + "\n" + addition);
        } else if (!TextUtils.isEmpty(base)) {
            return base;
        } else {
            return addition;
        }
    }
}
