package com.tcf_corp.android.aed;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.aed.http.MarkerItemResult;

/**
 * Shared data between tabs.
 * 
 * @author yamada isao
 * 
 */
public class SharedData implements Parcelable {

    private static final SharedData instance = new SharedData();

    private GeoPoint geoPoint;
    private MarkerItemResult lastResult;
    private boolean moveCurrent;
    private List<MarkerItem> editList = new ArrayList<MarkerItem>();

    private SharedData() {
    }

    private SharedData(Parcel in) {
        geoPoint = new GeoPoint(in.readInt(), in.readInt());
        lastResult = in.readParcelable(null);
        moveCurrent = Boolean.valueOf(in.readString());
        in.readTypedList(editList, MarkerItem.CREATOR);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(geoPoint.getLatitudeE6());
        dest.writeInt(geoPoint.getLongitudeE6());
        dest.writeParcelable(lastResult, 0);
        dest.writeString(Boolean.toString(moveCurrent));
        dest.writeTypedList(editList);
    }

    public static final Parcelable.Creator<SharedData> CREATOR = new Parcelable.Creator<SharedData>() {
        @Override
        public SharedData createFromParcel(Parcel in) {
            return new SharedData(in);
        }

        @Override
        public SharedData[] newArray(int size) {
            return new SharedData[size];
        }
    };
}
