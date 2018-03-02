package com.xiaomi.zkplug.fpinput;

import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.plugin.core.XmPluginPackage;
import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.smarthome.common.ui.dialog.XQProgressDialog;
import com.xiaomi.smarthome.device.api.DeviceStat;
import com.xiaomi.zkplug.CommonUtils;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.entity.MyEntity;
import com.xiaomi.zkplug.lockoperater.ILockDataOperator;
import com.xiaomi.zkplug.lockoperater.LockOperateCallback;
import com.xiaomi.zkplug.lockoperater.OperatorBuilder;
import com.xiaomi.zkplug.lockoperater.SecureFpAdd;
import com.xiaomi.zkplug.util.BitConverter;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import cn.zelkova.lockprotocol.BriefDate;
import cn.zelkova.lockprotocol.LockCmdFpAdd;

/**
 * 作者：liwenqi on 17/8/15 10:27
 * 邮箱：liwenqi@zelkova.cn
 * 描述：录入指纹
 */
public class FpInputHelper {

    private final String TAG = "FpInputHelper";
    private final int MSG_FP_INPUT_TIMEOUT = 0;
    Activity activity;
    ILockDataOperator iLockDataOperator;
    XmPluginPackage xmPluginPackage;
    DeviceStat mDeviceStat;
    Device mDevice;
    String memberId;//要操作的成员ID
    String nickName;//nickName
    DataManageUtil dataManageUtil;
    private int retryCount;//重试次数
    TextView fpCompleteTv, fpPutTv;
    LinearLayout fpTipLayout, fpInputBuzhouLayout;
    public static XQProgressDialog xqProgressDialog;
    protected FpInputHelper(Activity activity, DeviceStat deviceStat, XmPluginPackage xmPluginPackage) {
        this.activity = activity;
        this.mDeviceStat = deviceStat;
        this.mDevice = Device.getDevice(deviceStat);
        this.xmPluginPackage = xmPluginPackage;
        this.dataManageUtil = new DataManageUtil(deviceStat, activity);
        this.iLockDataOperator = OperatorBuilder.create(SecureFpAdd.FLAG, activity, mDevice);
        this.memberId = activity.getIntent().getStringExtra("memberId");
        this.nickName = activity.getIntent().getStringExtra("nickName");
        this.fpTipLayout = (LinearLayout) activity.findViewById(R.id.fpTipLayout);
        this.fpInputBuzhouLayout = (LinearLayout) activity.findViewById(R.id.fpInputBuzhouLayout);
        this.fpPutTv = (TextView) activity.findViewById(R.id.fpPutTv);//录入步骤
        this.fpCompleteTv = (TextView) activity.findViewById(R.id.fpCompleteTv);
    }

    protected void unregisterBluetoothReceiver(){
        iLockDataOperator.unregisterBluetoothReceiver();
    }
    protected void registerBluetoothReceiver(){
        iLockDataOperator.registerBluetoothReceiver();
    }
    /*
    * 拼接锁命令
    * */
    private LockCmdFpAdd getFpIputCmd(){
        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.MINUTE, -60);
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.MINUTE, 60);
        BriefDate vTo = BriefDate.fromNature(cld.getTime());

        byte[] macByte = BitConverter.convertMacAdd(mDevice.getMac());
        LockCmdFpAdd lockCmdAddFp = new LockCmdFpAdd(macByte, vFrom, vTo);
        lockCmdAddFp.setFpParam(8, 60);
        return lockCmdAddFp;
    }

    /**
     * 先找出对应人，再更新进去
     * @param fpBatchNo
     * @return 成员的指纹列表
     */
    private JSONArray addMemberFp(String fpBatchNo){
        JSONArray fpArray = new JSONArray();//指纹列表
        try{
            JSONArray mJsArray = new JSONArray();
            if(memberId.equals("")){
                mJsArray.put("keyid_master$fp_data");
                JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);
                if(resultObj.has("keyid_master$fp_data") && !resultObj.getString("keyid_master$fp_data").equals("")){
                    fpArray = new JSONArray(resultObj.getString("keyid_master$fp_data"));
                }
            }else{
                mJsArray.put("keyid_sm$fp$"+memberId+"_data");
                JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);
                if(resultObj.has("keyid_sm$fp$"+memberId+"_data") && !resultObj.getString("keyid_sm$fp$"+memberId+"_data").equals("")){
                    fpArray = new JSONArray(resultObj.getString("keyid_sm$fp$"+memberId+"_data"));
                }
            }
            JSONObject fpObj = new JSONObject();
            if(fpArray.length() > 0){
                /* 指纹命名规则 begin */
                int fpIndexNum = 1;
                for(int index=0; index < fpArray.length(); index++){
                    String fpName = fpArray.getJSONObject(index).getString("name");
                    if(fpName.startsWith("指纹")){
                        int m = 1;
                        try{
                            m = Integer.parseInt(fpName.substring(2));
                        }catch (Exception e){
                            e.printStackTrace();
                            continue;
                        }
                        fpIndexNum = Math.max(fpIndexNum, m);
                    }
                }
                Log.d(TAG, "fpIndexNum: "+fpIndexNum);
                /* 指纹命名规则 end */
                fpObj.put("name", "指纹"+(fpIndexNum+1));
                fpObj.put("batchno", fpBatchNo);

            }else{
                fpObj.put("name", "指纹1");
                fpObj.put("batchno", fpBatchNo);
            }
            fpArray.put(fpObj);
        }catch (JSONException e){
            e.printStackTrace();
            Toast.makeText(activity, "数据解析异常", Toast.LENGTH_SHORT).show();
        }
        return fpArray;
    }
    /*
    * 添加指纹
    * 服务器查询接口
    * */
    private void addMemberFpToServer(final String fpBatchNo){
        if(!ZkUtil.isNetworkAvailable(activity)){
            Toast.makeText(activity, "网络未连接，请确保网络畅通", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Log.d(TAG, "更新指纹到服务器");
            JSONArray fpArray = addMemberFp(fpBatchNo);
            JSONObject settingsObj = new JSONObject();
            if(memberId.equals("")){//管理员
                settingsObj.put("keyid_master$fp_data", fpArray.toString());
            }else{
                settingsObj.put("keyid_sm$fp$"+memberId+"_data", fpArray.toString());
            }
            dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                @Override
                public void dataUpdateSucc(String s) {
                    //设置key与name的对应关系
                    Log.d(TAG, fpBatchNo+", "+nickName);
                    dataManageUtil.setKeyToName(fpBatchNo, nickName);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fpPutTv.setText("添加完成");
                            fpPutTv.setTextColor(activity.getResources().getColor(R.color.text_succ));
                            fpCompleteTv.setText("请尝试用指纹开一次锁");
                            iLockDataOperator.unregisterBluetoothReceiver();
                        }
                    });
                }
                @Override
                public void dataUpateFail(int i, final String s) {
                    retryCount+=1;
                    Log.d(TAG, "设置失败, retryCount: "+retryCount);
                    if(retryCount > 3){
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                iLockDataOperator.unregisterBluetoothReceiver();
                                CommonUtils.toast(activity, s);
                                activity.finish();
                            }
                        });

                    }else{
                        addMemberFpToServer(fpBatchNo);//重试
                    }
                }
            });
        }catch (JSONException je){
            je.printStackTrace();
        }
    }

    //发送锁命令,并更新数据
    protected void operateLockMsg(LockCmdFpAdd lockCmdAddFp){
        //viewHanlder.removeMessages(MSG_FP_INPUT_TIMEOUT);
        iLockDataOperator.sendLockMsg(lockCmdAddFp.getBytes(), new LockOperateCallback() {
            @Override
            public void lockOperateSucc(String fpBatchNo) {
                //更新数据到服务器
                retryCount = 0;//每次存储服务器都置位
                addMemberFpToServer(fpBatchNo);
            }
            @Override
            public void lockOperateFail(String value) {
                if(value.equals("命令不在有效期内(3)")){
                    ZkUtil.showCmdTimeOutView(activity);
                }else{
                    CommonUtils.toast(activity, value);
                }
                fpTipLayout.setVisibility(View.VISIBLE);
                fpInputBuzhouLayout.setVisibility(View.GONE);
                iLockDataOperator.unregisterBluetoothReceiver();
                xqProgressDialog.dismiss();
                viewHanlder.removeMessages(MSG_FP_INPUT_TIMEOUT);

            }
        });
    }

    public static XQProgressDialog getXqProgressDialog(){
        return xqProgressDialog;
    }

    //录入指纹
    protected void addMemberFp(){
        xqProgressDialog = new XQProgressDialog(activity);
        xqProgressDialog.setMessage("正在请求录入");
        xqProgressDialog.setCancelable(false);
        xqProgressDialog.show();
        dataManageUtil.checkMemberAvaliable(memberId, new DataUpdateCallback() {
            @Override
            public void dataUpateFail(int i, final String s) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, s, Toast.LENGTH_SHORT).show();
                        xqProgressDialog.dismiss();
                    }
                });
            }

            @Override
            public void dataUpdateSucc(final String s) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (s.equals("true")) {//有效人员
                            viewHanlder.sendEmptyMessageDelayed(MSG_FP_INPUT_TIMEOUT, MyEntity.OPERATE_TIMEOUT);
                            final LockCmdFpAdd lockCmdAddFp = getFpIputCmd();
                            Log.d(TAG, "录入指纹锁命令:"+cn.zelkova.lockprotocol.BitConverter.toHexString(lockCmdAddFp.getBytes()));
                            if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
                                operateLockMsg(lockCmdAddFp);
                            }else{
                                XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                                    @Override
                                    public void onResponse(int i, Bundle bundle) {
                                        if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                                            operateLockMsg(lockCmdAddFp);
                                        }else if(i == XmBluetoothManager.Code.REQUEST_NOT_REGISTERED){
                                            Toast.makeText(activity, "设备已被重置，请解除绑定后重新添加", Toast.LENGTH_LONG).show();
                                            iLockDataOperator.unregisterBluetoothReceiver();
                                            viewHanlder.removeMessages(MSG_FP_INPUT_TIMEOUT);
                                            xqProgressDialog.dismiss();
                                        }
//                                        else {
//                                            CommonUtils.toast(activity, "未发现门锁，请靠近门锁重试");
//                                            iLockDataOperator.unregisterBluetoothReceiver();
//                                            viewHanlder.removeMessages(MSG_FP_INPUT_TIMEOUT);
//                                            xqProgressDialog.dismiss();
//                                        }
                                    }
                                });
                            }
                        }else{
                            Toast.makeText(activity, "成员已被删除", Toast.LENGTH_SHORT).show();
                            xqProgressDialog.dismiss();
                        }
                    }
                });
            }
        });
    }

    Handler viewHanlder = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FP_INPUT_TIMEOUT:
                    if(fpInputBuzhouLayout.getVisibility() == View.GONE){
                        Log.d(TAG, "启动超时，执行断开:"+mDeviceStat.mac);
                        XmBluetoothManager.getInstance().disconnect(mDeviceStat.mac);
                        CommonUtils.toast(activity, "未发现门锁，请靠近门锁重试");
                        iLockDataOperator.unregisterBluetoothReceiver();
                        xqProgressDialog.dismiss();
                    }
                    break;

                default:
                    break;
            }
        }
    };

}
