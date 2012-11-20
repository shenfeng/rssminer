package net.rssminer;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class BrowserActivity extends Activity {

	private WebView mWebView;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browser);
		mWebView = (WebView) findViewById(R.id.rssminer);
		WebSettings settings = mWebView.getSettings();
		settings.setJavaScriptEnabled(true);
		// settings.setUserAgentString(Constants.USER_AGENT);

		mWebView.setWebViewClient(new WebViewClient());

		mWebView.loadUrl(Constants.SERVER);

	}

	public void onBackPressed() {
		if (mWebView.canGoBack()) {
			mWebView.goBack();
		} else {
			super.onBackPressed();
		}
	}
}
