package net.rssminer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled")
public class BrowserActivity extends Activity {

	private WebView mWebView;
	private boolean isWarmed = false;

	private ProgressBar mPar;

	// private Handler mHandler = new Handler();

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.browser);

//		mPar = (ProgressBar) findViewById(R.id.pbar);

		mWebView = (WebView) findViewById(R.id.rssminer);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new WebViewClient());
		mWebView.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, final int progress) {
//				mPar.setProgress(progress);
//				if (progress == 100) {
//					mPar.setVisibility(View.INVISIBLE);
//				} else if (mPar.getVisibility() == View.INVISIBLE) {
//					mPar.setVisibility(ProgressBar.VISIBLE);
//				}
				// on the main thread
			}
		});

		mWebView.loadUrl(Constants.SERVER);
	}

	public void onBackPressed() {
		if (mWebView.canGoBack()) {
			isWarmed = false;
			mWebView.goBack();
		} else if (isWarmed) {
			super.onBackPressed();
		} else {
			Toast.makeText(this, R.string.warn_exits, Toast.LENGTH_SHORT)
					.show();
			isWarmed = true;
		}
	}
}
