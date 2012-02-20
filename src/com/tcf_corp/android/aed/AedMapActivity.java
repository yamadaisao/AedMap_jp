package com.tcf_corp.android.aed;

import java.io.IOException;
import java.util.Date;
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
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.tcf_corp.android.aed.DraggableOverlay.OnDropListener;
import com.tcf_corp.android.aed.baloon.LocationEditBalloonOverlayView;
import com.tcf_corp.android.aed.http.AsyncTaskCallback;
import com.tcf_corp.android.aed.http.MarkerEditAsyncTask;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.aed.http.MarkerItemQuery;
import com.tcf_corp.android.aed.http.MarkerItemResult;
import com.tcf_corp.android.aed.http.MarkerQueryAsyncTask;
import com.tcf_corp.android.map.CustomMapView;
import com.tcf_corp.android.util.GeocodeManager;
import com.tcf_corp.android.util.LogUtil;

public class AedMapActivity extends MapActivity {
    private static final String TAG = AedMapActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String ACTION_LOCATION_UPDATE = "com.android.practice.map.ACTION_LOCATION_UPDATE";

    private static final String SAVE_ZOOM_LEVEL = "zoom_level";

    private Context context;
    private MapController mapController;
    private CustomMapView mapView;
    private MyLocationOverlay myLocationOverlay;
    private AedOverlay aedOverlay;
    private ToggleButton moveCurrent;
    // 現在のGeoPoint
    private GeoPoint currentGeoPoint;
    private int zoomLevel = 19;

    private ToggleButton gpsButton;
    private ToggleButton wifiButton;
    private ProgressBar progress;
    private WifiManager wifi;
    private TextView address;

    // to edit
    private AedOverlay viewOverlay;
    private AedOverlay editOverlay;
    private DraggableOverlay dragOverlay;
    private MarkerItem draggingItem;
    private Vibrator vibrator;
    private ImageView newAedHolder;
    private Drawable aedMarker;
    private Drawable aedEditMarker;
    private Drawable aedNewMarker;

    // menu
    private boolean isEditMode = false;
    private MenuItem menuView;
    private MenuItem menuEdit;
    private MenuItem menuSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        // レイアウトの読み込み
        setContentView(R.layout.aed_map);
        initMapSet();

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

        // to edit
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        newAedHolder = (ImageView) findViewById(R.id.new_aed_holder);
        newAedHolder.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                newAedHolderGestureDetector.onTouchEvent(event);
                return false;
            }
        });

        aedMarker = getResources().getDrawable(R.drawable.ic_aed);
        aedEditMarker = getResources().getDrawable(R.drawable.ic_edit_aed);
        aedNewMarker = getResources().getDrawable(R.drawable.ic_new_aed);
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
        moveCurrent.setChecked(data.isMoveCurrent());

        // オーバーレイを設定します.
        setLocationOverlay();
        setMarkerOverlay();

        if (data.getLastResult() != null) {
            if (isEditMode) {
                viewOverlay
                        .setMarkerList(data.getLastResult().markers, editOverlay.getMarkerList());
            } else {
                aedOverlay.setMarkerList(data.getLastResult().markers);
            }
        }
        // Map取得サービスの起動.
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

    private void setEditMode() {
        List<Overlay> overlays = mapView.getOverlays();
        SharedData data = SharedData.getInstance();
        if (isEditMode) {
            if (data.getLastResult() != null) {
                viewOverlay
                        .setMarkerList(data.getLastResult().markers, editOverlay.getMarkerList());
            }
            aedOverlay.hideBalloon();
            overlays.remove(aedOverlay);
            overlays.add(viewOverlay);
            overlays.add(editOverlay);
            overlays.add(dragOverlay);
            newAedHolder.setVisibility(View.VISIBLE);
        } else {
            if (data.getLastResult() != null) {
                aedOverlay.setMarkerList(data.getLastResult().markers);
            }
            overlays.add(aedOverlay);
            viewOverlay.hideBalloon();
            overlays.remove(viewOverlay);
            editOverlay.hideBalloon();
            overlays.remove(editOverlay);
            overlays.remove(dragOverlay);
            newAedHolder.setVisibility(View.GONE);
        }
        mapView.invalidate();
    }

    // implements mapView
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    // menu
    @Override
    /**
     * メニューは切り替えられます.
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        menuEdit = menu.findItem(R.id.menu_to_edit);
        menuView = menu.findItem(R.id.menu_to_view);
        menuSettings = menu.findItem(R.id.menu_settings);
        return ret;
    }

    @Override
    /**
     * 表示モード/編集モードのメニューを切り替えます.
     */
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean ret = super.onPrepareOptionsMenu(menu);
        if (isEditMode) {
            menuEdit.setVisible(false);
            menuView.setVisible(true);
            menuSettings.setVisible(true);
        } else {
            menuEdit.setVisible(true);
            menuView.setVisible(false);
            menuSettings.setVisible(true);
        }
        return ret;
    }

    @Override
    /**
     * メニューのハンドラです.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret = super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
        case R.id.menu_to_view:
            isEditMode = false;
            setEditMode();
            break;
        case R.id.menu_to_edit:
            isEditMode = true;
            setEditMode();
            break;
        case R.id.menu_settings:
            break;
        default:
            break;
        }
        return ret;
    }

    /**
     * mapViewの初期化.
     */
    private void initMapSet() {
        // MapView objectの取得
        mapView = (CustomMapView) findViewById(R.id.mapview);
        // MapView#setBuiltInZoomControl()でZoom controlをbuilt-in moduleに任せる
        // mapView.setBuiltInZoomControls(true);

        // MapController objectを取得
        mapController = mapView.getController();
        mapController.setZoom(zoomLevel);

        // ボタンの設定
        // GPSボタンはGPSの設定画面を起動する.
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
        // wifiボタンでon/offを行う.
        wifiButton = (ToggleButton) findViewById(R.id.button_wifi);
        wifi = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                wifi.setWifiEnabled(isChecked);
            }

        });

        // 現在地ボタン
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

        // マップが移動したらマーカーを取得,住所取得.
        mapView.setPanListener(new CustomMapView.PanListener() {
            @Override
            public void onCenterChange(CustomMapView mapView, GeoPoint newGeoPoint,
                    GeoPoint oldGeoPoint) {
                int compLat = newGeoPoint.getLatitudeE6();
                int compLng = newGeoPoint.getLongitudeE6();

                SharedData data = SharedData.getInstance();
                if (data.getLastResult() == null) {
                    data.setLastResult(new MarkerItemResult(newGeoPoint));
                }
                MarkerItemResult lastResult = data.getLastResult();
                if (compLat < lastResult.minLatitude1E6 || compLat > lastResult.maxLatitude1E6
                        || compLng < lastResult.minLongitude1E6
                        || compLng > lastResult.maxLongitude1E6) {
                    if (DEBUG) {
                        LogUtil.v(TAG, String.format("lat=%d < %d < %d, lng=%d < %d < %d",
                                lastResult.minLatitude1E6, compLat, lastResult.maxLatitude1E6,
                                lastResult.minLongitude1E6, compLng, lastResult.maxLongitude1E6));
                    }
                    getMarkers(newGeoPoint);
                }
                getAddress(newGeoPoint);
            }
        });
    }

    /**
     * Locationオーバーレイの設定.
     */
    private void setLocationOverlay() {
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

        // overlayのlistにMyLocationOverlayを登録
        List<Overlay> overlays = mapView.getOverlays();
        overlays.add(myLocationOverlay);
    }

    /**
     * aedマーカーのオーバーレイを設定します.
     */
    private void setMarkerOverlay() {
        // 表示用AED
        aedMarker = getResources().getDrawable(R.drawable.ic_aed);
        aedOverlay = new AedOverlay(context, aedMarker, mapView, false);

        // to edit
        // Drag用
        dragOverlay = new DraggableOverlay(context, aedMarker);

        // 編集モードの表示用
        viewOverlay = new AedOverlay(context, aedMarker, mapView, true);
        viewOverlay.setGestureDetector(viewGestureDetector);
        viewOverlay
                .setOnItemChangedListener(new LocationEditBalloonOverlayView.OnItemChangedListener() {

                    @Override
                    public void onChanged(MarkerItem item) {
                        viewOverlay.remove(item);
                        editOverlay.addMarker(item);
                        item.setMarker(aedEditMarker);
                        mapView.invalidate();
                    }
                });
        viewOverlay.setOnItemStoreListener(storeListener);

        // 編集用AED
        editOverlay = new AedOverlay(context, aedEditMarker, mapView, true);
        editOverlay.setGestureDetector(editGestureDetector);
        editOverlay.setOnItemStoreListener(storeListener);

        aedEditMarker = aedOverlay.getBoundCenterBottom(aedEditMarker);
        aedNewMarker = aedOverlay.getBoundCenterBottom(aedNewMarker);

        // overlayのlistにMyLocationOverlayを登録
        List<Overlay> overlays = mapView.getOverlays();
        overlays.add(aedOverlay);
        if (isEditMode) {
            overlays.add(viewOverlay);
            overlays.add(editOverlay);
            overlays.add(dragOverlay);
        } else {
            newAedHolder.setVisibility(View.GONE);
        }
    }

    protected void resetOverlays() {
        // LocationManagerからのLocation update情報を取得をcancel
        myLocationOverlay.disableMyLocation();

        // overlayのlistからMyLocationOverlayを削除
        List<Overlay> overlays = mapView.getOverlays();
        overlays.remove(myLocationOverlay);
        overlays.remove(aedOverlay);
        if (isEditMode) {
            overlays.remove(viewOverlay);
            overlays.remove(editOverlay);
            overlays.remove(dragOverlay);
        }
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
                if (DEBUG) {
                    LogUtil.d(TAG, "Provider: " + providerName);
                }
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
        if (geoPoint != null) {
            if (moveCurrent.isChecked() == true) {
                mapController.animateTo(geoPoint);
            }
            if (DEBUG) {
                LogUtil.v(TAG, "latitude:" + geoPoint.getLatitudeE6() / 1E6 + ",longitude:"
                        + geoPoint.getLongitudeE6() / 1E6);
            }
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
                SharedData data = SharedData.getInstance();
                data.setLastResult(result);
                if (isEditMode == false) {
                    aedOverlay.setMarkerList(result.markers);
                } else {
                    viewOverlay.setMarkerList(result.markers, editOverlay.getMarkerList());
                }
                mapView.invalidate();
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

    // to edit
    /**
     * drag non-edit marker, on long tap.
     */
    private final GestureDetector viewGestureDetector = new GestureDetector(context,
            new SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    super.onLongPress(e);
                    List<MarkerItem> list = aedOverlay.getHitItems((int) e.getX(), (int) e.getY());
                    if (list.size() > 0) {
                        draggingItem = list.get(0);
                    } else {
                        draggingItem = null;
                    }
                    if (draggingItem != null) {
                        vibrator.vibrate(100);
                        dragOverlay.setMarker(draggingItem);
                        dragOverlay.setOnDropListener(aedDropListener);
                        viewOverlay.remove(draggingItem);
                        // オーバーレイアイテムを再描画
                        mapView.invalidate();
                    }
                }
            });
    /**
     * if non-edit marker dropped, set to edit overlay.
     */
    private final OnDropListener aedDropListener = new OnDropListener() {
        @Override
        public void onDrop(GeoPoint point, OverlayItem item) {
            draggingItem = (MarkerItem) item;
            if (draggingItem != null) {
                MarkerItem newItem = new MarkerItem(draggingItem.id, point,
                        draggingItem.getTitle(), draggingItem.getSnippet());
                newItem.editTitle = draggingItem.editTitle;
                newItem.editSnippet = draggingItem.editSnippet;
                newItem.able = draggingItem.able;
                newItem.src = draggingItem.src;
                newItem.spl = draggingItem.spl;
                newItem.time = draggingItem.time;
                newItem.setMarker(aedEditMarker);
                editOverlay.addMarker(newItem);
                draggingItem = null;
                mapView.invalidate();
            }
        }
    };

    /**
     * new aed-marker drag from holder.
     */
    private final GestureDetector newAedHolderGestureDetector = new GestureDetector(context,
            new SimpleOnGestureListener() {

                @Override
                public void onLongPress(MotionEvent e) {
                    vibrator.vibrate(100);
                    // RawX, RowYを取得.
                    // X, Y だとiconの中の位置(?)を取得している.
                    GeoPoint gp = mapView.getProjection().fromPixels((int) e.getRawX(),
                            (int) e.getRawY());
                    if (DEBUG) {
                        LogUtil.v(
                                TAG,
                                "onLongPress:lat=" + gp.getLatitudeE6() + ",lng="
                                        + gp.getLongitudeE6());
                    }
                    Date date = new Date();
                    MarkerItem item = new MarkerItem(date.getTime(), gp, "", "");
                    item.setMarker(aedNewMarker);
                    dragOverlay.setMarker(item);
                    dragOverlay.setOnDropListener(newAedDropListener);
                    mapView.invalidate();
                }
            });

    /**
     * drop new aed-marker.
     */
    private final OnDropListener newAedDropListener = new OnDropListener() {
        @Override
        public void onDrop(GeoPoint gp, OverlayItem item) {
            draggingItem = (MarkerItem) item;
            if (DEBUG) {
                LogUtil.v(TAG, "onDrop:lat=" + gp.getLatitudeE6() + ",lng=" + gp.getLongitudeE6());
            }
            MarkerItem newItem = new MarkerItem(draggingItem.id, gp, "", "");
            newItem.able = "";
            newItem.src = "";
            newItem.spl = "";
            newItem.time = "";
            newItem.type = MarkerItem.TYPE_NEW;
            newItem.setMarker(aedNewMarker);
            draggingItem = null;
            editOverlay.addMarker(newItem);
            if (DEBUG) {
                LogUtil.v(TAG, "editOverlay.size=" + editOverlay.size());
            }
            mapView.animateTo(gp);
            mapView.invalidate();
        }
    };

    /**
     * new/edit aed-marker drag from edit overlay.
     */
    private final GestureDetector editGestureDetector = new GestureDetector(context,
            new SimpleOnGestureListener() {

                @Override
                public void onLongPress(MotionEvent e) {
                    List<MarkerItem> list = editOverlay.getHitItems((int) e.getX(), (int) e.getY());
                    if (list.size() > 0) {
                        draggingItem = list.get(0);
                    } else {
                        draggingItem = null;
                    }
                    if (draggingItem != null) {
                        vibrator.vibrate(100);
                        dragOverlay.setMarker(draggingItem);
                        dragOverlay.setOnDropListener(editDropListener);
                        editOverlay.remove(draggingItem);
                        // オーバーレイアイテムを再描画
                        mapView.invalidate();
                    }
                }
            });

    /**
     * drop new/edit aed-marker into edit overlay.
     */
    private final OnDropListener editDropListener = new OnDropListener() {
        @Override
        public void onDrop(GeoPoint gp, OverlayItem item) {
            draggingItem = (MarkerItem) item;
            if (DEBUG) {
                LogUtil.v(
                        TAG,
                        "editDropListener:lat=" + gp.getLatitudeE6() + ",lng="
                                + gp.getLongitudeE6());
            }
            if (draggingItem != null) {
                MarkerItem newItem = new MarkerItem(draggingItem.id, gp, draggingItem.getTitle(),
                        draggingItem.getSnippet());
                newItem.editTitle = draggingItem.editTitle;
                newItem.editSnippet = draggingItem.editSnippet;
                newItem.able = draggingItem.able;
                newItem.src = draggingItem.src;
                newItem.spl = draggingItem.spl;
                newItem.time = draggingItem.time;
                newItem.setMarker(draggingItem.getMarker(0));
                editOverlay.addMarker(newItem);
                draggingItem = null;
                mapView.invalidate();
            }
        }
    };

    /**
     * 編集ダイアログで保存/削除/破棄は実行された時のリスナ.
     */
    private final LocationEditBalloonOverlayView.OnItemStoreListener storeListener = new LocationEditBalloonOverlayView.OnItemStoreListener() {

        /**
         * 保存
         */
        @Override
        public void onSave(MarkerItem item) {
            progress.setVisibility(View.VISIBLE);
            // 新規/編集共サーバーに送信
            MarkerEditAsyncTask task = new MarkerEditAsyncTask(editCallback);
            task.execute(item);
        }

        /**
         * 編集の取消
         */
        @Override
        public void onRollback(MarkerItem item) {
            // 新規はサーバに存在しないので、クライアント側から削除
            if (item.type == MarkerItem.TYPE_NEW) {
                editOverlay.remove(item);
            } else {
                // 編集中のアイコンはidで未編集のアイコンを探して元に戻す.
                for (MarkerItem marker : aedOverlay.getMarkerList()) {
                    if (marker.equals(item)) {
                        viewOverlay.addMarker(marker);
                        editOverlay.remove(item);
                        break;
                    }
                }
            }
        }

        /**
         * 削除
         */
        @Override
        public void onDelete(MarkerItem item) {
            // 新規はアイコンを消す
            if (item.type == MarkerItem.TYPE_NEW) {
                editOverlay.remove(item);
            } else {
                // 削除をマークして実行
                item.type = MarkerItem.TYPE_DELETE;
                progress.setVisibility(View.VISIBLE);
                MarkerEditAsyncTask task = new MarkerEditAsyncTask(editCallback);
                task.execute(item);
            }
        }

    };

    /**
     * サーバーの更新を行なったあとのコールバック.
     * <p>
     * 新規追加時のIDが欲しいため、成功したらマーカーの再取得を行います.
     * </p>
     */
    private final AsyncTaskCallback<MarkerItemResult> editCallback = new AsyncTaskCallback<MarkerItemResult>() {
        @Override
        public void onSuccess(MarkerItemResult result) {
            editOverlay.remove(result.markers.get(0));
            GeoPoint geoPoint = new GeoPoint(result.queryLatitude1E6, result.queryLongitude1E6);
            getMarkers(geoPoint);
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
}
