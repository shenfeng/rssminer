package net.rssminer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import android.net.http.AndroidHttpClient;
import android.util.Log;

public class RHttpClient {
	private static final HttpClient mClient = AndroidHttpClient
			.newInstance(Constants.USER_AGENT);

	public static String get(String uri) throws IOException {
		long start = System.currentTimeMillis();
		HttpGet get = new HttpGet(Constants.SERVER + uri);

		get.addHeader("Cookie", "_id_=zk15v22ul");
		get.addHeader("Accept-Encoding", "gzip,deflate,sdch");

		HttpResponse resp = mClient.execute(get);
		InputStream is = resp.getEntity().getContent();

		Header[] headers = resp.getHeaders("Content-Encoding");
		if (headers.length > 0
				&& "gzip".equalsIgnoreCase(headers[0].getValue())) {
			is = new GZIPInputStream(is);
		}

		final StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		is.close();
		long time = System.currentTimeMillis() - start;
		Log.d("HTTP", "get " + uri + " takes " + time + " ms");
		return sb.toString();
	}
}