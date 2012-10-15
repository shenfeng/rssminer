package net.rssminer;

import static net.rssminer.Constants.PREF_FULLSCREEN;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;

public class MainActivity extends ListActivity {
	static final String LOG_TAG = "main";
	private final Handler mHandler = new Handler();
	private boolean mFullScreen;
	private SharedPreferences mPreferences;
	private Window mWin;

	public List<String> getAccountEmail() {
		AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
		Account[] accounts = manager.getAccounts();
		List<String> emails = new ArrayList<String>(1);
		for (int i = 0; i < accounts.length; i++) {
			if (accounts[i].type == Constants.GOOGLE_ACCOUNT) {
				emails.add(accounts[i].name);
			}
		}
		return emails;
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mWin = getWindow();
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		getList();
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		Feed f = ((FeedAdapter) l.getAdapter()).getItem(position);
		Intent intent = new Intent(this, DetailActivity.class);
		intent.putExtra(Constants.FEED_ID_KEY, f.id);
		intent.putExtra(Constants.FEED_TITLE_KEY, f.title);
		startActivity(intent);
	}

	protected void onResume() {
		super.onResume();
		mFullScreen = mPreferences.getBoolean(PREF_FULLSCREEN, false);
		setFullscreen(mFullScreen);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_settings:
			setFullscreen(!mFullScreen);
			break;
		case R.id.item_refresh:
			getList();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

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

	private void getList() {
		setProgressBarIndeterminateVisibility(true);
		new Thread(new Runnable() {
			public void run() {
				try {
					String body = RHttpClient
							.get("/api/welcome?section=recommend&limit=50&offset=0");
					JSONArray array = new JSONArray(body);
					final ArrayList<Feed> feeds = new ArrayList<Feed>(array
							.length());
					for (int i = 0; i < array.length(); ++i) {
						feeds.add(new Feed(array.getJSONObject(i)));
					}
					mHandler.post(new Runnable() {
						public void run() {
							setProgressBarIndeterminateVisibility(false);
							setListAdapter(new FeedAdapter(getBaseContext(),
									R.layout.feed_layout, feeds));
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
