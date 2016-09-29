package com.satandigital.stocks.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.satandigital.stocks.R;
import com.satandigital.stocks.data.QuoteColumns;
import com.satandigital.stocks.data.QuoteProvider;
import com.satandigital.stocks.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    private String LOG_TAG = StockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public StockTaskService() {
    }

    public StockTaskService(Context context) {
        mContext = context;
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append(mContext.getString(R.string.YAHOO_BASE_URL));
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (params.getTag().equals("init") || params.getTag().equals("periodic")) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null) {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append(mContext.getString(R.string.YAHOO_OUTPUT_PREFS));

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    boolean addEntry = true;
                    ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate) {
                        contentValues.put(QuoteColumns.ISCURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                                null, null);
                    } else {
                        JSONObject jsonObject = null;
                        try {
                            jsonObject = new JSONObject(getResponse);
                            addEntry = !jsonObject.getJSONObject("query").getJSONObject("results").getJSONObject("quote").isNull("Name");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    if (addEntry) mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteJsonToContentVals(getResponse));
                    else {
                        Handler mHandler = new Handler(mContext.getMainLooper());
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, mContext.getString(R.string.invalid_stock), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        syncTemporalQuotes();

        return result;
    }

    private void syncTemporalQuotes() {
        Cursor queryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                null, null);

        if (queryCursor.getCount() != 0 && queryCursor != null) {
            queryCursor.moveToFirst();
            for (int i = 0; i < queryCursor.getCount(); i++) {
                fetchTemporalQuotes(queryCursor);
                queryCursor.moveToNext();
            }
            queryCursor.close();
        }
    }

    private void fetchTemporalQuotes(Cursor cursor) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Calendar calendarEnd = Calendar.getInstance();

        Calendar calendarStart = Calendar.getInstance();
        calendarStart.add(Calendar.MONTH, -1);

        String startDate = simpleDateFormat.format(calendarStart.getTime());
        String endDate = simpleDateFormat.format(calendarEnd.getTime());

        String urlString;
        StringBuilder urlStringBuilder = new StringBuilder();

        try {
            urlStringBuilder.append(mContext.getString(R.string.YAHOO_BASE_URL));
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol=\"", "UTF-8"));
            urlStringBuilder.append(cursor.getString(cursor.getColumnIndex("symbol")));
            urlStringBuilder.append(URLEncoder.encode("\" and startDate=\"" + startDate + "\" and endDate=\"" + endDate + "\"", "UTF-8"));
            urlStringBuilder.append(mContext.getString(R.string.YAHOO_OUTPUT_PREFS));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        urlString = urlStringBuilder.toString();

        try {
            String getResponse = fetchData(urlString);

            try {
                mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                        Utils.quoteTemporalJsonToContentVals(getResponse, mContext));
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, "Error applying batch insert", e);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
