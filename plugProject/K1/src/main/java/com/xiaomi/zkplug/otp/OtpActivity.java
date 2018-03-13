package com.xiaomi.zkplug.otp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.deviceinfo.SecureLevelActivity;
import com.xiaomi.zkplug.util.ZkUtil;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：临时密码
 */
public class OtpActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "OtpActivity";

    OtpManager otpManager;
    Device mDevice;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(R.string.main_menu_otp);
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.title_bar_share).setOnClickListener(this);
        findViewById(R.id.btnClose).setOnClickListener(this);
        findViewById(R.id.btnRefresh).setOnClickListener(this);
        findViewById(R.id.secLevelLayout).setOnClickListener(this);
        initView();
    }

    private void initView(){
        mDevice = Device.getDevice(mDeviceStat);
        otpManager = new OtpManager(activity(), mDevice, pluginPackage());
        findViewById(R.id.otpKtBtn).setOnClickListener(this);
        findViewById(R.id.btnRefresh).setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        otpManager.initView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.btnClose:
                Intent intent = new Intent();
                startActivity(intent, CloseZotpActivity.class.getName());
                break;
            case R.id.btnRefresh:
                otpManager.initTheZotp();//计算临时密码
                break;
            case R.id.title_bar_share:
                mHostActivity.openShareActivity();
                break;
            case R.id.otpKtBtn:
                if(!ZkUtil.isBleOpen()){
                    Toast.makeText(activity(), R.string.open_bluetooth, Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!ZkUtil.isNetworkAvailable(this)){
                    Toast.makeText(activity(), R.string.network_not_avilable, Toast.LENGTH_SHORT).show();
                    return;
                }
                otpManager.checkSkeyIsExist();
                break;
            case R.id.secLevelLayout:
                Intent intentLev = new Intent();
                startActivity(intentLev, SecureLevelActivity.class.getName());
                break;
            default:
                break;
        }
    }

}
