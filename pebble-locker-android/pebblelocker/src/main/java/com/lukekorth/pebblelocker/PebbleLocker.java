package com.lukekorth.pebblelocker;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.lukekorth.pebblelocker.logging.Logger;
import com.lukekorth.pebblelocker.receivers.ConnectionReceiver;
import com.lukekorth.pebblelocker.services.LockerService;
import com.lukekorth.pebblelocker.views.BluetoothPreference;
import com.lukekorth.pebblelocker.views.LockStatePreference;
import com.lukekorth.pebblelocker.views.PebbleWatchAppDownload;
import com.lukekorth.pebblelocker.views.Status;
import com.lukekorth.pebblelocker.views.WifiPreference;

import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;

public class PebbleLocker extends PremiumFeaturesActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 2;
	
	private DevicePolicyManager mDPM;
	private ComponentName mDeviceAdmin;

    private LockStatePreference mLockState;
	private Status mStatus;
	private CheckBoxPreference mAdmin;
	private EditTextPreference mPassword;
	private CheckBoxPreference mEnable;
	private CheckBoxPreference mForceLock;

	private SharedPreferences mPrefs;

	private AlertDialog requirePassword;
	private long timeStamp;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.main);

        ((PebbleWatchAppDownload) findPreference("pebble_watch_app_download")).setActivity(this);
        ((BluetoothPreference) findPreference("bluetooth_preference")).setActivity(this);
        ((WifiPreference) findPreference("wifi_preference")).setActivity(this);

        mLockState = (LockStatePreference) findPreference("lock_state_preference");
		mStatus = (Status) findPreference("status");
		mAdmin     = (CheckBoxPreference) findPreference("key_enable_admin");
		mPassword  = (EditTextPreference) findPreference("key_password");
		mEnable    = (CheckBoxPreference) findPreference("key_enable_locker");
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
                    mEnable.setChecked(false);
                    removePassword();

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
		
		mEnable.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if(Boolean.parseBoolean(newValue.toString())) {
                    enableLockOptions(true);
                    showAlert(R.string.pebble_locker_enabled);
                } else {
                    enableLockOptions(false);
                    removePassword();
                    showAlert(R.string.pebble_locker_disabled);
                }
				
				return true;
			}
		});

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        AppRate.with(this)
                .text("Rate Pebble Locker")
                .initialLaunchCount(3)
                .retryPolicy(RetryPolicy.EXPONENTIAL)
                .checkAndShow();
	}
	
	public void onResume() {
		super.onResume();

        mPrefs.registerOnSharedPreferenceChangeListener(this);

        ((PebbleWatchAppDownload) findPreference("pebble_watch_app_download")).refresh();

		checkForRequiredPasswordByOtherApps();
		checkForActiveAdmin();

        mLockState.registerListener();
        mStatus.registerListener();

		if(!mPrefs.getString("key_password", "").equals("") &&
                timeStamp < (System.currentTimeMillis() - 60000) &&
				mPrefs.getBoolean(ConnectionReceiver.LOCKED, true)) {
            requestPassword();
        } else {
            int response = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            if (response != ConnectionResult.SUCCESS) {
                GooglePlayServicesUtil.getErrorDialog(response, this, REQUEST_GOOGLE_PLAY_SERVICES)
                    .show();
            }
        }
	}
	
	public void onPause() {
		super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        mLockState.unregisterListener();
        mStatus.unregisterListener();
	}
	
	/**
     * This is dangerous, so we prevent automated tests from doing it, and we
     * remind the user after we do it.
     */
    private void doResetPassword(String newPassword) {
        if (alertIfMonkey()) {
            return;
        }

        // hack because we need the new password to be
        // set in shared prefs before this method returns
        mPrefs.edit().putString("key_password", newPassword).apply();

        if(newPassword.length() == 0) {
            new Logger(this).log("[USER]", "Password was set to empty");
            mEnable.setChecked(false);
            showAlert(R.string.password_cleared);
        } else {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage(getString(R.string.reset_password_warning, newPassword))
                    .setPositiveButton("Don't Forget It!", null)
                    .show();
        }

        Intent intent = new Intent(this, LockerService.class);
        intent.putExtra(LockerService.TAG, "[USER-TRIGGERED]");
        intent.putExtra(LockerService.WITH_DELAY, false);
        intent.putExtra(LockerService.FORCE_LOCK, false);
        startService(intent);
    }

    private void removePassword() {
        mPassword.setText("");
    }

    private void checkForActiveAdmin() {
        if(mDPM.isAdminActive(mDeviceAdmin)) {
            mAdmin.setChecked(true);
            enableOptions(true);

            if (mPrefs.getBoolean("key_enable_locker", false)) {
                enableLockOptions(true);
            } else {
                enableLockOptions(false);
            }
        } else {
            mAdmin.setChecked(false);
            enableOptions(false);
        }
    }

    private void enableOptions(boolean enabled) {
		mEnable.setEnabled(enabled);
        enableLockOptions(enabled);
	}

    private void enableLockOptions(boolean enabled) {
        mPassword.setEnabled(enabled);
        mForceLock.setEnabled(enabled);
    }
	
	@SuppressLint("NewApi")
	private void checkForRequiredPasswordByOtherApps() {
		int encryptionStatus = -1;
		if(Build.VERSION.SDK_INT >= 11) {
            encryptionStatus = mDPM.getStorageEncryptionStatus();
        }
		
		boolean encryptionEnabled = (
                encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING ||
				encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE
        );
		
		if((mDPM.getPasswordMinimumLength(null) > 0 || encryptionEnabled) && !mPrefs.getBoolean("ignore_warning", false)) {
			String warning = "Your device is encrypted or there are other apps installed that require a password or pin to be set. " +
					         "Pebble Locker does not work on encrypted devices or with other apps that require a pin or password. " +
					         "If you wish to use Pebble Locker you will need to decrypt your device, disabled or uninstall any apps " +
					         "that require a pin or password or try to use it anyway. WARNING: Trying to use Pebble Locker " +
                             "in spite of this warning may cause you to lose all data on your device, you have been warned.";

            new AlertDialog.Builder(this)
                    .setMessage(warning)
                    .setCancelable(false)
                    .setPositiveButton("Do not use", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            PebbleLocker.this.finish();
                        }
                    })
                    .setNegativeButton("Use anyway", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mPrefs.edit().putBoolean("ignore_warning", true).apply();
                        }
                    })
                    .show();
		}
	}
	
	private void requestPassword() {
		if(requirePassword == null || !requirePassword.isShowing()) {
			LayoutInflater factory = LayoutInflater.from(this);
	        final View textEntryView = factory.inflate(R.layout.password_prompt, null);
	        
	        if(mPrefs.getString("key_password", "").matches("[0-9]+") && android.os.Build.VERSION.SDK_INT >= 11)
	        	((EditText) textEntryView.findViewById(R.id.password_edit)).setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_PASSWORD);
	        
	        requirePassword = new AlertDialog.Builder(PebbleLocker.this)
	            .setTitle("Enter your pin/password to continue")
	            .setView(textEntryView)
	            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	String password = ((EditText) textEntryView.findViewById(R.id.password_edit)).getText().toString();
	                	
	                	dialog.cancel();
	                	
	                	if(!mPrefs.getString("key_password", "").equals(password))
	                		requestPassword();
	                	else
	                		timeStamp = System.currentTimeMillis();
	                }
	            })
	            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	dialog.cancel();
	                	requestPassword();
	                }
	            })
	            .setCancelable(false)
	            .create();
	        
	        requirePassword.show();
		}
	}

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String message;
        if (key.equals("key_password")) {
            if (TextUtils.isEmpty(sharedPreferences.getString("key_password", ""))) {
                message = "User changed their password to empty";
            } else {
                message = "User changed their password";
            }
        } else {
            message = "User changed " + key;
        }

        new Logger(this, "[SETTINGS_CHANGED]").log(message);
    }

	/**
     * If the "user" is a monkey, post an alert and notify the caller.  This prevents automated
     * test frameworks from stumbling into annoying or dangerous operations.
     */
    private boolean alertIfMonkey() {
        if (ActivityManager.isUserAMonkey()) {
            showAlert(R.string.monkey);
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
        	new Logger(context).log("[DEVICE_ADMIN_RECEIVER]", "Device admin enabled");
        }

        @Override
        public CharSequence onDisableRequested(Context context, Intent intent) {
        	new Logger(context).log("[DEVICE_ADMIN_RECEIVER]", "Device admin disable requested, disabling");

    		ComponentName deviceAdmin = new ComponentName(context, CustomDeviceAdminReceiver.class);    		
    		((DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)).removeActiveAdmin(deviceAdmin);
    		
        	return null;
        }

        @Override
        public void onDisabled(Context context, Intent intent) {
        	new Logger(context).log("[DEVICE_ADMIN_RECEIVER]", "Device admin disabled");
        }

        @Override
        public void onPasswordChanged(Context context, Intent intent) {
        	new Logger(context).log("[DEVICE_ADMIN_RECEIVER]", "Password changed");
        }

        @Override
        public void onPasswordFailed(Context context, Intent intent) {
        	new Logger(context).log("[DEVICE_ADMIN_RECEIVER]", "Password failed");
        }

        @Override
        public void onPasswordSucceeded(Context context, Intent intent) {
        	new Logger(context).log("[DEVICE_ADMIN_RECEIVER]", "Password succeeded");
        }

        @Override
        public void onPasswordExpiring(Context context, Intent intent) {
        	new Logger(context).log("[DEVICE_ADMIN_RECEIVER]", "Password expiring");
        }
    }
}
