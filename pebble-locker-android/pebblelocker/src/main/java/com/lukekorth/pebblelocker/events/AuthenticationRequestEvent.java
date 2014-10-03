package com.lukekorth.pebblelocker.events;

import android.content.Intent;

public class AuthenticationRequestEvent {

    public Intent params;

    public AuthenticationRequestEvent(Intent intent) {
        params = intent;
    }
}
