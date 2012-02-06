package com.tcf_corp.android.aed.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import com.google.android.maps.GeoPoint;
import com.tcf_corp.android.aed.Constants;
import com.tcf_corp.android.aed.R;
import com.tcf_corp.android.util.LogUtil;

public class MarkerQueryAsyncTask extends
        AsyncTask<MarkerItemQuery, Integer, AsyncTaskResult<MarkerItemResult>> {

    private static final String TAG = MarkerQueryAsyncTask.class.getSimpleName();

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
        String url = String.format("%s?lat=%s&lng=%s", param[0].getUrl(), param[0].getLatitude(),
                param[0].getLongitude());
        try {
            HttpGet get = new HttpGet(url);
            Locale locale = Locale.getDefault();
            get.addHeader("accept-language", locale.getLanguage());

            LogUtil.v(TAG, "connect to '" + url + "'");
            DefaultHttpClient httpClient = new DefaultHttpClient();
            response = httpClient.execute(get);
            LogUtil.v(TAG, "execute");

            int status = response.getStatusLine().getStatusCode();
            LogUtil.v(TAG, "status:" + status);
            switch (status) {
            case HttpStatus.SC_OK:
                InputStream is = response.getEntity().getContent();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(is, "UTF-8");
                // ・XmlPullParser.START_DOCUMENT
                // ・XmlPullParser.START_TAG
                // ・XmlPullParser.TEXT
                // ・XmlPullParser.END_TAG
                // ・XmlPullParser.END_DOCUMENT
                result = new MarkerItemResult(param[0].getPoint());
                for (int e = parser.getEventType(); e != XmlPullParser.END_DOCUMENT; e = parser
                        .next()) {
                    switch (e) {
                    case XmlPullParser.START_TAG:
                        try {
                            if ("marker".equals(parser.getName())) {
                                long id = Long.parseLong(parser.getAttributeValue(null, "id"));
                                int lat = (int) (Double.parseDouble(parser.getAttributeValue(null,
                                        "lat")) * 1E6);
                                int lng = (int) (Double.parseDouble(parser.getAttributeValue(null,
                                        "lng")) * 1E6);

                                result.minLatitude1E6 = Math.min(result.minLatitude1E6, lat
                                        + Constants.LATITUDE_1E6);
                                result.minLongitude1E6 = Math.min(result.minLongitude1E6, lng
                                        + Constants.LONGITUDE_1E6);
                                result.maxLatitude1E6 = Math.max(result.maxLatitude1E6, lat
                                        + Constants.LATITUDE_1E6);
                                result.maxLongitude1E6 = Math.max(result.maxLongitude1E6, lng
                                        + Constants.LONGITUDE_1E6);
                                String name = parser.getAttributeValue(null, "name");
                                String adr = parser.getAttributeValue(null, "adr");

                                MarkerItem marker = new MarkerItem(id, new GeoPoint(lat, lng),
                                        name, adr);
                                marker.able = parser.getAttributeValue(null, "able");
                                marker.src = parser.getAttributeValue(null, "src");
                                marker.spl = parser.getAttributeValue(null, "spl");
                                marker.time = parser.getAttributeValue(null, "time");
                                result.markers.add(marker);

                            } else if ("markers".equals(parser.getName())) {

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
            return AsyncTaskResult.createErrorResult(R.string.http_unkown_error);
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
