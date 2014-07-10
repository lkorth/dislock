package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.PremiumFeaturesActivity;
import com.lukekorth.pebblelocker.R;
import com.lukekorth.pebblelocker.WiFiNetworks;

public class WifiPreference extends Preference implements Preference.OnPreferenceClickListener {

    private Context mContext;
    private PremiumFeaturesActivity mPremiumFeaturesActivity;

    public WifiPreference(Context context) {
        super(context);
        init(context);
    }

    public WifiPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WifiPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        setTitle(R.string.wifi_preference_title);
        setSummary(R.string.wifi_preference_summary);
        setOnPreferenceClickListener(this);
    }

    public void setActivity(PremiumFeaturesActivity activity) {
        mPremiumFeaturesActivity = activity;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(mPremiumFeaturesActivity.hasPurchased()) {
            mContext.startActivity(new Intent(mContext, WiFiNetworks.class));
        } else {
            mPremiumFeaturesActivity.requirePurchase();
        }

        return true;
    }
}
