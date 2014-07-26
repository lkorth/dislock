package com.lukekorth.pebblelocker.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.PremiumFeaturesActivity;
import com.lukekorth.pebblelocker.R;
import com.lukekorth.pebblelocker.helpers.AndroidWearHelper;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.helpers.WifiHelper;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;
import com.lukekorth.pebblelocker.models.BluetoothDevices;
import com.lukekorth.pebblelocker.receivers.ConnectionReceiver;

import java.util.List;

public class Status extends Preference implements AndroidWearHelper.Listener {

    private static final String TAG = "[STATUS-PREFERENCE]";

    private Context mContext;
    private Logger mLogger;
    private BroadcastReceiver mStatusReceiver;
    private AndroidWearHelper mAndroidWearHelper;

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

        setTitle(R.string.connected_trusted_devices);

        mStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };
    }

    private void update() {
        String connectedDevices = getConnectedDevices();
        if (TextUtils.isEmpty(connectedDevices)) {
            connectedDevices = "No trusted devices connected";
        }

        setSummary(connectedDevices);

        if (PremiumFeaturesActivity.hasPurchased(mContext)) {
            getConnectedAndroidWears();
        }
    }

    private String getConnectedDevices() {
        String connectedDevices = "";

        connectedDevices = conditionallyAddNewLine(connectedDevices,
                new PebbleHelper(mContext, mLogger).getConnectionStatus());

        if (PremiumFeaturesActivity.hasPurchased(mContext)) {
            connectedDevices = conditionallyAddNewLine(connectedDevices,
                    new WifiHelper(mContext, mLogger).getConnectionStatus());
            connectedDevices = conditionallyAddNewLine(connectedDevices,
                    BluetoothDevices.getConnectionStatus());
        }

        return connectedDevices;
    }

    private void getConnectedAndroidWears() {
        if (mAndroidWearHelper == null) {
            mAndroidWearHelper = new AndroidWearHelper(mContext, mLogger);
        }

        mAndroidWearHelper.getConnectedDevices(this);
    }

    @Override
    public void onKnownDevicesLoaded(List<AndroidWearDevices> devices) {
        String connectedDevices = getConnectedDevices();

        if (devices.size() > 0) {
            for(AndroidWearDevices device : devices) {
                if (device.trusted) {
                    connectedDevices = conditionallyAddNewLine(connectedDevices, "Android Wear Connected");
                    break;
                }
            }
        }

        if (TextUtils.isEmpty(connectedDevices)) {
            connectedDevices = "No trusted devices connected";
        }

        setSummary(connectedDevices);
    }

    public void registerListener() {
        mContext.registerReceiver(mStatusReceiver,
                new IntentFilter(ConnectionReceiver.STATUS_CHANGED_INTENT));
        update();
    }

    public void unregisterListener() {
        mContext.unregisterReceiver(mStatusReceiver);
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
