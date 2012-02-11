package com.tcf_corp.android.aed;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.google.android.maps.MapView;
import com.tcf_corp.android.aed.baloon.LocationBalloonOverlay;
import com.tcf_corp.android.aed.http.MarkerItem;

/**
 * AEDアイコンのオーバーレイ
 * 
 * @author yamadaisao
 */
public class AedOverlay extends LocationBalloonOverlay<MarkerItem> {

    private static final int DEFAULT_LIMIT = 600;
    private List<MarkerItem> markerList = new ArrayList<MarkerItem>(DEFAULT_LIMIT);

    private final Context context;
    private GestureDetector gestureDetector = null;

    private final MapView mapView;

    public AedOverlay(Context context, Drawable defaultMarker, MapView mapView) {
        super(defaultMarker, mapView);
        // 呼び出しておかないとNullPointer Exception
        populate();
        this.context = context;
        this.mapView = mapView;
        // バルーンの位置はだいたいこのくらい
        int mh = defaultMarker.getMinimumHeight();
        setBalloonBottomOffset(mh / 3 * 2);
        // 呼び出しておかないと描画されない
        boundCenterBottom(defaultMarker);
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    public void setMarkerList(List<MarkerItem> markerList) {
        this.markerList = markerList;
        populate();
    }

    public void addMarkerList(List<MarkerItem> list) {
        for (MarkerItem item : list) {
            if (!markerList.contains(item)) {
                markerList.add(item);
            }
        }
        populate();
    }

    public void addMarker(MarkerItem item) {
        if (!markerList.contains(item)) {
            markerList.add(item);
        }
        populate();
    }

    public boolean remove(MarkerItem item) {
        boolean ret = markerList.remove(item);
        populate();
        return ret;
    }

    public List<MarkerItem> getMarkerList() {
        return markerList;
    }

    @Override
    protected MarkerItem createItem(int i) {
        return markerList.get(i);
    }

    @Override
    public int size() {
        return markerList.size();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionevent, MapView mapview) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(motionevent);
        }
        return super.onTouchEvent(motionevent, mapview);
    }
}
