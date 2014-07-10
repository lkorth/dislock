package com.lukekorth.pebblelocker.views;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.lukekorth.pebblelocker.R;
import com.lukekorth.pebblelocker.logging.LogReporting;

public class Contact extends Preference implements Preference.OnPreferenceClickListener {

    private Context mContext;

    public Contact(Context context) {
        super(context);
        init(context);
    }

    public Contact(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Contact(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        setTitle(R.string.contact_title);
        setSummary(R.string.contact_summary);
        setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        new LogReporting(mContext).collectAndSendLogs();
        return true;
    }

}
