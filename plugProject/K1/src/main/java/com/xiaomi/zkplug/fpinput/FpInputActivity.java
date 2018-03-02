package com.xiaomi.zkplug.fpinput;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.ZkUtil;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：录入指纹
 */
public class FpInputActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "FpInputActivity";
    Device mDevice;
    FpInputHelper fpInputHelper;
    String memberId;
    Button btnFpInput;
    LinearLayout fpInputBuzhouLayout;
    DataManageUtil dataManageUtil;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fp_input);
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText("添加指纹");
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        this.btnFpInput = (Button) findViewById(R.id.btnFpIput);
        this.fpInputBuzhouLayout = (LinearLayout) findViewById(R.id.fpInputBuzhouLayout);
        btnFpInput.setOnClickListener(this);
        mDevice = Device.getDevice(mDeviceStat);
        this.memberId = getIntent().getStringExtra("memberId");
        fpInputHelper = new FpInputHelper(activity(), mDeviceStat, pluginPackage());
        this.dataManageUtil = new DataManageUtil(mDeviceStat, this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.btnFpIput:
                if(!ZkUtil.isNetworkAvailable(this)){
                    Toast.makeText(activity(), R.string.network_not_avilable, Toast.LENGTH_LONG).show();
                    return;
                }
                if(!ZkUtil.isBleOpen()){
                    Toast.makeText(activity(), "请打开手机蓝牙", Toast.LENGTH_LONG).show();
                    return;
                }

                TextView fpPutTv = (TextView) findViewById(R.id.fpPutTv);
                fpPutTv.setText("放置手指");
                TextView fpCompleteTv = (TextView) findViewById(R.id.fpCompleteTv);
                fpCompleteTv.setText("将手指按在指纹头上再抬起，重复此步骤");
                fpInputHelper.registerBluetoothReceiver();
                fpInputHelper.addMemberFp();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fpInputHelper.unregisterBluetoothReceiver();
    }
}
