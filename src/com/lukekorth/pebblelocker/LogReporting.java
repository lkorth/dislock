package com.lukekorth.pebblelocker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

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
		@Override
		protected String doInBackground(Void... args) {
			String filename = "pebble-locker.log";
			StringBuilder message = new StringBuilder();
			
			Map<String,?> keys = PreferenceManager.getDefaultSharedPreferences(mContext).getAll();
			for(Map.Entry<String,?> entry : keys.entrySet()) {
				if(!entry.getKey().equals("key_password")) {
					message.append(entry.getKey() + " : " + entry.getValue().toString() + "\n");
				}
			}	
			message.append("\n\n");
			message.append(new Logger(mContext).getLog());
			
			try {
				File file = new File(mContext.getExternalFilesDir(null), filename);
				file.createNewFile();
				
				FileWriter out = new FileWriter(file);
	            out.write(message.toString());
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
