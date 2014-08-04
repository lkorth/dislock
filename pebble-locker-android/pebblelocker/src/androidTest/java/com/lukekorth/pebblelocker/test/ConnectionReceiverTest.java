package com.lukekorth.pebblelocker.test;

import android.content.Intent;

import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.receivers.ConnectionReceiver;
import com.lukekorth.pebblelocker.services.LockerService;

import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ConnectionReceiverTest extends BaseApplicationTestCase {

    @Mock Logger logger;
    @Mock DeviceHelper deviceHelper;

    public void testHandleBroadcastSendsLockStatusChangedEvent() {
        getConnectionReceiver().handleBroadcast();
        verify(deviceHelper, times(1)).sendLockStatusChangedEvent();
    }

    public void testHandleBroadcastDoesNotReturnIntentIfLockStateIsNotAuto() {
        LockState.setCurrentState(getContext(), logger, false, LockState.MANUAL_LOCKED.getState());

        Intent intent = getConnectionReceiver().handleBroadcast();

        assertNull(intent);
    }

    public void testGetActionIntentReturnsCorrectIntent() {
        Intent intent = getConnectionReceiver().getActionIntent();
        assertEquals("TEST", intent.getStringExtra(LockerService.TAG));
    }

    /* helpers */
    private ConnectionReceiver getConnectionReceiver() {
        return getConnectionReceiver(new Intent().setAction("test"), false);
    }

    private ConnectionReceiver getConnectionReceiver(Intent intent, boolean trustedWifiConnected) {
        return new ConnectionReceiver(getContext(), intent, "TEST", logger, deviceHelper,
                trustedWifiConnected);
    }
}
