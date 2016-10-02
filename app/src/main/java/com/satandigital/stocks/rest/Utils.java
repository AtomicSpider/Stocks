package com.satandigital.stocks.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.satandigital.stocks.data.QuoteColumns;
import com.satandigital.stocks.data.QuoteProvider;
import com.satandigital.stocks.data.QuoteTemporalColumns;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static com.satandigital.stocks.service.StockTaskService.ACTION_DATA_UPDATED;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    public static ArrayList quoteJsonToContentVals(String JSON, Context context) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    batchOperations.add(buildBatchOperation(jsonObject, 0));
                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject, 0));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static void updateWidgets(Context context) {
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                .setPackage(context.getPackageName());
        context.sendBroadcast(dataUpdatedIntent);

    }

    public static ArrayList<ContentProviderOperation> quoteTemporalJsonToContentVals(String JSON, Context context) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");

                resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                if (resultsArray != null && resultsArray.length() != 0) {
                    context.getContentResolver().delete(QuoteProvider.QuotesTemporal.CONTENT_URI,
                            QuoteTemporalColumns.SYMBOL + " = \"" + resultsArray.getJSONObject(0).getString("Symbol") + "\"", null);

                    for (int i = 0; i < resultsArray.length(); i++) {
                        jsonObject = resultsArray.getJSONObject(i);
                        batchOperations.add(buildBatchOperation(jsonObject, 1));
                    }

                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        if (change == null || change.equals("null")) return "0";
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject, int actionCode) {
        ContentProviderOperation.Builder builder = null;
        if (actionCode == 0) {
            builder = ContentProviderOperation.newInsert(
                    QuoteProvider.Quotes.CONTENT_URI);
            try {
                String change = jsonObject.getString("Change");
                builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
                builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
                builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                        jsonObject.getString("ChangeinPercent"), true));
                builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
                builder.withValue(QuoteColumns.ISCURRENT, 1);
                if (change.charAt(0) == '-') {
                    builder.withValue(QuoteColumns.ISUP, 0);
                } else {
                    builder.withValue(QuoteColumns.ISUP, 1);
                }
                builder.withValue(QuoteColumns.NAME, jsonObject.getString("Name"));
                builder.withValue(QuoteColumns.LAST_TRADE, jsonObject.getString("LastTradeTime"));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            builder = ContentProviderOperation.newInsert(
                    QuoteProvider.QuotesTemporal.CONTENT_URI);
            try {
                builder.withValue(QuoteTemporalColumns.SYMBOL, jsonObject.getString("Symbol"));
                builder.withValue(QuoteTemporalColumns.BIDPRICE, jsonObject.getString("Open"));
                builder.withValue(QuoteTemporalColumns.DATE, jsonObject.getString("Date"));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return builder.build();
    }
}
