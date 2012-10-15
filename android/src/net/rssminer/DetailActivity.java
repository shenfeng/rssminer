package net.rssminer;

import static net.rssminer.Constants.FEED_ID_KEY;
import static net.rssminer.Constants.PREF_FULLSCREEN;

import org.json.JSONArray;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;

public class DetailActivity extends Activity {

	private OnGestureListener listener = new SimpleOnGestureListener() {
		public boolean onFling(android.view.MotionEvent e1,
				android.view.MotionEvent e2, float velocityX, float velocityY) {
			return true;
		};
	};

	private int mFeedID;
	private WebView mDetail;
	private boolean mFullScreen;
	private SharedPreferences mPreferences;
	private Window mWin;
	private Handler mHandler = new Handler();
	private GestureDetector detector;

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
		mFullScreen = mPreferences.getBoolean(PREF_FULLSCREEN, false);
		setFullscreen(mFullScreen);
	}

	public boolean onTouchEvent(MotionEvent event) {

		return super.onTouchEvent(event);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.detail_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_fullscreen:
			setFullscreen(!mFullScreen);
			break;
		default:
			Toast.makeText(this, "not implemented", Toast.LENGTH_SHORT).show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		detector = new GestureDetector(this, listener);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mWin = getWindow();
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		ActionBar bar = getActionBar();
		bar.setTitle(getIntent().getExtras()
				.getString(Constants.FEED_TITLE_KEY));
		bar.setDisplayShowTitleEnabled(true);

		mFeedID = getIntent().getExtras().getInt(FEED_ID_KEY);
		setContentView(R.layout.feed_detail);
		mDetail = (WebView) findViewById(R.id.feed_detail);
		// mDetail.setOnTouchListener(new OnTouchListener() {
		// public boolean onTouch(View v, MotionEvent event) {
		// return detector.onTouchEvent(event);
		// }
		// });
		setProgressBarIndeterminateVisibility(true);
		new Thread(new Runnable() {
			public void run() {
				try {
					String body = RHttpClient.get("/api/feeds/" + mFeedID
							+ "?mr=1");
					JSONArray data = new JSONArray(body);
					final String summary = data.getJSONObject(0).getString(
							"summary");
					mHandler.post(new Runnable() {
						public void run() {
							setProgressBarIndeterminateVisibility(false);
							mDetail.loadDataWithBaseURL(null, Constants.CSS
									+ summary, "text/html", "utf-8", null);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
