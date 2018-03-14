package com.xiaomi.zkplug.log;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.lockoperater.ILogReadOperator;
import com.xiaomi.zkplug.lockoperater.LogReadCallback;
import com.xiaomi.zkplug.lockoperater.SecureLogRead;
import com.xiaomi.zkplug.util.BitConverter;
import com.xiaomi.zkplug.util.ZkUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import cn.zelkova.lockprotocol.BriefDate;
import cn.zelkova.lockprotocol.LockCmdGetLog;
import cn.zelkova.lockprotocol.LockCommGetLogResponse;

/**
 * 用来返回日志列表。由于返回日志比较繁琐，所以对返回逻辑做了封装。
 * <p>以同步的方式返回日志记录
 *
 * @author zp
 *         Created by zp on 2016/7/26.
 */
public class LockCommGetLogList {
    private static ILogReadOperator iLogReadOperator;

    Device mDevice;
    Activity activity;
    public LockCommGetLogList(Activity activity, Device mDevice) {
        this.activity = activity;
        this.mDevice = mDevice;
        iLogReadOperator = new SecureLogRead(activity, mDevice, beginIdx);
        iLogReadOperator.registerBluetoothReceiver();
    }

    /**
     * 每批次获得日志后，调用次接口
     */
    public interface IResultCallback{
        /**
         * 每页获得之后，调用此接口
         * @param logs 本批次获得的日志
         * @return 是否停止获取日志
         */
        boolean showLogs(LockCommGetLogResponse logs);
    }

    /**
     * 获取日志时，每页的返回数量，如果过大会导致通讯失败。<br>
     * 默认值能较为稳定的获得日志
     */
    public static short PageSize = 5;

    /*
    * 读取日志锁命令
    * */
    private LockCmdGetLog getLogReadCmd(byte orderType, long startIdx){
        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.MINUTE, -60);
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.MINUTE, 60);
        BriefDate vTo = BriefDate.fromNature(cld.getTime());
        byte[] macByte = BitConverter.convertMacAdd(mDevice.getMac());
        LockCmdGetLog lockCmdGetLog = new LockCmdGetLog(macByte, vFrom, vTo);
        Log.d("SecureLogRead", "startIdx-LogIndexMax: "+startIdx);
        lockCmdGetLog.setLogGetParam(orderType, startIdx, LockCommGetLogList.PageSize);//从最旧的开始读
        Log.d("SecureLogRead", "lockCmdGetLog: " + cn.zelkova.lockprotocol.BitConverter.toHexString(lockCmdGetLog.getBytes()));
        return lockCmdGetLog;
    }

    //orderType :0x01，从最旧开始；0x00：从最新开始
    ArrayList<LockCommGetLogResponse.LogRecord> result =new ArrayList<>();
    long beginIdx = 0xFFFFFFFFL;
    //long beginIdx = 470;
    int remaining = 3000;
    public LockCommGetLogResponse.LogRecord[] getLockLog(final byte orderType, final IResultCallback callback) {

        Log.d("SecureLogRead", "beginIdx-init: "+beginIdx);
        LockCmdGetLog lockCmdGetLog = getLogReadCmd(orderType, beginIdx);
        iLogReadOperator.sendLockMsg(lockCmdGetLog.getBytes(), new LogReadCallback() {
            @Override
            public void logReadSucc(LockCommGetLogResponse getLogResp) {
                Log.d("GetLogList", cn.zelkova.lockprotocol.BitConverter.toHexString(getLogResp.getBytes()));
                result.addAll(Arrays.asList(getLogResp.getLogRecord()));
                beginIdx = getLogResp.getLogIndexMin();
//                beginIdx = getLogResp.getLogIndexMax();//从旧的读
                beginIdx -= 1; //修正衔接部分
                remaining -= getLogResp.logCount();
                Log.d("SecureLogRead", "remaining: "+remaining+", logCount: "+getLogResp.logCount()+", beginIdx: "+beginIdx);
                if(callback!=null){
                    boolean stop = callback.showLogs(getLogResp);
                    if(stop) {
                        iLogReadOperator.unregisterBluetoothReceiver();
                        return;
                    }
                }
                if (remaining <= 0) {
                    Log.d("SecureLogRead", "remaining <= 0");
                    iLogReadOperator.unregisterBluetoothReceiver();
                    return;
                }else if (getLogResp.logCount() <= 0) {
                    Log.d("SecureLogRead", "getLogResp.logCount() <= 0");
                    iLogReadOperator.unregisterBluetoothReceiver();
                    return;
                }else if (getLogResp.hasMoreLog() <= 0) {
                    Log.d("SecureLogRead", "getLogResp.hasMoreLog() <= 0");
                    iLogReadOperator.unregisterBluetoothReceiver();
                    return;
                }else{
                    getLockLog(orderType, callback);
                }
            }

            @Override
            public void logReadFail(String value) {
                if(value.indexOf(activity.getString(R.string.device_cmd_timeout)) != -1){//需要同步时间
                    ZkUtil.showCmdTimeOutView(activity);
                }else{
                    Toast.makeText(activity, value, Toast.LENGTH_SHORT).show();
                }
                iLogReadOperator.unregisterBluetoothReceiver();
            }
        });

        return result.toArray(new LockCommGetLogResponse.LogRecord[0]);
    }
}
