package com.tcf_corp.android.aed.baloon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.readystatesoftware.mapviewballoons.BalloonOverlayView;
import com.tcf_corp.android.aed.R;
import com.tcf_corp.android.aed.http.MarkerItem;

public class LocationEditBalloonOverlayView extends BalloonOverlayView<MarkerItem> {

    private TextView title;
    private TextView snippet;
    private TextView able;
    private TextView src;
    private TextView spl;

    public LocationEditBalloonOverlayView(Context context, int balloonBottomOffset) {
        super(context, balloonBottomOffset);
    }

    @Override
    protected void setupView(Context context, final ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.location_edit_balloon_overlay, parent);
        title = (TextView) v.findViewById(R.id.balloon_item_title);
        snippet = (TextView) v.findViewById(R.id.balloon_item_snippet);
        able = (TextView) v.findViewById(R.id.balloon_item_able);
        src = (TextView) v.findViewById(R.id.balloon_item_src);
        spl = (TextView) v.findViewById(R.id.balloon_item_spl);

        ImageView close = (ImageView) v.findViewById(R.id.balloon_close);
        close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.setVisibility(GONE);
            }
        });
    }

    /**
     * Sets the view data from a given overlay item. Override this method to
     * create your own data/view mappings.
     * 
     * @param item
     *            - The overlay item containing the relevant view data.
     * @param parent
     *            - The parent layout for this BalloonOverlayView.
     */
    @Override
    protected void setBalloonData(MarkerItem item, ViewGroup parent) {
        if (item.getTitle() != null) {
            title.setVisibility(VISIBLE);
            title.setText(item.getTitle());
        } else {
            title.setText("");
            title.setVisibility(GONE);
        }
        if (item.getSnippet() != null) {
            snippet.setVisibility(VISIBLE);
            snippet.setText(item.getSnippet());
        } else {
            snippet.setText("");
            snippet.setVisibility(GONE);
        }
        if (item.able != null) {
            able.setVisibility(VISIBLE);
            able.setText(item.able);
        } else {
            able.setText("");
            able.setVisibility(GONE);
        }
        if (item.src != null) {
            src.setVisibility(VISIBLE);
            src.setText(item.src);
        } else {
            src.setText("");
            src.setVisibility(GONE);
        }
        if (item.spl != null) {
            spl.setVisibility(VISIBLE);
            spl.setText(item.spl);
        } else {
            spl.setText("");
            spl.setVisibility(GONE);
        }
    }
}
