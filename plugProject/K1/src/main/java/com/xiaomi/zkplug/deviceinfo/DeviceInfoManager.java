package com.xiaomi.zkplug.deviceinfo;

import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.smarthome.common.ui.dialog.MLAlertDialog;
import com.xiaomi.smarthome.common.ui.dialog.XQProgressDialog;
import com.xiaomi.smarthome.common.ui.widget.SwitchButton;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.lockoperater.ILockDataOperator;
import com.xiaomi.zkplug.lockoperater.LockOperateCallback;
import com.xiaomi.zkplug.lockoperater.OperatorBuilder;
import com.xiaomi.zkplug.lockoperater.SecureSetVolumn;
import com.xiaomi.zkplug.lockoperater.SecureStatus;
import com.xiaomi.zkplug.lockoperater.SecureSyncTime;
import com.xiaomi.zkplug.util.BitConverter;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

import cn.zelkova.lockprotocol.BriefDate;
import cn.zelkova.lockprotocol.LockCmdGetStatus;
import cn.zelkova.lockprotocol.LockCmdSetTime;
import cn.zelkova.lockprotocol.LockCmdSetVolumn;

/**
 * 作者：liwenqi on 18/2/2 10:28
 * 邮箱：liwenqi@zelkova.cn
 * 描述：MVC-M
 */

public class DeviceInfoManager {

    private final String TAG = "DeviceInfoManager";
    private final int GET_STATUS_FAIL_TIME = 20 * 1000;//15秒时间
    private final int MSG_TIME_OUT = 0x0;//超时
    private final int MSG_GET_STATUS = 0x10;
    private final int MSG_MUTE_SET_TIME_OUT = 0x11;//静音设置超时
    private final int MSG_SEND_SYNC_TIME_CMD = 0x20;//发送同步时间命令
    private final int MSG_SEND_VOLUMN_SET_CMD = 0x30;//发送设置静音模式命令
    boolean fromGetInfoFlag = false;//从获取设备信息而来, 则不需要监听muteSwitch的变化
    Activity activity;
    XQProgressDialog xqProgressDialog;
    Device mDevice;
    TextView devicePowerTv, deviceSecLevelTv, muteModeTv, fpCountTv, pwdCountTv, romVerTv, pluginVerTv, lockTimeTv, phoneTimeTv;
    SwitchButton muteSwitch;
    ILockDataOperator iStatusOperator, iSyncTimeOperator, iSetVolumnOperator;
    DataManageUtil dataManageUtil;
    boolean muteSetComplete = false;//静音模式设置完成标识位
    public DeviceInfoManager(Device device, final Activity activity) {
        this.mDevice = device;
        this.activity = activity;
        if(iStatusOperator == null){
            iStatusOperator = OperatorBuilder.create(SecureStatus.FLAG, activity, mDevice);
        }
        this.dataManageUtil = new DataManageUtil(mDevice.deviceStat(), activity);
        xqProgressDialog = new XQProgressDialog(activity);
        xqProgressDialog.setCancelable(false);
        this.muteSwitch = (SwitchButton) activity.findViewById(R.id.muteSwitch);
        this.devicePowerTv = (TextView) activity.findViewById(R.id.devicePowerTv);
        this.deviceSecLevelTv = (TextView) activity.findViewById(R.id.deviceSecLevelTv);
        this.muteModeTv = (TextView) activity.findViewById(R.id.muteModeTv);
        this.fpCountTv = (TextView) activity.findViewById(R.id.fpCountTv);
        this.pwdCountTv = (TextView) activity.findViewById(R.id.pwdCountTv);
        this.romVerTv = (TextView) activity.findViewById(R.id.romVerTv);
        this.pluginVerTv = (TextView) activity.findViewById(R.id.pluginVerTv);
        this.lockTimeTv = (TextView) activity.findViewById(R.id.lockTimeTv);
        this.phoneTimeTv = (TextView) activity.findViewById(R.id.phoneTimeTv);
        this.muteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(fromGetInfoFlag){
                    fromGetInfoFlag = false;
                }else{//不是从getInfo而来，才需要监听变化
                    if(!ZkUtil.isBleOpen()){
                        Toast.makeText(activity, "请打开手机蓝牙", Toast.LENGTH_LONG).show();
                        return;
                    }
                    muteSetComplete = false;
                    setMuteMode(isChecked);
                }
            }
        });
    }

    protected void getDeviceInfo(){
        if(!ZkUtil.isBleOpen()){
            MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity);
            builder.setTitle("提示");
            builder.setMessage("手机蓝牙未打开");
            builder.setNegativeButton("确定", new MLAlertDialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    activity.finish();
                }
            });
            builder.show();
            return;
        }
        xqProgressDialog.setMessage("正在获取设备信息");
        xqProgressDialog.show();

        getLockStatus();
    }

    /**
     *
     * 获取锁状态命令
     * @return
     */
    private LockCmdGetStatus getLockStatusCmd(){
        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.YEAR, -5);
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.YEAR, 180);
        BriefDate vTo = BriefDate.fromNature(cld.getTime());
        LockCmdGetStatus lockCmdGetStatus = new LockCmdGetStatus(BitConverter.convertMacAdd(mDevice.getMac()), vFrom, vTo);
        Log.d(TAG, vFrom.toString()+"-----"+vTo.toString());
        return lockCmdGetStatus;
    }

    /**
     * 发送获取状态命令
     */
    private void sendStatusCmd(){

        iStatusOperator.registerBluetoothReceiver();
        iStatusOperator.sendLockMsg(getLockStatusCmd().getBytes(), new LockOperateCallback() {
            @Override
            public void lockOperateSucc(String value) {//value为时间差
                //判断锁内时间偏差
                try {
                    iStatusOperator.unregisterBluetoothReceiver();
                    JSONObject statusObj = new JSONObject(value);
                    Log.d(TAG, "statusObj: "+statusObj.toString());
                    setDeviceInfoValue(statusObj);
                    xqProgressDialog.dismiss();
                    viewHanlder.removeMessages(MSG_TIME_OUT);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void lockOperateFail(String value) {
                Log.d(TAG, "getLockStatus:"+value);
                iStatusOperator.unregisterBluetoothReceiver();
                xqProgressDialog.dismiss();
                Toast.makeText(activity, value, Toast.LENGTH_LONG).show();
                if(value.indexOf("命令不在有效期内") != -1){//需要同步时间
                    showSyncTimeDialog();
                }
            }
        });
    }

    /**
     * 文字变色
     */
    private void showErrMsg(TextView tv, String msg){
        SpannableString spanttt = new SpannableString(msg);//renshu为要变色的字符串
        spanttt.setSpan(new ForegroundColorSpan(activity.getResources().getColor(R.color.warn_color)), 0, msg.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);//将字符串变色
        tv.append(spanttt);
    }

    /**
     * 数值设置
     */
    private void setDeviceInfoValue(JSONObject statusObj){

        try {
            devicePowerTv.setText(activity.getResources().getString(R.string.device_info_power, statusObj.getInt("power"))+"%");
            if(statusObj.getInt("power") < 10){
                showErrMsg(devicePowerTv, "（电量过低请及时更换电池）");
            }
            int securityLevel = (statusObj.getInt("seclevel") & 3);

            String securityLevelStr = "A";
            if(securityLevel == 1){
                securityLevelStr = "C";
            }else if(securityLevel == 2){
                securityLevelStr = "A";
            }else if(securityLevel == 3){
                securityLevelStr = "B";
            }else{
                securityLevelStr = "异常";
            }

            deviceSecLevelTv.setText(activity.getResources().getString(R.string.device_info_level_title, securityLevelStr));
            if(statusObj.getInt("volumn") == 0){//静音
                fromGetInfoFlag = true;
                muteSwitch.setChecked(true);
            }
            fpCountTv.setText(activity.getResources().getString(R.string.device_info_fp_count, statusObj.getInt("fpcount")));
            pwdCountTv.setText(activity.getResources().getString(R.string.device_info_pwd_count, statusObj.getInt("pwdcount")));
            romVerTv.setText(activity.getResources().getString(R.string.device_info_rom_ver, statusObj.getString("romver")));
            pluginVerTv.setText(activity.getResources().getString(R.string.device_info_plugin_ver, statusObj.getString("pluginver")));
            lockTimeTv.setText(activity.getResources().getString(R.string.device_info_lock_time, statusObj.getString("locktime")));
            phoneTimeTv.setText(activity.getResources().getString(R.string.device_info_phone_time, BriefDate.fromNature(new Date()).toString()));
            long timdiffsnd = statusObj.getLong("timdiffsnd");

            Log.d(TAG, "timdiffsnd"+timdiffsnd);
            if(timdiffsnd > 30){
                showErrMsg(phoneTimeTv,"（偏差"+timdiffsnd+"秒建议同步时间）");
                //showSyncTimeDialog();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置静音模式
     */
    private void setMuteMode(boolean isChecked){
        if(isChecked){
            xqProgressDialog.setMessage("正在开启");
        }else{
            xqProgressDialog.setMessage("正在关闭");
        }
        viewHanlder.sendEmptyMessageDelayed(MSG_MUTE_SET_TIME_OUT, GET_STATUS_FAIL_TIME);
        xqProgressDialog.show();
        operateLockVolumnSet();
    }
    /**
     * 进入首届面获取门锁状态
     * 进行更新门锁状态和授时
     */
    private void getLockStatus(){
        Log.d(TAG, "getLockStatus开始");
        XmBluetoothManager.getInstance().disconnect(mDevice.getMac());

        viewHanlder.sendEmptyMessageDelayed(MSG_GET_STATUS, 2000);
    }

    /**
     * 失败时重新获取
     */
    private void showGetStatusRetryDialog(){
        MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity);
        builder.setTitle("获取锁内信息失败");
        builder.setMessage("请将手机尽量靠近门锁重新获取");
        builder.setPositiveButton("重新获取", new MLAlertDialog.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getDeviceInfo();
            }
        });
        builder.setNegativeButton("退出", new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                activity.finish();
            }
        });
        builder.show();
    }
    /**
     * 时序: 用于界面控制
     */
    Handler viewHanlder = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_MUTE_SET_TIME_OUT:
                    Log.d(TAG, "连接设备失败");
                    unregisterBleReceiver();
                    Toast.makeText(activity, "未发现门锁，请靠近门锁重试", Toast.LENGTH_LONG).show();
                    XmBluetoothManager.getInstance().disconnect(mDevice.getMac());
                    xqProgressDialog.dismiss();
                    break;
                case MSG_TIME_OUT:
                    unregisterBleReceiver();
                    showGetStatusRetryDialog();
                    xqProgressDialog.dismiss();
                    break;
                case MSG_GET_STATUS:
                    iStatusOperator.registerBluetoothReceiver();
                    viewHanlder.sendEmptyMessageDelayed(MSG_TIME_OUT, GET_STATUS_FAIL_TIME);
                    XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                        @Override
                        public void onResponse(int i, Bundle bundle) {
                            if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                                sendStatusCmd();
                            }
                        }
                    });
                    break;
                case MSG_SEND_SYNC_TIME_CMD:
                    sendSyncTimeCmd();
                    break;
                case MSG_SEND_VOLUMN_SET_CMD:
                    sendVolumnSetCmd();
                    break;
                default:
                    break;
            }
        }
    };


    protected void unregisterBleReceiver() {
        if (iStatusOperator != null) {
            iStatusOperator.unregisterBluetoothReceiver();
        }
        if (iSyncTimeOperator != null) {
            iSyncTimeOperator.unregisterBluetoothReceiver();
        }
        if (iSetVolumnOperator != null) {
            iSetVolumnOperator.unregisterBluetoothReceiver();
        }
    }
    /**
     *
     * 获取授时命令
     * @return
     */
    private LockCmdSetTime getTimeSyncCmd(){
        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, -180);
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, 180);
        BriefDate vTo = BriefDate.fromNature(cld.getTime());
        LockCmdSetTime lockCmdSetTime = new LockCmdSetTime(BitConverter.convertMacAdd(mDevice.getMac()), vFrom, vTo, BriefDate.fromNature(new Date()));
        return lockCmdSetTime;
    }

    private void sendSyncTimeCmd(){
        iSyncTimeOperator.sendLockMsg(getTimeSyncCmd().getBytes(),new LockOperateCallback() {
            @Override
            public void lockOperateSucc(String lockTime) {
                iSyncTimeOperator.unregisterBluetoothReceiver();
                Log.d(TAG, "同步成功");
                Toast.makeText(activity, "同步成功", Toast.LENGTH_SHORT).show();
                xqProgressDialog.dismiss();
                sendStatusCmd();//同步成功后，再次获取锁信息

                //把同步时间的日期，存进服务器
                JSONObject settingsObj = new JSONObject();
                try {
                    settingsObj.put("keyid_syncTime_data", BriefDate.fromNature(new Date()).toString());
                    dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                        @Override
                        public void dataUpateFail(int i, String s) {
                            Log.d(TAG, "数据存储失败！");
                        }

                        @Override
                        public void dataUpdateSucc(String s) {
                            Log.d(TAG, "数据存储成功！");
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void lockOperateFail(String value) {
                Log.d(TAG, "同步失败");
                iSyncTimeOperator.unregisterBluetoothReceiver();
                Toast.makeText(activity, value, Toast.LENGTH_SHORT).show();
                xqProgressDialog.dismiss();
            }
        });
    }


    /**
     * 发送前状态判断
     */
    private void prepareSendSyncCmd(){
        iSyncTimeOperator = OperatorBuilder.create(SecureSyncTime.FLAG, activity, mDevice);
        iSyncTimeOperator.registerBluetoothReceiver();
        viewHanlder.sendEmptyMessageDelayed(MSG_SEND_SYNC_TIME_CMD, 2000);//注册广播后，延迟一秒发送
    }
    /**
     * 授时间
     */
    protected void operateLockSyncTime(){
        xqProgressDialog.setMessage("正在同步时间");
        xqProgressDialog.show();
        Log.d(TAG, "开始授时: "+BriefDate.fromNature(new Date()).toString());
        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "已连接，直接发送授时间命令");
            prepareSendSyncCmd();
        } else {
            //未连接就先建立连接
            Log.d(TAG, "授时时未连接，就先建立连接");
            XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                @Override
                public void onResponse(int i, Bundle bundle) {
                    Log.d(TAG, "回调状态:" + i);
                    if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                        prepareSendSyncCmd();
                    }else{
                        XmBluetoothManager.getInstance().disconnect(mDevice.getMac());
                        iSyncTimeOperator.unregisterBluetoothReceiver();
                        Log.d(TAG, "连接设备失败");
                        Toast.makeText(activity, "连接设备失败", Toast.LENGTH_SHORT).show();
                        xqProgressDialog.dismiss();
                    }
                }
            });
        }
    }
    /**
     * 命令不在有效期弹框
     */
    private void showSyncTimeDialog(){
        MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity);
        builder.setTitle("时间偏差过大");
        builder.setMessage("请将手机时间同步到锁内");
        builder.setPositiveButton("同步", new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                operateLockSyncTime();
            }
        });
        builder.setNegativeButton("取消", new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    /**
     *
     * 获取设置静音模式命令
     * @return
     */
    private LockCmdSetVolumn getVolumnSetCmd(){
        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, -180);
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, 180);
        BriefDate vTo = BriefDate.fromNature(cld.getTime());
        LockCmdSetVolumn lockCmdSetVolumn = new LockCmdSetVolumn(BitConverter.convertMacAdd(mDevice.getMac()), vFrom, vTo);
        if(muteSwitch.isChecked()){
            lockCmdSetVolumn.setVolumn(0);
        }else{
            lockCmdSetVolumn.setVolumn(127);
        }

        return lockCmdSetVolumn;
    }

    private void sendVolumnSetCmd(){
        Log.d(TAG, "sendVolumnSetCmd");
        iSetVolumnOperator.sendLockMsg(getVolumnSetCmd().getBytes(),new LockOperateCallback() {
            @Override
            public void lockOperateSucc(String result) {
                Log.d(TAG, "设置成功");
                muteSetComplete = true;
                viewHanlder.removeMessages(MSG_MUTE_SET_TIME_OUT);
                iSetVolumnOperator.unregisterBluetoothReceiver();
                Toast.makeText(activity, result, Toast.LENGTH_SHORT).show();
                xqProgressDialog.dismiss();
            }

            @Override
            public void lockOperateFail(String value) {
                Log.d(TAG, "设置失败");
                muteSetComplete = true;
                viewHanlder.removeMessages(MSG_MUTE_SET_TIME_OUT);
                iSetVolumnOperator.unregisterBluetoothReceiver();
                Toast.makeText(activity, value, Toast.LENGTH_SHORT).show();
                xqProgressDialog.dismiss();
                if(value.indexOf("命令不在有效期内") != -1){//需要同步时间
                    showSyncTimeDialog();
                }

            }
        });
    }

    /**
     * 发送前状态判断
     */
    private void prepareSendVolumnSetCmd(){
        iSetVolumnOperator = OperatorBuilder.create(SecureSetVolumn.FLAG, activity, mDevice);
        iSetVolumnOperator.registerBluetoothReceiver();
        viewHanlder.sendEmptyMessageDelayed(MSG_SEND_VOLUMN_SET_CMD, 2000);//注册广播后，延迟2秒发送
    }
    /**
     * 设置音量
     */
    protected void operateLockVolumnSet(){
        iSetVolumnOperator = OperatorBuilder.create(SecureSetVolumn.FLAG, activity, mDevice);
        iSetVolumnOperator.registerBluetoothReceiver();

        Log.d(TAG, "开始设置音量: "+BriefDate.fromNature(new Date()).toString());
        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "已连接，直接发送音量设置命令");
            //prepareSendVolumnSetCmd();
            viewHanlder.sendEmptyMessageDelayed(MSG_SEND_VOLUMN_SET_CMD, 2000);//注册广播后，延迟2秒发送
        } else {
            //未连接就先建立连接
            Log.d(TAG, "设置时未连接，就先建立连接");
            XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                @Override
                public void onResponse(int i, Bundle bundle) {
                    Log.d(TAG, "回调状态:" + i);
                    if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                        //prepareSendVolumnSetCmd();
                        viewHanlder.sendEmptyMessageDelayed(MSG_SEND_VOLUMN_SET_CMD, 2000);//注册广播后，延迟2秒发送
                    }//else使用超时来控制
//                    else{
//                        viewHanlder.removeMessages(MSG_MUTE_SET_TIME_OUT);
//                        XmBluetoothManager.getInstance().disconnect(mDevice.getMac());
//                        if(iSetVolumnOperator != null){
//                            iSyncTimeOperator.unregisterBluetoothReceiver();
//                        }
//                        Log.d(TAG, "连接设备失败");
//                        Toast.makeText(activity, "连接设备失败", Toast.LENGTH_SHORT).show();
//                        xqProgressDialog.dismiss();
//                    }
                }
            });
        }
    }


}
