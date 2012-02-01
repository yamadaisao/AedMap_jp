package com.tcf_corp.android.aed;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

public class GeocodeManager {
	// 座標から住所文字列へ変換
	public static String point2address(double latitude, double longitude,
			Context context) throws IOException {
		String address_string = new String();

		// 変換実行
		Geocoder coder = new Geocoder(context, Locale.JAPAN);
		List<Address> list_address = coder.getFromLocation(latitude, longitude,
				1);

		if (!list_address.isEmpty()) {

			// 変換成功時は，最初の変換候補を取得
			Address address = list_address.get(0);
			StringBuffer sb = new StringBuffer();

			// adressの大区分から小区分までを改行で全結合
			String s;
			for (int i = 0; (s = address.getAddressLine(i)) != null; i++) {
				sb.append(s + "\n");
			}

			address_string = sb.toString();
		}

		return address_string;
	}

}
