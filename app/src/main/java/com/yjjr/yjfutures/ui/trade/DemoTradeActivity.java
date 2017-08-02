package com.yjjr.yjfutures.ui.trade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.trello.rxlifecycle2.android.ActivityEvent;
import com.trello.rxlifecycle2.android.FragmentEvent;
import com.yjjr.yjfutures.R;
import com.yjjr.yjfutures.event.OneMinuteEvent;
import com.yjjr.yjfutures.event.RefreshEvent;
import com.yjjr.yjfutures.model.Quote;
import com.yjjr.yjfutures.model.Symbol;
import com.yjjr.yjfutures.model.UserLoginResponse;
import com.yjjr.yjfutures.model.biz.BizResponse;
import com.yjjr.yjfutures.model.biz.UserInfo;
import com.yjjr.yjfutures.store.StaticStore;
import com.yjjr.yjfutures.store.UserSharePrefernce;
import com.yjjr.yjfutures.ui.BaseActivity;
import com.yjjr.yjfutures.ui.BaseApplication;
import com.yjjr.yjfutures.ui.market.MarketPriceFragment;
import com.yjjr.yjfutures.utils.LogUtils;
import com.yjjr.yjfutures.utils.RxUtils;
import com.yjjr.yjfutures.utils.http.HttpManager;
import com.yjjr.yjfutures.widget.HeaderView;

import org.greenrobot.eventbus.EventBus;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class DemoTradeActivity extends BaseActivity {
    private Timer mTimer = new Timer();
    public static void startActivity(Context context) {
        context.startActivity(new Intent(context, DemoTradeActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_trade);
        HeaderView headerView = (HeaderView) findViewById(R.id.header_view);
        headerView.bindActivity(mContext);
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
                if(TextUtils.isEmpty(StaticStore.sDemoSymbols)) {
                    return;
                }
                HttpManager.getHttpService(true).getQuoteList(StaticStore.sDemoSymbols, StaticStore.sDemoExchange)
                        .map(new Function<List<Quote>, List<Quote>>() {
                            @Override
                            public List<Quote> apply(@NonNull List<Quote> quotes) throws Exception {
                                for (Quote quote : quotes) {
                                    StaticStore.sDemoQuoteMap.put(quote.getSymbol(), quote);
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
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                LogUtils.e(throwable);
                            }
                        });
            }
        }, 3000, 1000);

    }

    private void loadData() {
        final String account = UserSharePrefernce.getAccount(mContext);
        final String password = UserSharePrefernce.getPassword(mContext);
        HttpManager.getHttpService(true).userLogin(account, password)
                .flatMap(new Function<UserLoginResponse, ObservableSource<List<Symbol>>>() {
                    @Override
                    public ObservableSource<List<Symbol>> apply(@NonNull UserLoginResponse userLoginResponse) throws Exception {
                        if (userLoginResponse.getReturnCode() != 1) {// 账号密法错误，重新登录
                            BaseApplication.getInstance().logout(mContext);
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
                            StaticStore.sDemoQuoteMap.put(quote.getSymbol(), quote);
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
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        LogUtils.e(throwable);
//                        mLoadingView.loadFail();
                    }
                });
    }
}
