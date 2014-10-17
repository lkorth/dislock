package com.lukekorth.pebblelocker.logging;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;

import com.lukekorth.pebblelocker.BuildConfig;
import com.lukekorth.pebblelocker.PebbleLockerApplication;

import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

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
			message.append(getLog());

            File file = getFile();
			try {
				file.createNewFile();
				
				GZIPOutputStream gos = new GZIPOutputStream(new BufferedOutputStream(new PrintStream(file)));
				gos.write(message.toString().getBytes());
				gos.close();
			} catch (IOException e) {
                LoggerFactory.getLogger("LogBuilder").warn("IOException while building emailable log file. "
                        + e.getMessage());
            }

            buildEmailIntent(file);

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

        private String getLog() {
            StringBuilder response = new StringBuilder();
            InputStream in = null;
            try {
                in = new FileInputStream(
                        ((PebbleLockerApplication) mContext.getApplicationContext()).getLogFilePath());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line = reader.readLine();
                String currentTag;
                String lastTag = null;
                while(line != null) {
                    try {
                        currentTag = line.substring(line.indexOf("["), line.indexOf("]") + 1);
                        if (!currentTag.equals(lastTag)) {
                            lastTag = currentTag;
                            response.append("\n");
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                    }

                    response.append(line + "\n");
                    line = reader.readLine();
                }

                return response.toString();
            } catch (FileNotFoundException e) {
                return "FileNotFoundException: " + e.toString();
            } catch (IOException e) {
                return "IOException: " + e.toString();
            } finally {
                if (in != null) {
                    try { in.close(); } catch (IOException e) {}
                }
            }
        }

        private File getFile() {
            File emailableLogsDir  = new File(mContext.getFilesDir(), "emailable_logs");
            emailableLogsDir.mkdir();
            return new File(emailableLogsDir, "dislock.log.gz");
        }

        private void buildEmailIntent(File file) {
            Uri fileUri = FileProvider.getUriForFile(mContext, "com.lukekorth.pebblelocker.fileprovider", file);

            mEmailIntent = new Intent(Intent.ACTION_SEND);
		    mEmailIntent.setType("text/plain");
            mEmailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ "dislock@lukekorth.com" });
            mEmailIntent.putExtra(Intent.EXTRA_SUBJECT, "Dislock Debug Log");
		    mEmailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);

            // grant permissions for all apps that can handle given intent
            List<ResolveInfo> infoList = mContext.getPackageManager()
                    .queryIntentActivities(mEmailIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : infoList) {
                mContext.grantUriPermission(resolveInfo.activityInfo.packageName, fileUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        }

        @Override
		protected void onPostExecute(Void args) {
            mContext.startActivity(Intent.createChooser(mEmailIntent, "Send email via"));
			if(mLoading != null && mLoading.isShowing()) {
                mLoading.cancel();
            }
		}
	}
}
