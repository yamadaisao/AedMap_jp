package com.tcf_corp.android.aed.http;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class MarkerItem extends OverlayItem {

	public MarkerItem(long id, GeoPoint point, String title, String snippet) {
		super(point, title, snippet);
	}

	public Long id;
	public String able;
	public String src;
	public String spl;
	public String time;

	@Override
	public boolean equals(Object obj) {
		// 引数が自分自身かどうか
		if (obj == this) {
			return true;
		}
		// 型のチェック、nullチェックも兼ねる
		if (!(obj instanceof MarkerItem)) {
			return false;
		}
		// このキャストの成功は保証されている
		MarkerItem item = (MarkerItem) obj;
		return id != null && id.equals(item.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

}
