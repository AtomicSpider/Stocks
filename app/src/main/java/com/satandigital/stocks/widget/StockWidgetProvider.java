package com.satandigital.stocks.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.satandigital.stocks.R;
import com.satandigital.stocks.service.StockTaskService;
import com.satandigital.stocks.ui.MyStocksActivity;

/**
 * Project : Stocks
 * Created by Sanat Dutta on 9/29/2016.
 * http://www.satandigital.com/
 */

public class StockWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            // Create an Intent to launch ExampleActivity
            Intent intent = new Intent(context, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            Intent intent1 = new Intent(context, MyStocksActivity.class);
            intent1.putExtra("action", MyStocksActivity.ADD_STOCK_FROM_WIDGET);
            PendingIntent pendingIntent1 = PendingIntent.getActivity(context, 1, intent1, 0);

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);
            views.setOnClickPendingIntent(R.id.widgetFrame, pendingIntent);
            views.setOnClickPendingIntent(R.id.widget_add_stock, pendingIntent1);
            views.setRemoteAdapter(R.id.listview, new Intent(context, StockWidgetService.class));

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (StockTaskService.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listview);
        }
    }
}
