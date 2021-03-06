package com.aegamesi.steamtrade.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.OnTabSelectedListener;
import android.support.design.widget.TabLayout.Tab;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aegamesi.lib.android.ExpandableHeightGridView;
import com.aegamesi.steamtrade.R;
import com.aegamesi.steamtrade.fragments.support.ItemListAdapter;
import com.aegamesi.steamtrade.fragments.support.ItemListView;
import com.aegamesi.steamtrade.trade2.TradeOffer;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalAsset;
import com.nosoop.steamtrade.inventory.TradeInternalInventories;
import com.nosoop.steamtrade.inventory.TradeInternalInventory;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FragmentOffer extends FragmentBase implements OnClickListener, AdapterView.OnItemSelectedListener {
	public static int[] tab_notifications = new int[]{0, 0, 0};
	public View[] tab_views;
	public TradeOffer offer = null;
	public JSONObject jsonResult = null;

	public Menu optionsMenu = null;
	public View loadingView;
	public TextView loadingStatusView;

	public RadioButton tabInventoryRadioUs;
	public RadioButton tabInventoryRadioThem;
	public Spinner tabInventorySelect;
	public ArrayAdapter<AppContextPair> tabInventorySelectAdapter;
	public View tabInventoryLoading;
	public ItemListView tabInventoryList;
	public TextView tabInventorySearchResult;
	//
	public ExpandableHeightGridView tabOfferMeOffer;
	public ExpandableHeightGridView tabOfferOtherOffer;
	public ItemListAdapter tabOfferMeOfferAdapter;
	public ItemListAdapter tabOfferOtherOfferAdapter;
	//
	public TextView tabReviewHeading;
	public TextView tabReviewMessage;
	public EditText tabReviewMessageInput;
	public Button tabReviewAccept;
	public Button tabReviewDecline;
	public Button tabReviewCounter;
	public Button tabReviewSend;
	public Button tabReviewCancel;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		tab_views = new View[3];
	}

	@Override
	public void onResume() {
		super.onResume();

		// create tabs
		TabLayout tab_layout = activity().tabs;
		tab_layout.removeAllTabs();
		tab_layout.setTabMode(TabLayout.MODE_FIXED);
		tab_layout.setVisibility(View.VISIBLE);
		tab_layout.setOnTabSelectedListener(new OnTabSelectedListener() {
			@Override
			public void onTabSelected(Tab tab) {
				int i = tab.getPosition();
				if (i < 0 || i >= tab_views.length)
					return;
				View tabView = tab_views[i];
				tabView.setVisibility(View.VISIBLE);
				tabView.bringToFront();
				tabView.invalidate();

				tab_notifications[i] = 0;
				updateUITabButton(i);
				if (i == 0)
					updateUIInventory();
				if (i == 1)
					updateUIOffers();
				if (i == 2)
					updateUIReview();

				if (optionsMenu != null) {
					MenuItem itemToggleView = optionsMenu.findItem(R.id.menu_inventory_toggle_view);
					if (itemToggleView != null)
						itemToggleView.setVisible(i == 0 || i == 1);

					MenuItem itemSearch = optionsMenu.findItem(R.id.action_search);
					if (itemSearch != null)
						itemSearch.setVisible(i == 0);
				}
			}

			@Override
			public void onTabUnselected(Tab tab) {
				if (tab == null) {
					return;
				}

				int i = tab.getPosition();
				if (i < 0 || i >= tab_views.length)
					return;
				tab_views[tab.getPosition()].setVisibility(View.GONE);
			}

			@Override
			public void onTabReselected(Tab tab) {
				int i = tab.getPosition();
				if (i < 0 || i >= tab_views.length)
					return;
				onTabSelected(tab);
			}
		});
		for (int i = 0; i < tab_views.length; i++) {
			tab_layout.addTab(tab_layout.newTab(), i == 0);
			updateUITabButton(i);
		}

		// etc.
		if (offer == null)
			return;

		setTitle(String.format(activity().getString(R.string.offer_with), offer.partnerName));

		// update UI
		updateUIInventory();
		updateUIOffers();
		updateUIReview();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		inflater = activity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_offer, container, false);
		loadingView = view.findViewById(R.id.loading);
		loadingView.setVisibility(offer == null ? View.VISIBLE : View.GONE);
		loadingStatusView = (TextView) view.findViewById(R.id.loading_status);

		tab_views[0] = view.findViewById(R.id.offer_tab_inventories);
		tab_views[1] = view.findViewById(R.id.offer_tab_offerings);
		tab_views[2] = view.findViewById(R.id.offer_tab_review);
		//if (offer == null)
		//	return view;

		// TAB 0: Inventories
		tabInventoryRadioUs = (RadioButton) tab_views[0].findViewById(R.id.offer_tab_inventory_us);
		tabInventoryRadioThem = (RadioButton) tab_views[0].findViewById(R.id.offer_tab_inventory_them);
		tabInventoryRadioUs.setOnClickListener(this);
		tabInventoryRadioThem.setOnClickListener(this);
		tabInventorySelect = (Spinner) tab_views[0].findViewById(R.id.inventory_select);
		tabInventorySelect.setOnItemSelectedListener(this);
		tabInventorySelectAdapter = new ArrayAdapter<>(activity(), android.R.layout.simple_spinner_item);
		tabInventorySelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		tabInventorySelect.setAdapter(tabInventorySelectAdapter);
		tabInventoryList = (ItemListView) tab_views[0].findViewById(R.id.itemlist);
		tabInventoryList.setProvider(new ItemListView.IItemListProvider() {
			@Override
			public boolean onItemChecked(TradeInternalAsset item, boolean checked) {
				if (!offer.counterOffer && !offer.newOffer) {
					Toast.makeText(activity(), R.string.offer_must_counter, Toast.LENGTH_LONG).show();
					return false;
				}

				boolean isUs = tabInventoryRadioUs.isChecked();
				if (checked)
					(isUs ? offer.TRADE_USER_SELF : offer.TRADE_USER_PARTNER).getOffer().add(item);
				else
					(isUs ? offer.TRADE_USER_SELF : offer.TRADE_USER_PARTNER).getOffer().remove(item);
				updateUIOffers();

				return true;
			}

			@Override
			public boolean shouldItemBeChecked(TradeInternalAsset item) {
				boolean isUs = tabInventoryRadioUs.isChecked();
				return (isUs ? offer.TRADE_USER_SELF : offer.TRADE_USER_PARTNER).getOffer().contains(item);
			}
		});
		tabInventoryLoading = tab_views[0].findViewById(R.id.inventory_loading);
		tabInventorySearchResult = (TextView) tab_views[0].findViewById(R.id.inventory_search_result);
		// TAB 1: Offers
		tabOfferMeOffer = (ExpandableHeightGridView) tab_views[1].findViewById(R.id.trade_offer_mylist);
		tabOfferMeOffer.setExpanded(true);
		tabOfferMeOfferAdapter = new ItemListAdapter(activity(), tabOfferMeOffer, false, null);
		tabOfferMeOffer.setAdapter(tabOfferMeOfferAdapter);

		tabOfferOtherOffer = (ExpandableHeightGridView) tab_views[1].findViewById(R.id.trade_offer_otherlist);
		tabOfferOtherOffer.setExpanded(true);
		tabOfferOtherOfferAdapter = new ItemListAdapter(activity(), tabOfferOtherOffer, false, null);
		tabOfferOtherOffer.setAdapter(tabOfferOtherOfferAdapter);

		// TAB 2: Review
		tabReviewHeading = ((TextView) tab_views[2].findViewById(R.id.offer_tab_review_heading));
		if (offer != null)
			tabReviewHeading.setText(String.format(getString(R.string.offer_with), offer.partnerName));
		tabReviewMessage = ((TextView) tab_views[2].findViewById(R.id.offer_message));
		tabReviewMessageInput = ((EditText) tab_views[2].findViewById(R.id.offer_message_input));
		tabReviewAccept = ((Button) tab_views[2].findViewById(R.id.offer_tab_review_accept));
		tabReviewDecline = ((Button) tab_views[2].findViewById(R.id.offer_tab_review_decline));
		tabReviewCounter = ((Button) tab_views[2].findViewById(R.id.offer_tab_review_counter));
		tabReviewSend = ((Button) tab_views[2].findViewById(R.id.offer_tab_review_send));
		tabReviewCancel = ((Button) tab_views[2].findViewById(R.id.offer_tab_review_cancel));
		tabReviewAccept.setOnClickListener(this);
		tabReviewDecline.setOnClickListener(this);
		tabReviewCounter.setOnClickListener(this);
		tabReviewSend.setOnClickListener(this);
		tabReviewCancel.setOnClickListener(this);

		updateUIInventory();
		updateUIOffers();
		updateUIReview();

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		if (offer == null)
			(new LoadOfferTask()).execute();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (activity() != null && activity().tabs != null) {
			activity().tabs.setVisibility(View.GONE);
			activity().tabs.setOnTabSelectedListener(null);
			activity().tabs.removeAllTabs();
		}

		// TODO save offer temporarily
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.search, menu);
		inflater.inflate(R.menu.item_list, menu);

		SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return onQueryTextChange(query);
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				tabInventoryList.filter(newText);

				boolean filtering = newText != null && newText.trim().length() > 0;
				if (filtering) {
					tabInventorySearchResult.setVisibility(View.VISIBLE);
					tabInventorySearchResult.setText(String.format(getString(R.string.search_result_count), tabInventoryList.getFilteredItemCount(), tabInventoryList.getTotalItemCount()));
				} else {
					tabInventorySearchResult.setVisibility(View.GONE);
				}
				return true;
			}
		});

		optionsMenu = menu;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_inventory_toggle_view:
				int new_list_mode = tabInventoryList.getListMode() == ItemListAdapter.MODE_GRID ? ItemListAdapter.MODE_LIST : ItemListAdapter.MODE_GRID;
				item.setIcon((new_list_mode == ItemListAdapter.MODE_GRID) ? R.drawable.ic_view_list : R.drawable.ic_view_module);

				tabInventoryList.setListMode(new_list_mode);
				tabOfferMeOfferAdapter.setListMode(new_list_mode);
				tabOfferOtherOfferAdapter.setListMode(new_list_mode);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void updateUIInventory() {
		if (offer == null || activity() == null)
			return;
		// first do inventory select
		activity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (offer == null || tabInventorySelectAdapter == null)
					return;
				boolean isUs = tabInventoryRadioUs.isChecked();
				List<AppContextPair> appContextPairs = isUs ? offer.appContextData : offer.partnerAppContextData;
				Log.d("Offer", "Inventories #: " + appContextPairs.size());
				boolean first_load = tabInventorySelectAdapter.getCount() == 0 && appContextPairs.size() > 0;
				tabInventorySelectAdapter.clear();
				for (AppContextPair pair : appContextPairs)
					tabInventorySelectAdapter.add(pair);
				tabInventorySelectAdapter.notifyDataSetChanged();

				if (first_load) {
					// select last selected inventory
					SharedPreferences prefs = activity().getPreferences(Context.MODE_PRIVATE);
					int pref_appid = prefs.getInt("inv_last_appid", -1);
					long pref_context = prefs.getLong("inv_last_context", -1);
					int pref_index = -1;
					for (int i = 0; i < appContextPairs.size(); i++)
						if (appContextPairs.get(i).getAppid() == pref_appid && appContextPairs.get(i).getContextid() == pref_context)
							pref_index = i;
					if (pref_index != -1)
						tabInventorySelect.setSelection(pref_index);
					// end
				}

				TradeInternalInventory currentInv = null;
				if (offer != null) {
					AppContextPair pair = (AppContextPair) tabInventorySelect.getSelectedItem();
					TradeInternalInventories inventories = (isUs ? offer.TRADE_USER_SELF : offer.TRADE_USER_PARTNER).getInventories();
					if (inventories.hasInventory(pair))
						currentInv = inventories.getInventory(pair);
				}
				if (currentInv == null) {
					tabInventoryLoading.setVisibility(View.VISIBLE);
					tabInventoryList.setVisibility(View.GONE);
				} else {
					tabInventoryLoading.setVisibility(View.GONE);
					tabInventoryList.setVisibility(View.VISIBLE);
					tabInventoryList.setItems(currentInv.getItemList());
				}
			}
		});
	}

	public void updateUIOffers() {
		if (offer == null || activity() == null)
			return;

		tabOfferMeOfferAdapter.setItemList(new ArrayList<>(offer.TRADE_USER_SELF.getOffer()));
		tabOfferOtherOfferAdapter.setItemList(new ArrayList<>(offer.TRADE_USER_PARTNER.getOffer()));
		updateUITabButton(1);
	}

	public void updateUIReview() {
		if (offer == null || activity() == null)
			return;

		tabReviewHeading.setText(String.format(getString(R.string.offer_with), offer.partnerName));
		setTitle(tabReviewHeading.getText());
		tabReviewMessage.setVisibility(offer.newOffer ? View.GONE : View.VISIBLE);
		tabReviewMessageInput.setVisibility(offer.newOffer || offer.counterOffer ? View.VISIBLE : View.GONE);
		if (offer.message != null && offer.message.trim().length() > 0)
			tabReviewMessage.setText(String.format(getString(R.string.offer_message_quote), offer.message));
		else
			tabReviewMessage.setText(R.string.offer_no_message);

		if (offer.newOffer || offer.counterOffer) {
			tabReviewAccept.setVisibility(View.GONE);
			tabReviewDecline.setVisibility(View.GONE);
			tabReviewCounter.setVisibility(View.GONE);
			tabReviewSend.setVisibility(View.VISIBLE);
			tabReviewCancel.setVisibility(View.VISIBLE);
		} else {
			tabReviewAccept.setVisibility(View.VISIBLE);
			tabReviewDecline.setVisibility(View.VISIBLE);
			tabReviewCounter.setVisibility(View.VISIBLE);
			tabReviewSend.setVisibility(View.GONE);
			tabReviewCancel.setVisibility(View.GONE);
		}
	}

	public void updateUITabButton(int num) {
		if (activity() != null && activity().tabs != null && activity().tabs.getTabCount() > num) {
			Tab tab = activity().tabs.getTabAt(num);
			if (tab != null) {
				String text = activity().getResources().getStringArray(R.array.offer_tabs)[num];
				if (tab_notifications[num] > 0)
					text += " (" + tab_notifications[num] + ")";
				tab.setText(text);
			}
		}
	}

	@Override
	public void onClick(View v) {
		if (v == tabInventoryRadioUs || v == tabInventoryRadioThem) {
			tabInventorySelect.setSelection(0);
			onItemSelected(tabInventorySelect, null, 0, 0);
			updateUIOffers();
			updateUIInventory();
		}
		if (v == tabReviewCancel) {
			Toast.makeText(activity(), R.string.offer_cancelled, Toast.LENGTH_SHORT).show();
			activity().getSupportFragmentManager().popBackStackImmediate();
		}
		if (v == tabReviewCounter) {
			offer.counterOffer = true;
			updateUIInventory();
			updateUIOffers();
			updateUIReview();
		}
		if (v == tabReviewDecline) {
			new GenericAsyncTask(getString(R.string.offer_declining)).execute(new Runnable() {
				@Override
				public void run() {
					jsonResult = offer.declineOffer();
				}
			}, new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity(), getString(R.string.offer_declined), Toast.LENGTH_SHORT).show();
					activity().getSupportFragmentManager().popBackStackImmediate();
				}
			});
		}
		if (v == tabReviewAccept) {
			new GenericAsyncTask(getString(R.string.offer_accepting)).execute(new Runnable() {
				@Override
				public void run() {
					jsonResult = offer.acceptOffer();
				}
			}, new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity(), getString(R.string.offer_accepted), Toast.LENGTH_SHORT).show();
					onCompleted();
				}
			});
		}
		if (v == tabReviewSend) {
			final String message = tabReviewMessageInput.getText().toString();
			new GenericAsyncTask(getString(R.string.offer_submitting)).execute(new Runnable() {
				@Override
				public void run() {
					jsonResult = offer.send(message);
				}
			}, new Runnable() {
				@Override
				public void run() {
					Toast.makeText(activity(), getString(R.string.offer_submitted), Toast.LENGTH_SHORT).show();
					onCompleted();
				}
			});
		}
	}

	public void onCompleted() {
		if (activity() == null || getView() == null)
			return;
		ViewGroup viewGroup = ((ViewGroup) getView());
		viewGroup.removeAllViews();
		View result = activity().getLayoutInflater().inflate(R.layout.offer_result, viewGroup, false);
		viewGroup.addView(result);
		TextView resultHeader = (TextView) result.findViewById(R.id.trade_result_header);
		TextView resultText = (TextView) result.findViewById(R.id.trade_result_text);

		//strError
		if (jsonResult != null) {
			if (jsonResult.has("strError")) {
				// this was not a successful offer
				resultHeader.setText(R.string.trade_error);
				resultText.setText(Html.fromHtml(jsonResult.optString("strError", "")));
			} else {
				resultHeader.setText(R.string.trade_completed);
				boolean trade_confirm_email = jsonResult.optBoolean("needs_email_confirmation", false);
				if (trade_confirm_email) {
					String email_domain = jsonResult.optString("email_domain", "unknown");
					resultText.setText(String.format(getString(R.string.offer_confirm_email), email_domain));
				} else {
					resultText.setText(R.string.offer_successful);
				}
			}
		} else {
			resultHeader.setText(R.string.trade_error);
			resultText.setText(R.string.trade_status_unknown);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		if (parent == tabInventorySelect) {
			AppContextPair pair = (AppContextPair) tabInventorySelect.getSelectedItem();
			if (pair != null) {
				SharedPreferences.Editor prefs = activity().getPreferences(Context.MODE_PRIVATE).edit();
				prefs.putInt("inv_last_appid", pair.getAppid());
				prefs.putLong("inv_last_context", pair.getContextid());
				prefs.apply();
			}

			updateUIInventory();
			new Thread(new Runnable() {
				@Override
				public void run() {
					boolean isUs = tabInventoryRadioUs.isChecked();
					if (isUs)
						offer.loadOwnInventory((AppContextPair) tabInventorySelect.getSelectedItem());
					else
						offer.loadPartnerInventory((AppContextPair) tabInventorySelect.getSelectedItem());
					updateUIInventory();
				}
			}).start();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {
	}

	private class LoadOfferTask extends AsyncTask<Void, Void, TradeOffer> {
		private Bundle bundle;
		private ProgressDialog dialog;

		protected TradeOffer doInBackground(Void... voids) { // the fuck
			TradeOffer offer;

			try {
				if (!bundle.getBoolean("from_existing", false)) {
					// generate a new offer
					long user_id = bundle.getLong("user_id"); // either SteamId#getAccountId or the public url...
					String token = bundle.getString("token"); // only if public url
					offer = TradeOffer.createNewOffer(user_id, token);
				} else {
					// load an existing offer
					long id = bundle.getLong("offer_id");
					offer = TradeOffer.loadFromExistingOffer(id);
					offer.message = bundle.getString("offer_message");
				}
			} catch (Exception e) {
				return null;
			}

			return offer;
		}

		protected void onPreExecute() {
			if (loadingView != null)
				loadingView.setVisibility(View.VISIBLE);

			dialog = new ProgressDialog(activity());
			dialog.setIndeterminate(true);
			dialog.setMessage(getString(R.string.offer_loading));
			dialog.show();

			bundle = getArguments();
		}


		protected void onPostExecute(TradeOffer result) {
			offer = result;

			updateUIInventory();
			updateUIOffers();
			updateUIReview();

			if (dialog != null && dialog.isShowing())
				dialog.dismiss();
			if (offer == null) {
				if (loadingStatusView != null)
					loadingStatusView.setText(R.string.offer_error_loading);
			} else {
				if (loadingView != null)
					loadingView.setVisibility(View.GONE);
			}
		}
	}

	private class GenericAsyncTask extends AsyncTask<Runnable, Void, Void> {
		private Runnable after = null;
		private ProgressDialog dialog;
		private String message;

		public GenericAsyncTask(String message) {
			this.message = message;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = new ProgressDialog(activity());
			dialog.setIndeterminate(true);
			dialog.setMessage(message);
			dialog.show();
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			if (dialog != null && dialog.isShowing())
				dialog.dismiss();
			after.run();
		}

		@Override
		protected Void doInBackground(Runnable... runnables) {
			if (runnables.length > 0) {
				runnables[0].run();
				if (runnables.length > 1)
					after = runnables[1];
			}
			return null;
		}
	}
}