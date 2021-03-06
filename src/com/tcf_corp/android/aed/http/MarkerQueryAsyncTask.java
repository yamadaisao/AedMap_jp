package com.tcf_corp.android.aed.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import com.google.android.maps.GeoPoint;
import com.tcf_corp.android.aed.R;
import com.tcf_corp.android.util.LogUtil;

public class MarkerQueryAsyncTask extends
        AsyncTask<MarkerItemQuery, Integer, AsyncTaskResult<MarkerItemResult>> {

    private static final String TAG = MarkerQueryAsyncTask.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final String QUERY_URL = "http://aedm.jp/toxmltest.php";

    private final AsyncTaskCallback<MarkerItemResult> callback;

    public MarkerQueryAsyncTask(AsyncTaskCallback<MarkerItemResult> callback) {
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected AsyncTaskResult<MarkerItemResult> doInBackground(MarkerItemQuery... param) {
        MarkerItemResult result = null;

        HttpResponse response = null;
        String url = String.format("%s?lat=%s&lng=%s", QUERY_URL, param[0].getLatitude(),
                param[0].getLongitude());
        SimpleDateFormat fomatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MONTH, -1);
        Date nowDate = now.getTime();
        try {
            HttpGet get = new HttpGet(url);
            Locale locale = Locale.getDefault();
            get.addHeader("accept-language", locale.getLanguage());

            if (DEBUG) {
                LogUtil.v(TAG, url);
            }
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpParams params = httpClient.getParams();
            // 接続のタイムアウト
            HttpConnectionParams.setConnectionTimeout(params, 10000);
            // データ取得のタイムアウト
            HttpConnectionParams.setSoTimeout(params, 10000);
            response = httpClient.execute(get);

            int status = response.getStatusLine().getStatusCode();
            switch (status) {
            case HttpStatus.SC_OK:
                result = new MarkerItemResult(param[0].getPoint());
                InputStream is = response.getEntity().getContent();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(is, "UTF-8");
                // ・XmlPullParser.START_DOCUMENT
                // ・XmlPullParser.START_TAG
                // ・XmlPullParser.TEXT
                // ・XmlPullParser.END_TAG
                // ・XmlPullParser.END_DOCUMENT
                for (int e = parser.getEventType(); e != XmlPullParser.END_DOCUMENT; e = parser
                        .next()) {
                    switch (e) {
                    case XmlPullParser.START_TAG:
                        try {
                            // other tag : markers
                            if ("marker".equals(parser.getName())) {
                                long id = Long.parseLong(parser.getAttributeValue(null, "id"));
                                long lat = (long) (Double.parseDouble(parser.getAttributeValue(
                                        null, "lat")) * 1E6);
                                long lng = (long) (Double.parseDouble(parser.getAttributeValue(
                                        null, "lng")) * 1E6);

                                // 日本限定なので、西経・南緯は気にしない.
                                result.minLatitude1E6 = Math.min(result.minLatitude1E6, lat);
                                result.minLongitude1E6 = Math.min(result.minLongitude1E6, lng);
                                result.maxLatitude1E6 = Math.max(result.maxLatitude1E6, lat);
                                result.maxLongitude1E6 = Math.max(result.maxLongitude1E6, lng);
                                String name = parser.getAttributeValue(null, "name");
                                String adr = parser.getAttributeValue(null, "adr");

                                MarkerItem marker = new MarkerItem(id, new GeoPoint((int) lat,
                                        (int) lng), name, adr);
                                marker.able = parser.getAttributeValue(null, "able");
                                marker.src = parser.getAttributeValue(null, "src");
                                marker.spl = parser.getAttributeValue(null, "spl");
                                String time = parser.getAttributeValue(null, "time");
                                try {
                                    marker.time = fomatter.parse(time);
                                    if (nowDate.before(marker.time)) {
                                        marker.type = MarkerItem.TYPE_HOT;
                                    }
                                } catch (ParseException e1) {
                                    Log.e(TAG, "time=" + time);
                                    e1.printStackTrace();
                                    marker.time = new Date();
                                }
                                result.markers.add(marker);
                            }
                        } catch (NumberFormatException ex) {
                            Log.e(TAG, "NumberFormatException");
                            ex.printStackTrace();
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    case XmlPullParser.START_DOCUMENT:
                    case XmlPullParser.END_DOCUMENT:
                    case XmlPullParser.TEXT:
                    default:
                        break;
                    }
                }

                is.close();
                // 少しだけ範囲を狭くする
                result.minLatitude1E6 += (param[0].getPoint().getLatitudeE6() - result.minLatitude1E6) / 3;
                result.minLongitude1E6 += (param[0].getPoint().getLongitudeE6() - result.minLongitude1E6) / 3;
                result.maxLatitude1E6 -= (result.maxLatitude1E6 - param[0].getPoint()
                        .getLatitudeE6()) / 3;
                result.maxLongitude1E6 -= (result.maxLongitude1E6 - param[0].getPoint()
                        .getLongitudeE6()) / 3;
                return AsyncTaskResult.createNormalResult(result);
            default:
                Log.e(TAG, "server error");
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
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XmlPullParserException");
            e.printStackTrace();
            return AsyncTaskResult.createErrorResult(R.string.http_parse_error);
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

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }
}
