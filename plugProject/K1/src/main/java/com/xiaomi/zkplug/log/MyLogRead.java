package com.xiaomi.zkplug.log;

import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;

import java.util.Date;

import cn.zelkova.lockprotocol.LockCommGetLogResponse;

/**
 * 作者：liwenqi on 16/10/19 16:08
 * 邮箱：liwenqi@zelkova.cn
 * 描述：日志读取辅助类
 */
public class MyLogRead {
    private final String TAG = "MyLogRead";
    Activity activity;

    Device mDevice;
    /*
    * 打印过程信息
    * */
    TextView resultTv;
    ScrollView sv;
    Button readBtn;

    private long logIdxMax = 0;
    private long logIdxMin = 0;
    private int logRemainder = 0;

    public MyLogRead(Activity activity, Device mDevice) {
        this.activity = activity;
        this.mDevice = mDevice;
        readBtn = (Button) activity.findViewById(R.id.readBtn);
        this.resultTv = (TextView) activity.findViewById(R.id.resultTv);
        this.sv = (ScrollView) activity.findViewById(R.id.scv);
    }

    protected void startGetLockLog(final byte orderType){
        Date startDate = new Date();
        final LockCommGetLogList lockCommGetLogList = new LockCommGetLogList(activity, mDevice);

        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            lockCommGetLogList.getLockLog(orderType, showLogcallback);
        }else{
            XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                @Override
                public void onResponse(int i, Bundle bundle) {
                    Log.d(TAG, "回调状态:"+XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()));
                    if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                        Log.d(TAG, "登录成功");
                        lockCommGetLogList.getLockLog(orderType, showLogcallback);
                    }else if(i == XmBluetoothManager.Code.REQUEST_NOT_REGISTERED){
                        Toast.makeText(activity, R.string.device_has_been_reset, Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(activity, R.string.connect_time_out, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        long timeSpan = (new Date()).getTime() - startDate.getTime();
        timeSpan = timeSpan / 1000; //from ms to snd
        //showMsg("******* 完成，耗时snd：" + timeSpan + " *******");
    }

    private LockCommGetLogList.IResultCallback showLogcallback = new LockCommGetLogList.IResultCallback() {
        @Override
        public boolean showLogs(LockCommGetLogResponse logs) {

            String msg ="#################################";
            msg += "\n#### 日志索引[" + logs.getLogIndexMin() + "-" + logs.getLogIndexMax() + "]";
            msg += "，获取:" + logs.logCount();
            msg += "，剩余:" + logs.hasMoreLog();
            msg +="\n#################################";
            showMsg(msg);

            for (LockCommGetLogResponse.LogRecord curLog : logs.getLogRecord()) {
                int innerIdx = curLog.getIdx();
                try {
                    String dt = curLog.getLogDate().toString();
                    msg = "[ " + innerIdx + " ] [" + dt + "]";
                    msg += String.format("0x%02x", curLog.getTypeKey()) + ":" + curLog.getTypeName();
                    showMsg(msg);

                    String logContent = curLog.getPayloadDesc();
                    if (logContent.length() > 0) showMsg(logContent);
                }catch(Exception ex) {
                    showMsg("【" +innerIdx+ "】发生异常！！！！！！！！！！！！！！！");
                    showMsg("\t" +ex.getClass().getSimpleName());
                    showMsg("\t" +ex.getMessage());
                }
            }

            logIdxMax = logs.getLogIndexMax();
            logIdxMin = logs.getLogIndexMin();
            logRemainder = logs.hasMoreLog();
            logRemainder += LockCommGetLogList.PageSize;  //否则会少一页
            return false;
        }
    };

    private void showMsg(final CharSequence msg) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTv.append(msg+"\n");
                sv.smoothScrollBy(99999, 9999);
            }
        });
    }
}
