package com.lukekorth.pebblelocker;

import java.util.ArrayList;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.lukekorth.pebblelocker.PebbleLocker.CustomDeviceAdminReceiver;

public class Locker {

	private Context mContext;
	private SharedPreferences mPrefs;
	private Logger mLogger;
	private String mUniq;
	private DevicePolicyManager mDPM;

	public Locker(Context context, String tag) {
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mLogger = new Logger(context);
		mUniq = tag;
		mDPM = ((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE));
	}

	public void handleLocking() {
		handleLocking(true);
	}

	public void handleLocking(boolean forceLock) {
		if (connectedToDeviceOrWifi())
			unlock();
		else
			lock();
	}

	public void lock() {
		lock(true);
	}

	public void lock(boolean forceLock) {
		if (!enabled())
			return;

		boolean passwordChanged = mDPM.resetPassword(mPrefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
		mPrefs.edit().putBoolean(ConnectionReceiver.LOCKED, true).putBoolean(ConnectionReceiver.UNLOCK, false).commit();

		mLogger.log(mUniq, "Successfully locked: " + passwordChanged);

		if (forceLock && mPrefs.getBoolean("key_force_lock", false))
			mDPM.lockNow();
	}

	public void unlock() {
		if (!enabled())
			return;

		if (isDeviceOnLockscreen()) {
			mPrefs.edit().putBoolean(ConnectionReceiver.UNLOCK, true).commit();
			mLogger.log(mUniq, "Screen is on lockscreen, setting unlock true for future unlock");
		} else {
			boolean passwordChanged = false;

			try {
				passwordChanged = mDPM.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);

				mPrefs.edit().putBoolean(ConnectionReceiver.LOCKED, false).commit();
			} catch (IllegalArgumentException e) {
				boolean passwordReset = mDPM.resetPassword(mPrefs.getString("key_password", ""), DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
				mPrefs.edit().putBoolean(ConnectionReceiver.LOCKED, true).commit();

				mLogger.log(mUniq, "There was an exception when setting the password to blank, setting it back. Successfully reset: " + passwordReset + " " + Log.getStackTraceString(e));
			}

			mPrefs.edit().putBoolean(ConnectionReceiver.UNLOCK, false).commit();

			mLogger.log(mUniq, "Sucessfully unlocked: " + passwordChanged);
		}
	}

	private boolean enabled() {
		boolean activeAdmin = mDPM.isAdminActive(new ComponentName(mContext, CustomDeviceAdminReceiver.class));
		boolean enabled = mPrefs.getBoolean("key_enable_locker", false);

		if (!activeAdmin)
			mLogger.log(mUniq, "Not an active admin");
		if (!enabled)
			mLogger.log(mUniq, "key_enable_locker is false");

		return activeAdmin && enabled;
	}

	private boolean isDeviceOnLockscreen() {
		boolean keyguard = ((KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode();
		boolean screen = ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE)).isScreenOn();

		mLogger.log(mUniq, "Keyguard is showing: " + keyguard + " Screen is on: " + screen);

		return keyguard && screen;
	}

	public boolean connectedToDeviceOrWifi() {
		boolean pebble = isPebbleWatchConnected();
		boolean bluetooth = isTrustedBluetoothDeviceConnected();
		boolean wifi = isTrustedWifiConnected();

		mLogger.log(mUniq, "Pebble: " + pebble + " Bluetooth: " + bluetooth + " Wifi: " + wifi);

		return (pebble || bluetooth || wifi);
	}

	public boolean isPebbleWatchConnected() {
		if (mPrefs.getBoolean("pebble", true)) {
			Cursor c = null;
			try {
				c = mContext.getApplicationContext().getContentResolver().query(Uri.parse("content://com.getpebble.android.provider/state"), null, null, null, null);
			} catch (Exception e) {
				mLogger.log(mUniq, "Exception getting Pebble connection status: " + e);
			}

			if (c == null)
				return false;

			if (!c.moveToNext()) {
				c.close();
				return false;
			}

			boolean connected = (c.getInt(0) == 1);
			c.close();
			return connected;
		} else {
			mLogger.log(mUniq, "Unlock via any Pebble is not enabled");
			return false;
		}
	}

	public boolean isTrustedBluetoothDeviceConnected() {
		ArrayList<String> connectedBluetoothDevices = new DatabaseHelper(mContext).connectedDevices();

		for (String address : connectedBluetoothDevices) {
			mLogger.log(mUniq, "Connected bluetooth address: " + address);

			if (mPrefs.getBoolean(address, false))
				return true;
		}

		return false;
	}

	public boolean isTrustedWifiConnected() {
		WifiInfo wifiInfo = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
		if (wifiInfo != null) {
			if (wifiInfo.getSSID() != null) {
				String ssid = WiFiNetworks.stripQuotes(wifiInfo.getSSID());
				String encodedSsid = WiFiNetworks.base64Encode(ssid);

				mLogger.log(mUniq, "Wifi network " + ssid + " is connected: " + encodedSsid);

				if (mPrefs.getBoolean(encodedSsid, false))
					return true;
			} else {
				mLogger.log(mUniq, "wifiInfo.getSSID is null");
			}
		} else {
			mLogger.log(mUniq, "wifiInfo is null");
		}

		return false;
	}
}
