package com.lukekorth.pebblelocker;

import android.content.Context;
import android.preference.PreferenceManager;

public enum LockState {
    AUTO(0, "Auto"),
    MANUAL_UNLOCKED(1, "Manually unlocked"),
    MANUAL_LOCKED(2, "Manually locked");

    private int mState;
    private String mDisplayName;

    private LockState(int state, String name) {
        mState = state;
        mDisplayName = name;
    }

    public int getState() {
        return mState;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public static LockState getInstance(int value) {
        switch (value) {
            case 0:
                return AUTO;
            case 1:
                return MANUAL_UNLOCKED;
            case 2:
                return MANUAL_LOCKED;
        }

        return AUTO;
    }

    public static LockState getCurrentState(Context context) {
        return getInstance(
                PreferenceManager.getDefaultSharedPreferences(context).getInt(ConnectionReceiver.LOCK_STATE,
                LockState.AUTO.getState()));
    }

    public static LockState switchToNextState(Context context, Logger logger, boolean forceLock) {
        LockState lockState = getCurrentState(context);
        if (lockState == AUTO) {
            return setCurrentState(context, logger, forceLock, MANUAL_UNLOCKED.getState());
        } else if (lockState == MANUAL_UNLOCKED) {
            return setCurrentState(context, logger, forceLock, MANUAL_LOCKED.getState());
        } else if (lockState == MANUAL_LOCKED) {
            return setCurrentState(context, logger, forceLock, AUTO.getState());
        }

        return lockState;
    }

    public static LockState setCurrentState(Context context, Logger logger, boolean forceLock, int state) {
        LockState lockState = LockState.getInstance(state);
        logger.log("Setting lock state: " + lockState.getDisplayName());
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putInt(ConnectionReceiver.LOCK_STATE, state).commit();

        if (lockState == LockState.AUTO) {
            new Locker(context, "[MANUAL]").handleLocking(forceLock);
        } else if (lockState == LockState.MANUAL_UNLOCKED) {
            new Locker(context, "[MANUAL]").unlock();
        } else if (lockState == LockState.MANUAL_LOCKED) {
            new Locker(context, "[MANUAL]").lock(forceLock);
        }

        return lockState;
    }
}
