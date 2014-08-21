package com.lukekorth.pebblelocker.test;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.Locker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LockStateTest extends BaseApplicationTestCase {

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
        LockState state;
        setLockState(LockState.AUTO.getState());

        state = LockState.switchToNextState(getContext(), "TEST", false);
        assertEquals(LockState.MANUAL_UNLOCKED, state);
        assertEquals(LockState.MANUAL_UNLOCKED.getState(), LockState.getCurrentState(getContext()).getState());

        state = LockState.switchToNextState(getContext(), "TEST", false);
        assertEquals(LockState.MANUAL_LOCKED, state);
        assertEquals(LockState.MANUAL_LOCKED.getState(), LockState.getCurrentState(getContext()).getState());

        state = LockState.switchToNextState(getContext(), "TEST", false);
        assertEquals(LockState.AUTO, state);
        assertEquals(LockState.AUTO.getState(), LockState.getCurrentState(getContext()).getState());
    }

    public void pendingSetCurrentStateCallsHandleLockingForAuto() {
        Locker locker = mock(Locker.class);

//        LockState.setCurrentState(getContext(), logger, locker, false, LockState.AUTO.getState());
        verify(locker, times(1)).handleLocking(false);
    }

    public void pendingSetCurrentStateCallsUnlockForManualUnlock() {
        Locker locker = mock(Locker.class);

//        LockState.setCurrentState(getContext(), logger, locker, false, LockState.MANUAL_UNLOCKED.getState());
        verify(locker, times(1)).unlock();
    }

    public void pendingSetCurrentStateCallsLockForManualLock() {
        Locker locker = mock(Locker.class);

//        LockState.setCurrentState(getContext(), logger, locker, false, LockState.MANUAL_LOCKED.getState());
        verify(locker, times(1)).lock(false);
    }

    /* helpers */
    private void setLockState(int state) {
        mPrefs.edit().putInt(LockState.LOCK_STATE, state).apply();
    }
}
