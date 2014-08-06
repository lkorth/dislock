package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.PremiumFeaturesActivity;
import com.lukekorth.pebblelocker.events.StatusChangedEvent;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;
import com.lukekorth.pebblelocker.models.BluetoothDevices;
import com.squareup.otto.Subscribe;

public class Status extends Preference {

    private static final String TAG = "[STATUS-PREFERENCE]";

    private Context mContext;
    private Logger mLogger;

    public Status(Context context) {
        super(context);
        init(context);
    }

    public Status(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Status(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mLogger = new Logger(mContext, TAG);
        PebbleLockerApplication.getBus().register(this);
        update(new StatusChangedEvent());
    }

    @Override
    public void onPrepareForRemoval() {
        PebbleLockerApplication.getBus().unregister(this);
    }

    @Subscribe
    public void update(StatusChangedEvent event) {
        String connectedDevices = "";

        connectedDevices = conditionallyAddNewLine(connectedDevices,
                new PebbleHelper(mContext, mLogger).getConnectionStatus());

        if (PremiumFeaturesActivity.hasPurchased(mContext)) {
            connectedDevices = conditionallyAddNewLine(connectedDevices,
                    new WifiHelper(mContext, mLogger).getConnectionStatus());
            connectedDevices = conditionallyAddNewLine(connectedDevices,
                    AndroidWearDevices.getConnectionStatus());
            connectedDevices = conditionallyAddNewLine(connectedDevices,
                    BluetoothDevices.getConnectionStatus());
        }

        if (TextUtils.isEmpty(connectedDevices)) {
            connectedDevices = "No trusted devices connected";
        }

        setSummary(connectedDevices);
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
