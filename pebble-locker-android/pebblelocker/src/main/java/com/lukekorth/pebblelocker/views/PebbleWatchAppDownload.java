package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.PremiumFeaturesActivity;
import com.lukekorth.pebblelocker.R;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.logging.Logger;

public class PebbleWatchAppDownload extends Preference implements Preference.OnPreferenceClickListener {

    private Context mContext;
    private PremiumFeaturesActivity mPremiumFeaturesActivity;

    public PebbleWatchAppDownload(Context context) {
        super(context);
        init(context);
    }

    public PebbleWatchAppDownload(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PebbleWatchAppDownload(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        setTitle(R.string.pebble_watch_app_title);
        setSummary(R.string.pebble_watch_app_summary);
        setOnPreferenceClickListener(this);
        refresh();
    }

    public void setActivity(PremiumFeaturesActivity activity) {
        mPremiumFeaturesActivity = activity;
    }

    public void refresh() {
        setEnabled(new PebbleHelper(mContext, new Logger(mContext, "[PEBBLE-WATCH-DOWNLOAD]"))
                .isPebbleAppInstalled());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(mPremiumFeaturesActivity.hasPurchased()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("pebble://appstore/5386a0646189a1be8200007a"));
            mContext.startActivity(intent);
        } else {
            mPremiumFeaturesActivity.requirePurchase();
        }

        return true;
    }

}
