package com.tcf_corp.android.aed.http;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class MarkerItem extends OverlayItem {

	public MarkerItem(long id, GeoPoint point, String title, String snippet) {
		super(point, title, snippet);
	}

	public long id;
	public String able;
	public String src;
	public String spl;
	public String time;
}
