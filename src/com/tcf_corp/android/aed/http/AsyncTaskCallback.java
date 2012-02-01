package com.tcf_corp.android.aed.http;

public interface AsyncTaskCallback<T> {

	void onSuccess(T data);

	void onFailed(int resId, String... args);

	void onAppFailed(T data);
}
