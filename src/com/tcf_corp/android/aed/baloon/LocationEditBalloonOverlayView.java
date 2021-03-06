package com.tcf_corp.android.aed.baloon;

import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.tcf_corp.android.aed.R;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.util.GeocodeManager;
import com.tcf_corp.android.util.LogUtil;

/**
 * 編集用のバルーンを表示します.
 * 
 * @author yamadaisao
 * 
 */
public class LocationEditBalloonOverlayView extends LocationBalloonOverlayView {

    private static final String TAG = LocationEditBalloonOverlayView.class.getSimpleName();
    private static final boolean DEBUG = false;

    private final Context context;

    private MarkerItem item;
    private TextView title;
    private TextView snippet;
    private TextView able;
    private TextView src;
    private TextView spl;
    private Button btnSave;
    private Button btnDelete;
    private Button btnCancel;

    public LocationEditBalloonOverlayView(Context context, int balloonBottomOffset) {
        super(context, balloonBottomOffset);
        this.context = context;
    }

    @Override
    protected void setupView(Context context, final ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // Viewの構築とオブジェクトの取得
        View v = inflater.inflate(R.layout.location_edit_balloon_overlay, parent);
        title = (TextView) v.findViewById(R.id.balloon_item_title);
        snippet = (TextView) v.findViewById(R.id.balloon_item_snippet);
        able = (TextView) v.findViewById(R.id.balloon_item_able);
        src = (TextView) v.findViewById(R.id.balloon_item_src);
        spl = (TextView) v.findViewById(R.id.balloon_item_spl);

        // クローズボタンのハンドラ
        ImageView close = (ImageView) v.findViewById(R.id.balloon_close);
        close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMarkerItem();
                if (item.type == MarkerItem.TYPE_EDIT && listener != null) {
                    listener.onChanged(item);
                }
                parent.setVisibility(GONE);
            }
        });

        final Context ctx = context;
        btnSave = (Button) v.findViewById(R.id.button_save);
        btnDelete = (Button) v.findViewById(R.id.button_delete);
        btnCancel = (Button) v.findViewById(R.id.button_cancel);

        // 保存ボタンのリスナーを設定
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int msgId;
                if (item.type == MarkerItem.TYPE_NEW) {
                    msgId = R.string.dialog_save_new_message;
                } else {
                    msgId = R.string.dialog_save_message;
                }
                showDialog(ctx, ctx.getString(msgId), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            if (storeListener != null) {
                                saveMarkerItem();
                                parent.setVisibility(GONE);
                                storeListener.onSave(item);
                            }
                        }
                    }
                });
            }
        });

        // 削除ボタンのリスナーを設定
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(ctx, ctx.getString(R.string.dialog_delete_message),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    if (storeListener != null) {
                                        parent.setVisibility(GONE);
                                        storeListener.onDelete(item);
                                    }
                                }
                            }
                        });
            }
        });

        // 破棄ボタンのリスナーを設定
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int msgId;
                if (item.type == MarkerItem.TYPE_NEW) {
                    msgId = R.string.dialog_abort_new_message;
                } else {
                    msgId = R.string.dialog_abort_message;
                }
                showDialog(ctx, ctx.getString(msgId), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            if (storeListener != null) {
                                parent.setVisibility(GONE);
                                storeListener.onRollback(item);
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * 確認ダイアログを表示します.
     * 
     * @param context
     *            context
     * @param text
     *            メッセージ
     * @param listener
     *            ボタンを押したときのリスナー
     */
    private static void showDialog(Context context, String text,
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
            if (item.editSnippet == null || "".equals(item.editSnippet)) {
                getAddress(item.getPoint());
            }
            snippet.setText(item.editSnippet);
        } else {
            if (item.getSnippet() == null || "".equals(item.getSnippet())) {
                getAddress(item.getPoint());
            }
            snippet.setText(item.getSnippet());
        }
        able.setText(item.able);
        src.setText(item.src);
        spl.setText(item.spl);

        // 新規マーカーの場合は削除ボタンを非表示
        if (item.type == MarkerItem.TYPE_NEW) {
            btnDelete.setVisibility(View.GONE);
            btnSave.setText(R.string.button_save_new);
            btnCancel.setText(R.string.button_abort_new);
        } else {
            btnDelete.setVisibility(View.VISIBLE);
            btnSave.setText(R.string.button_save);
            btnCancel.setText(R.string.button_abort);
        }
    }

    private void getAddress(final GeoPoint geoPoint) {
        Thread searchAdress = new Thread() {
            @Override
            public void run() {
                // 場所名を文字列で取得する
                String strAddress = null;
                try {
                    // 住所を取得
                    double latitude = geoPoint.getLatitudeE6() / 1E6;
                    double longitude = geoPoint.getLongitudeE6() / 1E6;

                    Address address = GeocodeManager.point2address(latitude, longitude, context);
                    strAddress = GeocodeManager.concatAddress(address);
                } catch (IOException e) {
                    strAddress = "";
                }

                // 住所をメッセージに持たせて
                // ハンドラにUIを書き換えさせる
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("str_address", strAddress);
                message.setData(bundle);
                addrhandler.sendMessage(message);
            }
        };
        searchAdress.start();
    }

    // ラベルを書き換えるためのハンドラ
    final Handler addrhandler = new Handler() {
        // @Override
        @Override
        public void handleMessage(Message msg) {
            String str_address = msg.getData().get("str_address").toString();
            snippet.setText(str_address);
        }
    };

    /*
     * (非 Javadoc)
     * 
     * @see
     * com.tcf_corp.android.aed.baloon.LocationBalloonOverlayView#saveMarkerItem
     * ()
     */
    @Override
    public MarkerItem saveMarkerItem() {
        if (DEBUG) {
            LogUtil.v(TAG, "saveMarkerItem");
        }
        if (isChanged()) {
            // ダイアログを閉じた時に通常マーカーが編集されていたら
            // マーカーのインスタンスを生成する.
            item = new MarkerItem(item.id, item.getPoint(), title.getText().toString(), snippet
                    .getText().toString());
            item.type = MarkerItem.TYPE_EDIT;
        }
        item.editTitle = title.getText().toString();
        item.editSnippet = snippet.getText().toString();
        item.able = able.getText().toString();
        item.src = src.getText().toString();
        item.spl = spl.getText().toString();
        // キーボードを隠す
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(this.getApplicationWindowToken(), 0);
        return item;
    }

    private boolean isChanged() {
        boolean ret = false;
        if (item.type == MarkerItem.TYPE_ORIGNAL || item.type == MarkerItem.TYPE_HOT) {
            if (item.getTitle() != null && title.getText().toString() != null) {
                if (item.getTitle().equals(title.getText().toString()) == false) {
                    return true;
                }
            }
            if (item.getSnippet() != null && snippet.getText().toString() != null) {
                if (item.getSnippet().equals(snippet.getText().toString()) == false) {
                    return true;
                }
            }
            if (item.able != null && able.getText().toString() != null) {
                if (item.able.equals(able.getText().toString()) == false) {
                    return true;
                }
            }
            if (item.src != null && src.getText().toString() != null) {
                if (item.src.equals(src.getText().toString()) == false) {
                    return true;
                }
            }
            if (item.spl != null && spl.getText().toString() != null) {
                if (item.spl.equals(spl.getText().toString()) == false) {
                    return true;
                }
            }
        }
        return ret;
    }

    /**
     * 編集を通知するリスナーを設定します.
     * 
     * @param listener
     *            リスナー
     */
    public void setOnItemChangedListener(OnItemChangedListener listener) {
        this.listener = listener;
    }

    private OnItemChangedListener listener;

    /**
     * 編集を通知するためのリスナーです.
     * 
     * @author yamadaisao
     * 
     */
    public interface OnItemChangedListener {
        public void onChanged(MarkerItem item);
    }

    /**
     * 確認ダイアログでの結果を通知するリスナーを設定します.
     * 
     * @param storeListener
     *            リスナー
     */
    public void setOnItemStoreListener(OnItemStoreListener storeListener) {
        this.storeListener = storeListener;
    }

    private OnItemStoreListener storeListener;

    /**
     * マーカーの変更を通知するためのリスナーです.
     * 
     * @author yamadaisao
     * 
     */
    public interface OnItemStoreListener {
        /**
         * 保存ボタンを押した時のイベント.
         * 
         * @param item
         *            対象のマーカー
         */
        public void onSave(MarkerItem item);

        /**
         * 編集を破棄した時のイベント
         * 
         * @param item
         *            対象のマーカー
         */
        public void onRollback(MarkerItem item);

        /**
         * 削除した時のイベント
         * 
         * @param item
         *            対象のマーカー
         */
        public void onDelete(MarkerItem item);
    }
}
