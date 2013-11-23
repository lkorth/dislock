package com.lukekorth.pebblelocker;

import org.donations.DonationsActivity;

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
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.lukekorth.pebblelocker.util.IabHelper;
import com.lukekorth.pebblelocker.util.IabResult;
import com.lukekorth.pebblelocker.util.Inventory;

public class PebbleLocker extends PreferenceActivity {
	
	private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
	
	private IabHelper mHelper;
	
	private DevicePolicyManager mDPM;
	private ComponentName mDeviceAdmin;
	
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
		
		mAdmin = (CheckBoxPreference) findPreference("key_enable_admin");
		mPassword = (EditTextPreference) findPreference("key_password");
		mEnable = (CheckBoxPreference) findPreference("key_enable_locker");
		mForceLock = (CheckBoxPreference) findPreference("key_force_lock");
		
		mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mDeviceAdmin = new ComponentName(this, CustomDeviceAdminReceiver.class);
		
		((Preference) findPreference("donate")).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				PebbleLocker.this
					.startActivity(new Intent(PebbleLocker.this, DonationsActivity.class));
				return true;
			}
        });
		
		((Preference) findPreference("contact")).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				LogReporting reporter = new LogReporting(PebbleLocker.this);
				reporter.collectAndSendLogs();
				
				return true;
			}
		});
		
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
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	}
	
	public void onResume() {
		super.onResume();
		
		checkPurchaseHistory();
		
		if(mDPM.isAdminActive(mDeviceAdmin)) {
			mAdmin.setChecked(true);
			enableOptions(true);
			checkForRequiredPasswordByOtherApps();
		} else {
			mAdmin.setChecked(false);
			enableOptions(false);
		}
		
		if(!mPrefs.getString("key_password", "").equals("") && timeStamp < (System.currentTimeMillis() - 60000))
            requestPassword();
		
		mPrefs.edit().putBoolean("donated", true).commit();
	}
	
	public void onDestroy() {
		super.onDestroy();
		
		cleanupHelper();
	}
	
	private void checkPurchaseHistory() {
		cleanupHelper();
		
		mHelper = new IabHelper(this, getString(R.string.donations__google_pubkey));
		
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (result.isSuccess()) {
                	mHelper.queryInventoryAsync(mGotInventoryListener);	     
                } else {
                	showAlert("There was an issue checking for purchases, please contact the developer");
                }
            }
		});
	}
	
	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {			
			if (!result.isFailure()) {			
				if(inventory.hasPurchase("pebblelocker.donation.3") || inventory.hasPurchase("pebblelocker.donation.5") || 
					inventory.hasPurchase("pebblelocker.donation.10") || inventory.hasPurchase("pebblelocker.premium")) {
					mPrefs.edit().putBoolean("donated", true).commit();
					removeDonateOption();
				}
			} else {
				showAlert("There was an issue checking for purchases, please contact the developer");
			}
		}
	};
	
	private void cleanupHelper() {
		if(mHelper != null) {
			mHelper.dispose();
			mHelper = null;
		}
	}
	
	private void removeDonateOption() {
		if(findPreference("donateCategory") != null)
			((PreferenceScreen) findPreference("root")).removePreference(((PreferenceCategory) findPreference("donateCategory")));
	}
	
	/**
     * This is dangerous, so we prevent automated tests from doing it, and we
     * remind the user after we do it.
     */
    private void doResetPassword(String newPassword) {
        if (alertIfMonkey(this, "You can't reset my password, you are a monkey!")) {
            return;
        }
        
        new Locker(this).lockIfEnabled(false);
        
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
	
	private void checkForRequiredPasswordByOtherApps() {		
		if(mDPM.getPasswordMinimumLength(mDeviceAdmin) > 0) {
			showAlert("There are other apps installed that require a password or pin to be set on your device. " +
					"Pebble Locker cannot function unless these apps are disabled or uninstalled.", new OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					PebbleLocker.this.finish();
				}
			});
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
	
	private void showAlert(String message) {
		showAlert(message, null);
	}
	
	private void showAlert(String message, OnClickListener onClickListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", onClickListener);
        builder.show();
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
