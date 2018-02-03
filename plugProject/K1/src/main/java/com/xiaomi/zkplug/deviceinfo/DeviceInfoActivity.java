package com.xiaomi.zkplug.deviceinfo;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;

/**
 * 作者：liwenqi on 18/1/31 18:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：设备信息
 */
public class DeviceInfoActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "DeviceInfoActivity";

    DeviceInfoManager deviceInfoManager;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        initView();
    }

    private void initView(){
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText("设备信息");
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.btnSyncTime).setOnClickListener(this);

        this.deviceInfoManager = new DeviceInfoManager(Device.getDevice(mDeviceStat), activity());
        this.deviceInfoManager.getDeviceInfo();//获取设备信息
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.btnSyncTime:
                deviceInfoManager.operateLockSyncTime();
                break;
            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deviceInfoManager.unregisterBleReceiver();
    }
}
