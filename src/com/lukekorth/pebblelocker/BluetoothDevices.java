package com.lukekorth.pebblelocker;

import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class BluetoothDevices extends PreferenceActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Root
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
		
		// Any Pebble
		PreferenceCategory pebble = new PreferenceCategory(this);
		pebble.setTitle("Pebble");
		root.addPreference(pebble);
		CheckBoxPreference pebblePref = new CheckBoxPreference(this);
		pebblePref.setKey("pebble");
		pebblePref.setTitle("Allow any Pebble to unlock");
		pebblePref.setChecked(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pebble", true));
		pebble.addPreference(pebblePref);

		// Inline preferences
		PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
		inlinePrefCat.setTitle("Bluetooth Devices");
		root.addPreference(inlinePrefCat);

		Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
		// If there are paired devices
		if (pairedDevices.size() > 0) {
			// Loop through paired devices
			for (BluetoothDevice device : pairedDevices) {
				// Checkbox preference
				CheckBoxPreference checkboxPref = new CheckBoxPreference(this);
				checkboxPref.setKey(device.getAddress());
				checkboxPref.setTitle(device.getName());
				inlinePrefCat.addPreference(checkboxPref);
			}
		}

		setPreferenceScreen(root);
	}
}