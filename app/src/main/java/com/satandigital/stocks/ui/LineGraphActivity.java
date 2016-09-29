package com.satandigital.stocks.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.satandigital.stocks.R;
import com.satandigital.stocks.data.QuoteProvider;
import com.satandigital.stocks.data.QuoteTemporalColumns;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;

/**
 * Project : Stocks
 * Created by Sanat Dutta on 9/28/2016.
 * http://www.satandigital.com/
 */

public class LineGraphActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String TAG = LineGraphActivity.class.getSimpleName();

    //Views
    @BindView(R.id.stock_name)
    TextView stock_name;
    @BindView(R.id.stock_delete)
    ImageView stock_delete;
    @BindView(R.id.stock_symbol)
    TextView stock_symbol;
    @BindView(R.id.stock_bid)
    TextView stock_bid;
    @BindView(R.id.stock_change)
    TextView stock_change;
    @BindView(R.id.stock_close)
    TextView stock_close;
    @BindView(R.id.stock_chart)
    lecho.lib.hellocharts.view.LineChartView stock_chart;
    //Data
    String symbol;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_line_graph);
        ButterKnife.bind(this);

        showStockData();
        getSupportLoaderManager().initLoader(1, null, this);

        setUpOnClickListeners();
    }

    private void setUpOnClickListeners() {
        stock_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mIntent = new Intent();
                mIntent.putExtra("action", "delete");
                mIntent.putExtra("symbol", symbol);
                setResult(Activity.RESULT_OK, mIntent);
                finish();
            }
        });
    }

    private void showStockData() {
        symbol = getIntent().getStringExtra("symbol");
        stock_name.setText(getIntent().getStringExtra("name"));
        stock_symbol.setText(getString(R.string.detail_stock_symbol, symbol));
        stock_bid.setText(getIntent().getStringExtra("bid_price"));
        stock_change.setText(getString(R.string.detail_stock_change, getIntent().getStringExtra("change"), getIntent().getStringExtra("percent_change")));
        stock_close.setText(getString(R.string.detail_stock_last_trade, getIntent().getStringExtra("last_trade")));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = QuoteTemporalColumns._ID + " DESC";

        return new CursorLoader(LineGraphActivity.this, QuoteProvider.QuotesTemporal.CONTENT_URI,
                new String[]{QuoteTemporalColumns._ID, QuoteTemporalColumns.SYMBOL,
                        QuoteTemporalColumns.BIDPRICE, QuoteTemporalColumns.DATE},
                QuoteTemporalColumns.SYMBOL + " = \"" + symbol + "\"",
                null, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            try {
                drawGraph(cursor);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void drawGraph(Cursor cursor) throws ParseException {
        List<AxisValue> axisValues = new ArrayList<>();
        List<PointValue> pointValues = new ArrayList<>();

        cursor.moveToFirst();
        for (int i = 0; i < cursor.getCount(); i++) {
            String stringDate = cursor.getString(cursor.getColumnIndex(
                    QuoteTemporalColumns.DATE));

            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = format1.parse(stringDate);
            SimpleDateFormat format2 = new SimpleDateFormat("dd-MM", Locale.getDefault());
            stringDate = format2.format(date);

            String bidPrice = cursor.getString(cursor.getColumnIndex(
                    QuoteTemporalColumns.BIDPRICE));

            PointValue pointValue = new PointValue(i, Float.valueOf(bidPrice));
            pointValue.setLabel(stringDate);
            pointValues.add(pointValue);

            if (i % (cursor.getCount() / 3) == 0) {
                AxisValue axisValue = new AxisValue(i);
                axisValue.setLabel(stringDate);
                axisValues.add(axisValue);
            }

            cursor.moveToNext();
        }

        Line line = new Line(pointValues).setColor(getResources().getColor(R.color.graph_red)).setCubic(false);
        List<Line> lines = new ArrayList<>();
        lines.add(line);
        LineChartData lineChartData = new LineChartData();
        lineChartData.setValueLabelsTextColor(Color.BLACK);
        lineChartData.setLines(lines);


        Axis axisX = new Axis(axisValues);
        axisX.setHasLines(true);
        axisX.setLineColor(getResources().getColor(R.color.graph_accent));
        axisX.setMaxLabelChars(4);
        axisX.setTextColor(getResources().getColor(R.color.graph_accent));
        lineChartData.setAxisXBottom(axisX);

        Axis axisY = new Axis();
        axisY.setAutoGenerated(true);
        axisY.setHasLines(true);
        axisY.setLineColor(getResources().getColor(R.color.graph_accent));
        axisY.setMaxLabelChars(4);
        axisY.setTextColor(getResources().getColor(R.color.graph_accent));
        lineChartData.setAxisYLeft(axisY);

        stock_chart.setInteractive(true);
        stock_chart.setLineChartData(lineChartData);
        stock_chart.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
