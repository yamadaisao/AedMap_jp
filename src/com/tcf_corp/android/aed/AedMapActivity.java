package com.tcf_corp.android.aed;

import java.util.List;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.tcf_corp.android.aed.http.AsyncTaskCallback;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.aed.http.MarkerItemQuery;
import com.tcf_corp.android.aed.http.MarkerQueryAsyncTask;
import com.tcf_corp.android.map.CustomMapView;
import com.tcf_corp.android.util.LogUtil;

public class AedMapActivity extends MapActivity {
	private static final String TAG = AedMapActivity.class.getSimpleName();
	private static final boolean DEBUG = false;

	private static final String ACTION_LOCATION_UPDATE = "com.android.practice.map.ACTION_LOCATION_UPDATE";

	private static final String SAVE_ZOOM_LEVEL = "zoom_level";

	// private final Context context = this;
	private MapController mapController;
	private CustomMapView mapView;
	private MyLocationOverlay myLocationOverlay;
	private AedOverlay aedOverlay;
	// private MyItemizedOverlay mMyItemizedOverlay;
	private ToggleButton moveCurrent;
	// private GeoPoint currentGeoPoint;
	// private GeoPoint lastGeoPoint;
	private int zoomLevel = 19;

	ToggleButton gpsButton;
	ToggleButton wifiButton;
	private WifiManager wifi;

	// private Button button;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.aed_map);
		initMapSet();
		gpsButton = (ToggleButton) findViewById(R.id.button_gps);
		gpsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent callGPSSettingIntent = new Intent(
						android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(callGPSSettingIntent);
			}
		});
		gpsButton
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
					}
				});
		wifiButton = (ToggleButton) findViewById(R.id.button_wifi);
		wifi = (WifiManager) getSystemService(WIFI_SERVICE);
		wifiButton
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						wifi.setWifiEnabled(isChecked);
					}

				});

		moveCurrent = (ToggleButton) findViewById(R.id.button_my_location);
		moveCurrent.setChecked(true);
		moveCurrent
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked == true) {
							GeoPoint gp = myLocationOverlay.getMyLocation();
							moveToCurrent(gp);
						}
					}
				});
		// button = (Button) findViewById(R.id.button_search_address);

		mapView.setPanListener(new CustomMapView.PanListener() {

			@Override
			public void onCenterChange(CustomMapView mapView,
					GeoPoint newGeoPoint, GeoPoint oldGeoPoint) {
				getMarkers(newGeoPoint);
			}
		});

		mapView.setZoomListener(new CustomMapView.ZoomListener() {

			@Override
			public void onZoomChange(CustomMapView mapView, int newZoom,
					int oldZoom) {
				zoomLevel = newZoom;
			}
		});
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null) {
			zoomLevel = savedInstanceState.getInt(SAVE_ZOOM_LEVEL, 19);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SAVE_ZOOM_LEVEL, zoomLevel);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setOverlays();
		setIntentFilterToReceiver();
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.addGpsStatusListener(gpsStatusLitener);
		requestLocationUpdates();
		boolean isWifiEnabled = wifi.isWifiEnabled();
		LogUtil.d(TAG, "isWifiEnabled:" + isWifiEnabled);
		wifiButton.setChecked(isWifiEnabled);
	}

	@Override
	protected void onPause() {
		super.onPause();
		resetOverlays();
		removeUpdates();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private void initMapSet() {
		// MapView objectの取得
		mapView = (CustomMapView) findViewById(R.id.mapview);
		// MapView#setBuiltInZoomControl()でZoom controlをbuilt-in moduleに任せる
		mapView.setBuiltInZoomControls(true);

		// MapController objectを取得
		mapController = mapView.getController();
		mapController.setZoom(zoomLevel);
	}

	private void setOverlays() {
		// User location表示用のMyLocationOverlay objectを取得
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		// 初めてLocation情報を受け取った時の処理を記載
		// そのLocationにanimationで移動する
		myLocationOverlay.runOnFirstFix(new Runnable() {
			@Override
			public void run() {
				GeoPoint gp = myLocationOverlay.getMyLocation();
				moveToCurrent(gp);
				// getMarkers(gp.getLatitudeE6(), gp.getLongitudeE6());
			}
		});
		// LocationManagerからのLocation update取得
		myLocationOverlay.enableMyLocation();

		// OverlayItemを表示するためのMyItemizedOverlayを拡張したclassのobjectを取得
		// mMyItemizedOverlay = new
		// MyItemizedOverlay(getResources().getDrawable(
		// R.drawable.ic_aed), this);
		Drawable aedMarker = getResources().getDrawable(R.drawable.ic_aed);
		aedOverlay = new AedOverlay(aedMarker, mapView);

		// overlayのlistにMyLocationOverlayを登録
		List<Overlay> overlays = mapView.getOverlays();
		overlays.add(myLocationOverlay);
		// overlays.add(mMyItemizedOverlay);
		overlays.add(aedOverlay);
	}

	private void resetOverlays() {
		// LocationManagerからのLocation update情報を取得をcancel
		myLocationOverlay.disableMyLocation();

		// overlayのlistからMyLocationOverlayを削除
		List<Overlay> overlays = mapView.getOverlays();
		overlays.remove(myLocationOverlay);
		// overlays.remove(mMyItemizedOverlay);
		overlays.remove(aedOverlay);
	}

	private void setIntentFilterToReceiver() {
		final IntentFilter locationIntentFilter = new IntentFilter();
		locationIntentFilter.addAction(ACTION_LOCATION_UPDATE);
		registerReceiver(new LocationUpdateReceiver(), locationIntentFilter);
		final IntentFilter wifiIntentFilter = new IntentFilter();
		wifiIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		wifiIntentFilter
				.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		wifiIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		registerReceiver(new WifiStateUpdateReciever(), wifiIntentFilter);
	}

	private void requestLocationUpdates() {
		final PendingIntent requestLocation = getRequestLocationIntent(this);
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		for (String providerName : lm.getAllProviders()) {
			if (lm.isProviderEnabled(providerName)) {
				lm.requestLocationUpdates(providerName, 0, 0, requestLocation);
				if (DEBUG) {
					LogUtil.d(TAG, "Provider: " + providerName);
				}
			}
		}
	}

	private PendingIntent getRequestLocationIntent(Context context) {
		return PendingIntent.getBroadcast(context, 0, new Intent(
				ACTION_LOCATION_UPDATE), PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private void removeUpdates() {
		final PendingIntent requestLocation = getRequestLocationIntent(this);
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		lm.removeUpdates(requestLocation);
		if (DEBUG) {
			Toast.makeText(this, "Remove update", Toast.LENGTH_SHORT).show();
		}
	}

	private void moveToCurrent(Location location) {
		// GPSを切ったらnullになった
		if (location != null) {
			GeoPoint geoPoint = new GeoPoint(
					(int) (location.getLatitude() * 1E6),
					(int) (location.getLongitude() * 1E6));
			moveToCurrent(geoPoint);
			setZoomLevel(location.getAccuracy());
		}
	}

	private void moveToCurrent(GeoPoint geoPoint) {
		if (moveCurrent.isChecked() == true) {
			mapController.animateTo(geoPoint);
		}
		LogUtil.v(TAG, "latitude:" + geoPoint.getLatitudeE6() / 1E6
				+ ",longitude:" + geoPoint.getLongitudeE6() / 1E6);
	}

	private void setZoomLevel(float accuracy) {
		LogUtil.d(TAG,
				"accuracy=" + accuracy + ",zoom=" + mapView.getZoomLevel());
		if (moveCurrent.isChecked()) {
			if (accuracy < 100) {
				zoomLevel = 20;
			} else if (accuracy < 200) {
				zoomLevel = 19;
			} else if (accuracy < 400) {
				zoomLevel = 18;
			} else if (accuracy < 800) {
				zoomLevel = 17;
			} else if (accuracy < 1600) {
				zoomLevel = 16;
			} else if (accuracy < 3200) {
				zoomLevel = 15;
			} else if (accuracy < 6400) {
				zoomLevel = 14;
			} else if (accuracy < 12800) {
				zoomLevel = 13;
			} else if (accuracy < 25600) {
				zoomLevel = 12;
			} else if (accuracy < 51200) {
				zoomLevel = 11;
			} else if (accuracy < 102400) {
				zoomLevel = 10;
			}
			mapView.setZoom(zoomLevel);
		}
	}

	private void getMarkers(GeoPoint geoPoint) {
		AsyncTaskCallback<List<MarkerItem>> callback = new AsyncTaskCallback<List<MarkerItem>>() {

			@Override
			public void onSuccess(List<MarkerItem> narkerList) {
				aedOverlay.setMarkerList(narkerList);
			}

			@Override
			public void onFailed(int resId, String... args) {
			}

			@Override
			public void onAppFailed(List<MarkerItem> data) {
			}
		};
		MarkerItemQuery query = new MarkerItemQuery();
		query.setUrl("http://aedm.jp/toxml.php");
		query.setPoint(geoPoint);
		MarkerQueryAsyncTask task = new MarkerQueryAsyncTask(callback);
		task.execute(query);
	}

	public class LocationUpdateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null) {
				return;
			}
			if (action.equals(ACTION_LOCATION_UPDATE)) {
				// Location情報を取得
				Location loc = (Location) intent.getExtras().get(
						LocationManager.KEY_LOCATION_CHANGED);
				// MapControllerで現在値に移動する
				moveToCurrent(loc);
			}
		}

	}

	public class WifiStateUpdateReciever extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null) {
				return;
			}
			// Wifi の ON/OFF が切り替えられたら WifiChangeActivity を起動
			if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				if (wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
					wifiButton.setChecked(true);
				} else if (wifi.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
					wifiButton.setChecked(false);
				}
			}
		}
	}

	private final GpsStatus.Listener gpsStatusLitener = new GpsStatus.Listener() {

		@Override
		public void onGpsStatusChanged(int event) {
			LogUtil.d(TAG, "onGpsStatusChanged...");
			// GpsStatus.Listenerで呼ばれる
			switch (event) {
			case GpsStatus.GPS_EVENT_STARTED:
				LogUtil.d(TAG, "GPS_EVENT_STARTED");
				gpsButton.setChecked(true);
				break;
			case GpsStatus.GPS_EVENT_STOPPED:
				LogUtil.d(TAG, "GPS_EVENT_STOPPED");
				gpsButton.setChecked(false);
				break;
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				LogUtil.d(TAG, "GPS_EVENT_FIRST_FIX");
				break;
			case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
				// LocationManager lm = (LocationManager)
				// getSystemService(Context.LOCATION_SERVICE);
				// GpsStatus st = lm.getGpsStatus(null);
				// LocationProvider prod = lm
				// .getProvider(LocationManager.GPS_PROVIDER);
				break;
			}
		}

	};
}
