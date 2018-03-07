package com.xiaomi.zkplug.member;

import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.plugin.core.XmPluginPackage;
import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.smarthome.common.ui.dialog.MLAlertDialog;
import com.xiaomi.smarthome.common.ui.dialog.XQProgressDialog;
import com.xiaomi.smarthome.device.api.DeviceStat;
import com.xiaomi.zkplug.CommonUtils;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.entity.MyEntity;
import com.xiaomi.zkplug.lockoperater.ILockDataOperator;
import com.xiaomi.zkplug.lockoperater.LockOperateCallback;
import com.xiaomi.zkplug.lockoperater.OperatorBuilder;
import com.xiaomi.zkplug.lockoperater.SecureFpDel;
import com.xiaomi.zkplug.lockoperater.SecurePwdDel;
import com.xiaomi.zkplug.member.slideview.ListViewCompat;
import com.xiaomi.zkplug.util.BitConverter;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import cn.zelkova.lockprotocol.BriefDate;
import cn.zelkova.lockprotocol.LockCmdFpDel;
import cn.zelkova.lockprotocol.LockCmdSyncPwd;

/**
 * 作者：liwenqi on 17/8/15 10:27
 * 邮箱：liwenqi@zelkova.cn
 * 描述：
 */
class MemberHelper {

    private final String TAG = "MemberHelper";
    private final int MSG_PWD_DEL_TIMEOUT = 0;//密码删除超时
    private final int MSG_MEMBER_DEL_TIMEOUT = 1;//家人删除超时
    Activity activity;
    XmPluginPackage xmPluginPackage;
    DeviceStat mDeviceStat;
    Device mDevice;
    ListViewCompat pwdUnlockView;
    RelativeLayout pwdSetRel;
    String memberId;//要操作的成员ID
    XQProgressDialog xqProgressDialog;
    boolean isDelMember = false;//是否为删除家人
    int fpDelIndex = 0;//删除到第几个指纹了

    DataManageUtil dataManageUtil;
    /*
    * securePwdFlag: 要初始化为哪个对象
    * */
    protected MemberHelper(final Activity activity, DeviceStat deviceStat, XmPluginPackage xmPluginPackage, String memberId) {
        this.activity = activity;
        this.mDeviceStat = deviceStat;
        this.mDevice = Device.getDevice(deviceStat);
        this.xmPluginPackage = xmPluginPackage;
        this.pwdUnlockView = (ListViewCompat) activity.findViewById(R.id.pwdUnlockView);
        this.pwdSetRel = (RelativeLayout) activity.findViewById(R.id.pwdSetRel);
        this.memberId = memberId;
        this.dataManageUtil = new DataManageUtil(deviceStat, activity);

    }

    /*
    * 删除密码
    * */
    protected LockCmdSyncPwd getPwdDelCmd(int pwdIdxToDel){
        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, -60);
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, 180);
        BriefDate vTo = BriefDate.fromNature(cld.getTime());
        Log.d(TAG, "mDevice.getMac():"+mDevice.getMac()+", "+ BitConverter.convertMacAdd(mDevice.getMac()));
        LockCmdSyncPwd cmdSyncPwd = new LockCmdSyncPwd(BitConverter.convertMacAdd(mDevice.getMac()), vFrom, vTo);
        cmdSyncPwd.setPwdToDel(pwdIdxToDel);
        return cmdSyncPwd;
    }

    /**
     * 删除服务器和本地存储的密码
     * @param thekey
     */
    protected void delPwdInMemory(String thekey){
        try {
            JSONObject settingsObj = new JSONObject();
            settingsObj.put(thekey, "");
            dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                @Override
                public void dataUpdateSucc(String s) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(isDelMember){
                                delMemberNameInMemory();
                            }else{
                                viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                                viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
                                Toast.makeText(activity, R.string.gloable_del_succ, Toast.LENGTH_LONG).show();
                                pwdSetRel.setVisibility(View.VISIBLE);
                                pwdUnlockView.setVisibility(View.GONE);
                                xqProgressDialog.dismiss();
                            }
                        }
                    });
                }
                @Override
                public void dataUpateFail(int i, final String s) {
                    viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                    viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CommonUtils.toast(activity, s);
                            xqProgressDialog.dismiss();
                        }
                    });
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
            viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
            Toast.makeText(activity, R.string.gloable_data_error, Toast.LENGTH_LONG).show();
            xqProgressDialog.dismiss();
        }
    }

    /**
     * 删除服务器和本地存储的指纹
     */
    protected void delFpInMemory(){
        Log.d(TAG, "delFpInMemory");
        try {
            final String theFpkey;
            JSONArray mJsArray = new JSONArray();
            if(memberId.equals("")){
                theFpkey = "keyid_master$fp_data";
                mJsArray.put(theFpkey);
            }else{
                theFpkey = "keyid_sm$fp$"+memberId+"_data";
                mJsArray.put(theFpkey);
            }

            JSONObject settingsObj = new JSONObject();
            settingsObj.put(theFpkey, "");
            dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                @Override
                public void dataUpdateSucc(String s) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            delPwdInLock();//开始删除密码
                        }
                    });
                }
                @Override
                public void dataUpateFail(int i, final String s) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CommonUtils.toast(activity, s);
                            xqProgressDialog.dismiss();
                        }
                    });
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(activity, R.string.gloable_data_error, Toast.LENGTH_LONG).show();
            xqProgressDialog.dismiss();
        }
    }

    //点击了删除密码
    protected void delPwd(){
        isDelMember = false;
        final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity);
        builder.setTitle("是否删除密码？");
        builder.setPositiveButton(R.string.gloable_confirm, new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //确定
                if(!ZkUtil.isNetworkAvailable(activity)){
                    Toast.makeText(activity, R.string.network_not_avilable, Toast.LENGTH_LONG).show();
                    return;
                }
                if(!ZkUtil.isBleOpen()){
                    CommonUtils.toast(activity, activity.getResources().getString(R.string.open_bluetooth));
                    return;
                }
                viewHanlder.sendEmptyMessageDelayed(MSG_PWD_DEL_TIMEOUT, MyEntity.OPERATE_TIMEOUT);
                xqProgressDialog = new XQProgressDialog(activity);
                xqProgressDialog.setMessage("正在删除");
                xqProgressDialog.setCancelable(false);
                xqProgressDialog.show();
                delPwdInLock();
            }
        });
        builder.setNegativeButton("取消", new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    //---------------------------删除家人----------------------------------
    /**
     * 点击了删除家人
     */
    protected void delFamilyMember(){
        /* 根据界面做了逻辑判断，不太好 being */
        TextView memberNickNameTv = (TextView) activity.findViewById(R.id.memberNickNameTv);
        TextView keyTv = (TextView) activity.findViewById(R.id.keyTv);
        if (keyTv.getText().toString().equals(activity.getResources().getString(R.string.member_had_key))) {
            Toast.makeText(activity, "请先删除"+memberNickNameTv.getText().toString()+"的手机钥匙", Toast.LENGTH_LONG).show();
            return;
        }
        /* 根据界面做了逻辑判断，不太好 being */
        isDelMember = true;
        final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity);
        builder.setTitle("是否删除成员？");
        builder.setPositiveButton(R.string.gloable_confirm, new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //确定
                if(!ZkUtil.isNetworkAvailable(activity)){
                    Toast.makeText(activity, R.string.network_not_avilable, Toast.LENGTH_LONG).show();
                    return;
                }
                if(!ZkUtil.isBleOpen()){
                    CommonUtils.toast(activity, "请打开蓝牙并靠近门锁");
                    return;
                }
                viewHanlder.sendEmptyMessageDelayed(MSG_MEMBER_DEL_TIMEOUT, MyEntity.OPERATE_TIMEOUT);
                xqProgressDialog = new XQProgressDialog(activity);
                xqProgressDialog.setMessage("正在删除");
                xqProgressDialog.setCancelable(false);
                xqProgressDialog.show();
                try {
                    JSONArray mJsArray = new JSONArray();
                    String theFpKey = "keyid_sm$fp$"+memberId+"_data";
                    mJsArray.put(theFpKey);
                    JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);
                    Log.d(TAG, "resultObj: "+resultObj.toString());
                    if(resultObj.has(theFpKey) && !resultObj.getString(theFpKey).equals("")){
                        JSONArray fpArray = new JSONArray(resultObj.getString(theFpKey));
                        if(fpArray.length() > 0) {//设置过指纹了
                            delMemberFps(fpArray);//删除指纹
                        }else{
                            delPwdInLock();//未设置过指纹, 开始删除密码
                        }
                    }else{//没有指纹
                        delPwdInLock();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                    xqProgressDialog.dismiss();
                    CommonUtils.toast(activity, "数据解析出现错误");
                }
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
     *  删除锁内家人密码
     */
    private void delPwdInLock(){
        Log.d(TAG, "delPwdInLock");
        try{
            final String thekey;
            JSONArray mJsArray = new JSONArray();
            if(memberId.equals("")){
                thekey = "keyid_master$pwd_data";
                mJsArray.put(thekey);
            }else{
                thekey = "keyid_sm$pwd$"+memberId+"_data";
                mJsArray.put(thekey);
            }
            JSONObject pwdObj = dataManageUtil.getDataFromLocal(mJsArray);
            Log.d(TAG, "pwdObj: "+pwdObj.toString());
            String pwdIdx = pwdObj.getString(thekey);
            if(!TextUtils.isEmpty(pwdIdx)){
                final ILockDataOperator iLockPwdDelOperator = OperatorBuilder.create(SecurePwdDel.FLAG, activity, mDevice);
                iLockPwdDelOperator.registerBluetoothReceiver();
                final LockCmdSyncPwd lockCmdDelPwd = getPwdDelCmd(Integer.parseInt(pwdIdx));
                if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {

                    iLockPwdDelOperator.sendLockMsg(lockCmdDelPwd.getBytes(), new LockOperateCallback() {
                        @Override
                        public void lockOperateSucc(String value) {
                            iLockPwdDelOperator.unregisterBluetoothReceiver();
                            delPwdInMemory(thekey);//删除服务器和本地数据
                        }
                        @Override
                        public void lockOperateFail(String value) {
                            viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                            viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
                            if(value.equals("命令不在有效期内(3)")){
                                ZkUtil.showCmdTimeOutView(activity);
                            }else{
                                CommonUtils.toast(activity, value);
                            }
                            xqProgressDialog.dismiss();
                        }
                    });
                } else {
                    XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                        @Override
                        public void onResponse(int i, Bundle bundle) {
                            if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                                iLockPwdDelOperator.sendLockMsg(lockCmdDelPwd.getBytes(), new LockOperateCallback() {
                                    @Override
                                    public void lockOperateSucc(String value) {
                                        iLockPwdDelOperator.unregisterBluetoothReceiver();
                                        delPwdInMemory(thekey);//删除服务器和本地数据
                                    }
                                    @Override
                                    public void lockOperateFail(String value) {
                                        viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                                        viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
                                        if(value.equals("命令不在有效期内(3)")){
                                            ZkUtil.showCmdTimeOutView(activity);
                                        }else{
                                            CommonUtils.toast(activity, value);
                                        }
                                        xqProgressDialog.dismiss();
                                    }
                                });
                            }else if(i == XmBluetoothManager.Code.REQUEST_NOT_REGISTERED){
                                viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                                viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
                                Toast.makeText(activity, R.string.device_has_been_reset, Toast.LENGTH_LONG).show();
                                xqProgressDialog.dismiss();
                            } else {
                                viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                                viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
                                CommonUtils.toast(activity, activity.getResources().getString(R.string.connect_time_out));
                                xqProgressDialog.dismiss();
                            }
                        }
                    });
                }
            }else{//没有密码，则直接删除服务器数据
                viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
                delPwdInMemory(thekey);//删除服务器和本地数据
            }
        }catch (JSONException e){
            e.printStackTrace();
            viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
            viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
            CommonUtils.toast(activity, "删除失败");
            xqProgressDialog.dismiss();
        }
    }

    /**
     * 指纹删除
     * @param
     * @return
     */
    private LockCmdFpDel getFpDelCmd(int fpBatchId){
        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.MINUTE, -60);
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.MINUTE, 60);
        BriefDate vTo = BriefDate.fromNature(cld.getTime());
        byte[] macByte = BitConverter.convertMacAdd(mDevice.getMac());
        LockCmdFpDel lockCmdFpDel = new LockCmdFpDel(macByte, vFrom, vTo);
        lockCmdFpDel.setBatchId(fpBatchId);
        return lockCmdFpDel;
    }


    private void delFpInLock(final JSONArray fpArray, final ILockDataOperator iLockFpDelOperator){
        try {

            JSONObject fpObj = fpArray.getJSONObject(fpDelIndex);
            iLockFpDelOperator.sendLockMsg(getFpDelCmd(Integer.parseInt(fpObj.getString("batchno"))).getBytes(), new LockOperateCallback() {
                @Override
                public void lockOperateSucc(String value) {
                    if(fpDelIndex == (fpArray.length()-1)){//指纹删除完了
                        Log.d(TAG, "fpDelIndex == (fpArray.length()-1)");
                        iLockFpDelOperator.unregisterBluetoothReceiver();
                        delFpInMemory();//删除存储数据
                    }else{
                        fpDelIndex += 1;
                        Log.d(TAG, "fpDelIndex:"+fpDelIndex);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        delFpInLock(fpArray, iLockFpDelOperator);//递归函数
                    }
                }
                @Override
                public void lockOperateFail(String value) {
                    xqProgressDialog.dismiss();
                    if(value.equals("命令不在有效期内(3)")){
                        ZkUtil.showCmdTimeOutView(activity);
                    }else{
                        CommonUtils.toast(activity, value);
                    }
                    iLockFpDelOperator.unregisterBluetoothReceiver();
                }
            });
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * 删除锁内指纹
     */
    private void delMemberFps(final JSONArray faArray){
        fpDelIndex = 0;
        final ILockDataOperator iLockFpDelOperator = OperatorBuilder.create(SecureFpDel.FLAG, activity, mDevice);
        iLockFpDelOperator.registerBluetoothReceiver();
        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            delFpInLock(faArray, iLockFpDelOperator);
        }else{
            XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                @Override
                public void onResponse(int i, Bundle bundle) {
                    Log.d(TAG, "回调状态:"+XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()));
                    if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                        delFpInLock(faArray, iLockFpDelOperator);
                    }else if(i == XmBluetoothManager.Code.REQUEST_NOT_REGISTERED){
                        Toast.makeText(activity, R.string.device_has_been_reset, Toast.LENGTH_LONG).show();
                        xqProgressDialog.dismiss();
                        viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                    } else {
                        viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                        xqProgressDialog.dismiss();
                        CommonUtils.toast(activity, activity.getResources().getString(R.string.connect_time_out));
                    }
                }
            });
        }
    }

    /**
     * 删除家人:id，nickName
     */
    private void delMemberNameInMemory(){
        try {
            JSONArray smArray = new JSONArray();//成员列表
            JSONArray mJsArray = new JSONArray();
            mJsArray.put("keyid_smlist_data");//里面放的是key
            JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);
            if(resultObj.has("keyid_smlist_data") && !resultObj.get("keyid_smlist_data").equals("")){
                smArray = new JSONArray(resultObj.getString("keyid_smlist_data"));//本地存储的都成了string
            }
            for(int i=0; i<smArray.length(); i++){
                if(smArray.getJSONObject(i).getString("id").equals(memberId)){
                    smArray.remove(i);
                    JSONObject settingsObj = new JSONObject();
                    settingsObj.put("keyid_smlist_data", smArray.toString());//存储时，value只能是string
                    dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                        @Override
                        public void dataUpateFail(int i, final String s) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                                    viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
                                    xqProgressDialog.dismiss();
                                    CommonUtils.toast(activity, s);
                                }
                            });
                        }
                        @Override
                        public void dataUpdateSucc(final String s) {
                            Log.d(TAG, "删除成功");
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
                                    viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
                                    Toast.makeText(activity, R.string.gloable_del_succ, Toast.LENGTH_LONG).show();
                                    xqProgressDialog.dismiss();
                                    activity.finish();
                                }
                            });


                        }
                    });
                    break;
                }
            }

        } catch (JSONException e) {
            viewHanlder.removeMessages(MSG_MEMBER_DEL_TIMEOUT);
            viewHanlder.removeMessages(MSG_PWD_DEL_TIMEOUT);
            e.printStackTrace();
        }
    }

    //------------新的存储格式-------------------------------
    Handler viewHanlder = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PWD_DEL_TIMEOUT:
                    Log.d(TAG, "启动超时，执行断开:"+mDeviceStat.mac);
                    xqProgressDialog.dismiss();
                    XmBluetoothManager.getInstance().disconnect(mDeviceStat.mac);
                    CommonUtils.toast(activity, activity.getResources().getString(R.string.connect_time_out));
                    Log.d(TAG, "超时未发现门锁");
                    break;

                case MSG_MEMBER_DEL_TIMEOUT:
                    Log.d(TAG, "启动超时，执行断开:"+mDeviceStat.mac);
                    xqProgressDialog.dismiss();
                    XmBluetoothManager.getInstance().disconnect(mDeviceStat.mac);
                    CommonUtils.toast(activity, activity.getResources().getString(R.string.connect_time_out));
                    break;
                default:
                    break;
            }
        }
    };
}
