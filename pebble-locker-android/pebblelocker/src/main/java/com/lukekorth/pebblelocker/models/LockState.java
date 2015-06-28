package com.lukekorth.pebblelocker.models;

import android.content.Context;
import android.content.Intent;

import com.lukekorth.pebblelocker.R;
import com.lukekorth.pebblelocker.helpers.Settings;
import com.lukekorth.pebblelocker.services.LockerService;

public enum LockState {
    AUTO(0, 0),
    MANUAL_UNLOCKED(1, R.string.manually_unlocked),
    MANUAL_LOCKED(2, R.string.manually_locked);

    private int mState;
    private int mDisplayText;

    LockState(int state, int displayText) {
        mState = state;
        mDisplayText = displayText;
    }

    public int getState() {
        return mState;
    }

    public int getDisplayName(Context context) {
        if (this == AUTO) {
            if (Settings.isLocked(context)) {
                return R.string.automatically_locked;
            } else {
                return R.string.automatically_unlocked;
            }
        }

        return mDisplayText;
    }

    public static LockState getInstance(int value) {
        switch (value) {
            case 0:
                return AUTO;
            case 1:
                return MANUAL_UNLOCKED;
            case 2:
                return MANUAL_LOCKED;
            default:
                return AUTO;
        }
    }

    public static LockState getCurrentState(Context context) {
        return getInstance(Settings.getLockState(context));
    }

    public static LockState switchToNextState(Context context) {
        LockState lockState = getCurrentState(context);
        if (lockState == AUTO) {
            return setCurrentState(context, MANUAL_UNLOCKED);
        } else if (lockState == MANUAL_UNLOCKED) {
            return setCurrentState(context, MANUAL_LOCKED);
        } else if (lockState == MANUAL_LOCKED) {
            return setCurrentState(context, AUTO);
        }

        return lockState;
    }

    public static LockState setCurrentState(Context context, LockState state) {
        Settings.setLockState(context, state);
        Intent intent = new Intent(context, LockerService.class)
                .putExtra(LockerService.EXTRA_FORCE_LOCK, false);
        context.startService(intent);
        return state;
    }
}
