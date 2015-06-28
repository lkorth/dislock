package com.lukekorth.pebblelocker.test;

import com.lukekorth.pebblelocker.R;
import com.lukekorth.pebblelocker.helpers.Settings;
import com.lukekorth.pebblelocker.models.LockState;

public class LockStateTest extends BaseApplicationTestCase {

    public void testAutoReturnsCorrectValues() {
        assertEquals(0, LockState.AUTO.getState());
        assertEquals(R.string.automatically_locked, LockState.AUTO.getDisplayName(mContext));
    }

    public void testManualUnlockedReturnsCorrectValues() {
        assertEquals(1, LockState.MANUAL_UNLOCKED.getState());
        assertEquals(R.string.manually_unlocked, LockState.MANUAL_UNLOCKED.getDisplayName(mContext));
    }

    public void testManualLockedReturnsCorrectValues() {
        assertEquals(2, LockState.MANUAL_LOCKED.getState());
        assertEquals(R.string.manually_locked, LockState.MANUAL_LOCKED.getDisplayName(mContext));
    }

    public void testGetInstanceReturnsTheCorrectStates() {
        assertEquals(LockState.AUTO, LockState.getInstance(0));
        assertEquals(LockState.MANUAL_UNLOCKED, LockState.getInstance(1));
        assertEquals(LockState.MANUAL_LOCKED, LockState.getInstance(2));
        assertEquals(LockState.AUTO, LockState.getInstance(6));
    }

    public void testGetCurrentStateReturnsCorrectState() {
        setLockState(LockState.AUTO);
        assertEquals(LockState.AUTO, LockState.getCurrentState(getContext()));
        setLockState(LockState.MANUAL_UNLOCKED);
        assertEquals(LockState.MANUAL_UNLOCKED, LockState.getCurrentState(getContext()));
        setLockState(LockState.MANUAL_LOCKED);
        assertEquals(LockState.MANUAL_LOCKED, LockState.getCurrentState(getContext()));
    }

    public void testGetCurrentStateDefaultsToAuto() {
        assertEquals(LockState.AUTO, LockState.getCurrentState(getContext()));
    }

    public void testSwitchToNextStateTransitionsCorrectly() {
        LockState state;
        setLockState(LockState.AUTO);

        state = LockState.switchToNextState(getContext());
        assertEquals(LockState.MANUAL_UNLOCKED, state);
        assertEquals(LockState.MANUAL_UNLOCKED.getState(), LockState.getCurrentState(getContext()).getState());

        state = LockState.switchToNextState(getContext());
        assertEquals(LockState.MANUAL_LOCKED, state);
        assertEquals(LockState.MANUAL_LOCKED.getState(), LockState.getCurrentState(getContext()).getState());

        state = LockState.switchToNextState(getContext());
        assertEquals(LockState.AUTO, state);
        assertEquals(LockState.AUTO.getState(), LockState.getCurrentState(getContext()).getState());
    }

    /* helpers */
    private void setLockState(LockState state) {
        Settings.setLockState(mContext, state);
    }
}
