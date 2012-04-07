package com.tcf_corp.android.aed.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import com.google.android.maps.GeoPoint;
import com.tcf_corp.android.aed.R;
import com.tcf_corp.android.util.LogUtil;

public class MarkerEditAsyncTask extends
        AsyncTask<MarkerItem, Integer, AsyncTaskResult<MarkerItemResult>> {

    private static final String TAG = MarkerEditAsyncTask.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String POST_URL = "http://aedm.jp/posttest.php";

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

            URL url = new URL(POST_URL);
            if (item.type == MarkerItem.TYPE_NEW) {
                paramList.add(new BasicNameValuePair("cmd", "new"));
            } else if (item.type == MarkerItem.TYPE_EDIT || item.type == MarkerItem.TYPE_ORIGNAL) {
                paramList.add(new BasicNameValuePair("id", Long.toString(item.id)));
                paramList.add(new BasicNameValuePair("cmd", "edit"));
            } else if (item.type == MarkerItem.TYPE_DELETE) {
                paramList.add(new BasicNameValuePair("id", Long.toString(item.id)));
                paramList.add(new BasicNameValuePair("cmd", "delete"));
            } else {
                LogUtil.v(TAG, "type:" + item.type);
                return AsyncTaskResult.createErrorResult(R.string.http_parameter_error);
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
                LogUtil.v(TAG, POST_URL);
                LogUtil.v(TAG, query);
            }
            osw.write(query);
            osw.flush();
            osw.close();

            // 接続
            conn.connect();
            // if (false && DEBUG) {
            // BufferedReader reader = new BufferedReader(new InputStreamReader(
            // conn.getInputStream()));
            // String s;
            // while ((s = reader.readLine()) != null) {
            // Log.d(TAG, s);
            // }
            // }

            int status = conn.getResponseCode();
            switch (status) {
            case HttpStatus.SC_OK:
                SimpleDateFormat fomatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Calendar now = Calendar.getInstance();
                now.add(Calendar.MONTH, -1);
                Date nowDate = now.getTime();

                result = new MarkerItemResult(param[0].getPoint());
                result.targetMarker = item;

                InputStream is = new BufferedInputStream(conn.getInputStream());

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
                    case XmlPullParser.START_TAG: {
                        try {
                            // other tag : markers
                            String tag = parser.getName();
                            if ("marker".equals(tag)) {
                                long id = Long.parseLong(parser.getAttributeValue(null, "id"));
                                long latitude = (long) (Double.parseDouble(parser
                                        .getAttributeValue(null, "lat")) * 1E6);
                                long longitude = (long) (Double.parseDouble(parser
                                        .getAttributeValue(null, "lng")) * 1E6);

                                String name = parser.getAttributeValue(null, "name");
                                String adr = parser.getAttributeValue(null, "adr");

                                MarkerItem marker = new MarkerItem(id, new GeoPoint((int) latitude,
                                        (int) longitude), name, adr);
                                marker.able = parser.getAttributeValue(null, "able");
                                marker.src = parser.getAttributeValue(null, "src");
                                marker.spl = parser.getAttributeValue(null, "spl");
                                String time = parser.getAttributeValue(null, "time");
                                try {
                                    marker.time = fomatter.parse(time);
                                    if (nowDate.before(marker.time)) {
                                        marker.type = MarkerItem.TYPE_HOT;
                                    } else {
                                        marker.type = MarkerItem.TYPE_ORIGNAL;
                                    }
                                } catch (ParseException e1) {
                                    Log.e(TAG, "time=" + time);
                                    e1.printStackTrace();
                                    marker.time = new Date();
                                }
                                result.markers.add(marker);
                            } else {
                                if (DEBUG) {
                                    LogUtil.v(TAG, tag);
                                }
                            }
                        } catch (NumberFormatException ex) {
                            Log.e(TAG, "NumberFormatException");
                            ex.printStackTrace();
                        }
                        break;
                    }
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
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XmlPullParserException");
            e.printStackTrace();
            return AsyncTaskResult.createErrorResult(R.string.http_parse_error);
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
