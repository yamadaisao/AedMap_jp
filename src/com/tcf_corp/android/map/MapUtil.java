package com.tcf_corp.android.map;

import android.location.Location;

import com.google.android.maps.GeoPoint;

public class MapUtil {

    /**
     * 2転換の距離をメートル単位で算出します.
     * <p>
     * 誤差があるとの話もありますが、アプリケーションの緊急度合を考えると大きな誤差が発生する大陸間の距離を測りたいわけではないので、
     * この方法をとることにします.
     * </p>
     * 
     * @param from
     *            2点間の始点
     * @param to
     *            2点間の終点
     * @return 2点間の距離メートル単位)
     */
    public static long getDistance(GeoPoint from, GeoPoint to) {
        double fromLat = from.getLatitudeE6() / 1E6;
        double fromLng = from.getLongitudeE6() / 1E6;
        double toLat = to.getLatitudeE6() / 1E6;
        double toLng = to.getLongitudeE6() / 1E6;
        float[] result = { 0, 0, 0 };
        Location.distanceBetween(fromLat, fromLng, toLat, toLng, result);
        return (long) result[0];
    }
}
