package com.tcf_corp.android.aed;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.map.MapUtil;
import com.tcf_corp.android.util.LogUtil;

/**
 * マーカーをマップの中心地からの直線距離順に並び替えて表示します.
 * 
 * 対象のマーカーには編集中のマーカーは含まれません.
 * 
 * @author yamada.isao
 * 
 */
public class AedListActivity extends Activity {
    private static final String TAG = AedListActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    ListView listView;
    TabHostActivity parent;

    private String srcHeader;
    private String splHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.aed_list);
        listView = (ListView) findViewById(R.id.aed_list_view);

        srcHeader = getResources().getString(R.string.header_src);
        splHeader = getResources().getString(R.string.header_spl);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.v(TAG, "onResume");
        SharedData data = SharedData.getInstance();
        GeoPoint current = data.getGeoPoint();

        if (data.getLastResult() != null) {
            List<MarkerItem> list = data.getLastResult().markers;
            for (MarkerItem item : list) {
                item.dist = MapUtil.getDistance(current, item.getPoint());
            }
            Collections.sort(list, new MarkerComparator());
            // TextView の autoLinkがある場合は、
            // getApplicationContextではなくthisを渡さないといけない.
            listView.setAdapter(new AedAdapter(this, data.getLastResult().markers));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ListView listView = (ListView) parent;
                    MarkerItem marker = (MarkerItem) listView.getItemAtPosition(position);
                    Toast.makeText(getApplicationContext(), marker.getTitle(), Toast.LENGTH_SHORT)
                            .show();
                    if (listener != null) {
                        listener.OnItemClick(marker);
                    }
                }
            });
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        LogUtil.v(TAG, "onRestoreInstanceState");
        if (state != null) {
            SharedData data = state.getParcelable("data");
            if (DEBUG) {
                LogUtil.d(TAG, "edit:" + data.getEditList().size());
            }
        }
    }

    /**
     * MarkerItemの比較クラス.
     * 
     * @author yamadaisao
     * 
     */
    class MarkerComparator implements Comparator<MarkerItem> {
        @Override
        public int compare(MarkerItem o1, MarkerItem o2) {
            return (int) (o1.dist - o2.dist);
        }
    }

    /**
     * ViewHolder クラス. ListViewの各行です.
     * 
     * @author yamada.isao
     * 
     */
    static class ViewHolder {
        TextView title;
        TextView snippet;
        TextView able;
        TextView src;
        TextView spl;
        TextView dist;
    }

    /**
     * カスタムリスト用のadapter.
     * 
     * @author yamada.isao
     * 
     */
    class AedAdapter extends ArrayAdapter<MarkerItem> {

        private final LayoutInflater inflater;

        public AedAdapter(Context context, List<MarkerItem> objects) {
            super(context, 0, objects);
            this.inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Viewの取得
            ViewHolder holder;
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.aed_list_row, null);
                holder = new ViewHolder();
                holder.title = (TextView) view.findViewById(R.id.row_title);
                holder.snippet = (TextView) view.findViewById(R.id.row_snippet);
                holder.able = (TextView) view.findViewById(R.id.row_able);
                holder.src = (TextView) view.findViewById(R.id.row_src);
                holder.spl = (TextView) view.findViewById(R.id.row_spl);
                holder.dist = (TextView) view.findViewById(R.id.row_dist);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // データの設定
            MarkerItem item = getItem(position);
            if (item != null) {
                holder.title.setText(item.getTitle());
                holder.snippet.setText(item.getSnippet());
                holder.able.setText(item.able);
                holder.src.setText(String.format(srcHeader, item.src));
                holder.spl.setText(String.format(splHeader, item.spl));
                holder.dist.setText(item.dist.toString() + "m");
            }
            return view;
        }
    }

    OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void OnItemClick(MarkerItem markerItem);
    }
}
