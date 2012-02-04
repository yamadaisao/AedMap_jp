package com.tcf_corp.android.map;

import android.location.Location;

import com.google.android.maps.GeoPoint;

public class MapUtil {

    public static long getDistance(GeoPoint from, GeoPoint to) {
        // double fromX = from.getLongitudeE6() * Math.PI / 180;
        // double fromY = from.getLatitudeE6() * Math.PI / 180;
        // double toX = to.getLongitudeE6() * Math.PI / 180;
        // double toY = to.getLatitudeE6() * Math.PI / 180;
        // double deg = Math.sin(fromY) * Math.sin(toY) + Math.cos(fromY) *
        // Math.cos(toY)
        // + Math.cos(toX - fromX);
        // double dist = (6378140 * (Math.atan(-deg / Math.sqrt(-deg * deg + 1))
        // + Math.PI / 2) / 1000) * 1000;
        //
        // return (long) dist;
        double fromLat = from.getLatitudeE6() / 1E6;
        double fromLng = from.getLongitudeE6() / 1E6;
        double toLat = to.getLatitudeE6() / 1E6;
        double toLng = to.getLongitudeE6() / 1E6;
        float[] result = { 0, 0, 0 };
        Location.distanceBetween(fromLat, fromLng, toLat, toLng, result);
        return (long) result[0];

    }
}
