package com.tcf_corp.android.aed;

import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.GeoPoint;
import com.tcf_corp.android.aed.http.MarkerItem;

/**
 * Shared data between tabs.
 * 
 * @author yamada isao
 * 
 */
public class SharedData {

    private static final SharedData instance = new SharedData();

    private GeoPoint geoPoint;
    private List<MarkerItem> markerList;
    private boolean moveCurrent;

    private SharedData() {
    }

    public static SharedData getInstance() {
        return instance;
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public List<MarkerItem> getMarkerList() {
        if (markerList == null) {
            markerList = new ArrayList<MarkerItem>();
        }
        return markerList;
    }

    public void setMarkerList(List<MarkerItem> markerList) {
        this.markerList = markerList;
    }

    public boolean isMoveCurrent() {
        return moveCurrent;
    }

    public void setMoveCurrent(boolean moveCurrent) {
        this.moveCurrent = moveCurrent;
    }
}
