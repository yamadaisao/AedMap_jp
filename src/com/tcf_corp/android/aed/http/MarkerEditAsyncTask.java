package com.tcf_corp.android.aed.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;
import android.util.Log;

import com.tcf_corp.android.aed.R;
import com.tcf_corp.android.util.LogUtil;

public class MarkerEditAsyncTask extends
        AsyncTask<MarkerItem, Integer, AsyncTaskResult<MarkerItemResult>> {

    private static final String TAG = MarkerEditAsyncTask.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final String NEW_URL = "http://aedm.jp/post.php";
    private static final String EDIT_URL = "http://aedm.jp/editdelete.php";

    private final AsyncTaskCallback<MarkerItemResult> callback;

    public MarkerEditAsyncTask(AsyncTaskCallback<MarkerItemResult> callback) {
        this.callback = callback;
    }

    @Override
    protected AsyncTaskResult<MarkerItemResult> doInBackground(MarkerItem... param) {
        MarkerItemResult result = null;
        MarkerItem item = param[0];
        HttpURLConnection conn = null;
        try {
            List<NameValuePair> paramList = new ArrayList<NameValuePair>();
            double lat = item.getPoint().getLatitudeE6() / 1E6;
            double lng = item.getPoint().getLongitudeE6() / 1E6;
            paramList.add(new BasicNameValuePair("lat", Double.toString(lat)));
            paramList.add(new BasicNameValuePair("lng", Double.toString(lng)));
            paramList.add(new BasicNameValuePair("name", item.editTitle));
            paramList.add(new BasicNameValuePair("adr", item.editSnippet));
            paramList.add(new BasicNameValuePair("able", item.able));
            paramList.add(new BasicNameValuePair("src", item.src));
            paramList.add(new BasicNameValuePair("spl", item.spl));

            URL url;
            if (item.type == MarkerItem.TYPE_NEW) {
                url = new URL(NEW_URL);
            } else if (item.type == MarkerItem.TYPE_EDIT) {
                url = new URL(EDIT_URL);
                paramList.add(new BasicNameValuePair("id", Long.toString(item.id)));
                paramList.add(new BasicNameValuePair("edit", "上記の通り内容を変更"));
            } else {
                url = new URL(EDIT_URL);
                paramList.add(new BasicNameValuePair("id", Long.toString(item.id)));
                paramList.add(new BasicNameValuePair("delete", "マーカーを削除"));
            }
            conn = (HttpURLConnection) url.openConnection();

            Locale locale = Locale.getDefault();
            conn.setRequestMethod("POST");
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("accept-language", locale.getLanguage());
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
            String query = URLEncodedUtils.format(paramList, "UTF-8");
            if (DEBUG) {
                LogUtil.v(TAG, query);
            }
            osw.write(query);
            osw.flush();
            osw.close();

            // 接続
            conn.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String s;
            while ((s = reader.readLine()) != null) {
                Log.d(TAG, s);
            }

            int status = conn.getResponseCode();
            switch (status) {
            case HttpStatus.SC_OK:
            case HttpStatus.SC_MOVED_TEMPORARILY:
            case HttpStatus.SC_NOT_FOUND:
                result = new MarkerItemResult(param[0].getPoint());
                result.markers.add(item);
                return AsyncTaskResult.createNormalResult(result);
            default:
                Log.e(TAG, "server error:" + status);
                return AsyncTaskResult.createErrorResult(R.string.http_server_error);
            }

        } catch (IllegalStateException e) {
            Log.e(TAG, "illegal state");
            e.printStackTrace();
            return AsyncTaskResult.createErrorResult(R.string.http_illegal_state);
        } catch (UnknownHostException e) {
            Log.e(TAG, "unknown host:" + e.getMessage());
            e.printStackTrace();
            return AsyncTaskResult.createErrorResult(R.string.http_unknown_host);
        } catch (IOException e) {
            Log.e(TAG, "io error");
            e.printStackTrace();
            return AsyncTaskResult.createErrorResult(R.string.http_io_error);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    protected void onPostExecute(AsyncTaskResult<MarkerItemResult> result) {
        if (result.isError()) {
            if (result.getResId() == 0) {
                callback.onAppFailed(result.getResult());
            } else {
                callback.onFailed(result.getResId(), (String[]) null);
            }
        } else {
            callback.onSuccess(result.getResult());
        }
    }
}
