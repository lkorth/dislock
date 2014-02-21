package com.lukekorth.pebblelocker;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class PebbleLocker extends PremiumFeatures {
	
	public static final int VERSION = 15;
	
	private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
	
	private DevicePolicyManager mDPM;
	private ComponentName mDeviceAdmin;
	
	private Preference         mStatus;
	private CheckBoxPreference mAdmin;
	private EditTextPreference mPassword;
	private CheckBoxPreference mEnable;
	private CheckBoxPreference mForceLock;
	
	private SharedPreferences mPrefs;
	
	private BroadcastReceiver mStatusReceiver;
	
	private AlertDialog requirePassword;
	private long timeStamp;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.main);
		
		mStatus    = (Preference) findPreference("visible_status");
		mAdmin     = (CheckBoxPreference) findPreference("key_enable_admin");
		mPassword  = (EditTextPreference) findPreference("key_password");
		mEnable    = (CheckBoxPreference) findPreference("key_enable_locker");
		mForceLock = (CheckBoxPreference) findPreference("key_force_lock");
		
		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(this, CustomDeviceAdminReceiver.class);
		
		((Preference) findPreference("contact")).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				LogReporting reporter = new LogReporting(PebbleLocker.this);
				reporter.collectAndSendLogs();
				
				return true;
			}
		});
		
		((Preference) findPreference("viewVersion")).setSummary(currentVersion());
		
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
				// hack because we need the new password to be 
				// sent in shared prefs before this method returns
				mPrefs.edit().putString("key_password", newValue.toString()).commit();
				
				doResetPassword((String) newValue);
				return true;
			}
		});
		
		mEnable.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if(Boolean.parseBoolean(newValue.toString()))
					showAlert("Pebble Locker is enabled, please set your password");
				else
					mPrefs.edit().putString("key_password", "").commit();
				
				return true;
			}
		});
		
		mStatusReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				PebbleLocker.this.updateStatus();
			}
		};
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	}
	
	public void onResume() {
		super.onResume();
		
		checkForRequiredPasswordByOtherApps();
		checkForPreviousPurchases();
		checkForActiveAdmin();
		updateStatus();
		
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(mStatusReceiver, new IntentFilter(ConnectionReceiver.STATUS_CHANGED_INTENT));
		
		if(!mPrefs.getString("key_password", "").equals("") && timeStamp < (System.currentTimeMillis() - 60000) &&
				mPrefs.getBoolean(ConnectionReceiver.LOCKED, true))
            requestPassword();
	}
	
	public void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mStatusReceiver);
	}
	
	/**
     * This is dangerous, so we prevent automated tests from doing it, and we
     * remind the user after we do it.
     */
    private void doResetPassword(String newPassword) {
        if (alertIfMonkey(this, "You can't reset my password, you are a monkey!")) {
            return;
        }
        
        new Locker(this, "[USER_TRIGGERED]").handleLocking(false);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message = getString(R.string.reset_password_warning, newPassword);
        builder.setMessage(message);
        builder.setPositiveButton("Don't Forget It!", null);
        builder.show();
    }
	
	private void enableOptions(boolean isEnabled) {
		mPassword.setEnabled(isEnabled);
		mEnable.setEnabled(isEnabled);
		mForceLock.setEnabled(isEnabled);
	}
	
	@SuppressLint("NewApi")
	private void checkForRequiredPasswordByOtherApps() {
		int encryptionStatus = -1;
		if(Build.VERSION.SDK_INT >= 11)
			encryptionStatus = mDPM.getStorageEncryptionStatus();
		
		boolean encryptionEnabled = 
				encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING || 
				encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE;
		
		if((mDPM.getPasswordMinimumLength(null) > 0 || encryptionEnabled) && !mPrefs.getBoolean("ignore_warning", false)) {
			String warning = "Your device is encrypted or there are other apps installed that require a password or pin to be set. " +
					         "Pebble Locker does not work on encrypted devices or with other apps that require a pin or password. " +
					         "If you wish to use Pebble Locker you will need to decrypt your device, disabled or uninstall any apps " +
					         "that require a pin or password or try to use it anyway. WARNING: Trying to use Pebble Locker " +
                             "in spite of this warning may cause you to lose all data on your device, you have been warned.";

            new AlertDialog.Builder(this)
                    .setMessage(warning)
                    .setPositiveButton("Do not use", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            PebbleLocker.this.finish();
                        }
                    })
                    .setNegativeButton("Use anyway", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mPrefs.edit().putBoolean("ignore_warning", true);
                        }
                    })
                    .show();
		}
	}
	
	private void checkForActiveAdmin() {
		if(mDPM.isAdminActive(mDeviceAdmin)) {
			mAdmin.setChecked(true);
			enableOptions(true);
		} else {
			mAdmin.setChecked(false);
			enableOptions(false);
		}
	}
	
	private void updateStatus() {
		int lockState = mPrefs.getInt(ConnectionReceiver.LOCK_STATE, ConnectionReceiver.AUTO);
		String statusMessage = "";
		
		switch(lockState) {
		case 0:
			if(mPrefs.getBoolean(ConnectionReceiver.LOCKED, false))
				statusMessage = "Locked";
			else
				statusMessage = "Unlocked";
			break;
		case 1:
			statusMessage = "Manually locked via Pebble watch app";
			break;
		case 2:
			statusMessage = "Manually unlocked via Pebble watch app";
			break;
		}
		
		Locker locker = new Locker(this, "[LOADING_STATUS]");
		String connectionStatus = "";
		
		if(locker.checkPebbleConnectionStatus())
			connectionStatus += "Pebble watch connected";
		else
			connectionStatus += "Pebble watch disconnected";
		
		if(mPrefs.getBoolean("donated", false)) {
			if(locker.isTrustedBluetoothDeviceConnected()) {
				String deviceNames = locker.getConnectedBluetoothDeviceNames();
				connectionStatus += "\n" + "Trusted bluetooth device connected \n\t" + deviceNames + "\n";
			} else
				connectionStatus += "\n" + "No trusted bluetooth device connected" + "\n";
			
			if(locker.isTrustedWifiConnected()) {
				connectionStatus += "Trusted WiFi network connected \n\t(" + locker.getConnectedWifiSsid() + ")";
			} else
				connectionStatus += "No trusted WiFi network connected";
		}
		
		mStatus.setTitle(statusMessage);
		mStatus.setSummary(connectionStatus);
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
	
	 /**
     * If the "user" is a monkey, post an alert and notify the caller.  This prevents automated
     * test frameworks from stumbling into annoying or dangerous operations.
     */
    private boolean alertIfMonkey(Context context, String string) {
        if (ActivityManager.isUserAMonkey()) {
            showAlert(string);
            return true;
        } else {
            return false;
        }
    }
    
    private String currentVersion() {
    	try {
			return ((PackageInfo) getPackageManager().getPackageInfo(getPackageName(), 0)).versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
    }
    
	@Override
	public void purchaseCanceled() {
		// noop
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
