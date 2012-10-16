package net.rssminer;

import static net.rssminer.Constants.PREF_FULLSCREEN;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Toast;

public class DetailActivity extends FragmentActivity {

	private boolean mFullScreen;
	private SharedPreferences mPreferences;
	private ViewPager mPager;
	private Handler mHandler = new Handler();

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		final Bundle extras = getIntent().getExtras();
		final ActionBar bar = getActionBar();

		setContentView(R.layout.feeds_pager);
		final String ids = extras.getString(Constants.FEED_ID_KEYS);
		mPager = (ViewPager) findViewById(R.id.feeds_pager);

		setProgressBarIndeterminateVisibility(true);
		new Thread(new Runnable() {
			public void run() {
				try {
					String body = RHttpClient
							.get("/api/feeds/" + ids + "?mr=1");
					JSONArray data = new JSONArray(body);
					final ArrayList<Feed> feeds = new ArrayList<Feed>(data
							.length());

					for (int i = 0; i < data.length(); ++i) {
						feeds.add(new Feed(data.getJSONObject(i)));
					}
					mHandler.post(new Runnable() {
						public void run() {
							setProgressBarIndeterminateVisibility(false);
							mPager.setAdapter(new MyAdapter(
									getSupportFragmentManager(), feeds));
							int current = extras
									.getInt(Constants.FEED_ID_POSITION);
							mPager.setCurrentItem(current);
							bar.setTitle(feeds.get(current).title);
							mPager.setOnPageChangeListener(new SimpleOnPageChangeListener() {
								public void onPageSelected(int position) {
									bar.setTitle(feeds.get(position).title);
								};
							});
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
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

	protected void onResume() {
		super.onResume();
		mFullScreen = mPreferences.getBoolean(PREF_FULLSCREEN, false);
		setFullscreen(mFullScreen);
	}

	private void setFullscreen(boolean on) {
		Utils.setFullScreen(getWindow(), mPreferences, on);
		mFullScreen = on;
	}
}

class DetailFragment extends Fragment {
	private Feed feed;

	public DetailFragment(Feed feed) {
		this.feed = feed;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.feed_detail, container, false);
		WebView detail = (WebView) view.findViewById(R.id.feed_detail);
		detail.loadDataWithBaseURL(null, Constants.CSS + feed.summary,
				"text/html", "utf-8", null);

		return view;
	}

}

class MyAdapter extends FragmentPagerAdapter {
	private List<Feed> feeds;

	public MyAdapter(FragmentManager fm, List<Feed> feeds) {
		super(fm);
		this.feeds = feeds;
	}

	public int getCount() {
		return feeds.size();
	}

	public Fragment getItem(int position) {
		return new DetailFragment(feeds.get(position));
	}

}
