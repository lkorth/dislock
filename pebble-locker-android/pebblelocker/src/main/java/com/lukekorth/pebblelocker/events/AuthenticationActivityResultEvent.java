package com.lukekorth.pebblelocker.events;

import android.content.Intent;

public class AuthenticationActivityResultEvent {

    public int resultCode;
    public Intent response;

    public AuthenticationActivityResultEvent(int resultCode, Intent response) {
        this.resultCode = resultCode;
        this.response = response;
    }
}
