package com.tcf_corp.android.aed;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Overlay;
import com.tcf_corp.android.aed.DraggableOverlay.OnDropListener;
import com.tcf_corp.android.aed.http.MarkerItem;

public class AedEditActivity extends AedMapActivity {

    private static final String TAG = AedEditActivity.class.getSimpleName();
    private static final boolean DEBUG = true;

    private DraggableOverlay dragOverlay;
    private MarkerItem draggingItem;
    private Vibrator vibrator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    @Override
    protected void setOverlays() {
        super.setOverlays();
        aedOverlay.setGestureDetector(new GestureDetector(context, onGestureListener));
        Drawable aedMarker = getResources().getDrawable(R.drawable.ic_aed);
        dragOverlay = new DraggableOverlay(context, aedMarker);
        dragOverlay.setOnDropListener(new OnDropListener() {
            @Override
            public void onDrop(GeoPoint point) {
                if (draggingItem != null) {
                    MarkerItem newItem = new MarkerItem(draggingItem.id, point, draggingItem
                            .getTitle(), draggingItem.getSnippet());
                    newItem.able = draggingItem.able;
                    newItem.src = draggingItem.src;
                    newItem.spl = draggingItem.spl;
                    newItem.time = draggingItem.time;
                    draggingItem = null;
                    aedOverlay.addMarker(newItem);
                    mapView.invalidate();
                }
            }
        });

        // overlayのlistにDraggableOverlayを登録
        List<Overlay> overlays = mapView.getOverlays();
        overlays.add(dragOverlay);
    }

    @Override
    protected void resetOverlays() {
        super.resetOverlays();
        List<Overlay> overlays = mapView.getOverlays();
        overlays.remove(dragOverlay);
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
                dragOverlay.setPoint(draggingItem.getPoint());
                aedOverlay.remove(draggingItem);
                // オーバーレイアイテムを再描画
                mapView.invalidate();
            }
        }
    };
}
