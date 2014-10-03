package com.lukekorth.pebblelocker;

import android.content.Context;
import android.preference.PreferenceManager;

public enum ScreenLockType {
    SLIDE("0", R.string.slide),
    PIN("1", R.string.pin),
    PASSWORD("2", R.string.password);

    public static final String SCREEN_LOCK_TYPE_KEY = "key_screen_lock_type";

    private String mType;
    private int mDescription;

    private ScreenLockType(String type, int description) {
        mType = type;
        mDescription = description;
    }

    public String getType() {
        return mType;
    }

    public int getDescription() {
        return mDescription;
    }

    public static String getCurrentDescription(Context context) {
        return context.getString(getCurrent(context).getDescription());
    }

    public static ScreenLockType getCurrent(Context context) {
        String currentType = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SCREEN_LOCK_TYPE_KEY, SLIDE.getType());

        if (currentType.equals(PIN.getType())) {
            return PIN;
        } else if (currentType.equals(PASSWORD.getType())) {
            return PASSWORD;
        } else {
            return SLIDE;
        }
    }

    public static void setCurrent(Context context, ScreenLockType type) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(SCREEN_LOCK_TYPE_KEY, type.getType())
                .apply();
    }

    public static void changeToSlide(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove("key_password")
                .putString(SCREEN_LOCK_TYPE_KEY, SLIDE.getType())
                .apply();
    }

}
