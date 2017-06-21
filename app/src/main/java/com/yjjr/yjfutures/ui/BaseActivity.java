package com.yjjr.yjfutures.ui;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;
import com.yjjr.yjfutures.R;
import com.yjjr.yjfutures.utils.InputMethodUtil;
import com.yjjr.yjfutures.utils.LogUtils;
import com.yjjr.yjfutures.utils.SystemBarHelper;


/**
 * Created by Administrator on 2016/11/22.
 */

public class BaseActivity extends RxAppCompatActivity {
    protected final String TAG = this.getClass().getSimpleName();
    protected boolean isDestroy = false;
    protected BaseActivity mContext;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * 是否设置默认的状态栏
     */
    private boolean mIsSetDefaultStatusBar = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        LogUtils.d("onCreate (" + getClass().getSimpleName() + ".java:1)");
    }

    @Override
    public void finish() {
        super.finish();
        // 在这里统一加了关闭键盘的逻辑，防止用户点击左上角返回键而键盘没有关闭
        InputMethodUtil.hiddenInputMethod(this);
    }

    public void finishDelay() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    finish();
                }
            }
        }, 1000);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setStatusBarColor();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        setStatusBarColor();
    }

    /**
     * 重置statusbar，必须在setContentView之前调用
     */
    protected void resetStatusBar() {
        mIsSetDefaultStatusBar = false;
    }

    protected void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (mIsSetDefaultStatusBar) {
                // 设置状态栏的颜色
                if (SystemBarHelper.isMIUI6Later() || SystemBarHelper.isFlyme4Later() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    SystemBarHelper.setStatusBarDarkMode(this);
                    SystemBarHelper.tintStatusBar(this, ContextCompat.getColor(this, R.color.white), 0);
                } else {
                    SystemBarHelper.tintStatusBar(this, ContextCompat.getColor(this, R.color.color_bbbbbb), 0);
                }
            }
            // 设置导航栏的颜色
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.color_bbbbbb));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroy = true;
    }

}
