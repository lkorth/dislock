package com.lukekorth.pebblelocker;

import com.getpebble.android.kit.PebbleKit;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class PebbleLocker extends PreferenceActivity {
	
	private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
	
	private DevicePolicyManager mDPM;
	private ComponentName mDeviceAdmin;
	
	private CheckBoxPreference mAdmin;
	private EditTextPreference mPassword;
	private CheckBoxPreference mForceLock;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.main);
		
		mAdmin = (CheckBoxPreference) findPreference("key_enable_admin");
		mPassword = (EditTextPreference) findPreference("key_password");
		mForceLock = (CheckBoxPreference) findPreference("key_force_lock");
		
		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(this, CustomDeviceAdminReceiver.class);
		
		mAdmin.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if ((Boolean) newValue) {
                    // Launch the activity to have the user enable our admin.
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Pebble Locker needs device admin access to lock your device on disconnect");
                    startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
                    
                    // return false - don't update checkbox until we're really active
                    return false;
                } else {
                    mDPM.removeActiveAdmin(mDeviceAdmin);
                    PebbleLocker.this.enableOptions(false);
                    
                    return true;
                }
			}
			
		});
		
		mPassword.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				doResetPassword((String) newValue);
				return true;
			}
		});
	}
	
	public void onResume() {
		super.onResume();
		
		if(mDPM.isAdminActive(mDeviceAdmin)) {
			mAdmin.setChecked(true);
			enableOptions(true);
		} else {
			mAdmin.setChecked(false);
			enableOptions(false);
		}
	}
	
	/**
     * This is dangerous, so we prevent automated tests from doing it, and we
     * remind the user after we do it.
     */
    private void doResetPassword(String newPassword) {
        if (alertIfMonkey(this, "You can't reset my password, you are a monkey!")) {
            return;
        }
        
        if(!PebbleKit.isWatchConnected(this))
        	mDPM.resetPassword(newPassword, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message = getString(R.string.reset_password_warning, newPassword);
        builder.setMessage(message);
        builder.setPositiveButton("Don't Forget It!", null);
        builder.show();
    }
	
	private void enableOptions(boolean isEnabled) {
		mPassword.setEnabled(isEnabled);
		mForceLock.setEnabled(isEnabled);
	}
	
	 /**
     * If the "user" is a monkey, post an alert and notify the caller.  This prevents automated
     * test frameworks from stumbling into annoying or dangerous operations.
     */
    private static boolean alertIfMonkey(Context context, String string) {
        if (ActivityManager.isUserAMonkey()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(string);
            builder.setPositiveButton("Ok", null);
            builder.show();
            return true;
        } else {
            return false;
        }
    }
	
	/**
     * All callbacks are on the UI thread and your implementations should not engage in any
     * blocking operations, including disk I/O.
     */
    public static class CustomDeviceAdminReceiver extends DeviceAdminReceiver {
    	
        @Override
        public void onEnabled(Context context, Intent intent) {
        	// intentionally left blank
        }

        @Override
        public CharSequence onDisableRequested(Context context, Intent intent) {
        	// intentionally left blank
        	return null;
        }

        @Override
        public void onDisabled(Context context, Intent intent) {
        	// intentionally left blank
        }

        @Override
        public void onPasswordChanged(Context context, Intent intent) {
        	// intentionally left blank
        }

        @Override
        public void onPasswordFailed(Context context, Intent intent) {
        	Toast.makeText(context, "Password change failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPasswordSucceeded(Context context, Intent intent) {
        	// intentionally left blank
        }

        @Override
        public void onPasswordExpiring(Context context, Intent intent) {
            // intentionally left blank
        }
    }

}
