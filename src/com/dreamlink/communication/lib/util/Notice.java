package com.dreamlink.communication.lib.util;

import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.widget.Toast;

/**
 * Show Android toast.
 * 
 */
public class Notice {
	private static final String TAG = "Notice";

	private Toast toast;
	private Context context;

	public Notice(Context context) {
		this.context = context;
	}

	public void showToast(String msg) {
		try {
			if (toast != null) {
				toast.setText(msg);
			} else {
				toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
			}
			toast.show();
		} catch (Exception e) {
			Log.e(TAG, "showToast error, " + e);
		}
	}
	
	public void showToast(int  msgResId) {
		String msg = context.getResources().getString(msgResId);
		showToast(msg);
	}

	public void closeToast() {
		if (toast != null)
			toast.cancel();
	}

}