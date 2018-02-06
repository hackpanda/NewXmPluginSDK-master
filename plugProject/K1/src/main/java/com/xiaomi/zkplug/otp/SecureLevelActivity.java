package com.xiaomi.zkplug.otp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.R;

/**
 * 作者：liwenqi on 18/26 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：门锁安全等级介绍
 */
public class SecureLevelActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "SecureLevelActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secure_level);
        initView();
    }

    private void initView(){
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText("门锁安全等级");
        findViewById(R.id.title_bar_return).setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            default:
                break;
        }
    }
}
