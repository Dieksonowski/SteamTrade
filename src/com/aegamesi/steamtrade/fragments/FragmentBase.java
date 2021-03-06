package com.aegamesi.steamtrade.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.aegamesi.steamtrade.MainActivity;
import com.aegamesi.steamtrade.steam.SteamMessageHandler;

import uk.co.thomasc.steamkit.steam3.steamclient.callbackmgr.CallbackMsg;

public class FragmentBase extends Fragment implements SteamMessageHandler {
	protected boolean abort = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// make sure we're actually connected to steam...
		abort = !activity().assertSteamConnection();
	}

	@Override
	public void onResume() {
		super.onResume();

		setAnalyticsScreen(getClass().getName());
		// make sure we're actually connected to steam...
		abort = !activity().assertSteamConnection();
	}

	public void setAnalyticsScreen(String name) {
		// no op
	}

	public final MainActivity activity() {
		return (MainActivity) getActivity();
	}

	public void sendAnalyticsEvent(String category, String action, String label, Long value) {
		// no op
	}

	public void setTitle(CharSequence title) {
		if (activity() != null && activity().toolbar != null) {
			activity().toolbar.setTitle(title);
		}
	}

	@Override
	public void handleSteamMessage(CallbackMsg msg) {
		// by default, do nothing
	}

	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		return false;
	}
}
