package com.xiaomi.zkplug.log;

import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;

/**
 * 作者：liwenqi on 17/10/10 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：日志读取
 */
public class LogReadActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "LogReadActivity";
    Button readBtn;
    TextView resultTv;
    MyLogRead myLogRead;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_read);
        initView();
    }

    private void initView(){
        initTitalBar();
        resultTv = (TextView)findViewById(R.id.resultTv);
        readBtn = (Button) findViewById(R.id.readBtn);
        readBtn.setOnClickListener(this);
        myLogRead = new MyLogRead(activity(), Device.getDevice(mDeviceStat));
    }
    //titlebar初始化
    private void initTitalBar(){
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText("日志读取");
        findViewById(R.id.title_bar_return).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.readBtn:
                resultTv.setText("");
                myLogRead.startGetLockLog((byte)0x00);
                break;
            default:
                break;
        }
    }
}