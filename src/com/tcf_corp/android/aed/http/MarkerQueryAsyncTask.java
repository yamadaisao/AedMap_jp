package com.tcf_corp.android.aed.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
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
import com.tcf_corp.android.aed.R;
import com.tcf_corp.android.util.LogUtil;

public class MarkerQueryAsyncTask extends
		AsyncTask<MarkerItemQuery, Integer, AsyncTaskResult<List<MarkerItem>>> {

	private static final String TAG = MarkerQueryAsyncTask.class
			.getSimpleName();

	private final AsyncTaskCallback<List<MarkerItem>> callback;

	public MarkerQueryAsyncTask(AsyncTaskCallback<List<MarkerItem>> callback) {
		this.callback = callback;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected AsyncTaskResult<List<MarkerItem>> doInBackground(
			MarkerItemQuery... param) {
		List<MarkerItem> markers = null;

		HttpResponse response = null;
		String url = String.format("%s?lat=%s&lng=%s", param[0].getUrl(),
				param[0].getLatitude(), param[0].getLongitude());
		try {
			HttpGet get = new HttpGet(url);
			Locale locale = Locale.getDefault();
			get.addHeader("accept-language", locale.getLanguage());

			LogUtil.d(TAG, "connect to '" + url + "'");
			DefaultHttpClient httpClient = new DefaultHttpClient();
			response = httpClient.execute(get);
			LogUtil.d(TAG, "execute");

			int status = response.getStatusLine().getStatusCode();
			LogUtil.d(TAG, "status:" + status);
			switch (status) {
			case HttpStatus.SC_OK:
				InputStream is = response.getEntity().getContent();
				XmlPullParser xmlPullParser = Xml.newPullParser();
				xmlPullParser.setInput(is, "UTF-8");
				// ・XmlPullParser.START_DOCUMENT
				// ・XmlPullParser.START_TAG
				// ・XmlPullParser.TEXT
				// ・XmlPullParser.END_TAG
				// ・XmlPullParser.END_DOCUMENT
				markers = new ArrayList<MarkerItem>();
				for (int e = xmlPullParser.getEventType(); e != XmlPullParser.END_DOCUMENT; e = xmlPullParser
						.next()) {
					switch (e) {
					case XmlPullParser.START_TAG:
						try {
							if ("marker".equals(xmlPullParser.getName())) {
								long id = Long.parseLong(xmlPullParser
										.getAttributeValue(null, "id"));
								int lat = (int) (Double
										.parseDouble(xmlPullParser
												.getAttributeValue(null, "lat")) * 1E6);
								int lng = (int) (Double
										.parseDouble(xmlPullParser
												.getAttributeValue(null, "lng")) * 1E6);
								String name = xmlPullParser.getAttributeValue(
										null, "name");
								String adr = xmlPullParser.getAttributeValue(
										null, "adr");

								MarkerItem marker = new MarkerItem(id,
										new GeoPoint(lat, lng), name, adr);
								marker.able = xmlPullParser.getAttributeValue(
										null, "able");
								marker.src = xmlPullParser.getAttributeValue(
										null, "src");
								marker.spl = xmlPullParser.getAttributeValue(
										null, "spl");
								marker.time = xmlPullParser.getAttributeValue(
										null, "time");
								markers.add(marker);

							} else if ("markers"
									.equals(xmlPullParser.getName())) {

							}
						} catch (NumberFormatException ex) {
							Log.e(TAG, "NumberFormatException");
							ex.printStackTrace();
						}
						break;
					case XmlPullParser.END_TAG:
						if ("marker".equals(xmlPullParser.getName())) {

						} else if ("markers".equals(xmlPullParser.getName())) {

						}
						break;
					case XmlPullParser.START_DOCUMENT:
					case XmlPullParser.END_DOCUMENT:
					case XmlPullParser.TEXT:
					default:
						break;
					}
				}

				is.close();
				return AsyncTaskResult.createNormalResult(markers);
			default:
				Log.e(TAG, "server error");
				return AsyncTaskResult
						.createErrorResult(R.string.http_server_error);
			}

		} catch (IllegalStateException e) {
			Log.e(TAG, "illegal state");
			e.printStackTrace();
			return AsyncTaskResult
					.createErrorResult(R.string.http_illegal_state);
		} catch (UnknownHostException e) {
			Log.e(TAG, "unknown host:" + e.getMessage());
			e.printStackTrace();
			return AsyncTaskResult
					.createErrorResult(R.string.http_unknown_host);
		} catch (IOException e) {
			Log.e(TAG, "io error");
			e.printStackTrace();
			return AsyncTaskResult
					.createErrorResult(R.string.http_unkown_error);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "XmlPullParserException");
			e.printStackTrace();
			return AsyncTaskResult.createErrorResult(R.string.http_parse_error);
		}
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<List<MarkerItem>> result) {
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
