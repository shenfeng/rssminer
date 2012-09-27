package net.rssminer;

import org.json.JSONException;
import org.json.JSONObject;

public class Feed {

	public final String title;
	public final String link;
	public final int id;

	public Feed(JSONObject obj) throws JSONException {
		title = obj.getString("title");
		link = obj.getString("link");
		id = obj.getInt("id");
	}
}
