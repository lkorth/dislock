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

public class PremiumFeatures extends PreferenceActivity implements IabHelper.QueryInventoryFinishedListener, 
																   IabHelper.OnIabPurchaseFinishedListener, 
																   OnIabSetupFinishedListener{

	private IabHelper mHelper;
	private boolean mInitialized = false; 
	private boolean mCheckForPurchases = false;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHelper = new IabHelper(this, getString(R.string.billing_public_key));
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
					purchaseCanceled();
				}
			})
			.show();
	}
	
	public void checkForPreviousPurchases() {
		if(mInitialized)
			mHelper.queryInventoryAsync(this);
		else
			mCheckForPurchases = true;
	}
	
	public void purchaseCanceled() {
		finish();
	}
	
	public void purchaseSuccessful() {
		PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("donated", true).commit();
	}

	private void initiatePurchase() {
		mHelper.launchPurchaseFlow(this, "pebblelocker.premium", 1, this, "premium");
	}

	@Override
	public void onIabSetupFinished(IabResult result) {
		if(result.isSuccess()) {
			if(mCheckForPurchases) {
				mCheckForPurchases = false;
				mHelper.queryInventoryAsync(this);
			}
		}
	}
	
	@Override
	public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
		if (!result.isFailure()) {			
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
		if (result.isFailure()) {
			new AlertDialog.Builder(PremiumFeatures.this)
				.setMessage("There was an error purchasing, please try again later")
				.setCancelable(false)
				.setPositiveButton("Ok", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						purchaseCanceled();
					}
				})
				.show();

			return;
		}

		if (purchase.getSku().equals("pebblelocker.premium"))
			purchaseSuccessful();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mHelper != null)
			mHelper.dispose();
	}
	
	public void showAlert(String message) {
		showAlert(message, null);
	}
	
	public void showAlert(String message, OnClickListener onClickListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("Ok", onClickListener);
        builder.show();
	}
}