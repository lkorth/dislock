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
import android.net.Uri;
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

import com.lukekorth.pebblelocker.helpers.BluetoothHelper;
import com.lukekorth.pebblelocker.helpers.PebbleHelper;
import com.lukekorth.pebblelocker.helpers.WifiHelper;

import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;

public class PebbleLocker extends PremiumFeatures implements OnPreferenceClickListener {

	private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
	
	private DevicePolicyManager mDPM;
	private ComponentName mDeviceAdmin;
	
	private Preference         mStatus;
	private CheckBoxPreference mAdmin;
	private EditTextPreference mPassword;
	private CheckBoxPreference mEnable;
	private CheckBoxPreference mForceLock;
	private Preference         mWatchApp;
	
	private SharedPreferences mPrefs;
	
	private BroadcastReceiver mStatusReceiver;
	
	private AlertDialog requirePassword;
	private long timeStamp;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.main);
		
		mStatus    = findPreference("visible_status");
		mAdmin     = (CheckBoxPreference) findPreference("key_enable_admin");
		mPassword  = (EditTextPreference) findPreference("key_password");
		mEnable    = (CheckBoxPreference) findPreference("key_enable_locker");
		mForceLock = (CheckBoxPreference) findPreference("key_force_lock");
		mWatchApp  = findPreference("pebble_watch_app");
		
		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(this, CustomDeviceAdminReceiver.class);

        findPreference("other_bluetooth_devices").setOnPreferenceClickListener(this);
        findPreference("wifi").setOnPreferenceClickListener(this);
        findPreference("contact").setOnPreferenceClickListener(this);
        findPreference("viewVersion").setSummary(BuildConfig.VERSION_NAME);
		
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
		
		mWatchApp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if(!PebbleLocker.this.hasPurchased()) {
                    requirePremiumPurchase();
                } else {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://ofkorth.net/pebble/pebble-locker-1.pbw"));
				    startActivity(intent);
				}

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

        AppRate.with(this)
                .text("Rate Pebble Locker")
                .initialLaunchCount(3)
                .retryPolicy(RetryPolicy.EXPONENTIAL)
                .checkAndShow();
	}
	
	public void onResume() {
		super.onResume();
		
		checkForRequiredPasswordByOtherApps();
		checkForPreviousPurchases();
		checkForActiveAdmin();
		updateStatus();
		
		LocalBroadcastManager.getInstance(this)
			.registerReceiver(mStatusReceiver, new IntentFilter(ConnectionReceiver.STATUS_CHANGED_INTENT));
		
		if(!mPrefs.getString("key_password", "").equals("") &&
                timeStamp < (System.currentTimeMillis() - 60000) &&
				mPrefs.getBoolean(ConnectionReceiver.LOCKED, true)) {
            requestPassword();
        }
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
        if (alertIfMonkey("You can't reset my password, you are a monkey!")) {
            return;
        }

        // hack because we need the new password to be
        // sent in shared prefs before this method returns
        mPrefs.edit().putString("key_password", newPassword).commit();

        if(newPassword.length() == 0) {
            new Logger(this).log("[USER]", "Password was set to empty");
        }

        new Locker(this, "[USER_TRIGGERED]").handleLocking(false);
        
        new AlertDialog.Builder(this)
            .setMessage(getString(R.string.reset_password_warning, newPassword))
            .setPositiveButton("Don't Forget It!", null)
            .show();
    }
	
	private void enableOptions(boolean isEnabled) {
		mPassword.setEnabled(isEnabled);
		mEnable.setEnabled(isEnabled);
		mForceLock.setEnabled(isEnabled);
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
                    .setPositiveButton("Do not use", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            PebbleLocker.this.finish();
                        }
                    })
                    .setNegativeButton("Use anyway", new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mPrefs.edit().putBoolean("ignore_warning", true).commit();
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
		case ConnectionReceiver.AUTO:
			if(mPrefs.getBoolean(ConnectionReceiver.LOCKED, false))
				statusMessage = "Locked (Automatic)";
			else
				statusMessage = "Unlocked (Automatic)";
			break;
		case ConnectionReceiver.MANUAL_UNLOCKED:
			statusMessage = "Manually unlocked via watch app";
			break;
		case ConnectionReceiver.MANUAL_LOCKED:
			statusMessage = "Manually locked via watch app";
			break;
		}
		
        Logger logger = new Logger(this, "[LOADING-STATUS]");
        StringBuilder connectionStatusBuilder = new StringBuilder();
        connectionStatusBuilder.append(new PebbleHelper(this, logger).getConnectionStatus());

		if(mPrefs.getBoolean("donated", false)) {
            connectionStatusBuilder.append(new BluetoothHelper(this, logger).getConnectionStatus());
            connectionStatusBuilder.append(new WifiHelper(this, logger).getConnectionStatus());
		}
		
		mStatus.setTitle(statusMessage);
		mStatus.setSummary(connectionStatusBuilder.toString());
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
    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equals("other_bluetooth_devices") || preference.getKey().equals("wifi")) {
            if(!hasPurchased()) {
                requirePremiumPurchase();
            } else {
                if(preference.getKey().equals("other_bluetooth_devices")) {
                    startActivity(new Intent(this, BluetoothDevices.class));
                } else {
                    startActivity(new Intent(this, WiFiNetworks.class));
                }
            }
        } else if(preference.getKey().equals("contact")) {
            new LogReporting(PebbleLocker.this).collectAndSendLogs();
        }

        return true;
    }

	/**
     * If the "user" is a monkey, post an alert and notify the caller.  This prevents automated
     * test frameworks from stumbling into annoying or dangerous operations.
     */
    private boolean alertIfMonkey(String string) {
        if (ActivityManager.isUserAMonkey()) {
            showAlert(string);
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
