package com.tcf_corp.android.aed;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;

import com.google.android.maps.MapView;
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;
import com.tcf_corp.android.aed.http.MarkerItem;

public class AedOverlay extends BalloonItemizedOverlay<MarkerItem> {
	private static final String TAG = AedOverlay.class.getSimpleName();

	private List<MarkerItem> markerList = new ArrayList<MarkerItem>();

	public AedOverlay(Drawable defaultMarker, MapView mapView) {
		super(defaultMarker, mapView);
		populate();
		int mh = defaultMarker.getMinimumHeight();
		setBalloonBottomOffset(mh / 3 * 2);
		// 呼び出しておかないと描画されない
		boundCenterBottom(defaultMarker);
	}

	public void setMarkerList(List<MarkerItem> markerList) {
		this.markerList = markerList;
		populate();
	}

	@Override
	protected MarkerItem createItem(int i) {
		return markerList.get(i);
	}

	@Override
	public int size() {
		return markerList.size();
	}

}
