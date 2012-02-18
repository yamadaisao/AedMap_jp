package com.tcf_corp.android.aed.baloon;

import android.content.Context;

import com.readystatesoftware.mapviewballoons.BalloonOverlayView;
import com.tcf_corp.android.aed.http.MarkerItem;

public abstract class LocationBalloonOverlayView extends BalloonOverlayView<MarkerItem> {

    public LocationBalloonOverlayView(Context context, int balloonBottomOffset) {
        super(context, balloonBottomOffset);
    }

    public abstract MarkerItem saveMarkerItem();

}