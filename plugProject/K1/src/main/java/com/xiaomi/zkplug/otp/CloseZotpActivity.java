package com.xiaomi.zkplug.otp;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.common.ui.dialog.MLAlertDialog;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.util.ZkUtil;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：关闭临时密码功能
 */
public class CloseZotpActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "CloseZotpActivity";

    OtpManager otpManager;
    Device mDevice;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close_otp);
        initView();
    }

    private void initView(){
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(R.string.otp_close_title);
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        mDevice = Device.getDevice(mDeviceStat);
        otpManager = new OtpManager(activity(), mDevice, pluginPackage());
        findViewById(R.id.btnCloseOtp).setOnClickListener(this);

    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.btnCloseOtp:
                if(!ZkUtil.isBleOpen()){
                    Toast.makeText(activity(), R.string.open_bluetooth, Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!ZkUtil.isNetworkAvailable(this)){
                    Toast.makeText(activity(), R.string.network_not_avilable, Toast.LENGTH_SHORT).show();
                    return;
                }
                final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
                builder.setTitle(R.string.otp_does_close);
                builder.setPositiveButton(R.string.gloable_confirm, new MLAlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //确定
                        otpManager.doSsKeyExchange(false);
                    }
                });
                builder.setNegativeButton(R.string.gloable_cancel, new MLAlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();
                break;
            default:
                break;
        }
    }
}
