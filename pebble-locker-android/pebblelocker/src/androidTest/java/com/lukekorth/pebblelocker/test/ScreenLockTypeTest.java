package com.lukekorth.pebblelocker.test;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.lukekorth.pebblelocker.R;
import com.lukekorth.pebblelocker.ScreenLockType;

public class ScreenLockTypeTest extends AndroidTestCase {

    @Override
    public void tearDown() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().clear().commit();
    }

    public void testEnumReturnsCorrectValues() {
        assertEquals("0", ScreenLockType.SLIDE.getType());
        assertEquals(R.string.slide, ScreenLockType.SLIDE.getDescription());

        assertEquals("1", ScreenLockType.PIN.getType());
        assertEquals(R.string.pin, ScreenLockType.PIN.getDescription());

        assertEquals("2", ScreenLockType.PASSWORD.getType());
        assertEquals(R.string.password, ScreenLockType.PASSWORD.getDescription());
    }

    public void testGetCurrentReturnsCorrectType() {
        setType(ScreenLockType.SLIDE);
        ScreenLockType type = ScreenLockType.getCurrent(getContext());
        assertEquals(ScreenLockType.SLIDE, type);

        setType(ScreenLockType.PIN);
        type = ScreenLockType.getCurrent(getContext());
        assertEquals(ScreenLockType.PIN, type);

        setType(ScreenLockType.PASSWORD);
        type = ScreenLockType.getCurrent(getContext());
        assertEquals(ScreenLockType.PASSWORD, type);
    }

    public void testGetCurrentDefaultsToSlide() {
        ScreenLockType type = ScreenLockType.getCurrent(getContext());
        assertEquals(ScreenLockType.SLIDE, type);
    }

    public void testGetCurrentDescriptionReturnsCorrectString() {
        setType(ScreenLockType.PIN);

        String description = ScreenLockType.getCurrentDescription(getContext());

        assertEquals(getContext().getString(R.string.pin), description);
    }

    public void testSetCurrentSetsCorrectly() {
        setType(ScreenLockType.SLIDE);

        ScreenLockType.setCurrent(getContext(), ScreenLockType.PIN);

        assertEquals(ScreenLockType.PIN, ScreenLockType.getCurrent(getContext()));
    }

    public void testChangeToSlideSetsCorrectlyAndClearsPassword() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        setType(ScreenLockType.PIN);
        prefs.edit().putString("key_password", "password").apply();

        ScreenLockType.changeToSlide(getContext());

        assertEquals(ScreenLockType.SLIDE, ScreenLockType.getCurrent(getContext()));
        assertNull(prefs.getString("key_password", null));
    }

    /* helpers */
    private void setType(ScreenLockType type) {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit()
                .putString(ScreenLockType.SCREEN_LOCK_TYPE_KEY, type.getType())
                .commit();
    }
}
