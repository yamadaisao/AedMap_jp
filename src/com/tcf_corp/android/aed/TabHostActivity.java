package com.tcf_corp.android.aed;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Address;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.util.GeocodeManager;
import com.tcf_corp.android.util.LogUtil;

/**
 * メインのタブactivityです.
 * 
 * @author yamada isao
 * 
 */
public class TabHostActivity extends TabActivity {

    private static final String TAG = TabHostActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String TAB_MAP = "map";
    private static final String TAB_LIST = "list";

    private AlertDialog dialog = null;

    /**
     * タブを生成します.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Debug.startMethodTracing("aedmap");
        // タイトルバーを非表示にする
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        TabHost host = getTabHost();
        Resources r = getResources();

        host.addTab(createTabSpec(host, AedMapActivity.class, TAB_MAP, 0,
                r.getString(R.string.tab_map), true));
        host.addTab(createTabSpec(host, AedListActivity.class, TAB_LIST, 0,
                r.getString(R.string.tab_list), false));

        // リストが表示された場合にリスナーを登録
        host.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (TAB_LIST.equals(tabId)) {
                    Activity activity = getCurrentActivity();
                    if (activity instanceof AedListActivity) {
                        AedListActivity listActivity = (AedListActivity) activity;
                        listActivity.setOnItemClickListener(listener);
                    }
                }
            }
        });
    }

    private final AedListActivity.OnItemClickListener listener = new AedListActivity.OnItemClickListener() {
        @Override
        public void OnItemClick(MarkerItem markerItem) {
            TabHost host = getTabHost();
            host.setCurrentTabByTag(TAB_MAP);
            moveToSearchResult(markerItem.getPoint());
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Debug.stopMethodTracing();
    }

    /**
     * タブを生成します.
     * 
     * @param host
     *            TabHost.
     * @param tabId
     *            タブid.
     * @param icon
     *            アイコンのリソース識別子.
     * @param text
     *            テキスト.
     * @param isFirst
     *            最初のタブの場合はtrue.
     * 
     * @return 生成されたタブ情報.
     */
    private TabSpec createTabSpec(TabHost host, Class<? extends Activity> child, String tabId,
            int icon, String text, boolean isFirst) {
        View v = LayoutInflater.from(this).inflate(R.layout.tab_item, null);

        // 始点なら区切り線を消す
        if (isFirst) {
            View sep = v.findViewById(R.id.tab_item_separator);
            sep.setBackgroundColor(0);
        }

        if (icon != 0) {
            ((ImageView) v.findViewById(R.id.tab_item_icon)).setImageResource(icon);
        }
        ((TextView) v.findViewById(R.id.tab_item_text)).setText(text);

        TabSpec spec = host.newTabSpec(tabId);
        spec.setIndicator(v);

        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), child);
        spec.setContent(intent);

        return spec;
    }

    /**
     * 検索ダイアログで入力された文字列で住所検索を行います.
     */
    @Override
    public void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            // GeoCoderで地名検索させて、Addressに変換させた。
            List<Address> addressList = GeocodeManager.address2Point(query, this);
            if (addressList == null) {
                Toast.makeText(this, R.string.msg_query_fail, Toast.LENGTH_LONG).show();
            } else {
                if (addressList.size() == 0) {
                    Toast.makeText(this, R.string.msg_result_zero, Toast.LENGTH_LONG).show();
                } else if (addressList.size() == 1) {
                    Address address = addressList.get(0);
                    AddressView row = new AddressView();
                    row.address = GeocodeManager.concatAddress(address);
                    row.latitude = address.getLatitude();
                    row.longitude = address.getLongitude();
                    moveToSearchResult(row);
                } else {
                    final List<AddressView> rows = new ArrayList<AddressView>();
                    for (Address address : addressList) {
                        AddressView row = new AddressView();
                        row.address = GeocodeManager.concatAddress(address);
                        row.latitude = address.getLatitude();
                        row.longitude = address.getLongitude();
                        rows.add(row);
                    }
                    ListView lv = new ListView(this);
                    lv.setAdapter(new AddressAdapter(this, R.layout.address_list, rows));
                    lv.setScrollingCacheEnabled(false);
                    lv.setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> items, View view, int position,
                                long id) {
                            dialog.dismiss();
                            moveToSearchResult(rows.get(position));
                        }
                    });
                    dialog = new AlertDialog.Builder(this).setTitle(R.string.msg_some_location)
                            .setPositiveButton(R.string.button_cancel, null).setView(lv).create();
                    dialog.show();

                }
            }
        }
    }

    /**
     * マップの中心点を移動します.
     * 
     * @param row
     *            中心点のAddressView
     */
    private void moveToSearchResult(AddressView row) {
        Activity target = getCurrentActivity();
        if (target instanceof AedMapActivity) {
            Toast.makeText(this, row.address, Toast.LENGTH_SHORT).show();
            if (DEBUG) {
                LogUtil.v(TAG, "current is AedMapActivity");
            }
            int lat = (int) (row.latitude * 1E6);
            int lng = (int) (row.longitude * 1E6);
            ((AedMapActivity) target).moveToSearchResult(new GeoPoint(lat, lng));
        } else {
            if (DEBUG) {
                LogUtil.v(TAG, "current is nothing");
            }
        }
    }

    /**
     * マップの中心点を移動します.
     * 
     * @param point
     *            中心点のGeoPoint
     */
    private void moveToSearchResult(GeoPoint point) {
        Activity target = getCurrentActivity();
        if (target instanceof AedMapActivity) {
            if (DEBUG) {
                LogUtil.v(TAG, "current is AedMapActivity");
            }
            ((AedMapActivity) target).moveToSearchResult(point);
        } else {
            if (DEBUG) {
                LogUtil.v(TAG, "current is nothing");
            }
        }
    }

    /**
     * 住所候補のViewHolder
     * 
     * @author yamadaisao
     * 
     */
    static class ViewHolder {
        TextView address;
    }

    /**
     * 住所候補のデータ
     * 
     * @author yamadaisao
     * 
     */
    class AddressView {
        String address;
        double latitude;
        double longitude;
    }

    /**
     * 住所検索の結果が複数あった場合のリスト表示用Adapter
     * 
     * @author yamadaisao
     * 
     */
    private class AddressAdapter extends ArrayAdapter<AddressView> {
        private final LayoutInflater inflater;
        private final List<AddressView> list;
        private final int rowLayout;

        public AddressAdapter(Context context, int textViewResourceId, List<AddressView> objects) {
            super(context, textViewResourceId, objects);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            list = objects;
            rowLayout = textViewResourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                view = inflater.inflate(rowLayout, null);
                holder.address = (TextView) view.findViewById(R.id.address);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            AddressView address = list.get(position);
            if (address != null) {
                holder.address.setText(address.address);
            }
            return view;
        }
    }
}