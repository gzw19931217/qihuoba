package com.yjjr.yjfutures.ui.mine;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.yjjr.yjfutures.R;
import com.yjjr.yjfutures.contants.Constants;
import com.yjjr.yjfutures.ui.BaseApplication;
import com.yjjr.yjfutures.ui.BaseFragment;
import com.yjjr.yjfutures.ui.MainActivity;

public class GuideFragment extends BaseFragment implements View.OnClickListener {

    private int index;

    public GuideFragment() {
        // Required empty public constructor
    }

    public static GuideFragment newInstance(int param) {
        GuideFragment fragment = new GuideFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.CONTENT_PARAMETER, param);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            index = getArguments().getInt(Constants.CONTENT_PARAMETER);
        }
    }

    @Override
    public View initViews(LayoutInflater inflater, ViewGroup container,
                          Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_guide, container, false);
        ImageView ivBg = (ImageView) v.findViewById(R.id.iv_bg);
        final View tvTop = v.findViewById(R.id.tv_top);
        switch (index) {
            case 1:
                tvTop.setVisibility(View.VISIBLE);
                ivBg.setImageResource(R.drawable.step1);
                break;
            case 2:
                tvTop.setVisibility(View.VISIBLE);
                ivBg.setImageResource(R.drawable.step2);
                break;
            case 3:
                tvTop.setVisibility(View.GONE);
                ivBg.setImageResource(R.drawable.step3);
                break;
        }
        tvTop.setOnClickListener(this);
        ivBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (index == 3) {
                    tvTop.performClick();
                }
            }
        });
        return v;
    }

    @Override
    public void onClick(View v) {
        if (BaseApplication.getInstance().isLogin()) {
            MainActivity.startActivity(mContext);
        } else {
            //入口改成注册
//            LoginActivity.startActivity(mContext);
            RegisterActivity.startActivity(mContext);
        }
        getActivity().finish();
    }
}
