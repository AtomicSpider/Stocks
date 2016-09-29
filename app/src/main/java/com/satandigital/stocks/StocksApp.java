package com.satandigital.stocks;

import android.app.Application;

import butterknife.BindString;

/**
 * Project : Stocks
 * Created by Sanat Dutta on 9/29/2016.
 * http://www.satandigital.com/
 */

public class StocksApp extends Application {

    private final String TAG = StocksApp.class.getSimpleName();

    //Strings
    @BindString(R.string.invalid_stock) public static String invalid_stock;
    @BindString(R.string.stock_exists) public static String stock_exists;
    @BindString(R.string.network_toast) public static String network_toast;
    @BindString(R.string.YAHOO_BASE_URL) public static String YAHOO_BASE_URL;
    @BindString(R.string.YAHOO_OUTPUT_PREFS) public static String YAHOO_OUTPUT_PREFS;

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
