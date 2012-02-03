package com.tcf_corp.android.aed;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;

import com.google.android.maps.MapView;
import com.tcf_corp.android.aed.baloon.LocationBalloonOverlay;
import com.tcf_corp.android.aed.http.MarkerItem;

public class AedOverlay extends LocationBalloonOverlay<MarkerItem> {

    private static final int DEFAULT_LIMIT = 600;
    private List<MarkerItem> markerList = new ArrayList<MarkerItem>(DEFAULT_LIMIT);

    public AedOverlay(Drawable defaultMarker, MapView mapView) {
        super(defaultMarker, mapView);
        // 呼び出しておかないとNullPointer
        populate();
        // バルーンの位置はだいたいこのくらい
        int mh = defaultMarker.getMinimumHeight();
        setBalloonBottomOffset(mh / 3 * 2);
        // 呼び出しておかないと描画されない
        boundCenterBottom(defaultMarker);
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

    @Override
    protected MarkerItem createItem(int i) {
        return markerList.get(i);
    }

    @Override
    public int size() {
        return markerList.size();
    }

}
