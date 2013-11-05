package com.lukekorth.pebblelocker;

import com.lukekorth.pebblelocker.util.IabHelper;
import com.lukekorth.pebblelocker.util.IabResult;
import com.lukekorth.pebblelocker.util.Purchase;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PremiumFeatures extends PreferenceActivity {
	
	private IabHelper mHelper;
	
	private String payload;
	private int request;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mHelper = new IabHelper(this, getString(R.string.donations__google_pubkey));
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {}
		});     
	}
	
	public void configureOptions(String payload, int request) {
		this.payload = payload;
		this.request = request;
	}
	
	public void requirePremiumPurchase() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("These options require the premium version of the app, press ok to purchase");
        builder.setCancelable(false);
        builder.setPositiveButton("Ok", new OnClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int arg1) {          
				mHelper.launchPurchaseFlow(PremiumFeatures.this, "pebblelocker.premium", request,  
				        mPurchaseFinishedListener, payload);               
			}
        });
        builder.setNegativeButton("Cancel", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PremiumFeatures.this.finish();
			}
        });
        builder.show();
	}
	
	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {      
	     public void onIabPurchaseFinished(IabResult result, Purchase purchase) {                                                               
	         if (mHelper == null) 
	        	 return;                                                                                     
	                                                                                                                          
	         if (result.isFailure()) {                                                                                      
	        	AlertDialog.Builder builder = new AlertDialog.Builder(PremiumFeatures.this);
	 	        builder.setMessage("There was an error purchasing, please try again later");
	 	        builder.setCancelable(false);
	 	        builder.setPositiveButton("Ok", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						PremiumFeatures.this.finish();	
					}
	 	        });
	 	        builder.show();
                                                                                       
	            return;                                                                                                      
	         }                                                                                                                                                                                     
	                                                                                                                          
	         if (purchase.getSku().equals("pebblelocker.premium"))                                           
	             PreferenceManager.getDefaultSharedPreferences(PremiumFeatures.this).edit()
	             	.putBoolean("donated", true).commit();                                                                                                                                                                                                                                                   
	     }                                                                                                                    
	 };                                                                                                                       

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mHelper != null) {
			mHelper.dispose();
			mHelper = null;
		}
	}  

}
