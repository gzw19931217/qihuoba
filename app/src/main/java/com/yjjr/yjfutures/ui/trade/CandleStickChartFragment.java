package com.yjjr.yjfutures.ui.trade;


import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.gson.Gson;
import com.yjjr.yjfutures.R;
import com.yjjr.yjfutures.contants.Constants;
import com.yjjr.yjfutures.event.OneMinuteEvent;
import com.yjjr.yjfutures.model.HisData;
import com.yjjr.yjfutures.model.HistoryDataRequest;
import com.yjjr.yjfutures.model.Quote;
import com.yjjr.yjfutures.store.StaticStore;
import com.yjjr.yjfutures.ui.BaseFragment;
import com.yjjr.yjfutures.utils.DateUtils;
import com.yjjr.yjfutures.utils.DisplayUtils;
import com.yjjr.yjfutures.utils.LogUtils;
import com.yjjr.yjfutures.utils.SocketUtils;
import com.yjjr.yjfutures.utils.StringUtils;
import com.yjjr.yjfutures.utils.http.HttpConfig;
import com.yjjr.yjfutures.widget.chart.AppCombinedChart;
import com.yjjr.yjfutures.widget.chart.ChartInfoView;
import com.yjjr.yjfutures.widget.chart.ChartInfoViewHandler;
import com.yjjr.yjfutures.widget.chart.InfoViewListener;
import com.yjjr.yjfutures.widget.chart.KLineChartInfoView;
import com.yjjr.yjfutures.widget.chart.KLineXValueFormatter;
import com.yjjr.yjfutures.widget.chart.LineChartXMarkerView;
import com.yjjr.yjfutures.widget.chart.LineChartYMarkerView;
import com.yjjr.yjfutures.widget.chart.YValueFormatter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import io.socket.emitter.Emitter;

/**
 * K线图Fragment
 */
public class CandleStickChartFragment extends BaseFragment {

    public static final int MAX_COUNT = 60;

    private Gson mGson = new Gson();

    private AppCombinedChart mChart;
    private String mSymbol;
    private boolean mIsDemo;
    /**
     * 数据类型  day=日线 hour=小时图 min15=15分钟图 min5=5分钟图 min=1分钟图
     */
    private String mType = HttpConfig.MIN;
    private List<HisData> mList = new ArrayList<>(200);
    private ChartInfoView mInfoView;
    private LineChartXMarkerView mMvx;

    public CandleStickChartFragment() {
        // Required empty public constructor
    }

    public static CandleStickChartFragment newInstance(String symbol, boolean isDemo, String type) {
        CandleStickChartFragment fragment = new CandleStickChartFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Constants.CONTENT_PARAMETER, symbol);
        bundle.putBoolean(Constants.CONTENT_PARAMETER_2, isDemo);
        bundle.putString(Constants.CONTENT_PARAMETER_3, type);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        if (getArguments() != null) {
            mSymbol = getArguments().getString(Constants.CONTENT_PARAMETER);
            mIsDemo = getArguments().getBoolean(Constants.CONTENT_PARAMETER_2);
            mType = getArguments().getString(Constants.CONTENT_PARAMETER_3);
        }
    }

    @Override
    protected View initViews(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final Quote quote = StaticStore.getQuote(mSymbol, mIsDemo);
        FrameLayout fl = new FrameLayout(mContext);
        mChart = new AppCombinedChart(mContext);
        mInfoView = new KLineChartInfoView(mContext);
        mInfoView.setLayoutParams(new FrameLayout.LayoutParams(DisplayUtils.dip2px(mContext, 120), ViewGroup.LayoutParams.WRAP_CONTENT));
        mInfoView.setVisibility(View.GONE);
        fl.addView(mChart);
        fl.addView(mInfoView);

        mChart.setBackgroundColor(ContextCompat.getColor(mContext, R.color.chart_background));
        mChart.getDescription().setEnabled(false);
        mChart.setNoDataText(mContext.getString(R.string.loading));
        mChart.setNoDataTextColor(ContextCompat.getColor(mContext, R.color.third_text_color));

        mChart.setScaleYEnabled(false);

        mChart.setAutoScaleMinMaxEnabled(true);
        mChart.setPinchZoom(true);
        mChart.setDrawGridBackground(false);
        mMvx = new LineChartXMarkerView(mContext, mList);
        mMvx.setChartView(mChart);
        mChart.setXMarker(mMvx);

        LineChartYMarkerView mv = new LineChartYMarkerView(mContext, quote.getTick());
        mv.setChartView(mChart);
        mChart.setMarker(mv);

        int whiteColor = ContextCompat.getColor(mContext, R.color.main_text_color);
        int dividerColor = ContextCompat.getColor(mContext, R.color.divider_color);
        XAxis xAxis = mChart.getXAxis();
        xAxis.setTextColor(whiteColor);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(5, true);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setValueFormatter(new KLineXValueFormatter(mType, mList));

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setTextColor(whiteColor);
        rightAxis.setLabelCount(6, true);
        rightAxis.setDrawGridLines(true);
        rightAxis.setGridColor(dividerColor);
        rightAxis.setGridLineWidth(0.5f);
        rightAxis.setDrawAxisLine(false);
        rightAxis.enableGridDashedLine(5, 5, 0);
        rightAxis.setValueFormatter(new YValueFormatter(quote.getTick()));
//        rightAxis.setDrawAxisLine(false);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        leftAxis.setDrawLabels(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawGridLines(false);


        mChart.getLegend().setEnabled(false);
        mChart.setOnChartValueSelectedListener(new InfoViewListener(mContext, quote.getLastclose(), mList, mInfoView));
        mChart.setOnTouchListener(new ChartInfoViewHandler(mChart));
        mChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                mChart.setDragEnabled(true);
//                mInfoView.setVisibility(View.GONE);
            }

            @Override
            public void onChartLongPressed(MotionEvent me) {
            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {

            }
        });
        return fl;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OneMinuteEvent event) {
        final boolean isMin = TextUtils.equals(mType, HttpConfig.MIN);
        if (!mList.isEmpty() && isMin && SocketUtils.getSocket() != null) {
            final HisData hisData = mList.get(mList.size() - 1);
            final Quote quote = StaticStore.getQuote(mSymbol, mIsDemo);
            SocketUtils.getSocket().emit(SocketUtils.HIS_DATA, mGson.toJson(new HistoryDataRequest(quote.getSymbol(), quote.getExchange(), hisData.getsDate(), mType)));
            SocketUtils.getSocket().once(SocketUtils.HIS_DATA, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    LogUtils.d("history data -> " + args[0].toString());
                    final List<HisData> hisDatas = StringUtils.parseHisData(args[0].toString(), hisData);
                    if (hisDatas == null || hisDatas.isEmpty()) {
                        return;
                    }
                    mChart.post(new Runnable() {
                        @Override
                        public void run() {
                            mList.removeAll(hisDatas);
                            mList.addAll(hisDatas);
                            fullData(mList, isMin);
                        }
                    });
                }
            });
        }
    }

    public void loadDataByType(String type) {
        mType = type;
        mMvx.setType(type);
        final Quote quote = StaticStore.getQuote(mSymbol, mIsDemo);

        DateTime dateTime = DateUtils.getChartStartTime(quote, mType);
        if (SocketUtils.getSocket() == null) {
            mChart.setNoDataText(getString(R.string.data_load_fail));
            return;
        }

        String json = mGson.toJson(new HistoryDataRequest(quote.getSymbol(), quote.getExchange(), DateUtils.formatData(dateTime.getMillis()), mType));
        SocketUtils.getSocket().emit(SocketUtils.HIS_DATA, json);
        LogUtils.d("history request data -> %s", json);
        SocketUtils.getSocket().once(SocketUtils.HIS_DATA, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                LogUtils.d("history data -> " + args[0].toString());
                final List<HisData> hisDatas = StringUtils.parseHisData(args[0].toString(), null);
                mList.clear();
                mList.addAll(hisDatas);
                mChart.post(new Runnable() {
                    @Override
                    public void run() {
                        fullData(mList, TextUtils.equals(mType, HttpConfig.MIN));
                    }
                });
            }
        });
    }

    private void fullData(List<HisData> datas, boolean isShowAve) {
        if (datas == null || datas.isEmpty()) {
            mChart.setNoDataText(getString(R.string.data_is_null));
            return;
        }

        //调整x轴第一个和最后一个的位置，否则会显示不完整
        mChart.getXAxis().setAxisMinimum(-0.5f);
        mChart.getXAxis().setAxisMaximum(datas.size() - 0.5f);
        ArrayList<CandleEntry> yVals1 = new ArrayList<>();
        ArrayList<Entry> aveList = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            HisData data = datas.get(i);
            yVals1.add(new CandleEntry(
                    i, (float) data.getHigh(),
                    (float) data.getLow(),
                    (float) data.getOpen(),
                    (float) data.getClose()
            ));
            aveList.add(new Entry(i, (float) data.getAvePrice()));
        }

        // K线
        CandleDataSet set1 = new CandleDataSet(yVals1, "日期");
        set1.setDrawIcons(false);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setShadowColor(Color.DKGRAY);
        set1.setShadowWidth(0.7f);
        set1.setDecreasingColor(ContextCompat.getColor(getContext(), R.color.decreasing_color));
        set1.setDecreasingPaintStyle(Paint.Style.FILL);
        set1.setShadowColorSameAsCandle(true);
        set1.setIncreasingColor(ContextCompat.getColor(getContext(), R.color.increasing_color));
        set1.setIncreasingPaintStyle(Paint.Style.FILL);
        set1.setNeutralColor(ContextCompat.getColor(getContext(), R.color.increasing_color));
        //set1.setHighlightLineWidth(1f);
        set1.setDrawValues(false);
        CandleData cd = new CandleData(set1);

        // 均线
        LineDataSet lineDataSet = new LineDataSet(aveList, "均线");
        lineDataSet.setCircleColor(ContextCompat.getColor(mContext, R.color.transparent));
        lineDataSet.setColor(ContextCompat.getColor(mContext, R.color.ave_color));
        lineDataSet.setLineWidth(1f);
        lineDataSet.setDrawCircleHole(false);

        CombinedData data = new CombinedData();
        data.setData(cd);
        if (isShowAve) {
            data.setData(new LineData(lineDataSet));
        }
        mChart.setData(data);
        mChart.highlightValue(null);
        mInfoView.setVisibility(View.GONE);
        mChart.setVisibleXRange(MAX_COUNT, 20);
        ViewPortHandler port = mChart.getViewPortHandler();
        mChart.setViewPortOffsets(0, port.offsetTop(), port.offsetRight(), port.offsetBottom());
        mChart.notifyDataSetChanged();
        mChart.invalidate();
        mChart.moveViewToX(mChart.getCandleData().getEntryCount());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

}
