package com.lukekorth.pebblelocker.test;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.lukekorth.pebblelocker.receivers.ConnectionReceiver;
import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.PebbleLocker;
import com.lukekorth.pebblelocker.helpers.BluetoothHelper;
import com.lukekorth.pebblelocker.helpers.DeviceHelper;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.helpers.WifiHelper;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LockerTest extends AndroidTestCase {

    private final static String TAG = "TEST";

    @Mock DeviceHelper mDeviceHelper;
    @Mock WifiHelper mWifiHelper;
    @Mock BluetoothHelper mBluetoothHelper;
    @Mock PebbleHelper mPebbleHelper;
    @Mock DevicePolicyManager mDPM;

    private SharedPreferences mPrefs;
    private Locker mLocker;

    @Override
    public void setUp() throws Exception {
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        MockitoAnnotations.initMocks(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mLocker = new Locker(getContext(), TAG, mDeviceHelper, mWifiHelper, mBluetoothHelper,
                mPebbleHelper, mDPM);
    }

    @Override
    public void tearDown() {
        mPrefs.edit().clear().commit();
    }

    public void testHandleLockingDefaultsToForceLocking() {
        setEnabled();
        mPrefs.edit().putBoolean("key_force_lock", true).commit();

        mLocker.handleLocking();

        verify(mDPM, times(1)).lockNow();
    }

    public void testHandleLockingProxiesForceLockOption() {
        setEnabled();
        mPrefs.edit().putBoolean("key_force_lock", true).commit();

        mLocker.handleLocking(false);
        verify(mDPM, never()).lockNow();

        mLocker.handleLocking(true);
        verify(mDPM, times(1)).lockNow();
    }

    public void testHandleLockingUnlocksWhenConnectedAndLocked() {
        setEnabled();
        setConnected(true);
        when(mDeviceHelper.isLocked(true)).thenReturn(true);

        mLocker.handleLocking();

        verify(mDPM, times(1)).resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
    }

    public void testHandleLockingDoesNotExcessivelyUnlock() {
        setEnabled();
        setConnected(true);
        when(mDeviceHelper.isLocked(true)).thenReturn(false);

        mLocker.handleLocking();

        verify(mDPM, never()).resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
    }

    public void testHandleLockingLocksWhenNotConnectedAndNotLocked() {
        setEnabled();
        setConnected(false);
        when(mDeviceHelper.isLocked(false)).thenReturn(false);

        mLocker.handleLocking();

        verify(mDPM, times(1)).resetPassword("1234", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
    }

    public void testHandleLockingDoesNotExcessivelyLock() {
        setEnabled();
        setConnected(false);
        when(mDeviceHelper.isLocked(false)).thenReturn(true);

        mLocker.handleLocking();

        verify(mDPM, never()).resetPassword("1234", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
    }

    public void testLockReturnsEarlyIfNotEnabled() {
        mLocker.lock();

        verify(mDPM, never()).resetPassword("1234", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
    }

    public void testLockSetsPasswordCorrectlyAndSendsBroadcast() {
        setEnabled();

        mLocker.lock();

        verify(mDPM).resetPassword("1234", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        assertTrue(mPrefs.getBoolean(ConnectionReceiver.LOCKED, false));
        assertFalse(mPrefs.getBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, true));
        verify(mDeviceHelper).sendLockStatusChangedBroadcast();
    }

    public void testLockDefaultsToForceLocking() {
        setEnabled();
        mPrefs.edit().putBoolean("key_force_lock", true).commit();

        mLocker.lock();

        verify(mDPM).lockNow();
    }

    public void testLockForceLocksScreenWhenForceLockIsTrue() {
        setEnabled();
        mPrefs.edit().putBoolean("key_force_lock", true).commit();

        mLocker.lock(true);

        verify(mDPM).lockNow();
    }

    public void testLockDoesNotForceLockScreenWhenForceLockIsFalse() {
        setEnabled();
        mPrefs.edit().putBoolean("key_force_lock", true).commit();

        mLocker.lock(false);

        verify(mDPM, never()).lockNow();
    }

    public void testLockOnlyForceLocksScreenWhenPreferenceIsTrue() {
        setEnabled();

        mPrefs.edit().putBoolean("key_force_lock", false).commit();
        mLocker.lock();
        verify(mDPM, never()).lockNow();

        mPrefs.edit().putBoolean("key_force_lock", true).commit();
        mLocker.lock();
        verify(mDPM, times(1)).lockNow();
    }

    public void testUnlockReturnsEarlyIfNotEnabled() {
        mLocker.unlock();

        verify(mDPM, never()).resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
    }

    public void testSetsNeedToUnlockToTrueWhenScreenIsOnAndOnLockscreen() {
        setEnabled();

        when(mDeviceHelper.isOnLockscreen()).thenReturn(true);
        when(mDeviceHelper.isScreenOn()).thenReturn(true);

        mLocker.unlock();

        assertTrue(mPrefs.getBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, false));
        verify(mDPM, never()).resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        verify(mDeviceHelper, times(1)).sendLockStatusChangedBroadcast();
    }

    public void testUnlockRequiresPasswordOnceOnReconnectIfOptionIsEnabled() {
        setEnabled();
        mPrefs.edit().putBoolean("key_require_password_on_reconnect", true).commit();
        mPrefs.edit().putBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, false).commit();

        mLocker.unlock();

        verify(mDPM, never()).resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        assertTrue(mPrefs.getBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, false));
    }

    public void testUnlockUnlocksAndSendsBroadcast() {
        setEnabled();

        mLocker.unlock();

        verify(mDPM, times(1)).resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        assertFalse(mPrefs.getBoolean(DeviceHelper.NEED_TO_UNLOCK_KEY, true));
        verify(mDeviceHelper, times(1)).sendLockStatusChangedBroadcast();
    }

    public void testUnlockRestoresPasswordWhenUnlockFails() {
        setEnabled();
        when(mDPM.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY)).thenThrow(new IllegalArgumentException());

        mLocker.unlock();

        verify(mDPM, times(1)).resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        verify(mDPM, times(1)).resetPassword("1234", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
    }

    public void testUnlockTurnsOffScreenIfItWasTurnedOnDuringUnlock() {
        setEnabled();
        when(mDeviceHelper.isOnLockscreen()).thenReturn(true);
        when(mDeviceHelper.isScreenOn()).thenReturn(false).thenReturn(false).thenReturn(true);
        when(mDPM.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY)).thenReturn(true);

        mLocker.unlock();

        verify(mDPM, times(1)).resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        verify(mDPM, times(1)).lockNow();
    }

    public void testEnabledIsTrueWhenAllConditionsAreMet() {
        setEnabled();
        assertTrue(mLocker.enabled());
    }

    public void testEnabledIsFalseWhenAnyConditionIsNotMet() {
        when(mDPM.isAdminActive(new ComponentName(mContext, PebbleLocker.CustomDeviceAdminReceiver.class))).thenReturn(true);
        mPrefs.edit().putBoolean("key_enable_locker", true).commit();

        assertFalse(mLocker.enabled());
    }

    public void testConnectedToDeviceOrWifiIsTrueWhenAllDevicesAreConnected() {
        when(mWifiHelper.isTrustedWifiConnected()).thenReturn(true);
        when(mBluetoothHelper.isTrustedDeviceConnected()).thenReturn(true);
        when(mPebbleHelper.isEnabledAndConnected()).thenReturn(true);

        assertTrue(mLocker.isConnectedToDeviceOrWifi());
    }

    public void testConnectedToDeviceOrWifiIsFalseWhenNoDevicesAreConnected() {
        when(mWifiHelper.isTrustedWifiConnected()).thenReturn(false);
        when(mBluetoothHelper.isTrustedDeviceConnected()).thenReturn(false);
        when(mPebbleHelper.isEnabledAndConnected()).thenReturn(false);

        assertFalse(mLocker.isConnectedToDeviceOrWifi());
    }

    public void testConnectedToDeviceOrWifiIsTrueWhenOneDeviceIsConnected() {
        when(mWifiHelper.isTrustedWifiConnected()).thenReturn(false);
        when(mBluetoothHelper.isTrustedDeviceConnected()).thenReturn(true);
        when(mPebbleHelper.isEnabledAndConnected()).thenReturn(false);

        assertTrue(mLocker.isConnectedToDeviceOrWifi());
    }

    /* helpers */
    private void setEnabled() {
        when(mDPM.isAdminActive(new ComponentName(mContext, PebbleLocker.CustomDeviceAdminReceiver.class))).thenReturn(true);
        mPrefs.edit().putBoolean("key_enable_locker", true).commit();
        mPrefs.edit().putString("key_password", "1234").commit();
    }

    private void setConnected(boolean connected) {
        when(mPebbleHelper.isEnabledAndConnected()).thenReturn(connected);
    }
}
