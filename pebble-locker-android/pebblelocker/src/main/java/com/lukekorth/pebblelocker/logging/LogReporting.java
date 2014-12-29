package com.lukekorth.pebblelocker.logging;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.lukekorth.mailable_log.MailableLog;
import com.lukekorth.pebblelocker.BuildConfig;

import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class LogReporting {

	private Context mContext;
	private ProgressDialog mLoading;
    private Intent mEmailIntent;
	
	public LogReporting(Context context) {
		mContext = context;
	}
	
	public void collectAndSendLogs() {
		mLoading = ProgressDialog.show(mContext, "", "Loading. Please wait...", true);
		new GenerateLogFile().execute();
	}

	private class GenerateLogFile extends AsyncTask<Void, Void, Void> {

		@SuppressLint("NewApi")
		@Override
		protected Void doInBackground(Void... args) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            DevicePolicyManager dpm = ((DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE));

			StringBuilder message = new StringBuilder();
			
			message.append("Android version: " + Build.VERSION.SDK_INT + "\n");
            message.append("Device manufacturer: " + Build.MANUFACTURER + "\n");
            message.append("Device model: " + Build.MODEL + "\n");
            message.append("Device product: " + Build.PRODUCT + "\n");
			message.append("App version: " + getAppVersion(mContext.getPackageName())+ "\n");
			message.append("Pebble app version: " + getAppVersion("com.getpebble.android") + "\n");
			message.append("Minimum password length: " + dpm.getPasswordMinimumLength(null) + "\n");
            message.append("Minimum letters in password: " + dpm.getPasswordMinimumLetters(null) + "\n");
            message.append("Minimum lower case letters in password: " + dpm.getPasswordMinimumLowerCase(null) + "\n");
            message.append("Minimum non-letters in password: " + dpm.getPasswordMinimumNonLetter(null) + "\n");
            message.append("Minimum numeric in password: " + dpm.getPasswordMinimumNumeric(null) + "\n");
            message.append("Minimum symbols in password: " + dpm.getPasswordMinimumSymbols(null) + "\n");
            message.append("Minimum upper case letters in password: " + dpm.getPasswordMinimumUpperCase(null) + "\n");
            message.append("Dislock password length: " + prefs.getString("key_password", "").length() + "\n");
			message.append("Encryption status: " + dpm.getStorageEncryptionStatus() + "\n");
            message.append("Device rooted: " + isDeviceRooted() + "\n");
            message.append("Debug: " + BuildConfig.DEBUG + "\n");

            List<ComponentName> admins = dpm.getActiveAdmins();
            if (admins != null) {
                for (ComponentName componentName : dpm.getActiveAdmins()) {
                    message.append("Active Admin: " + componentName.getClassName() + "\n");
                }
            } else {
                message.append("No active admins");
            }

			Map<String,?> keys = prefs.getAll();
			for(Map.Entry<String,?> entry : keys.entrySet()) {
				if(!entry.getKey().equals("key_password")) {
					message.append(entry.getKey() + ": " + entry.getValue().toString() + "\n");
				}
			}
			message.append("---------------------------");
            message.append("\n");

            try {
                mEmailIntent = MailableLog.buildEmailIntent(mContext, "dislock@lukekorth.com",
                        "Dislock Debug Log", "dislock.log", message.toString());
            } catch (IOException e) {
                LoggerFactory.getLogger("LogBuilder").warn("IOException while building emailable log file. "
                        + e.getMessage());
            }

            // Ensure we show the spinner and don't just flash the screen
            SystemClock.sleep(1000);

            return null;
		}

        private String getAppVersion(String app) {
            try {
                return mContext.getPackageManager().getPackageInfo(app, 0).versionName;
            } catch (NameNotFoundException e1) {
                return "not found";
            }
        }

        private String isDeviceRooted() {
            String buildTags = android.os.Build.TAGS;
            boolean check1 = buildTags != null && buildTags.contains("test-keys");

            boolean check2;
            try {
                File file = new File("/system/app/Superuser.apk");
                check2 = file.exists();
            } catch (Exception e) {
                check2 = false;
            }

            boolean check3;
            try {
                Process process = Runtime.getRuntime().exec(new String[] { "/system/xbin/which", "su" });
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                check3 = in.readLine() != null;
            } catch (Exception e) {
                check3 = false;
            }

            return Boolean.toString(check1 || check2 || check3);
        }

        @Override
		protected void onPostExecute(Void args) {
            mContext.startActivity(mEmailIntent);
			if(mLoading != null && mLoading.isShowing()) {
                mLoading.cancel();
            }
		}
	}
}
