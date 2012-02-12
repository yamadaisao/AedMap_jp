package com.tcf_corp.android.aed;

import java.util.Date;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Overlay;
import com.tcf_corp.android.aed.DraggableOverlay.OnDropListener;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.util.LogUtil;

public class AedEditActivity extends AedMapActivity {

    private static final String TAG = AedEditActivity.class.getSimpleName();
    private static final boolean DEBUG = true;

    private AedOverlay editOverlay;
    private DraggableOverlay dragOverlay;
    private MarkerItem draggingItem;
    private Vibrator vibrator;
    private ImageView newAedHolder;
    private Drawable aedMarker;
    private Drawable aedEditMarker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        LayoutInflater inflater = getLayoutInflater();
        newAedHolder = (ImageView) inflater.inflate(R.layout.new_aed_holder, null);
        ViewGroup v = (ViewGroup) findViewById(R.id.controls);
        v.addView(newAedHolder);
        aedEditMarker = getResources().getDrawable(R.drawable.ic_new_aed);
        aedMarker = getResources().getDrawable(R.drawable.ic_aed);
        newAedHolder.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                newAedHolderGestureDetector.onTouchEvent(event);
                return false;
            }
        });
    }

    @Override
    protected void setOverlays() {
        super.setOverlays();
        aedOverlay.setGestureDetector(new GestureDetector(context, onGestureListener));
        Drawable aedMarker = getResources().getDrawable(R.drawable.ic_aed);
        dragOverlay = new DraggableOverlay(context, aedMarker);
        // OverlayItemを表示するためのMyItemizedOverlayを拡張したclassのobjectを取得
        Drawable editMarker = getResources().getDrawable(R.drawable.ic_new_aed);
        editOverlay = new AedOverlay(context, editMarker, mapView);
        // overlayのlistにDraggableOverlayを登録
        List<Overlay> overlays = mapView.getOverlays();
        overlays.add(dragOverlay);
        overlays.add(editOverlay);
    }

    @Override
    protected void resetOverlays() {
        super.resetOverlays();
        List<Overlay> overlays = mapView.getOverlays();
        overlays.remove(dragOverlay);
        overlays.remove(editOverlay);
    }

    /**
     * 複雑なタッチイベントを検知 (ここでは、長押しイベントを取得)
     */
    private final SimpleOnGestureListener onGestureListener = new SimpleOnGestureListener() {
        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            List<MarkerItem> list = aedOverlay.getHitItems((int) e.getX(), (int) e.getY());
            if (list.size() > 0) {
                draggingItem = list.get(0);
            } else {
                draggingItem = null;
            }
            if (draggingItem != null) {
                vibrator.vibrate(100);
                dragOverlay.setPoint(draggingItem.getPoint(), aedMarker);
                dragOverlay.setOnDropListener(aedDropListener);
                aedOverlay.remove(draggingItem);
                // オーバーレイアイテムを再描画
                mapView.invalidate();
            }
        }
    };
    private final OnDropListener aedDropListener = new OnDropListener() {
        @Override
        public void onDrop(GeoPoint point) {
            if (draggingItem != null) {
                MarkerItem newItem = new MarkerItem(draggingItem.id, point,
                        draggingItem.getTitle(), draggingItem.getSnippet());
                newItem.able = draggingItem.able;
                newItem.src = draggingItem.src;
                newItem.spl = draggingItem.spl;
                newItem.time = draggingItem.time;
                draggingItem = null;
                aedOverlay.addMarker(newItem);
                mapView.invalidate();
            }
        }
    };

    private final GestureDetector newAedHolderGestureDetector = new GestureDetector(context,
            new SimpleOnGestureListener() {

                @Override
                public void onLongPress(MotionEvent e) {
                    vibrator.vibrate(100);
                    GeoPoint gp = mapView.getProjection()
                            .fromPixels((int) e.getX(), (int) e.getY());
                    if (DEBUG) {
                        LogUtil.v(
                                TAG,
                                "onLongPress:lat=" + gp.getLatitudeE6() + ",lng="
                                        + gp.getLongitudeE6());
                    }
                    dragOverlay.setPoint(gp, aedEditMarker);
                    dragOverlay.setOnDropListener(newAedDropListener);
                    mapView.invalidate();
                }
            });

    private final OnDropListener newAedDropListener = new OnDropListener() {
        @Override
        public void onDrop(GeoPoint gp) {
            if (DEBUG) {
                LogUtil.v(TAG, "onDrop:lat=" + gp.getLatitudeE6() + ",lng=" + gp.getLongitudeE6());
            }
            Date date = new Date();
            MarkerItem newItem = new MarkerItem(date.getTime(), gp, "", "");
            newItem.able = "";
            newItem.src = "";
            newItem.spl = "";
            newItem.time = "";
            draggingItem = null;
            editOverlay.addMarker(newItem);
            if (DEBUG) {
                LogUtil.v(TAG, "editOverlay.size=" + editOverlay.size());
            }
            mapView.animateTo(gp);
            mapView.invalidate();
        }
    };
}