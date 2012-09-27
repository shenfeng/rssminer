package net.rssminer;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class RssminerPref extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref);
		// addPreferencesFromResource(R.xml.pref);
	}
}
