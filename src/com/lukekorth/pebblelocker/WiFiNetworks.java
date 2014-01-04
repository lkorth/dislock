package com.lukekorth.pebblelocker;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Base64;

public class WiFiNetworks extends PremiumFeatures {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	public void onResume() {
		super.onResume();
		
		if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("donated", false)) {
			requirePremiumPurchase();
		} else {
			// Root
			PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

			// Inline preferences
			PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
			inlinePrefCat.setTitle("WiFi Networks");
			root.addPreference(inlinePrefCat);

			// List stored networks
			WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		    List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
		    
		    if(configs == null) {
		    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        builder.setMessage("Wifi must be enabled to get a list of saved networks, please enable it before using this feature");
		        builder.setCancelable(false);
		        builder.setPositiveButton("Open Wifi Settings", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
					}
		        });
		        builder.setNegativeButton("Cancel", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						WiFiNetworks.this.finish();
					}
		        });
		        builder.show();
		    } else {
			    for (WifiConfiguration config : configs) {
			    	String ssid = WiFiNetworks.stripQuotes(config.SSID);
			    	
			    	if(ssid != null && ssid != "") {
				    	// Checkbox preference
						CheckBoxPreference checkboxPref = new CheckBoxPreference(this);
						checkboxPref.setKey(WiFiNetworks.base64Encode(ssid));
						checkboxPref.setTitle(ssid);
						inlinePrefCat.addPreference(checkboxPref);
			    	}
			    }
	
				setPreferenceScreen(root);
		    }
		}
	}
	
	public void onDestroy() {
		super.onDestroy();
	}

	public static String stripQuotes(String input) {
		if(input != null && input.startsWith("\"") && input.endsWith("\""))
			return input.substring(1, input.length() - 1);
		else
			return input;
	}
	
	public static String base64Encode(String input) {
		return Base64.encodeToString(input.getBytes(), Base64.DEFAULT).trim();
	}
}
