package com.lukekorth.pebblelocker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

public class LogReporting {

	private Context mContext;
	private String mTag;
	
	private ProgressDialog mLoading;
	
	public LogReporting(Context context, String tag) {
		mContext = context;
		mTag = tag;
	}
	
	public void collectAndSendLogs() {
		mLoading = ProgressDialog.show(mContext, "", "Loading. Please wait...", true);
		new GenerateLogFile().execute();
	}
	
	private class GenerateLogFile extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void... args) {
			String filename = "pebble-locker" + System.currentTimeMillis() + ".log";
			
			try {
				Process process = Runtime.getRuntime().exec("logcat -v time -d -s " + mTag);
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				StringBuilder log = new StringBuilder(); 
				String line;
				while ((line = bufferedReader.readLine()) != null) { 
					log.append(line);
					log.append("\n"); 
				}
				
				File file = new File(mContext.getExternalFilesDir(null), filename);
				file.createNewFile();
				
				FileWriter out = new FileWriter(file);
	            out.write(log.toString());
	            out.close();
			} catch (IOException e) {
			}
			
			return filename;
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
