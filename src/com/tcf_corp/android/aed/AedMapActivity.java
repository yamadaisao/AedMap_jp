package com.tcf_corp.android.aed;

import java.io.IOException;
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
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.tcf_corp.android.aed.http.AsyncTaskCallback;
import com.tcf_corp.android.aed.http.MarkerItemQuery;
import com.tcf_corp.android.aed.http.MarkerItemResult;
import com.tcf_corp.android.aed.http.MarkerQueryAsyncTask;
import com.tcf_corp.android.map.CustomMapView;
import com.tcf_corp.android.util.LogUtil;

public class AedMapActivity extends MapActivity {
    private static final String TAG = AedMapActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String ACTION_LOCATION_UPDATE = "com.android.practice.map.ACTION_LOCATION_UPDATE";

    private static final String SAVE_ZOOM_LEVEL = "zoom_level";

    protected Context context;
    protected MapController mapController;
    protected CustomMapView mapView;
    protected MyLocationOverlay myLocationOverlay;
    protected AedOverlay aedOverlay;
    protected ToggleButton moveCurrent;
    // 現在のGeoPoint
    protected GeoPoint currentGeoPoint;
    protected MarkerItemResult lastResult;
    // private GeoPoint lastGeoPoint;
    protected int zoomLevel = 19;

    protected ToggleButton gpsButton;
    protected ToggleButton wifiButton;
    protected ProgressBar progress;
    protected WifiManager wifi;
    protected TextView address;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
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
        gpsButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            }
        });
        wifiButton = (ToggleButton) findViewById(R.id.button_wifi);
        wifi = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                wifi.setWifiEnabled(isChecked);
            }

        });

        moveCurrent = (ToggleButton) findViewById(R.id.button_my_location);
        moveCurrent.setChecked(true);
        moveCurrent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    GeoPoint gp = myLocationOverlay.getMyLocation();
                    moveToCurrent(gp);
                }
            }
        });
        address = (TextView) findViewById(R.id.text_address);

        mapView.setPanListener(new CustomMapView.PanListener() {

            @Override
            public void onCenterChange(CustomMapView mapView, GeoPoint newGeoPoint,
                    GeoPoint oldGeoPoint) {
                int compLat = newGeoPoint.getLatitudeE6();
                int compLng = newGeoPoint.getLongitudeE6();

                if (lastResult == null) {
                    lastResult = new MarkerItemResult(newGeoPoint);
                }
                if (compLat < lastResult.minLatitude1E6 || compLat > lastResult.maxLatitude1E6
                        || compLng < lastResult.minLongitude1E6
                        || compLng > lastResult.maxLongitude1E6) {
                    LogUtil.v(TAG, String.format("lat=%d < %d < %d, lng=%d < %d < %d",
                            lastResult.minLatitude1E6, compLat, lastResult.maxLatitude1E6,
                            lastResult.minLongitude1E6, compLng, lastResult.maxLongitude1E6));
                    getMarkers(newGeoPoint);
                }
                getAddress(newGeoPoint);
            }
        });

        mapView.setZoomListener(new CustomMapView.ZoomListener() {

            @Override
            public void onZoomChange(CustomMapView mapView, int newZoom, int oldZoom) {
                zoomLevel = newZoom;
            }
        });
        mapView.setTouchListener(new CustomMapView.TouchListener() {

            @Override
            public void onActionDown() {
                moveCurrent.setChecked(false);
            }
        });
        progress = (ProgressBar) findViewById(R.id.progress);
        progress.setVisibility(View.INVISIBLE);
        progress.setIndeterminate(true);
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
        // tab間の共有データの復元
        SharedData data = SharedData.getInstance();
        currentGeoPoint = data.getGeoPoint();
        if (data.getLastResult() != null) {
            aedOverlay.setMarkerList(data.getLastResult().markers);
        }
        moveCurrent.setChecked(data.isMoveCurrent());

        setOverlays();
        setIntentFilterToReceiver();
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        lm.addGpsStatusListener(gpsStatusLitener);
        requestLocationUpdates();
        boolean isWifiEnabled = wifi.isWifiEnabled();
        if (DEBUG) {
            LogUtil.v(TAG, "isWifiEnabled:" + isWifiEnabled);
        }
        wifiButton.setChecked(isWifiEnabled);
        getMarkers(mapView.getMapCenter());
        getAddress(mapView.getMapCenter());
    }

    @Override
    protected void onPause() {
        super.onPause();

        // tab間の共有データの保存
        SharedData data = SharedData.getInstance();
        data.setGeoPoint(currentGeoPoint);
        if (data.getLastResult() != null) {
            data.getLastResult().markers = aedOverlay.getMarkerList();
        }
        data.setMoveCurrent(moveCurrent.isChecked());

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

    /**
     * mapViewの初期化.
     */
    private void initMapSet() {
        // MapView objectの取得
        mapView = (CustomMapView) findViewById(R.id.mapview);
        // MapView#setBuiltInZoomControl()でZoom controlをbuilt-in moduleに任せる
        mapView.setBuiltInZoomControls(true);

        // MapController objectを取得
        mapController = mapView.getController();
        mapController.setZoom(zoomLevel);
    }

    /**
     * オーバーレイの設定.
     */
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
            }
        });
        // LocationManagerからのLocation update取得
        myLocationOverlay.enableMyLocation();

        // OverlayItemを表示するためのMyItemizedOverlayを拡張したclassのobjectを取得
        Drawable aedMarker = getResources().getDrawable(R.drawable.ic_aed);
        aedOverlay = new AedOverlay(aedMarker, mapView);

        // overlayのlistにMyLocationOverlayを登録
        List<Overlay> overlays = mapView.getOverlays();
        overlays.add(myLocationOverlay);
        overlays.add(aedOverlay);
    }

    private void resetOverlays() {
        // LocationManagerからのLocation update情報を取得をcancel
        myLocationOverlay.disableMyLocation();

        // overlayのlistからMyLocationOverlayを削除
        List<Overlay> overlays = mapView.getOverlays();
        overlays.remove(myLocationOverlay);
        overlays.remove(aedOverlay);
    }

    private void setIntentFilterToReceiver() {
        final IntentFilter locationIntentFilter = new IntentFilter();
        locationIntentFilter.addAction(ACTION_LOCATION_UPDATE);
        registerReceiver(new LocationUpdateReceiver(), locationIntentFilter);
        final IntentFilter wifiIntentFilter = new IntentFilter();
        wifiIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        wifiIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        wifiIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(new WifiStateUpdateReciever(), wifiIntentFilter);
    }

    private void requestLocationUpdates() {
        final PendingIntent requestLocation = getRequestLocationIntent(this);
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        for (String providerName : lm.getAllProviders()) {
            if (lm.isProviderEnabled(providerName)) {
                lm.requestLocationUpdates(providerName, 0, 0, requestLocation);
                LogUtil.d(TAG, "Provider: " + providerName);
            }
        }
    }

    private PendingIntent getRequestLocationIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0, new Intent(ACTION_LOCATION_UPDATE),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void removeUpdates() {
        final PendingIntent requestLocation = getRequestLocationIntent(this);
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        lm.removeUpdates(requestLocation);
    }

    private void moveToCurrent(Location location) {
        // GPSを切ったらnullになった
        if (location != null) {
            GeoPoint geoPoint = new GeoPoint((int) (location.getLatitude() * 1E6),
                    (int) (location.getLongitude() * 1E6));
            moveToCurrent(geoPoint);
            setZoomLevel(location.getAccuracy());
        }
    }

    private void moveToCurrent(GeoPoint geoPoint) {
        if (moveCurrent.isChecked() == true) {
            mapController.animateTo(geoPoint);
        }
        if (DEBUG) {
            LogUtil.v(
                    TAG,
                    "latitude:" + geoPoint.getLatitudeE6() / 1E6 + ",longitude:"
                            + geoPoint.getLongitudeE6() / 1E6);
        }
    }

    /**
     * 現在地の精度にあわせてZoom Levelを変更します.
     * 
     * @param accuracy
     */
    private void setZoomLevel(float accuracy) {
        if (DEBUG) {
            LogUtil.d(TAG, "accuracy=" + accuracy + ",zoom=" + mapView.getZoomLevel());
        }
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
        currentGeoPoint = geoPoint;
        AsyncTaskCallback<MarkerItemResult> callback = new AsyncTaskCallback<MarkerItemResult>() {

            @Override
            public void onSuccess(MarkerItemResult result) {
                // LogUtil.v(TAG, String.format("onSuccess:%d,%d",
                // result.minLongitude1E6,
                // result.maxLongitude1E6));
                lastResult = result;
                aedOverlay.setMarkerList(result.markers);
                progress.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onFailed(int resId, String... args) {
                progress.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAppFailed(MarkerItemResult result) {
                progress.setVisibility(View.INVISIBLE);
            }
        };
        progress.setVisibility(View.VISIBLE);

        MarkerItemQuery query = new MarkerItemQuery();
        query.setUrl("http://aedm.jp/toxml.php");
        query.setPoint(geoPoint);
        MarkerQueryAsyncTask task = new MarkerQueryAsyncTask(callback);
        task.execute(query);
    }

    private void getAddress(GeoPoint geoPoint) {
        // 場所名を文字列で取得する
        String str_address = null;
        try {
            // 住所を取得
            double latitude = geoPoint.getLatitudeE6() / 1E6;
            double longitude = geoPoint.getLongitudeE6() / 1E6;

            str_address = GeocodeManager.point2address(latitude, longitude, context);
        } catch (IOException e) {
            str_address = getString(R.string.msg_location_fail);
        }

        // 住所をメッセージに持たせて
        // ハンドラにUIを書き換えさせる
        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("str_address", str_address);
        message.setData(bundle);
        addrhandler.sendMessage(message);
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
            // GpsStatus.Listenerで呼ばれる
            switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                gpsButton.setChecked(true);
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                gpsButton.setChecked(false);
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                gpsButton.setChecked(true);
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                gpsButton.setChecked(true);
                // LocationManager lm = (LocationManager)
                // getSystemService(Context.LOCATION_SERVICE);
                // GpsStatus st = lm.getGpsStatus(null);
                // LocationProvider prod = lm
                // .getProvider(LocationManager.GPS_PROVIDER);
                break;
            }
        }

    };

    // ラベルを書き換えるためのハンドラ
    final Handler addrhandler = new Handler() {
        // @Override
        @Override
        public void handleMessage(Message msg) {
            String str_address = msg.getData().get("str_address").toString();
            address.setText(str_address);
        }
    };
}
