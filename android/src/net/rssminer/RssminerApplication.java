package net.rssminer;

import org.apache.http.client.HttpClient;

import android.app.Application;
import android.net.http.AndroidHttpClient;

public class RssminerApplication extends Application {
	private HttpClient mClient;

	public void onCreate() {
		super.onCreate();
		mClient = AndroidHttpClient.newInstance("android-client");
	}

	public HttpClient getHttpClient() {
		return mClient;
	}

}
