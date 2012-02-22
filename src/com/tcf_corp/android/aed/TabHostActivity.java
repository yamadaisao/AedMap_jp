package com.tcf_corp.android.aed;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Debug;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class TabHostActivity extends TabActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Debug.startMethodTracing("aedmap");

        TabHost host = this.getTabHost();
        Resources r = this.getResources();

        host.addTab(createTabSpec(host, AedMapActivity.class, 0, r.getString(R.string.tab_map),
                true));
        host.addTab(createTabSpec(host, AedListActivity.class, 0, r.getString(R.string.tab_list),
                false));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Debug.stopMethodTracing();
    }

    /**
     * タブを生成します。
     * 
     * @param owner
     *            タブ画面。
     * @param icon
     *            アイコンのリソース識別子。
     * @param text
     *            テキスト。
     * @param layout
     *            タブのレイアウトを示すリソース識別子。
     * 
     * @return 生成されたタブ情報。
     */
    private TabSpec createTabSpec(TabHost host, Class<? extends Activity> child, int icon,
            String text, boolean isFirst) {
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

        TabSpec spec = host.newTabSpec(text);
        spec.setIndicator(v);

        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), child);
        spec.setContent(intent);

        return spec;
    }
}