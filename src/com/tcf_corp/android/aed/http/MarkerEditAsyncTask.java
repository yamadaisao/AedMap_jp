package com.tcf_corp.android.aed.http;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

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
        HttpResponse response = null;
        MarkerItem item = param[0];
        // HttpURLConnection
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

            HttpPost httpPost;
            if (item.type == MarkerItem.TYPE_NEW) {
                // パラメータをクエリに変換
                httpPost = new HttpPost(NEW_URL);
                httpPost.setEntity(new UrlEncodedFormEntity(paramList, "UTF-8"));
            } else if (item.type == MarkerItem.TYPE_EDIT) {
                paramList.add(new BasicNameValuePair("edit", ""));
                httpPost = new HttpPost(EDIT_URL);
                httpPost.setEntity(new UrlEncodedFormEntity(paramList, "UTF-8"));
            } else {
                paramList.add(new BasicNameValuePair("delete", ""));
                httpPost = new HttpPost(EDIT_URL);
                httpPost.setEntity(new UrlEncodedFormEntity(paramList, "UTF-8"));
            }
            Locale locale = Locale.getDefault();
            httpPost.addHeader("accept-language", locale.getLanguage());
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

            if (DEBUG) {
                LogUtil.v(TAG, "connect to '" + httpPost.getURI() + "'");
            }
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpParams params = httpClient.getParams();
            // 接続のタイムアウト
            HttpConnectionParams.setConnectionTimeout(params, 10000);
            // データ取得のタイムアウト
            HttpConnectionParams.setSoTimeout(params, 10000);
            response = httpClient.execute(httpPost);

            int status = response.getStatusLine().getStatusCode();
            switch (status) {
            case HttpStatus.SC_OK:
                Log.d(TAG, EntityUtils.toString(response.getEntity(), "UTF-8"));
                result = new MarkerItemResult(param[0].getPoint());
                return AsyncTaskResult.createNormalResult(result);
            default:
                Log.e(TAG, "server error:" + status);
                Log.d(TAG, EntityUtils.toString(response.getEntity(), "UTF-8"));
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
            // } catch (XmlPullParserException e) {
            // Log.e(TAG, "XmlPullParserException");
            // e.printStackTrace();
            // return
            // AsyncTaskResult.createErrorResult(R.string.http_parse_error);
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
