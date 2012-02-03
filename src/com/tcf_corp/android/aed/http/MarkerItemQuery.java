package com.tcf_corp.android.aed.http;

import com.google.android.maps.GeoPoint;

public class MarkerItemQuery {

	private String url;
	private GeoPoint point;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public GeoPoint getPoint() {
		return point;
	}

	public void setPoint(GeoPoint point) {
		this.point = point;
	}

	public String getLatitude() {
		double lat = (point.getLatitudeE6() / 1E6);
		return new Double(lat).toString();
	}

	public String getLongitude() {
		double lng = (point.getLongitudeE6() / 1E6);
		return new Double(lng).toString();
	}
}
