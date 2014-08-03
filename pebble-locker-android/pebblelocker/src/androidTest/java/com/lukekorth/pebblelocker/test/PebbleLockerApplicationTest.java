package com.lukekorth.pebblelocker.test;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ApplicationTestCase;

import com.lukekorth.pebblelocker.BuildConfig;
import com.lukekorth.pebblelocker.PebbleLockerApplication;

public class PebbleLockerApplicationTest extends ApplicationTestCase<PebbleLockerApplication> {

    private SharedPreferences mPrefs;

    public PebbleLockerApplicationTest() {
        super(PebbleLockerApplication.class);
    }

    @Override
    public void setUp() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public void tearDown() throws Exception {
        mPrefs.edit().clear().apply();
        super.tearDown();
    }

    public void testSetsInstallUpgradeDateAndVersionOnFirstRun() {
        createApplication();

        assertNotNull(mPrefs.getString("install_date", null));
        assertNotNull(mPrefs.getString("upgrade_date", null));
        assertEquals(BuildConfig.VERSION_CODE, mPrefs.getInt("version", -1));
    }

    public void testSetsUpgradeDateAndVersionOnAppUpdate() {
        mPrefs.edit().putString("upgrade_date", "test_upgrade").apply();
        mPrefs.edit().putInt("version", BuildConfig.VERSION_CODE - 1).apply();

        createApplication();

        assertNull(mPrefs.getString("install_date", null));
        assertNotSame("test_upgrade", mPrefs.getString("upgrade_date", "test_upgrade"));
        assertEquals(BuildConfig.VERSION_CODE, mPrefs.getInt("version", -1));
    }

    public void testDoesNotSetDatesAndVersionOnSubsequentRuns() {
        mPrefs.edit().putString("install_date", "test_install").apply();
        mPrefs.edit().putString("upgrade_date", "test_upgrade").apply();
        mPrefs.edit().putInt("version", BuildConfig.VERSION_CODE).apply();

        createApplication();

        assertEquals("test_install", mPrefs.getString("install_date", null));
        assertEquals("test_upgrade", mPrefs.getString("upgrade_date", null));
    }
}
