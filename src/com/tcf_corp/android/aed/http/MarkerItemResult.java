package com.tcf_corp.android.aed.http;

import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.GeoPoint;

public class MarkerItemResult {

    public int queryLatitude1E6;
    public int queryLongitude1E6;
    public long minLatitude1E6;
    public long minLongitude1E6;
    public long maxLatitude1E6;
    public long maxLongitude1E6;
    public List<MarkerItem> markers = new ArrayList<MarkerItem>();

    public MarkerItemResult(GeoPoint geoPoint) {
        queryLatitude1E6 = geoPoint.getLatitudeE6();
        queryLongitude1E6 = geoPoint.getLongitudeE6();
        minLatitude1E6 = queryLatitude1E6;
        minLongitude1E6 = queryLongitude1E6;
        maxLatitude1E6 = queryLatitude1E6;
        maxLongitude1E6 = queryLongitude1E6;
    }
}
