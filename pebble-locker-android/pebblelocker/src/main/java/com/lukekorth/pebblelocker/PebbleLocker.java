package com.lukekorth.pebblelocker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.lukekorth.pebblelocker.events.ActivityResumedEvent;
import com.lukekorth.pebblelocker.events.AuthenticationActivityResultEvent;
import com.lukekorth.pebblelocker.events.AuthenticationRequestEvent;
import com.lukekorth.pebblelocker.events.RequirePurchaseEvent;
import com.lukekorth.pebblelocker.helpers.CustomDeviceAdminReceiver;
import com.lukekorth.pebblelocker.receivers.BaseBroadcastReceiver;
import com.lukekorth.pebblelocker.services.AndroidWearDetectionService;
import com.lukekorth.pebblelocker.views.ScreenLockTypePreference;
import com.squareup.otto.Subscribe;

import org.slf4j.LoggerFactory;

import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;

public class PebbleLocker extends PremiumFeaturesActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 2;
    public static final int AUTHENTICATION_REQUEST = 3;

    private CheckBoxPreference mAdmin;
    private ScreenLockTypePreference mLockType;

    private DevicePolicyManager mDPM;
    private ComponentName mDeviceAdmin;
    private SharedPreferences mPrefs;
    private long mTimeStamp;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.main);

        mAdmin = (CheckBoxPreference) findPreference("key_enable_admin");
        mLockType = (ScreenLockTypePreference) findPreference("key_screen_lock_type");

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
                    ScreenLockType.changeToSlide(PebbleLocker.this);

                    return true;
                }
            }
        });

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        PebbleLockerApplication.getBus().register(this);
        startService(new Intent(this, AndroidWearDetectionService.class));

        AppRate.with(this)
                .text(R.string.rate)
                .initialLaunchCount(3)
                .retryPolicy(RetryPolicy.EXPONENTIAL)
                .checkAndShow();
	}

    @Subscribe
    public void onRequirePurchaseEvent(RequirePurchaseEvent event) {
        requirePurchase();
    }

    @Subscribe
    public void onAuthenticationRequestEvent(AuthenticationRequestEvent event) {
        startActivityForResult(event.params, AUTHENTICATION_REQUEST);
    }

	public void onResume() {
		super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        PebbleLockerApplication.getBus().post(new ActivityResumedEvent());

		checkForRequiredPasswordByOtherApps();
		checkForActiveAdmin();

		if(!TextUtils.isEmpty(mPrefs.getString("key_password", "")) &&
                ScreenLockType.getCurrent(this) != ScreenLockType.SLIDE &&
                mTimeStamp < (System.currentTimeMillis() - 60000) &&
				mPrefs.getBoolean(BaseBroadcastReceiver.LOCKED, true)) {
            Intent intent = new Intent(this, AuthenticationActivity.class)
                    .putExtra(AuthenticationActivity.AUTHENTICATION_TYPE_KEY, AuthenticationActivity.AUTHENTICATE);
            startActivityForResult(intent, AUTHENTICATION_REQUEST);
        } else {
            int response = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            if (response != ConnectionResult.SUCCESS) {
                GooglePlayServicesUtil.getErrorDialog(response, this, REQUEST_GOOGLE_PLAY_SERVICES)
                        .show();
            }
        }
	}

    @Override
	public void onPause() {
		super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        PebbleLockerApplication.getBus().unregister(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AUTHENTICATION_REQUEST) {
            PebbleLockerApplication.getBus().post(new AuthenticationActivityResultEvent(resultCode, data));
            if (resultCode == RESULT_OK) {
                mTimeStamp = System.currentTimeMillis();
            }
        }
    }

    private void checkForActiveAdmin() {
        if(mDPM.isAdminActive(mDeviceAdmin)) {
            mAdmin.setChecked(true);
        } else {
            mAdmin.setChecked(false);
        }
    }

    private void enableOptions(boolean enabled) {
        mLockType.setEnabled(enabled);
    }

	@SuppressLint("NewApi")
	private void checkForRequiredPasswordByOtherApps() {
		int encryptionStatus = mDPM.getStorageEncryptionStatus();

		boolean encryptionEnabled = (
                encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING ||
				encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE
        );

		if((mDPM.getPasswordMinimumLength(null) > 0 || encryptionEnabled) && !mPrefs.getBoolean("ignore_warning", false)) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.incompatable_warning)
                    .setCancelable(false)
                    .setPositiveButton(R.string.do_not_use, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            PebbleLocker.this.finish();
                        }
                    })
                    .setNegativeButton(R.string.use_anyway, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mPrefs.edit().putBoolean("ignore_warning", true).apply();
                        }
                    })
                    .show();
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
            message = "User changed " + key + " to " + sharedPreferences.getAll().get(key);
        }

        LoggerFactory.getLogger("Settings_Changed").debug(message);
    }

}
