package com.tcf_corp.android.aed.baloon;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.tcf_corp.android.aed.http.MarkerItem;

public class DraggableBalloonOverlay extends LocationBalloonOverlay<MarkerItem> {

    Resources resources;
    GestureDetector gesDetect;

    /**
     * アノテーションアイテムの画像
     */
    Bitmap marker;

    /**
     * 描画ポイント
     */
    GeoPoint point;

    /**
     * ドラッグ時の地図の中心点(ドラッグ中は地図をスクロールさせないようにする)
     */
    GeoPoint touchCenterPoint;

    /**
     * 長押ししたことを示すフラグ
     */
    boolean longPressFlag;

    private static final int dragUpPoint = 80;
    private static final int dropUpPoint = 65;

    public DraggableBalloonOverlay(Context context, Drawable defaultMarker, MapView mapView) {
        super(defaultMarker, mapView);
        this.gesDetect = new GestureDetector(context, onGestureListener);
        this.marker = ((BitmapDrawable) defaultMarker).getBitmap();
        populate();
    }

    @Override
    protected MarkerItem createItem(int i) {
        return null;
    }

    @Override
    public int size() {
        // TODO 自動生成されたメソッド・スタブ
        return 0;
    }

    @Override
    public void draw(Canvas canvas, MapView mapview, boolean flag) {
        Point screenPoint = new Point();
        mapview.getProjection().toPixels(point, screenPoint);
        canvas.drawBitmap(marker, screenPoint.x - (marker.getWidth() / 2),
                screenPoint.y - (marker.getHeight() / 2), null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionevent, MapView mapview) {
        gesDetect.onTouchEvent(motionevent);

        if (motionevent.getAction() == MotionEvent.ACTION_MOVE) {
            if (longPressFlag == true) {
                // ドラッグ中の処理
                inDrag(motionevent, mapview);
            }
        } else if (motionevent.getAction() == MotionEvent.ACTION_UP) {
            if (longPressFlag) {
                // ドラッグが完了したあとの処理
                afterDrop(motionevent, mapview);
            }
            longPressFlag = false;
        }
        if (touchCenterPoint != null) {
            mapview.getController().animateTo(touchCenterPoint);
            return true;
        }
        return super.onTouchEvent(motionevent, mapview);
    }

    /**
     * オーバーレイアイテムを移動させているときの処理
     */
    private void inDrag(MotionEvent motionEvent, MapView mapView) {
        // 地図のスクロールを止めるために、現在の地図の中心を記録
        touchCenterPoint = mapView.getMapCenter();
        // オーバーレイアイテムの中心点を変更
        point = mapView.getProjection().fromPixels((int) motionEvent.getX(),
                (int) motionEvent.getY() - dragUpPoint);
        // オーバーレイアイテムを再描画
        mapView.invalidate();
    }

    /**
     * オーバーレイアイテムを移動させてドロップしたあとの処理
     * 
     * @param mapView
     */
    private void afterDrop(MotionEvent motionEvent, MapView mapView) {
        point = mapView.getProjection().fromPixels((int) motionEvent.getX(),
                (int) motionEvent.getY() - dropUpPoint);
        mapView.getController().animateTo(point);
        touchCenterPoint = null;
    }

    /**
     * 複雑なタッチイベントを検知 (ここでは、長押しイベントを取得)
     */
    private final SimpleOnGestureListener onGestureListener = new SimpleOnGestureListener() {
        @Override
        public void onLongPress(MotionEvent e) {
            longPressFlag = true;
            super.onLongPress(e);
        }
    };
}
