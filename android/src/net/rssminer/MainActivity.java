package net.rssminer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;

import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;

public class MainActivity extends ListActivity {

	static final String LOG_TAG = "rssminer";

	private final Handler mHandler = new Handler();
	private HttpClient mClient;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mClient = ((RssminerApplication) getApplication()).getHttpClient();
		getList();
	}

	private void getList() {
		new Thread(new Runnable() {
			public void run() {
				HttpGet get = new HttpGet(
						"http://192.168.1.101:9090/api/welcome?section=newest&limit=20&offset=0");
				get.addHeader("Cookie", "_id_=zk15v22ul");

				try {
					HttpResponse resp = mClient.execute(get);
					InputStream is = resp.getEntity().getContent();
					final StringBuilder sb = new StringBuilder();
					BufferedReader br = new BufferedReader(
							new InputStreamReader(is));
					String line;
					while ((line = br.readLine()) != null) {
						sb.append(line);
					}
					is.close();

					JSONArray array = new JSONArray(sb.toString());
					sb.setLength(0);

					final ArrayList<Feed> feeds = new ArrayList<Feed>(array
							.length());

					for (int i = 0; i < array.length(); ++i) {
						String title = array.getJSONObject(i)
								.getString("title");
						feeds.add(new Feed(title, ""));
					}

					mHandler.post(new Runnable() {
						public void run() {
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
