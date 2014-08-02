package com.lukekorth.pebblelocker.logging;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class LogReporting {

	private Context mContext;
	
	private ProgressDialog mLoading;
	
	public LogReporting(Context context) {
		mContext = context;
	}
	
	public void collectAndSendLogs() {
		mLoading = ProgressDialog.show(mContext, "", "Loading. Please wait...", true);
		new GenerateLogFile().execute();
	}
	
	private class GenerateLogFile extends AsyncTask<Void, Void, String> {
		@SuppressLint("NewApi")
		@Override
		protected String doInBackground(Void... args) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            DevicePolicyManager dpm = ((DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE));

			String filename = "pebble-locker.log.gz";
			StringBuilder message = new StringBuilder();
			
			message.append("Android version: " + Build.VERSION.SDK_INT + "\n");
            message.append("Device manufacturer: " + Build.MANUFACTURER + "\n");
            message.append("Device model: " + Build.MODEL + "\n");
            message.append("Device product: " + Build.PRODUCT + "\n");
			message.append("App version: " + getAppVersion(mContext.getPackageName())+ "\n");
			message.append("Pebble app version: " + getAppVersion("com.getpebble.android") + "\n");
			message.append("Minimum password length: " + dpm.getPasswordMinimumLength(null) + "\n");
            message.append("Pebble Locker password length: " + prefs.getString("key_password", "").length() + "\n");
			message.append("Encryption status: " + dpm.getStorageEncryptionStatus() + "\n");

            for (ComponentName componentName : dpm.getActiveAdmins()) {
                message.append("Active Admin: " + componentName.getClassName());
            }

			Map<String,?> keys = prefs.getAll();
			for(Map.Entry<String,?> entry : keys.entrySet()) {
				if(!entry.getKey().equals("key_password")) {
					message.append(entry.getKey() + ": " + entry.getValue().toString() + "\n");
				}
			}	
			message.append("\n\n");
			message.append(new Logger(mContext).getLog());
			
			try {				
				File file = new File(mContext.getExternalFilesDir(null), filename);
				file.createNewFile();
				
				GZIPOutputStream gos = new GZIPOutputStream(new BufferedOutputStream(new PrintStream(file)));
				gos.write(message.toString().getBytes());
				gos.close();
			} catch (IOException e) {
			}
			
			return filename;
		}

        private String getAppVersion(String app) {
            try {
                return mContext.getPackageManager().getPackageInfo(app, 0).versionName;
            } catch (NameNotFoundException e1) {
                return "not found";
            }
        }

		@Override
		protected void onPostExecute(String filename) {
			if(mLoading != null && mLoading.isShowing())
				mLoading.cancel();
			
			Uri fileUri = Uri.fromFile(new File(mContext.getExternalFilesDir(null), filename));
			
		    Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		    emailIntent.setType("text/plain");
		    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ "korth.luke@gmail.com" });
		    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Pebble Locker Debug Log"); 
		    emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
		    mContext.startActivity(Intent.createChooser(emailIntent, "Send email via"));
		}
	}
}
