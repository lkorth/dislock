package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.PremiumFeaturesActivity;
import com.lukekorth.pebblelocker.events.RequirePurchaseEvent;

public abstract class PremiumFeaturesPreference extends Preference
        implements Preference.OnPreferenceClickListener {

    public PremiumFeaturesPreference(Context context) {
        super(context);
        init();
    }

    public PremiumFeaturesPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PremiumFeaturesPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(PremiumFeaturesActivity.hasPurchased(getContext())) {
            getContext().startActivity(getActionIntent());
        } else {
            PebbleLockerApplication.getBus().post(new RequirePurchaseEvent());
        }

        return true;
    }

    public abstract Intent getActionIntent();

}
