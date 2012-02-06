package com.tcf_corp.android.aed.http;

import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.GeoPoint;
import com.tcf_corp.android.aed.Constants;

public class MarkerItemResult {

    public int queryLatitude1E6;
    public int queryLongitude1E6;
    public int minLatitude1E6;
    public int minLongitude1E6;
    public int maxLatitude1E6;
    public int maxLongitude1E6;
    public List<MarkerItem> markers = new ArrayList<MarkerItem>();

    public MarkerItemResult(GeoPoint geoPoint) {
        queryLatitude1E6 = geoPoint.getLatitudeE6();
        queryLatitude1E6 = geoPoint.getLatitudeE6();
        minLatitude1E6 = geoPoint.getLatitudeE6() + Constants.LATITUDE_1E6;
        minLongitude1E6 = geoPoint.getLatitudeE6() + Constants.LONGITUDE_1E6;
        maxLatitude1E6 = geoPoint.getLatitudeE6() + Constants.LATITUDE_1E6;
        maxLongitude1E6 = geoPoint.getLatitudeE6() + Constants.LONGITUDE_1E6;
    }
}
