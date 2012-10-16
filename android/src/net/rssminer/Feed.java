package net.rssminer;

import org.json.JSONException;
import org.json.JSONObject;

public class Feed {

	public static final String SUMMARY = "summary";

	public final String title;
	public final String link;
	public final int id;
	public final int publishTs;
	public final String author;
	public final int readts;
	public final String summary;

	public Feed(JSONObject obj) throws JSONException {
		title = obj.getString("title");
		link = obj.getString("link");
		id = obj.getInt("id");
		author = obj.getString("author");
		publishTs = obj.getInt("pts");
		readts = obj.getInt("readts");
		if (obj.has(SUMMARY)) {
			summary = obj.getString(SUMMARY);
		} else {
			summary = null;
		}
	}
}
