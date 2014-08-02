package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.DevicesActivity;
import com.lukekorth.pebblelocker.R;

public class BluetoothPreference extends PremiumFeaturesPreference {

    public BluetoothPreference(Context context) {
        super(context);
    }

    public BluetoothPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public int getDisplayTitle() {
        return R.string.bluetooth_devices_preference_title;
    }

    @Override
    public int getDisplaySummary() {
        return R.string.bluetooth_devices_preference_summary;
    }

    @Override
    public Intent getActionIntent() {
        return new Intent(mContext, DevicesActivity.class);
    }

}
