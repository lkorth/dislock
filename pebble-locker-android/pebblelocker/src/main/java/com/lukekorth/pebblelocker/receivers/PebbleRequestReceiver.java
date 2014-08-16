package com.lukekorth.pebblelocker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.lukekorth.pebblelocker.LockState;
import com.lukekorth.pebblelocker.Locker;
import com.lukekorth.pebblelocker.PebbleLockerApplication;
import com.lukekorth.pebblelocker.logging.Logger;

import org.json.JSONException;

import java.util.UUID;

public class PebbleRequestReceiver extends BroadcastReceiver {

	private final static UUID PEBBLE_APP_UUID = UUID.fromString("7B1CAB5D-1E4B-4F4C-B253-EEB2834D4D67");
	private final static int GET_STATE = 0x0;
	private final static int SET_STATE = 0x1;

	@Override
	public void onReceive(Context context, Intent intent) {
        Logger logger = new Logger(context, PebbleLockerApplication.getUniqueTag());

        UUID uuid = (UUID) intent.getSerializableExtra(Constants.APP_UUID);
        if(uuid == null || uuid.compareTo(PEBBLE_APP_UUID) != 0) {
            logger.log("UUID was not present in request or didn't match");
            return;
        }

        int transactionId = intent.getIntExtra(Constants.TRANSACTION_ID, Integer.MIN_VALUE);
        String data = intent.getStringExtra(Constants.MSG_DATA);

        logger.log("Pebble request txn id: " + transactionId);
        logger.log("Pebble raw request: " + data);

        if (transactionId == Integer.MIN_VALUE) {
            return;
        }

        transactionId &= 0xff;
        if (transactionId >= 0 && transactionId <= 255 && data != null && data.length() != 0) {
            PebbleKit.sendAckToPebble(context, transactionId);

            try {
                PebbleDictionary pebbleDictionary = PebbleDictionary.fromJson(data);
                PebbleDictionary responseDictionary = new PebbleDictionary();

                if (!new Locker(context, logger.getTag()).enabled()) {
                    responseDictionary.addInt32(SET_STATE, 3);
                } else if (pebbleDictionary.getInteger(GET_STATE) != null) {
                    responseDictionary.addInt32(SET_STATE, getState(context, logger));
                } else if (pebbleDictionary.getInteger(SET_STATE) != null) {
                    int state = (int) ((long) pebbleDictionary.getInteger(SET_STATE));
                    responseDictionary.addInt32(SET_STATE,
                            LockState.setCurrentState(context, logger, true, state).getState());
                }

                if (responseDictionary.size() > 0) {
                    logger.log("Sending response to Pebble: " + responseDictionary.toJsonString());
                    PebbleKit.sendDataToPebble(context, PEBBLE_APP_UUID, responseDictionary);
                }
            } catch (JSONException e) {
                logger.log("JSON exception while handling Pebble request");
            }
        }
	}

	private int getState(Context context, Logger logger) {
        LockState state = LockState.getCurrentState(context);
        logger.log("Getting current lock state: " + state.getDisplayName());

		return state.getState();
	}

}