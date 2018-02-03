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
        mTitleView.setText("关闭临时密码");
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
                    Toast.makeText(activity(), "请打开蓝牙", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!ZkUtil.isNetworkAvailable(this)){
                    Toast.makeText(activity(), "网络未连接，请保持网络畅通", Toast.LENGTH_SHORT).show();
                    return;
                }
                final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
                builder.setTitle("确定要关闭临时密码吗?");
                builder.setPositiveButton("确定", new MLAlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //确定
                        otpManager.doSsKeyExchange(false);
                    }
                });
                builder.setNegativeButton("取消", new MLAlertDialog.OnClickListener() {
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