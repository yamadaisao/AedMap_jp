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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.tcf_corp.android.aed.http.MarkerItem;
import com.tcf_corp.android.map.MapUtil;
import com.tcf_corp.android.util.LogUtil;

/**
 * マーカーをマップの中心地からの直線距離順に並び替えて表示します.
 * 
 * @author yamada.isao
 * 
 */
public class AedListActivity extends Activity {
    private static final String TAG = AedListActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    ListView listView;
    TabHostActivity parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.aed_list);
        listView = (ListView) findViewById(R.id.aed_list_view);
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
                holder.src.setText(item.src);
                holder.spl.setText(item.spl);
                holder.dist.setText(item.dist.toString() + "m");
            }
            return view;
        }
    }
}
