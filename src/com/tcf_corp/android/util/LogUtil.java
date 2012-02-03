package com.tcf_corp.android.util;

import android.util.Log;

import com.tcf_corp.android.app.CustomApplication;

public class LogUtil {

	/**
	 * デバッグログを出力する マニュフェストファイルでデバッグモードになっていなければ出力しない
	 */
	public static final void d(String tag, String msg) {
		if (CustomApplication.isDebuggable) {
			Log.d(tag, msg);
		}
	}

	public static final void v(String tag, String msg) {
		if (CustomApplication.isDebuggable) {
			Log.v(tag, msg);
		}
	}
}
