package com.tcf_corp.android.aed;

import java.lang.reflect.Field;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class HelpView extends FrameLayout {
    WebView webView;

    public HelpView(Context context, String url) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.help_view, null);
        addView(v);

        webView = (WebView) v.findViewById(R.id.webView);
        WebViewClient client = new WebViewClient();
        webView.setWebViewClient(client);
        WebChromeClient chromeClient = new WebChromeClient();
        webView.setWebChromeClient(chromeClient);
        WebSettings ws = webView.getSettings();
        ws.setBuiltInZoomControls(true);
        ws.setSupportZoom(true);
        try {
            // マルチタッチを有効にしたまま、zoom controlを消す
            Field nameField = ws.getClass().getDeclaredField("mBuiltInZoomControls");
            nameField.setAccessible(true);
            nameField.set(ws, false);
        } catch (Exception e) {
            e.printStackTrace();
            ws.setBuiltInZoomControls(false);
        }

        ImageView close = (ImageView) v.findViewById(R.id.help_close);
        close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                HelpView.this.setVisibility(GONE);
            }
        });

        webView.loadUrl(url);
    }

    public void loadUrl(String url) {
        webView.loadUrl(url);
    }
}
