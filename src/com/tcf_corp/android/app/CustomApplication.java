package com.tcf_corp.android.app;

import android.app.Application;

import com.tcf_corp.android.util.DeployUtil;

/**
 * Application クラスの拡張.
 * 
 * @author yamadaisao
 */
public class CustomApplication extends Application {

    /**
     * 
     */
    public static boolean isDebuggable;

    @Override
    public void onCreate() {
        // デバッグモードか調べる
        isDebuggable = DeployUtil.isDebuggable(getApplicationContext());
    }
}
