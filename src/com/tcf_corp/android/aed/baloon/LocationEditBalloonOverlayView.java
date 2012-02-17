package com.tcf_corp.android.aed.baloon;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tcf_corp.android.aed.R;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.util.LogUtil;

public class LocationEditBalloonOverlayView extends LocationBalloonOverlayView {

    private static final String TAG = LocationEditBalloonOverlayView.class.getSimpleName();
    private static final boolean DEBUG = true;

    private MarkerItem item;
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
                saveMarkerItem();
                parent.setVisibility(GONE);
            }
        });
        final Context ctx = context;
        Button btnSave = (Button) v.findViewById(R.id.button_save);
        Button btnDelete = (Button) v.findViewById(R.id.button_delete);
        Button btnCancel = (Button) v.findViewById(R.id.button_cancel);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(ctx, ctx.getString(R.string.dialog_save_message),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
            }
        });
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(ctx, ctx.getString(R.string.dialog_delete_message),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(ctx, ctx.getString(R.string.dialog_cancel_message),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
            }
        });
    }

    // カスタムダイアログの表示
    public static void showDialog(Context context, String text,
            final DialogInterface.OnClickListener listener) {
        // カスタムダイアログの生成
        final Dialog dialog = new Dialog(context, R.style.Theme_CustomDialog);
        dialog.setContentView(R.layout.dialog);
        dialog.setOwnerActivity((Activity) context);

        // テキストの指定
        TextView textView = (TextView) dialog.findViewById(R.id.dialog_message);
        textView.setText(text);

        // ボタンの指定
        Button btnOK = (Button) dialog.findViewById(R.id.button_ok);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onClick(dialog, Dialog.BUTTON_POSITIVE);
                }
            }
        });
        Button btnCancel = (Button) dialog.findViewById(R.id.button_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onClick(dialog, Dialog.BUTTON_NEGATIVE);
                }
            }
        });
        dialog.show();
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
        this.item = item;
        if (item.editTitle != null) {
            title.setText(item.editTitle);
        } else {
            title.setText(item.getTitle());
        }
        if (item.editSnippet != null) {
            snippet.setText(item.editSnippet);
        } else {
            snippet.setText(item.getSnippet());
        }
        able.setText(item.able);
        src.setText(item.src);
        spl.setText(item.spl);
    }

    /*
     * (非 Javadoc)
     * 
     * @see
     * com.tcf_corp.android.aed.baloon.LocationBalloonOverlayView#saveMarkerItem
     * ()
     */
    @Override
    public void saveMarkerItem() {
        if (DEBUG) {
            LogUtil.v(TAG, "saveMarkerItem");
        }
        item.editTitle = title.getText().toString();
        item.editSnippet = snippet.getText().toString();
        item.able = able.getText().toString();
        item.src = src.getText().toString();
        item.spl = spl.getText().toString();
    }
}
