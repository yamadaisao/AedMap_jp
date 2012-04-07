package com.tcf_corp.android.util;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

public class GeocodeManager {
    private static final String TAG = GeocodeManager.class.getSimpleName();
    @SuppressWarnings("unused")
    private static final boolean DEBUG = false;

    // 座標から住所文字列へ変換
    public static Address point2address(double latitude, double longitude, Context context)
            throws IOException {

        // 変換実行
        Geocoder coder = new Geocoder(context, Locale.getDefault());
        List<Address> list_address = coder.getFromLocation(latitude, longitude, 1);
        Address address;
        if (!list_address.isEmpty()) {
            // 変換成功時は，最初の変換候補を取得
            address = list_address.get(0);
        } else {
            address = new Address(Locale.getDefault());
        }
        return address;
    }

    public static List<Address> address2Point(String query, Context context) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> addressList = null;
        try {
            addressList = geocoder.getFromLocationName(query, 20);
        } catch (IOException e) {
            Log.e(TAG, "IOException 発生");
        }
        return addressList;
    }

    public static String concatAddress(Address address) {
        StringBuffer sb = new StringBuffer();

        // adressの大区分から小区分までを全結合
        String s;
        for (int i = 1; (s = address.getAddressLine(i)) != null; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
