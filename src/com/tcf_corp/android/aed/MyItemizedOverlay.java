package com.tcf_corp.android.aed;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.tcf_corp.android.util.LogUtil;

public class MyItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	private static final String TAG = MyItemizedOverlay.class.getSimpleName();
	private static final boolean DEBUG = false;

	private static final int ACTION_MOVE_THRESHOLD = 20;
	private static final int NO_HIT = -1;

	private static final int TOUCH_STATE_NONE = 0;
	private static final int TOUCH_STATE_TAP = 1;
	private static final int TOUCH_STATE_MOVE = 2;

	private final Context context;
	private int touchState;
	private Point touchStartPoint;

	private final ArrayList<OverlayItem> mItems;

	public MyItemizedOverlay(Drawable defaultMarker, Context context) {
		super(defaultMarker);
		this.context = context;
		mItems = new ArrayList<OverlayItem>();
		// populateをしないとcrashする
		populate();
	}

	public void addItem(OverlayItem item) {
		mItems.add(item);
		if (DEBUG) {
			LogUtil.v(TAG, "addItem");
		}
		// 表示対象のitem dataを更新したのでItemizedOverlay#populate()を呼んで再処理
		populate();
	}

	// 引数に渡されるindexのitemを返す
	@Override
	protected OverlayItem createItem(int i) {
		if (DEBUG) {
			LogUtil.v(TAG, "createItem i: " + i);
		}
		return mItems.get(i);
	}

	// itemの数を返す
	@Override
	public int size() {
		if (DEBUG) {
			LogUtil.v(TAG, "size: " + mItems.size());
		}
		return mItems.size();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);
		if (DEBUG) {
			LogUtil.v(TAG, "draw");
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) {
		int act = event.getAction();

		switch (act & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			if (DEBUG) {
				LogUtil.v(TAG, "ACTION_DOWN");
			}
			touchState = TOUCH_STATE_TAP;
			if (touchStartPoint == null) {
				touchStartPoint = new Point();
				touchStartPoint.x = (int) event.getX();
				touchStartPoint.y = (int) event.getY();
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (DEBUG) {
				LogUtil.v(TAG, "ACTION_MOVE");

			}
			if (touchState == TOUCH_STATE_TAP) {
				float curX = event.getX();
				float curY = event.getY();
				double dist = Math.sqrt(Math
						.pow((curX - touchStartPoint.x), 2)
						+ Math.pow((curY - touchStartPoint.y), 2));
				if (dist > ACTION_MOVE_THRESHOLD) {
					touchState = TOUCH_STATE_MOVE;
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			if (DEBUG) {
				LogUtil.d(TAG, "ACTION_UP");
			}
			if (touchState == TOUCH_STATE_TAP) {
				// Projection objectを取得
				Projection pj = mapView.getProjection();
				// ItemizedOverlay#hitTest()というmethodがあったが上手くいかなかったので自分で実装
				int hitIndex = hitTest(pj, (int) event.getX(),
						(int) event.getY());
				if (hitIndex != -1) {
					OverlayItem item = mItems.get(hitIndex);
					Toast.makeText(
							context,
							"Latitude1E6:" + item.getPoint().getLatitudeE6()
									+ "\nLongitude1E6:"
									+ item.getPoint().getLongitudeE6(),
							Toast.LENGTH_SHORT).show();
				} else {
					// Projection#fromPixels()でtouchした位置のGeoPointを取得する
					GeoPoint touchLocation = pj.fromPixels((int) event.getX(),
							(int) event.getY());
					// OverlayItem objectを作成
					OverlayItem item = new OverlayItem(touchLocation, "test",
							"test");
					Drawable drawable = context.getResources().getDrawable(
							R.drawable.ic_aed);
					// OverlayItem#setMarker()するDrawableはDrawable#setBounds()はしなくてもいい
					// OverlayItem#setMarker()しないとtouchの際にcrashする
					item.setMarker(drawable);
					// OverlayItemのicon表示の基準点をどこにするか設定
					// ItemizedOverlay#boundCenter()もしくはItemizedOverlay#boundCenterBottom()を呼ばないと生成されたOverlayItemが表示されない
					boundCenterBottom(item.getMarker(0));
					addItem(item);
				}
			}

			touchState = TOUCH_STATE_NONE;
			touchStartPoint = null;

			break;
		default:
			return super.onTouchEvent(event, mapView);
		}

		return super.onTouchEvent(event, mapView);
	}

	private int hitTest(Projection pj, int hitX, int hitY) {
		int hitIndex = NO_HIT;

		for (int i = 0; i < mItems.size(); i++) {
			OverlayItem item = mItems.get(i);
			Point point = new Point();
			pj.toPixels(item.getPoint(), point);

			int halfWidth = item.getMarker(0).getIntrinsicWidth() / 2;

			int left = point.x - halfWidth;
			int right = point.x + halfWidth;
			int top = point.y - item.getMarker(0).getIntrinsicHeight();
			int bottom = point.y;

			if (DEBUG) {
				LogUtil.d(TAG, "left: " + left);
				LogUtil.d(TAG, "rihgt: " + right);
				LogUtil.d(TAG, "top: " + top);
				LogUtil.d(TAG, "bottom: " + bottom);
				LogUtil.d(TAG, "hitX: " + hitX);
				LogUtil.d(TAG, "hitY: " + hitY);
			}

			if (left <= hitX && hitX <= right) {
				if (top <= hitY && hitY <= bottom) {
					hitIndex = i;
					return i;
				}
			}
		}

		return hitIndex;
	}

	// onTap()はUser
	// interactionを契機にOverlayItemを追加していくといった操作をした場合、OverlayItemを表示時も呼ばれてしまうので注意が必要
	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		if (DEBUG) {
			LogUtil.d(TAG, "onTap - Latitude1E6:" + p.getLatitudeE6());
			LogUtil.d(TAG, "onTap - Longitude1E6:" + p.getLongitudeE6());
		}
		// ItemizedOverlay#onTap(GeoPoint p, MapView
		// mapView)でtrueを返すとItemizedOverlay#onTap(int index)が呼ばれない
		return super.onTap(p, mapView);
	}

	@Override
	protected boolean onTap(int index) {
		if (DEBUG) {
			LogUtil.d(TAG, "onTap - index:" + index);
		}
		return super.onTap(index);
	}
}