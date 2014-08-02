package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.R;
import com.lukekorth.pebblelocker.WiFiNetworks;

public class WifiPreference extends PremiumFeaturesPreference {

    public WifiPreference(Context context) {
        super(context);
    }

    public WifiPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WifiPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public int getDisplayTitle() {
        return R.string.wifi_preference_title;
    }

    @Override
    public int getDisplaySummary() {
        return R.string.wifi_preference_summary;
    }

    @Override
    public Intent getActionIntent() {
        return new Intent(mContext, WiFiNetworks.class);
    }
}
