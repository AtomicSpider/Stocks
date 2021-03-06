package com.satandigital.stocks.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.satandigital.stocks.R;
import com.satandigital.stocks.data.QuoteColumns;
import com.satandigital.stocks.data.QuoteProvider;

/**
 * Project : Stocks
 * Created by Sanat Dutta on 9/29/2016.
 * http://www.satandigital.com/
 */

public class StockWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {

                if (data != null) {
                    data.close();
                }


                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(
                        QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.NAME, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.LAST_TRADE},
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {

                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_list_item_quote);

                if (data.moveToPosition(position)) {
                    views.setTextViewText(R.id.stock_symbol, data.getString(data.getColumnIndex("symbol")));
                    views.setTextViewText(R.id.bid_price, data.getString(data.getColumnIndex("bid_price")));
                    views.setTextViewText(R.id.change, data.getString(data.getColumnIndex("percent_change")));
                    if (data.getInt(data.getColumnIndex("is_up")) == 1) {
                        views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
                    } else {
                        views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
                    }

                    return views;
                } else return null;
            }

            @Override
            public RemoteViews getLoadingView() {
                return null;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
