package com.lukekorth.pebblelocker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.lukekorth.pebblelocker.billing.IabHelper;
import com.lukekorth.pebblelocker.billing.IabHelper.OnIabSetupFinishedListener;
import com.lukekorth.pebblelocker.billing.IabResult;
import com.lukekorth.pebblelocker.billing.Inventory;
import com.lukekorth.pebblelocker.billing.Purchase;
import com.lukekorth.pebblelocker.helpers.Settings;
import com.lukekorth.pebblelocker.models.AndroidWearDevices;
import com.lukekorth.pebblelocker.models.BluetoothDevices;
import com.lukekorth.pebblelocker.models.WifiNetworks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PremiumFeaturesActivity extends PreferenceActivity implements OnIabSetupFinishedListener,
    IabHelper.OnIabPurchaseFinishedListener, IabHelper.QueryInventoryFinishedListener {

    private static final String BILLING_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmKCACsyFRONCbAFoNV+e1h+o9AXUTgiEZPDV24aaWyhTAixwK+sEHtcjlmElYHSO/9a6HBXa03JCsSNWc9Ngqpj3fTiZ+IZQ9HRLUt4mDWVh0/fKSiTfS9bEjGx8oHnteHTe+BvsTkkM5OR00n0zG+++a+948P8pv1B6f/QvJ1y9mgGyy5LR0CXVEnZrQJonqofPxBDaPvbNYNTGFkNSEepDk5xRfIfDpbftvgnDBtJya1BWc1aGksJURUyFZntj7fin3i05PbIiTztGWOKCGubegcJt+HIl304nvBZCbUVZpiSaOqWDQZO6kmloMoy5vC1GP1/WaN554XKNcDZ0rwIDAQAB";
    private static final String TAG = "Premium_Features";

    private enum IAB_STATUS {
        SETTING_UP, SET_UP, FAILED
    }

    private Logger mLogger;
    private IabHelper mIabHelper;
    private IAB_STATUS mIabStatus;
    private boolean mIabOperationInProgress;
    private int mEnabledDevices;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLogger = LoggerFactory.getLogger(TAG);
        mIabHelper = new IabHelper(this, BILLING_PUBLIC_KEY);
        mIabHelper.startSetup(this);
        mIabStatus = IAB_STATUS.SETTING_UP;

        getEnabledDevices();
    }

    protected void getEnabledDevices() {
        mEnabledDevices = 0;

        if (Settings.isPebbleEnabled(this)) {
            mEnabledDevices++;
        }

        mEnabledDevices += BluetoothDevices.countTrustedDevices();
        mEnabledDevices += AndroidWearDevices.countTrustedDevices();
        mEnabledDevices += WifiNetworks.countTrustedDevices();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIabHelper.dispose();
    }

    public boolean isPurchaseRequired() {
        if (!Settings.hasPurchased(this) && mEnabledDevices >= 1) {
            requirePurchase();
            return true;
        }
        return false;
    }

    public void requirePurchase() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.purchase_required)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                        initiatePurchase();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @Override
    public void onIabSetupFinished(IabResult result) {
        mLogger.debug("IabHelper setup finished, result success: " + result.isSuccess() +
                " message: " + result.getMessage());

        if (result.isSuccess()) {
            mIabStatus = IAB_STATUS.SET_UP;
            mIabOperationInProgress = true;
            mLogger.debug("Checking for previous purchases");
            mIabHelper.queryInventoryAsync(this);
        } else {
            mIabStatus = IAB_STATUS.FAILED;
        }
    }

    @Override
    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
        mLogger.debug("Inventory query finished, result success: " + result.isSuccess() +
                " message: " + result.getMessage());

        mIabOperationInProgress = false;

        if (result.isSuccess()) {
            if (inv.hasPurchase("pebblelocker.donation.3") ||
                    inv.hasPurchase("pebblelocker.donation.5") ||
                    inv.hasPurchase("pebblelocker.donation.10") ||
                    inv.hasPurchase("pebblelocker.premium")) {
                Settings.setPurchased(this, true);
            } else {
                mLogger.debug("User has not purchased any of the qualifying items");
                Settings.setPurchased(this, false);
            }
        }
    }

    @Override
    public void onIabPurchaseFinished(IabResult result, Purchase info) {
        mLogger.debug("Purchase finished, result success: " + result.isSuccess() + " message: " + result.getMessage());

        mIabOperationInProgress = false;

        if (result.isSuccess()) {
            if (info.getSku().equals("pebblelocker.premium")) {
                Settings.setPurchased(this, true);
            }
        } else {
            showAlert(R.string.iab_error);
        }
    }

    private void initiatePurchase() {
        mLogger.debug("Attempting purchase");

        if (mIabStatus == IAB_STATUS.SET_UP) {
            if (mIabOperationInProgress) {
                mLogger.debug("Another IAB task is in progress");
                showAlert(R.string.iab_setting_up);
            } else {
                mIabOperationInProgress = true;
                mIabHelper.launchPurchaseFlow(this, "pebblelocker.premium", 1, this, "premium");
            }
        } else if (mIabStatus == IAB_STATUS.SETTING_UP) {
            mLogger.debug("Still waiting for IAB setup to complete");
            showAlert(R.string.iab_setting_up);
        } else if (mIabStatus == IAB_STATUS.FAILED) {
           showAlert(R.string.iab_setup_error);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mIabHelper.handleActivityResult(requestCode, resultCode, data);
    }

	public void showAlert(int message) {
		showAlert(message, null);
	}
	
	public void showAlert(int message, OnClickListener onClickListener) {
		new AlertDialog.Builder(this)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Ok", onClickListener)
            .show();
	}
}