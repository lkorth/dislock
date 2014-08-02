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

    protected Context mContext;

    public PremiumFeaturesPreference(Context context) {
        super(context);
        init(context);
    }

    public PremiumFeaturesPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PremiumFeaturesPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        setTitle(getDisplayTitle());
        setSummary(getDisplaySummary());
        setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(PremiumFeaturesActivity.hasPurchased(mContext)) {
            mContext.startActivity(getActionIntent());
        } else {
            PebbleLockerApplication.getBus().post(new RequirePurchaseEvent());
        }

        return true;
    }

    public abstract int getDisplayTitle();
    public abstract int getDisplaySummary();
    public abstract Intent getActionIntent();

}
