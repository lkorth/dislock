package com.lukekorth.pebblelocker.helpers;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import org.slf4j.LoggerFactory;

/**
 * All callbacks are on the UI thread and your implementations should not engage in any
 * blocking operations, including disk I/O.
 */
public class CustomDeviceAdminReceiver extends DeviceAdminReceiver {

    private static final String TAG = "Device_Admin_Receiver";

    @Override
    public void onEnabled(Context context, Intent intent) {
        LoggerFactory.getLogger(TAG).debug("Device admin enabled");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        LoggerFactory.getLogger(TAG).debug("Device admin disable requested, disabling");

        ComponentName deviceAdmin = new ComponentName(context, CustomDeviceAdminReceiver.class);
        ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)).removeActiveAdmin(deviceAdmin);

        return null;
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        LoggerFactory.getLogger(TAG).debug("Device admin disabled");
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        LoggerFactory.getLogger(TAG).debug("Password changed");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        LoggerFactory.getLogger(TAG).debug("Password failed");
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        LoggerFactory.getLogger(TAG).debug("Password succeeded");
    }

    @Override
    public void onPasswordExpiring(Context context, Intent intent) {
        LoggerFactory.getLogger(TAG).debug("Password expiring");
    }

}

