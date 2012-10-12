package net.rssminer;

import org.json.JSONException;
import org.json.JSONObject;

public class Feed {

	public final String title;
	public final String link;
	public final int id;
	public final int publishTs;
	public final String author;

	public Feed(JSONObject obj) throws JSONException {
		title = obj.getString("title");
		link = obj.getString("link");
		id = obj.getInt("id");
		author = obj.getString("author");
		publishTs = obj.getInt("pts");
	}
}
