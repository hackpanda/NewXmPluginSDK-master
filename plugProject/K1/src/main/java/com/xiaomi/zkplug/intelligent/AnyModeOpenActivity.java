package com.xiaomi.zkplug.intelligent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.MessageReceiver;
import com.xiaomi.zkplug.R;

/**
 * 作者：liwenqi on 18/3/02 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：任意方式开锁联动页面
 * Action+Method+Key ID
 */
public class AnyModeOpenActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "AnyModeOpenActivity";
    public static final int MSG_GET_SCENE_VALUE = 3;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_any_mode);

        initView();
    }

    private void initView(){
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(R.string.zelkova_lock_name);
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.title_bar_more).setVisibility(View.GONE);
        findViewById(R.id.anyModeOpenLayout).setOnClickListener(this);
        findViewById(R.id.anyFpOpenLayout).setOnClickListener(this);
        findViewById(R.id.anyPwdOpenLayout).setOnClickListener(this);
        findViewById(R.id.anyBlekeyOpenLayout).setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.title_bar_return://点击返回
                MessageReceiver.myCallback.onFailure(-1, "do noting");
                finish();
                break;
            case R.id.anyModeOpenLayout:
                MessageReceiver.myCallback.onSuccess(getIntent());
                finish();
                break;
            case R.id.anyPwdOpenLayout://任意密码开门
                Intent pwdIntent = getIntent();
                getIntent().putExtra("value", "0001");//密码开门
                MessageReceiver.myCallback.onSuccess(pwdIntent);
                finish();
                break;
            case R.id.anyFpOpenLayout:
                Intent fpIntent = getIntent();
                fpIntent.putExtra("value", "0002");//密码开门
                MessageReceiver.myCallback.onSuccess(fpIntent);
                finish();
                break;
            case R.id.anyBlekeyOpenLayout:
                Intent bleIntent = getIntent();
                bleIntent.putExtra("value", "0000");//手机钥匙开门
                MessageReceiver.myCallback.onSuccess(bleIntent);
                finish();
                break;
        }
    }

}
