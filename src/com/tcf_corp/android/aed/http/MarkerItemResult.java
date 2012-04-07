package com.tcf_corp.android.aed.http;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

public class MarkerItemResult implements Parcelable {

    public int queryLatitude1E6;
    public int queryLongitude1E6;
    public long minLatitude1E6;
    public long minLongitude1E6;
    public long maxLatitude1E6;
    public long maxLongitude1E6;
    public MarkerItem targetMarker;
    public List<MarkerItem> markers = new ArrayList<MarkerItem>();

    public MarkerItemResult(GeoPoint geoPoint) {
        queryLatitude1E6 = geoPoint.getLatitudeE6();
        queryLongitude1E6 = geoPoint.getLongitudeE6();
        minLatitude1E6 = queryLatitude1E6;
        minLongitude1E6 = queryLongitude1E6;
        maxLatitude1E6 = queryLatitude1E6;
        maxLongitude1E6 = queryLongitude1E6;
    }

    private MarkerItemResult(Parcel in) {
        queryLatitude1E6 = in.readInt();
        queryLongitude1E6 = in.readInt();
        minLatitude1E6 = in.readLong();
        minLongitude1E6 = in.readLong();
        maxLatitude1E6 = in.readLong();
        maxLongitude1E6 = in.readLong();
        targetMarker = in.readParcelable(MarkerItem.class.getClassLoader());
        markers = in.createTypedArrayList(MarkerItem.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(queryLatitude1E6);
        dest.writeInt(queryLongitude1E6);
        dest.writeLong(minLatitude1E6);
        dest.writeLong(minLongitude1E6);
        dest.writeLong(maxLatitude1E6);
        dest.writeLong(maxLongitude1E6);
        dest.writeParcelable(targetMarker, flags);
        dest.writeTypedList(markers);
    }

    public static final Parcelable.Creator<MarkerItemResult> CREATOR = new Parcelable.Creator<MarkerItemResult>() {
        @Override
        public MarkerItemResult createFromParcel(Parcel in) {
            return new MarkerItemResult(in);
        }

        @Override
        public MarkerItemResult[] newArray(int size) {
            return new MarkerItemResult[size];
        }
    };
}
