package com.lukekorth.pebblelocker.test;

import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LockStateTest extends AndroidTestCase {

    @Override
    public void setUp() throws Exception {
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
    }

    @Override
    public void tearDown() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().clear().commit();
    }

    public void testAutoReturnsCorrectValues() {
        assertEquals(0, LockState.AUTO.getState());
        assertEquals("Auto", LockState.AUTO.getDisplayName());
    }

    public void testManualUnlockedReturnsCorrectValues() {
        assertEquals(1, LockState.MANUAL_UNLOCKED.getState());
        assertEquals("Manually unlocked", LockState.MANUAL_UNLOCKED.getDisplayName());
    }

    public void testManualLockedReturnsCorrectValues() {
        assertEquals(2, LockState.MANUAL_LOCKED.getState());
        assertEquals("Manually locked", LockState.MANUAL_LOCKED.getDisplayName());
    }

    public void testGetInstanceReturnsTheCorrectStates() {
        assertEquals(LockState.AUTO, LockState.getInstance(0));
        assertEquals(LockState.MANUAL_UNLOCKED, LockState.getInstance(1));
        assertEquals(LockState.MANUAL_LOCKED, LockState.getInstance(2));
        assertEquals(LockState.AUTO, LockState.getInstance(6));
    }

    public void testGetCurrentStateReturnsCorrectState() {
        setLockState(LockState.AUTO.getState());
        assertEquals(LockState.AUTO, LockState.getCurrentState(getContext()));
        setLockState(LockState.MANUAL_UNLOCKED.getState());
        assertEquals(LockState.MANUAL_UNLOCKED, LockState.getCurrentState(getContext()));
        setLockState(LockState.MANUAL_LOCKED.getState());
        assertEquals(LockState.MANUAL_LOCKED, LockState.getCurrentState(getContext()));
    }

    public void testGetCurrentStateDefaultsToAuto() {
        assertEquals(LockState.AUTO, LockState.getCurrentState(getContext()));
    }

    public void testSwitchToNextStateTransitionsCorrectly() {
        Logger logger = mock(Logger.class);
        LockState state;
        setLockState(LockState.AUTO.getState());

        state = LockState.switchToNextState(getContext(), logger, false);
        assertEquals(LockState.MANUAL_UNLOCKED, state);
        assertEquals(LockState.MANUAL_UNLOCKED.getState(), LockState.getCurrentState(getContext()).getState());

        state = LockState.switchToNextState(getContext(), logger, false);
        assertEquals(LockState.MANUAL_LOCKED, state);
        assertEquals(LockState.MANUAL_LOCKED.getState(), LockState.getCurrentState(getContext()).getState());

        state = LockState.switchToNextState(getContext(), logger, false);
        assertEquals(LockState.AUTO, state);
        assertEquals(LockState.AUTO.getState(), LockState.getCurrentState(getContext()).getState());
    }

    public void testSetCurrentStateCallsHandleLockingForAuto() {
        Logger logger = mock(Logger.class);
        Locker locker = mock(Locker.class);

        LockState.setCurrentState(getContext(), logger, locker, false, LockState.AUTO.getState());
        verify(locker, times(1)).handleLocking(false);
    }

    public void testSetCurrentStateCallsUnlockForManualUnlock() {
        Logger logger = mock(Logger.class);
        Locker locker = mock(Locker.class);

        LockState.setCurrentState(getContext(), logger, locker, false, LockState.MANUAL_UNLOCKED.getState());
        verify(locker, times(1)).unlock();
    }

    public void testSetCurrentStateCallsLockForManualLock() {
        Logger logger = mock(Logger.class);
        Locker locker = mock(Locker.class);

        LockState.setCurrentState(getContext(), logger, locker, false, LockState.MANUAL_LOCKED.getState());
        verify(locker, times(1)).lock(false);
    }

    /* helpers */
    private void setLockState(int state) {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit()
                .putInt(LockState.LOCK_STATE, state)
                .commit();
    }
}
