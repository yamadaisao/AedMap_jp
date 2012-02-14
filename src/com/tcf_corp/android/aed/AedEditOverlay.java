package com.tcf_corp.android.aed;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.google.android.maps.MapView;
import com.readystatesoftware.mapviewballoons.BalloonOverlayView;
import com.tcf_corp.android.aed.baloon.LocationEditBalloonOverlayView;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.util.LogUtil;

public class AedEditOverlay extends AedOverlay {

	private static final String TAG = AedEditOverlay.class.getSimpleName();
	private static final boolean DEBUG = true;

	private LocationEditBalloonOverlayView baloonView;
	private GestureDetector gestureDetector = null;

	public AedEditOverlay(Context context, Drawable defaultMarker,
			MapView mapView) {
		super(context, defaultMarker, mapView);
	}

	@Override
	protected void hideBalloon() {
		baloonView.saveMarkerItem();
		super.hideBalloon();
	}

	@Override
	protected BalloonOverlayView<MarkerItem> createBalloonOverlayView() {
		if (DEBUG) {
			LogUtil.v(TAG, "offset=" + getBalloonBottomOffset());
		}
		baloonView = new LocationEditBalloonOverlayView(context,
				getBalloonBottomOffset());
		return baloonView;
	}

	@Override
	public boolean onTouchEvent(MotionEvent motionevent, MapView mapview) {
		if (gestureDetector != null) {
			gestureDetector.onTouchEvent(motionevent);
		}
		return super.onTouchEvent(motionevent, mapview);
	}

	public void setGestureDetector(GestureDetector gestureDetector) {
		this.gestureDetector = gestureDetector;
	}

}
