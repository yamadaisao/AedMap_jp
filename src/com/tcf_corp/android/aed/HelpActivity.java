package com.tcf_corp.android.aed;

import java.lang.reflect.Field;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

public class HelpActivity extends Activity {
    public static final String ARG_URL = "url";
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_view);

        webView = (WebView) findViewById(R.id.webView);
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

        ImageView close = (ImageView) findViewById(R.id.help_close);
        close.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String url = intent.getStringExtra(ARG_URL);
        webView.loadUrl(url);
    }
}
