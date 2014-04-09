package com.lukekorth.pebblelocker;

import com.lukekorth.pebblelocker.util.IabHelper;
import com.lukekorth.pebblelocker.util.IabHelper.OnIabSetupFinishedListener;
import com.lukekorth.pebblelocker.util.IabResult;
import com.lukekorth.pebblelocker.util.Inventory;
import com.lukekorth.pebblelocker.util.Purchase;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import java.util.UUID;

public class PremiumFeatures extends PreferenceActivity implements IabHelper.QueryInventoryFinishedListener, 
																   IabHelper.OnIabPurchaseFinishedListener, 
																   OnIabSetupFinishedListener{

	private IabHelper mHelper;
	private boolean mCheckForPurchases = false;
    private boolean mInitiatePurchase = false;

    private Logger mLogger;
    private String mTag;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLogger = new Logger(this);
        mTag = "[" + UUID.randomUUID().toString().split("-")[1] + "]";
    }

    private void initialize() {
        disposeHelper();
        mHelper = new IabHelper(this, getString(R.string.billing_public_key));
        mLogger.log(mTag, "Starting billing helper setup");
        mHelper.startSetup(this);
    }

	public void requirePremiumPurchase() {
		new AlertDialog.Builder(this)
			.setMessage("This option requires the premium version of the app, press ok to purchase")
		    .setCancelable(false)
		    .setPositiveButton("Ok", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int arg1) {
					dialog.dismiss();
					initiatePurchase();
				}
		    })
			.setNegativeButton("Cancel", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.show();
	}
	
	public void checkForPreviousPurchases() {
        if(!mCheckForPurchases) {
            mCheckForPurchases = true;
            initialize();
        } else {
            mCheckForPurchases = false;
			mHelper.queryInventoryAsync(this);
        }
	}
	
	private void purchaseSuccessful() {
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("donated", true).commit();
	}

    public boolean hasPurchased() {
        if(BuildConfig.DEBUG)
            return true;

        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("donated", false);
    }

	private void initiatePurchase() {
        if(!mInitiatePurchase) {
            mInitiatePurchase = true;
            initialize();
        } else {
            mInitiatePurchase = false;
            mHelper.launchPurchaseFlow(this, "pebblelocker.premium", 1, this, "premium");
        }
	}

	@Override
	public void onIabSetupFinished(IabResult result) {
        mLogger.log(mTag, "Helper setup finished, result success: " + result.isSuccess() + " message: " + result.getMessage());

		if(result.isSuccess()) {
			if(mCheckForPurchases)
			    checkForPreviousPurchases();
            else if(mInitiatePurchase)
                initiatePurchase();
		}
	}
	
	@Override
	public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
        mLogger.log(mTag, "Query inventory finished, result success: " + result.isSuccess() + " message: " + result.getMessage());

		if (result.isSuccess()) {
			if(inventory.hasPurchase("pebblelocker.donation.3") || inventory.hasPurchase("pebblelocker.donation.5") || 
				inventory.hasPurchase("pebblelocker.donation.10") || inventory.hasPurchase("pebblelocker.premium")) {
				purchaseSuccessful();
			}
		} else {
			showAlert("There was an issue checking for purchases, please contact the developer");
		}
	}
	
	@Override
	public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
        mLogger.log(mTag, "Purchase finished, result success: " + result.isSuccess() + " message: " + result.getMessage());

		if (result.isSuccess()) {
            if (purchase.getSku().equals("pebblelocker.premium"))
                purchaseSuccessful();
		} else {
            new AlertDialog.Builder(PremiumFeatures.this)
                .setMessage("There was an error purchasing, please try again later")
                .setCancelable(false)
                .setPositiveButton("Ok", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
        }
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		disposeHelper();
	}

    private void disposeHelper() {
        if (mHelper != null)
            mHelper.dispose();
        mHelper = null;
    }
	
	public void showAlert(String message) {
		showAlert(message, null);
	}
	
	public void showAlert(String message, OnClickListener onClickListener) {
		new AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("Ok", onClickListener)
            .show();
	}
}