package com.lukekorth.pebblelocker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.lukekorth.pebblelocker.billing.IabHelper;
import com.lukekorth.pebblelocker.billing.IabHelper.OnIabSetupFinishedListener;
import com.lukekorth.pebblelocker.billing.IabResult;
import com.lukekorth.pebblelocker.billing.Inventory;
import com.lukekorth.pebblelocker.billing.Purchase;

import java.util.UUID;

public class PremiumFeatures extends PreferenceActivity {

    private Logger mLogger;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogger = new Logger(this, "[" + UUID.randomUUID().toString().split("-")[1] + "]");
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

	private void setPurchaseSuccessful() {
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("donated", true).commit();
	}

    public boolean hasPurchased() {
        if(BuildConfig.DEBUG)
            return true;

        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("donated", false);
    }

	private void initiatePurchase() {
        final IabHelper iabHelper = new IabHelper(this, getString(R.string.billing_public_key));
        mLogger.log("Starting billing helper to make a purchase");
        iabHelper.startSetup(new OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                mLogger.log("Helper setup finished, result success: " + result.isSuccess() + " message: " + result.getMessage());

                if(result.isSuccess()) {
                    iabHelper.launchPurchaseFlow(PremiumFeatures.this, "pebblelocker.premium", 1,
                            new IabHelper.OnIabPurchaseFinishedListener() {
                                @Override
                                public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                                    mLogger.log("Purchase finished, result success: " + result.isSuccess() + " message: " + result.getMessage());

                                    if (result.isSuccess()) {
                                        if (purchase.getSku().equals("pebblelocker.premium")) {
                                            setPurchaseSuccessful();
                                        }
                                    } else {
                                        showAlert("There was an error completing your purchase, " +
                                                "please try again later. If the problem persists, please " +
                                                "contact the developer");
                                    }

                                }
                            }, "premium");
                } else {
                    PremiumFeatures.this.showAlert("We were unable complete your purchase request, " +
                            "please try again later. If this problem persists, please contact the developer.");
                }
            }
        });
	}

    public void checkForPreviousPurchases() {
        final IabHelper iabHelper = new IabHelper(this, getString(R.string.billing_public_key));
        mLogger.log("Starting billing helper to check for purchases");
        iabHelper.startSetup(new OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                mLogger.log("Helper setup finished, result success: " + result.isSuccess() + " message: " + result.getMessage());

                if(result.isSuccess()) {
                    iabHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                            mLogger.log("Query inventory finished, result success: " + result.isSuccess() + " message: " + result.getMessage());

                            if (result.isSuccess()) {
                                if (inventory.hasPurchase("pebblelocker.donation.3") ||
                                        inventory.hasPurchase("pebblelocker.donation.5") ||
                                        inventory.hasPurchase("pebblelocker.donation.10") ||
                                        inventory.hasPurchase("pebblelocker.premium")) {
                                    setPurchaseSuccessful();
                                } else {
                                    mLogger.log("User has not purchased any of the qualifying items");
                                }
                            }
                        }
                    });
                }
            }
        });
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