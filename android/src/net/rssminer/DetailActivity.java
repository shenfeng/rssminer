package net.rssminer;

import static net.rssminer.Constants.FEED_ID_KEY;
import static net.rssminer.Constants.PREF_FULLSCREEN;

import org.json.JSONArray;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;

public class DetailActivity extends Activity {

	private int mFeedID;
	private WebView mDetail;
	private boolean mFullScreen;
	private SharedPreferences mPreferences;
	private Window mWin;
	private Handler mHandler = new Handler();

	private void setFullscreen(boolean on) {
		WindowManager.LayoutParams winParams = mWin.getAttributes();
		final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
		if (on) {
			winParams.flags |= bits;
		} else {
			winParams.flags &= ~bits;
		}
		mWin.setAttributes(winParams);
		mFullScreen = on;
		mPreferences.edit().putBoolean(PREF_FULLSCREEN, on).commit();
	}

	protected void onResume() {
		super.onResume();
		mFullScreen = mPreferences.getBoolean(PREF_FULLSCREEN, true);
		setFullscreen(mFullScreen);
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mWin = getWindow();
		mWin.requestFeature(Window.FEATURE_ACTION_BAR);
		mFeedID = getIntent().getExtras().getInt(FEED_ID_KEY);
		setContentView(R.layout.feed_detail);
		mDetail = (WebView) findViewById(R.id.feed_detail);
		new Thread(new Runnable() {
			public void run() {
				try {
					String body = RHttpClient.get("/api/feeds/" + mFeedID);
					JSONArray data = new JSONArray(body);
					final String summary = data.getJSONObject(0).getString(
							"summary");
					mHandler.post(new Runnable() {
						public void run() {
							mDetail.loadDataWithBaseURL(null, summary,
									"text/html", "utf-8", null);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
