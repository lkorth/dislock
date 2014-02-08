package com.lukekorth.pebblelocker;

import java.util.UUID;

import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class PebbleRequestReceiver extends BroadcastReceiver {

	private final static UUID PEBBLE_APP_UUID = UUID.fromString("7B1CAB5D-1E4B-4F4C-B253-EEB2834D4D67");
	private final static int GET_STATE = 0x0;
	private final static int SET_STATE = 0x1;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (((UUID) intent.getSerializableExtra(Constants.APP_UUID)) == PEBBLE_APP_UUID) {
			int transactionId = intent.getIntExtra(Constants.TRANSACTION_ID, Integer.MIN_VALUE);
			String data = intent.getStringExtra(Constants.MSG_DATA);

			if (transactionId == Integer.MIN_VALUE)
				return;

			transactionId &= 0xff;
			if (transactionId >= 0 && transactionId <= 255 && data != null && data.length() != 0) {
				PebbleKit.sendAckToPebble(context, transactionId);

				try {
					PebbleDictionary pebbleDictionary = PebbleDictionary.fromJson(data);
					PebbleDictionary responseDictionary = new PebbleDictionary();

					if (pebbleDictionary.getInteger(GET_STATE) != null)
						responseDictionary.addInt32(SET_STATE, getState(context));
					else if (pebbleDictionary.getInteger(SET_STATE) != null)
						responseDictionary.addInt32(SET_STATE, setState(context, (int) ((long) pebbleDictionary.getInteger(SET_STATE))));

					if (responseDictionary.size() > 0)
						PebbleKit.sendDataToPebble(context, PEBBLE_APP_UUID, responseDictionary);
				} catch (JSONException e) {
					// noop
				}
			}
		}
	}

	private int getState(Context context) {
		return getSharedPrefs(context).getInt(ConnectionReceiver.LOCK_STATE, ConnectionReceiver.AUTO);
	}

	private int setState(Context context, int state) {
		getSharedPrefs(context).edit().putInt(ConnectionReceiver.LOCK_STATE, state).commit();

		switch (state) {
		case 0:
			new Locker(context, "[MANUAL]").handleLocking();
			break;
		case 1:
			new Locker(context, "[MANUAL]").lock();
			break;
		case 2:
			new Locker(context, "[MANUAL]").unlock();
			break;
		}

		return state;
	}

	private SharedPreferences getSharedPrefs(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
}
