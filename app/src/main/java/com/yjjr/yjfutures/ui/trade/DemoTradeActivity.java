package com.yjjr.yjfutures.ui.trade;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.trello.rxlifecycle2.android.ActivityEvent;
import com.yjjr.yjfutures.R;
import com.yjjr.yjfutures.event.RefreshEvent;
import com.yjjr.yjfutures.event.SendOrderEvent;
import com.yjjr.yjfutures.model.Quote;
import com.yjjr.yjfutures.model.Symbol;
import com.yjjr.yjfutures.model.UserLoginResponse;
import com.yjjr.yjfutures.model.biz.BizResponse;
import com.yjjr.yjfutures.model.biz.Funds;
import com.yjjr.yjfutures.model.biz.Holds;
import com.yjjr.yjfutures.store.StaticStore;
import com.yjjr.yjfutures.store.UserSharePrefernce;
import com.yjjr.yjfutures.ui.BaseActivity;
import com.yjjr.yjfutures.ui.BaseApplication;
import com.yjjr.yjfutures.ui.market.MarketPriceFragment;
import com.yjjr.yjfutures.utils.LogUtils;
import com.yjjr.yjfutures.utils.RxUtils;
import com.yjjr.yjfutures.utils.http.HttpManager;
import com.yjjr.yjfutures.widget.HeaderView;
import com.yjjr.yjfutures.widget.TradeInfoView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class DemoTradeActivity extends BaseActivity {
    private Timer mTimer = new Timer();
    private TradeInfoView mTradeInfoView;

    public static void startActivity(Context context) {
        context.startActivity(new Intent(context, DemoTradeActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_trade);
        EventBus.getDefault().register(this);
        HeaderView headerView = (HeaderView) findViewById(R.id.header_view);
        headerView.bindActivity(mContext);
        mTradeInfoView = (TradeInfoView) findViewById(R.id.trade_info);
        MarketPriceFragment fragment = MarketPriceFragment.newInstance(false);
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_container, fragment).commit();
        fragment.setUserVisibleHint(true);
        loadData();
        startTimer();
    }

    private void startTimer() {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(StaticStore.sDemoSymbols)) {
                    return;
                }
                if (BaseApplication.getInstance().isBackground()) {
                    return;
                }
                HttpManager.getHttpService(true).getQuoteList(StaticStore.sDemoSymbols, StaticStore.sDemoExchange)
                        .map(new Function<List<Quote>, List<Quote>>() {
                            @Override
                            public List<Quote> apply(@NonNull List<Quote> quotes) throws Exception {
                                for (Quote quote : quotes) {
                                    //设置一下商品是否持仓
                                    quote.setHolding(StaticStore.sDemoHoldSet.contains(quote.getSymbol()));
                                    StaticStore.putQuote(quote, true);
                                }
                                return quotes;
                            }
                        })
                        .compose(RxUtils.<List<Quote>>applySchedulers())
                        .compose(mContext.<List<Quote>>bindUntilEvent(ActivityEvent.DESTROY))
                        .subscribe(new Consumer<List<Quote>>() {
                            @Override
                            public void accept(@NonNull List<Quote> quotes) throws Exception {
//                                EventBus.getDefault().post(new RefreshEvent());
                            }
                        }, RxUtils.commonErrorConsumer());
                HttpManager.getBizService(true).getFunds()
                        .compose(RxUtils.<BizResponse<Funds>>applyBizSchedulers())
                        .compose(mContext.<BizResponse<Funds>>bindUntilEvent(ActivityEvent.DESTROY))
                        .subscribe(new Consumer<BizResponse<Funds>>() {
                            @Override
                            public void accept(@NonNull BizResponse<Funds> fundsBizResponse) throws Exception {
                                Funds result = fundsBizResponse.getResult();
                                StaticStore.setFunds(true, result);
                            }
                        }, RxUtils.commonErrorConsumer());
            }
        }, 3000, 1000);

    }

    private void loadData() {
        final String account = UserSharePrefernce.getAccount(mContext);
        final String password = /*UserSharePrefernce.getPassword(mContext)*/"123456";
        HttpManager.getHttpService(true).userLogin(account, password)
                .flatMap(new Function<UserLoginResponse, ObservableSource<List<Symbol>>>() {
                    @Override
                    public ObservableSource<List<Symbol>> apply(@NonNull UserLoginResponse userLoginResponse) throws Exception {
                        if (userLoginResponse.getReturnCode() != 1) {// 账号密法错误，重新登录
//                            BaseApplication.getInstance().logout(mContext);
                            throw new RuntimeException("账号密码错误");
                        }
                        BaseApplication.getInstance().setDemoTradeToken(userLoginResponse.getCid());
                        return HttpManager.getHttpService(true).getSymbols(BaseApplication.getInstance().getTradeToken(true));
                    }
                })
                .flatMap(new Function<List<Symbol>, ObservableSource<List<Quote>>>() {
                    @Override
                    public ObservableSource<List<Quote>> apply(@NonNull List<Symbol> symbols) throws Exception {
                        StringBuilder symbol = new StringBuilder();
                        StringBuilder exchange = new StringBuilder();
                        for (int i = 0; i < symbols.size(); i++) {
                            symbol.append(symbols.get(i).getSymbol());
                            exchange.append(symbols.get(i).getExchange());
                            if (i < symbols.size() - 1) {
                                symbol.append(",");
                                exchange.append(",");
                            }
                        }
                        StaticStore.sDemoSymbols = symbol.toString();
                        StaticStore.sDemoExchange = exchange.toString();
                        return HttpManager.getHttpService(true).getQuoteList(symbol.toString(), exchange.toString());
                    }
                })
                .map(new Function<List<Quote>, Boolean>() {
                    @Override
                    public Boolean apply(@NonNull List<Quote> quotes) throws Exception {
                        for (Quote quote : quotes) {
                            StaticStore.putQuote(quote, true);
                        }
                        return true;
                    }
                })
                .compose(RxUtils.<Boolean>applySchedulers())
                .compose(this.<Boolean>bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(@NonNull Boolean symbols) throws Exception {
//                        mAdapter.setNewData(new ArrayList<>(StaticStore.sQuoteMap.values()));
//                        mLoadingView.setVisibility(View.GONE);
                        getHolding();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        LogUtils.e(throwable);
//                        mLoadingView.loadFail();
                    }
                });
    }

    private void getHolding() {
        HttpManager.getBizService(true).getHolding()
                .compose(RxUtils.<BizResponse<List<Holds>>>applyBizSchedulers())
                .compose(this.<BizResponse<List<Holds>>>bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(new Consumer<BizResponse<List<Holds>>>() {
                    @Override
                    public void accept(@NonNull BizResponse<List<Holds>> response) throws Exception {
                        //将持仓的品种保存起来
                        StaticStore.sDemoHoldSet = new HashSet<>();
                        for (Holds holding : response.getResult()) {
                            if (holding.getQty() == 0) continue;
                            StaticStore.sDemoHoldSet.add(holding.getSymbol());
                        }
                    }
                }, RxUtils.commonErrorConsumer());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SendOrderEvent event) {
        getHolding();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RefreshEvent event) {
        if (mTradeInfoView == null) return;
        Funds result = StaticStore.getFunds(true);
        mTradeInfoView.setValues(result.getFrozenMargin(), result.getAvailableFunds(), result.getNetAssets());
    }

//    private void getFund() {
//        HttpManager.getBizService(true).getFunds()
//                .retry(3)
//                .compose(RxUtils.<BizResponse<Funds>>applyBizSchedulers())
//                .compose(this.<BizResponse<Funds>>bindUntilEvent(ActivityEvent.DESTROY))
//                .subscribe(new Consumer<BizResponse<Funds>>() {
//                    @Override
//                    public void accept(@NonNull BizResponse<Funds> fundsBizResponse) throws Exception {
//                        Funds result = fundsBizResponse.getResult();
//                        mTradeInfoView.setValues(result.getFrozenMargin(), result.getAvailableFunds(), result.getNetAssets());
//                    }
//                }, RxUtils.commonErrorConsumer());
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
