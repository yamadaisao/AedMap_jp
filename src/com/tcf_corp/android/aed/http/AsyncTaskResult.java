package com.tcf_corp.android.aed.http;

public class AsyncTaskResult<T> {
	private final T result;
	private final int resId;
	private final boolean error;

	private AsyncTaskResult(T result, boolean isError, int resId) {
		this.result = result;
		this.error = isError;
		this.resId = resId;
	}

	public static <T> AsyncTaskResult<T> createNormalResult(T result) {
		return new AsyncTaskResult<T>(result, false, 0);
	}

	public static <T> AsyncTaskResult<T> createAppErrorResult(T result) {
		return new AsyncTaskResult<T>(result, true, 0);
	}

	public static <T> AsyncTaskResult<T> createErrorResult(int resId) {
		return new AsyncTaskResult<T>(null, true, resId);
	}

	public T getResult() {
		return result;
	}

	public int getResId() {
		return resId;
	}

	public boolean isError() {
		return error;
	}
}
