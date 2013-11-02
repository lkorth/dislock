package com.lukekorth.pebblelocker;

import java.util.List;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class WiFiNetworks extends PreferenceActivity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Root
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

		// Inline preferences
		PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
		inlinePrefCat.setTitle("WiFi Networks");
		root.addPreference(inlinePrefCat);

		// List stored networks
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	    List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
	    for (WifiConfiguration config : configs) {
	    	String ssid = WiFiNetworks.stripQuotes(config.SSID);
	    	
	    	// Checkbox preference
			CheckBoxPreference checkboxPref = new CheckBoxPreference(this);
			checkboxPref.setKey(ssid);
			checkboxPref.setTitle(ssid);
			inlinePrefCat.addPreference(checkboxPref);
	    }

		setPreferenceScreen(root);
	}

	public static String stripQuotes(String input) {
		if(input.startsWith("\"") && input.endsWith("\""))
			return input.substring(1, input.length() - 1);
		else
			return input;
	}
	
}
