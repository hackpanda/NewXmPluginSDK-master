package com.xiaomi.zkplug.otp;

import android.os.Bundle;
import android.view.View;

import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.R;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：安全等级介绍页面
 */
public class SecLevelActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "SecLevelActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close_otp);
    }


    @Override
    public void onClick(View v) {

    }
}
