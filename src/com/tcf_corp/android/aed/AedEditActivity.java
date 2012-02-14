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
import com.google.android.maps.OverlayItem;
import com.tcf_corp.android.aed.DraggableOverlay.OnDropListener;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.util.LogUtil;

public class AedEditActivity extends AedMapActivity {

    private static final String TAG = AedEditActivity.class.getSimpleName();
    private static final boolean DEBUG = true;

    private AedEditOverlay editOverlay;
    private DraggableOverlay dragOverlay;
    private MarkerItem draggingItem;
    private Vibrator vibrator;
    private ImageView newAedHolder;
    private Drawable aedMarker;
    private Drawable aedEditMarker;
    private Drawable aedNewMarker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        LayoutInflater inflater = getLayoutInflater();
        newAedHolder = (ImageView) inflater.inflate(R.layout.new_aed_holder, null);
        ViewGroup v = (ViewGroup) findViewById(R.id.controls);
        v.addView(newAedHolder);
        aedMarker = getResources().getDrawable(R.drawable.ic_aed);
        aedEditMarker = getResources().getDrawable(R.drawable.ic_edit_aed);
        aedNewMarker = getResources().getDrawable(R.drawable.ic_new_aed);
        newAedHolder.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                newAedHolderGestureDetector.onTouchEvent(event);
                return false;
            }
        });
    }

    @Override
    protected void setMarkerOverlay() {
        // OverlayItemを表示するためのMyItemizedOverlayを拡張したclassのobjectを取得
        AedEditOverlay eo = new AedEditOverlay(context, aedMarker, mapView);
        eo.setGestureDetector(new GestureDetector(context, onGestureListener));
        aedOverlay = eo;
        dragOverlay = new DraggableOverlay(context, aedMarker);

        // OverlayItemを表示するためのMyItemizedOverlayを拡張したclassのobjectを取得
        editOverlay = new AedEditOverlay(context, aedEditMarker, mapView);
        editOverlay.setGestureDetector(editGestureDetector);
        // overlayのlistにDraggableOverlayを登録
        List<Overlay> overlays = mapView.getOverlays();
        overlays.add(aedOverlay);
        overlays.add(dragOverlay);
        overlays.add(editOverlay);
        aedEditMarker = aedOverlay.getBoundCenterBottom(aedEditMarker);
        aedNewMarker = aedOverlay.getBoundCenterBottom(aedNewMarker);
    }

    @Override
    protected void resetOverlays() {
        super.resetOverlays();
        List<Overlay> overlays = mapView.getOverlays();
        overlays.remove(dragOverlay);
        overlays.remove(editOverlay);
    }

    /**
     * drag non-edit marker, on long tap.
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
                dragOverlay.setMarker(draggingItem);
                dragOverlay.setOnDropListener(aedDropListener);
                aedOverlay.remove(draggingItem);
                // オーバーレイアイテムを再描画
                mapView.invalidate();
            }
        }
    };
    /**
     * if non-edit marker dropped, set to edit overlay.
     */
    private final OnDropListener aedDropListener = new OnDropListener() {
        @Override
        public void onDrop(GeoPoint point, OverlayItem item) {
            draggingItem = (MarkerItem) item;
            if (draggingItem != null) {
                MarkerItem newItem = new MarkerItem(draggingItem.id, point,
                        draggingItem.getTitle(), draggingItem.getSnippet());
                newItem.able = draggingItem.able;
                newItem.src = draggingItem.src;
                newItem.spl = draggingItem.spl;
                newItem.time = draggingItem.time;
                newItem.setMarker(aedEditMarker);
                editOverlay.addMarker(newItem);
                draggingItem = null;
                mapView.invalidate();
            }
        }
    };

    /**
     * new aed-marker drag from holder.
     */
    private final GestureDetector newAedHolderGestureDetector = new GestureDetector(context,
            new SimpleOnGestureListener() {

                @Override
                public void onLongPress(MotionEvent e) {
                    vibrator.vibrate(100);
                    // RawX, RowYを取得.
                    // X, Y だとiconの中の位置(?)を取得している.
                    GeoPoint gp = mapView.getProjection().fromPixels((int) e.getRawX(),
                            (int) e.getRawY());
                    if (DEBUG) {
                        LogUtil.v(
                                TAG,
                                "onLongPress:lat=" + gp.getLatitudeE6() + ",lng="
                                        + gp.getLongitudeE6());
                    }
                    Date date = new Date();
                    MarkerItem item = new MarkerItem(date.getTime(), gp, "", "");
                    item.setMarker(aedNewMarker);
                    dragOverlay.setMarker(item);
                    dragOverlay.setOnDropListener(newAedDropListener);
                    mapView.invalidate();
                }
            });

    /**
     * drop new aed-marker.
     */
    private final OnDropListener newAedDropListener = new OnDropListener() {
        @Override
        public void onDrop(GeoPoint gp, OverlayItem item) {
            draggingItem = (MarkerItem) item;
            if (DEBUG) {
                LogUtil.v(TAG, "onDrop:lat=" + gp.getLatitudeE6() + ",lng=" + gp.getLongitudeE6());
            }
            MarkerItem newItem = new MarkerItem(draggingItem.id, gp, "", "");
            newItem.able = "";
            newItem.src = "";
            newItem.spl = "";
            newItem.time = "";
            newItem.setMarker(aedNewMarker);
            draggingItem = null;
            editOverlay.addMarker(newItem);
            if (DEBUG) {
                LogUtil.v(TAG, "editOverlay.size=" + editOverlay.size());
            }
            mapView.animateTo(gp);
            mapView.invalidate();
        }
    };

    /**
     * new/edit aed-marker drag from edit overlay.
     */
    private final GestureDetector editGestureDetector = new GestureDetector(context,
            new SimpleOnGestureListener() {

                @Override
                public void onLongPress(MotionEvent e) {
                    List<MarkerItem> list = editOverlay.getHitItems((int) e.getX(), (int) e.getY());
                    if (list.size() > 0) {
                        draggingItem = list.get(0);
                    } else {
                        draggingItem = null;
                    }
                    if (draggingItem != null) {
                        vibrator.vibrate(100);
                        dragOverlay.setMarker(draggingItem);
                        dragOverlay.setOnDropListener(editDropListener);
                        editOverlay.remove(draggingItem);
                        // オーバーレイアイテムを再描画
                        mapView.invalidate();
                    }
                }
            });

    /**
     * drop new/edit aed-marker into edit overlay.
     */
    private final OnDropListener editDropListener = new OnDropListener() {
        @Override
        public void onDrop(GeoPoint gp, OverlayItem item) {
            draggingItem = (MarkerItem) item;
            if (DEBUG) {
                LogUtil.v(
                        TAG,
                        "editDropListener:lat=" + gp.getLatitudeE6() + ",lng="
                                + gp.getLongitudeE6());
            }
            if (draggingItem != null) {
                MarkerItem newItem = new MarkerItem(draggingItem.id, gp, draggingItem.getTitle(),
                        draggingItem.getSnippet());
                newItem.able = draggingItem.able;
                newItem.src = draggingItem.src;
                newItem.spl = draggingItem.spl;
                newItem.time = draggingItem.time;
                newItem.setMarker(draggingItem.getMarker(0));
                editOverlay.addMarker(newItem);
                draggingItem = null;
                mapView.invalidate();
            }
        }
    };
}