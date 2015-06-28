package com.lukekorth.pebblelocker;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.lukekorth.pebblelocker.events.ActivityResumedEvent;
import com.lukekorth.pebblelocker.events.RequirePurchaseEvent;
import com.lukekorth.pebblelocker.receivers.DislockDeviceAdminReciever;
import com.lukekorth.pebblelocker.services.AndroidWearDetectionService;
import com.squareup.otto.Subscribe;

import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;

public class PebbleLocker extends PremiumFeaturesActivity implements Preference.OnPreferenceChangeListener {

    private DevicePolicyManager mDPM;
    private ComponentName mDeviceAdmin;
    private CheckBoxPreference mForceLockPreference;

    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.main);

        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDeviceAdmin = new ComponentName(this, DislockDeviceAdminReciever.class);
        mForceLockPreference = (CheckBoxPreference) findPreference("key_force_lock");
        mForceLockPreference.setOnPreferenceChangeListener(this);

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

	public void onResume() {
		super.onResume();

        PebbleLockerApplication.getBus().post(new ActivityResumedEvent());

        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (response != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, response, 1).show();
        }

        if(mDPM.isAdminActive(mDeviceAdmin)) {
            mForceLockPreference.setChecked(true);
        } else {
            mForceLockPreference.setChecked(false);
        }
	}

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ((Boolean) newValue) {
            showAlert(R.string.device_admin_switch, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Launch the activity to have the user enable our admin.
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, R.string.device_admin_explanation);
                    startActivityForResult(intent, 1);
                }
            });

            // don't update checkbox until we're really active
            return false;
        } else {
            mDPM.removeActiveAdmin(mDeviceAdmin);
            return true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PebbleLockerApplication.getBus().unregister(this);
    }
}
