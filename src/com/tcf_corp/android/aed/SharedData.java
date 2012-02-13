package com.tcf_corp.android.aed;

import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.GeoPoint;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.aed.http.MarkerItemResult;

/**
 * Shared data between tabs.
 * 
 * @author yamada isao
 * 
 */
public class SharedData {

    private static final SharedData instance = new SharedData();

    private GeoPoint geoPoint;
    private MarkerItemResult lastResult;
    private boolean moveCurrent;
    private List<MarkerItem> editList = new ArrayList<MarkerItem>();

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

    public MarkerItemResult getLastResult() {
        return lastResult;
    }

    public void setLastResult(MarkerItemResult lastResult) {
        this.lastResult = lastResult;
    }

    public boolean isMoveCurrent() {
        return moveCurrent;
    }

    public void setMoveCurrent(boolean moveCurrent) {
        this.moveCurrent = moveCurrent;
    }

    public List<MarkerItem> getEditList() {
        return editList;
    }

    public void setEditList(List<MarkerItem> editList) {
        this.editList = editList;
    }
}
