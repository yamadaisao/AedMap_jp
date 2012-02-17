package com.tcf_corp.android.aed;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.google.android.maps.MapView;
import com.google.android.maps.Projection;
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;
import com.tcf_corp.android.aed.baloon.LocationBalloonOverlayView;
import com.tcf_corp.android.aed.baloon.LocationDisplayBalloonOverlayView;
import com.tcf_corp.android.aed.baloon.LocationEditBalloonOverlayView;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.util.LogUtil;

/**
 * AEDアイコンのオーバーレイ
 * 
 * @author yamadaisao
 */
public class AedOverlay extends BalloonItemizedOverlay<MarkerItem> {
    private static final String TAG = AedOverlay.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int DEFAULT_LIMIT = 600;
    private List<MarkerItem> markerList = new ArrayList<MarkerItem>(DEFAULT_LIMIT);

    protected final Context context;
    protected final MapView mapView;
    private final int markerHalfWidth;
    private final int markerHeight;
    private GestureDetector gestureDetector = null;
    private boolean isEdit = false;
    private LocationBalloonOverlayView balloonView;

    public AedOverlay(Context context, Drawable defaultMarker, MapView mapView) {
        super(defaultMarker, mapView);
        // 呼び出しておかないとNullPointer Exception
        populate();
        this.context = context;
        this.mapView = mapView;
        // バルーンの位置はだいたいこのくらい
        int mh = defaultMarker.getMinimumHeight();
        setBalloonBottomOffset(mh / 3 * 2);
        // 呼び出しておかないと描画されない
        boundCenterBottom(defaultMarker);
        markerHalfWidth = defaultMarker.getIntrinsicWidth() / 2;
        markerHeight = defaultMarker.getIntrinsicHeight();
    }

    public void setMarkerList(List<MarkerItem> list) {
        this.markerList = list;
        populate();
    }

    public void addMarkerList(List<MarkerItem> list) {
        for (MarkerItem item : list) {
            if (!markerList.contains(item)) {
                markerList.add(item);
            }
        }
        populate();
    }

    public void addMarker(MarkerItem item) {
        if (!markerList.contains(item)) {
            markerList.add(item);
        }
        populate();
    }

    public boolean remove(MarkerItem item) {
        boolean ret = markerList.remove(item);
        hideBalloon();
        populate();
        return ret;
    }

    public List<MarkerItem> getMarkerList() {
        return markerList;
    }

    @Override
    protected MarkerItem createItem(int i) {
        return markerList.get(i);
    }

    @Override
    public int size() {
        return markerList.size();
    }

    public Drawable getBoundCenterBottom(Drawable drawable) {
        return boundCenterBottom(drawable);
    }

    /**
     * イベントの位置に存在するItemのリストを取得する.
     * 
     * @param hitX
     *            イベントの位置 x
     * @param hitY
     *            イベントの位置 y
     * @return Itemのリスト
     */
    public List<MarkerItem> getHitItems(int hitX, int hitY) {
        List<MarkerItem> hitList = new ArrayList<MarkerItem>();
        Projection pj = mapView.getProjection();
        for (MarkerItem item : getMarkerList()) {
            Point point = new Point();
            pj.toPixels(item.getPoint(), point);
            int left = point.x - markerHalfWidth;
            int right = point.x + markerHalfWidth;
            int top = point.y - markerHeight;
            int bottom = point.y;
            if (left <= hitX && hitX <= right) {
                if (top <= hitY && hitY <= bottom) {
                    hitList.add(item);
                }
            }
        }
        return hitList;
    }

    /**
     * 編集できる場合はtrueを設定します.
     * 
     * @param isEdit
     */
    public void setEdit(boolean isEdit) {
        this.isEdit = isEdit;
        balloonView = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionevent, MapView mapview) {
        if (isEdit == true) {
            if (gestureDetector != null) {
                gestureDetector.onTouchEvent(motionevent);
            }
        }
        return super.onTouchEvent(motionevent, mapview);
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    @Override
    public void hideBalloon() {
        Log.d(TAG, "hideBalloon");
        if (balloonView != null) {
            balloonView.saveMarkerItem();
        }
        super.hideBalloon();
    }

    @Override
    protected LocationBalloonOverlayView createBalloonOverlayView() {
        if (DEBUG) {
            LogUtil.v(TAG, "offset=" + getBalloonBottomOffset());
        }
        if (isEdit == false) {
            balloonView = new LocationDisplayBalloonOverlayView(context, getBalloonBottomOffset());
        } else {
            balloonView = new LocationEditBalloonOverlayView(context, getBalloonBottomOffset());
        }
        return balloonView;
    }
}
