package net.rssminer;

import static net.rssminer.Constants.PREF_FULLSCREEN;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.SharedPreferences;
import android.view.Window;
import android.view.WindowManager;

public class Utils {

	static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-dd-dd");

	public static String dateStr(long seconds) {
		Date d = new Date(seconds * 1000);
		return dateFormat.format(d);
	}

	public static void setFullScreen(Window win, SharedPreferences pref,
			boolean full) {
		WindowManager.LayoutParams winParams = win.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		if (full) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
		}
		win.setAttributes(winParams);
		pref.edit().putBoolean(PREF_FULLSCREEN, full).commit();
	}

}
