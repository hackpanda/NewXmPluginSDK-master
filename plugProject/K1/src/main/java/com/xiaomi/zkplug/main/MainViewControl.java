package com.xiaomi.zkplug.main;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.plugin.core.XmPluginPackage;
import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.smarthome.common.ui.dialog.MLAlertDialog;
import com.xiaomi.smarthome.common.ui.dialog.XQProgressDialog;
import com.xiaomi.smarthome.device.api.Callback;
import com.xiaomi.smarthome.device.api.DeviceStat;
import com.xiaomi.smarthome.device.api.UserInfo;
import com.xiaomi.smarthome.device.api.XmPluginHostApi;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.entity.MyEntity;
import com.xiaomi.zkplug.lockoperater.LockOperateCallback;
import com.xiaomi.zkplug.lockoperater.SecureOpen;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;
import com.xiaomi.zkplug.view.GifView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import cn.zelkova.lockprotocol.BriefDate;

/**
 * 作者：liwenqi on 17/6/21 16:28
 * 邮箱：liwenqi@zelkova.cn
 * 描述：mainView界面控制
 */
public class MainViewControl {
    private final String KEYID_NICKNAME_MASTER= "keyid_master$nickname_data";//管理员服务器keyId
    private final String KEYID_ADD_LOCK_TIME = "keyid_addLockTime_data";//添加门锁的时间
    private final String KEYID_PWD_MASTER = "keyid_master$pwd_data";//管理员密码数据
    private final String KEYID_FP_MASTER = "keyid_master$fp_data";//管理员密码数据
    private final String KEYID_SM_LIST = "keyid_smlist_data";//成员列表
    private final String KEYID_SKEY = "keyid_skey_data";//秘钥
    private final String KEYID_SYNC_TIME = "keyid_syncTime_data";//授时的时间

    private final int CONNECT_FAIL_TIME = 15 * 1000;//开锁5秒时间
    private final int MSG_CONNECT_TIMEOUT = 0;//开锁超时
    private final int BASHOU_ANIMA_STOP_FAIL = 4;//把手停止动画
    private final int BASHOU_ANIMA_STOP_SUCC = 5;//把手停止动画
    private final int BASHOU_TO_NORMAL = 6;//
    private volatile boolean isOperating = false;//正在操作锁
    Activity activity;
    SecureOpen secureOpen;
    GifView openLockGif;//等待三秒动画
    TextView refreshTimeTv, powerTv, openWarnTv, keyPeriodTv;//刷新时间
    ImageView bashouImg, openCircleImg, openLockImg, powerImg;//原始样式
    RelativeLayout longTimeNoSyncTimeRel;//长时间未授时Rel
    TextView longTimeNoSyncTimeTv;//长时间未授时Tv
    private final String TAG = "MainActivityView";
    Device mDevice;
    DeviceStat mDeviceStat;
    DataManageUtil dataManageUtil;//数据管理类
    XmPluginPackage xmPluginPackage;
    XQProgressDialog xqProgressDialog;
    public MainViewControl(Activity activity, DeviceStat mDeviceStat, XmPluginPackage xmPluginPackage) {
        this.mDeviceStat = mDeviceStat;
        this.mDevice = Device.getDevice(mDeviceStat);
        this.dataManageUtil = new DataManageUtil(mDeviceStat, activity);
        this.activity = activity;
        this.xmPluginPackage = xmPluginPackage;
        this.xqProgressDialog = new XQProgressDialog(activity);
        this.bashouImg = (ImageView) activity.findViewById(R.id.bashouImg);
        this.openLockGif = (GifView) activity.findViewById(R.id.openLockGif);
        this.openLockGif.setMovieResource(R.drawable.btn_open_nor);
        this.openLockImg = (ImageView) activity.findViewById(R.id.openLockImg);
        this.openCircleImg = (ImageView) activity.findViewById(R.id.openCircleImg);
        this.refreshTimeTv = (TextView) activity.findViewById(R.id.refreshTimeTv);
        this.longTimeNoSyncTimeRel = (RelativeLayout) activity.findViewById(R.id.longTimeNoSyncTimeRel);
        this.longTimeNoSyncTimeTv = (TextView) activity.findViewById(R.id.longTimeNoSyncTimeTv);
        this.powerTv = (TextView) activity.findViewById(R.id.powerTv);
        this.keyPeriodTv = (TextView) activity.findViewById(R.id.keyPeriodTv);
        this.powerImg = (ImageView) activity.findViewById(R.id.powerImg);
        openWarnTv = (TextView) activity.findViewById(R.id.openWarnTv);
        if(secureOpen == null){
            secureOpen = new SecureOpen(Device.getDevice(mDeviceStat));
        }
        //doScan();
    }

    /**
     * 展示门锁连接界面和动画
     */
    protected void showConnectingView(){
        if(!ZkUtil.isBleOpen()){
            openWarnTv.setText(R.string.bluetooth_not_open);
            return;
        }
        viewHanlder.sendEmptyMessageDelayed(MSG_CONNECT_TIMEOUT, CONNECT_FAIL_TIME);
        //每次点击置位
        isOperating = true;
        //开锁逻辑
        ZkUtil.startAnima(activity, openCircleImg);
        openWarnTv.setText(activity.getResources().getString(R.string.main_connecting));
        openWarnTv.setTextColor(activity.getResources().getColor(R.color.colorPrimary));
    }
    /**
     * 展示门锁成功界面
     */
    protected void showConnecSuccView(){
        Log.d(TAG , "----showConnecSuccView--"+isOperating);
        isOperating = false;
        openWarnTv.setText(activity.getResources().getString(R.string.main_click_to_open));
        ZkUtil.stopAnima(openCircleImg);
        viewHanlder.removeMessages(MSG_CONNECT_TIMEOUT);
    }
    /**
     * 展示门锁连接失败界面
     */
    protected void showConnecFailView(){
        Log.d(TAG , "----showConnecFailView--"+isOperating);
        if(isOperating == false){
            openWarnTv.setText(R.string.main_connect_fail);
        }
        isOperating = false;
        if(!ZkUtil.isBleOpen()){
            openWarnTv.setText(R.string.bluetooth_not_open);
        }
    }
    /**
     * 开锁逻辑
     */
    public void openLock(){
        Log.d(TAG , "----onclick--"+isOperating);
        if(isOperating){
            return;
        }
        if(!ZkUtil.isBleOpen()){
            openWarnTv.setText(R.string.bluetooth_not_open);
            openWarnTv.setTextColor(activity.getResources().getColor(R.color.colorPrimary));
            Toast.makeText(activity, activity.getResources().getString(R.string.open_bluetooth), Toast.LENGTH_LONG).show();
            return;
        }

        isOperating = true;//点击开锁时，也需再次屏蔽点击事件
        if (mDevice.isOwner()) {//主人
            openMasterLock();
        } else {
            openSharerLock();
        }
    }
    /**
     * 门锁连接逻辑
     * 被分享者和管理员的连接接口是不一样的
     */
    protected void conneMasterctLock(){
        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            showConnecSuccView();
        }else{
            showConnectingView();//动画和界面变化
            XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                @Override
                public void onResponse(int i, Bundle bundle) {
                    Log.d(TAG, "connect return: "+i);
                    if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                        showConnecSuccView();
                    }else if(i == XmBluetoothManager.Code.REQUEST_NOT_REGISTERED){
                        displayFailView(activity.getResources().getString(R.string.device_has_been_reset));
                    }else{
                        doScan();
                    }
                }
            });
        }
    }
    /**
     * 被分享者连接门锁
     * 被分享者和管理员的连接接口是不一样的
     */
    protected void connectSharedLock(){
        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            showConnecSuccView();
        }else{
            showConnectingView();//动画和界面变化
            XmBluetoothManager.getInstance().securityChipSharedDeviceConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                @Override
                public void onResponse(int i, Bundle bundle) {
                    isOperating = true;//有回调
                    if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                        showConnecSuccView();
                    }else if(i == XmBluetoothManager.Code.REQUEST_SHARED_KEY_EXPIRED){
                        displayFailView(activity.getResources().getString(R.string.main_key_expired));
                    }else if(i == XmBluetoothManager.Code.REQUEST_NOT_REGISTERED){
                        displayFailView(activity.getResources().getString(R.string.device_has_been_reset));

                    } else {
                        displayFailView(activity.getResources().getString(R.string.connect_time_out));//连接失败
                    }
                }
            });
        }
    }

    /**
     * 被分享者开门
     */
    private void openSharerLock(){
        //被分享者
        boolean isSecurityChipSharedKeyValid = XmBluetoothManager.getInstance().isSecurityChipSharedKeyValid(mDevice.getMac());
        if (!isSecurityChipSharedKeyValid) {
            Toast.makeText(activity, R.string.main_no_shared_key, Toast.LENGTH_SHORT).show();
            showNoShareKeyView();
        } else {
            if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
                sendOpenMsg();
            }else{
                connectSharedLock();
            }
        }
    }

    /**
     * 管理员开门
     */
    public void openMasterLock(){
        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            sendOpenMsg();
        }else{
            conneMasterctLock();
        }
    }

    private void sendOpenMsg(){
        ZkUtil.startAnima(activity, openCircleImg);
        openWarnTv.setText(R.string.main_opening);
        openWarnTv.setTextColor(activity.getResources().getColor(R.color.colorPrimary));
        secureOpen.sendLockMsg(null,new LockOperateCallback() {
            @Override
            public void lockOperateSucc(final String value) {
                Log.d(TAG, value);
                Log.d(TAG, "open complete: "+BriefDate.fromNature(new Date()).toString());
                if(value.equals("7f")){
                    openWarnTv.setTextColor(activity.getResources().getColor(R.color.two_factor_authen));
                    displaySuccView(activity.getResources().getString(R.string.main_two_factor_auth));
                }else if(value.equals("02")){
                    displayFailView(activity.getResources().getString(R.string.main_door_fansuo));
                }else{
                    displaySuccView(activity.getResources().getString(R.string.main_door_opened));
                }
            }
            @Override
            public void lockOperateFail(final String value) {
                displayFailView(activity.getResources().getString(R.string.main_open_fail, value));
            }
        });
    }

    /**
     * 添加管理员昵称
     *
     * 还有一下服务器其他数据
     */
    protected void checkMasterNickName(){
        if(!ZkUtil.isNetworkAvailable(activity)) return;
        final String accountId = XmPluginHostApi.instance().getAccountId();//accountId就是userId，也即小米帐号
        XmPluginHostApi.instance().getUserInfo(accountId, new Callback<UserInfo>() {
            @Override
            public void onSuccess(final UserInfo userInfo) {
                //查询是否已设置管理员
                JSONArray mJsArray = new JSONArray();
                mJsArray.put(KEYID_NICKNAME_MASTER);
                mJsArray.put(KEYID_ADD_LOCK_TIME);
                mJsArray.put(KEYID_SYNC_TIME);//授时的日期
                Log.d(TAG, "accountId: "+accountId+", userInfo: "+userInfo.nickName);
                dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
                    @Override
                    public void dataUpateFail(int i, String s) {
                    }
                    @Override
                    public void dataUpdateSucc(String s) {
                        try{
                            JSONObject settingsObj = new JSONObject();
                            JSONObject resultObj = new JSONObject(s);

                            if(!resultObj.has(KEYID_NICKNAME_MASTER)){//第一次添加锁
                                settingsObj.put(KEYID_NICKNAME_MASTER, userInfo.nickName.equals("") ? accountId : userInfo.nickName);
                                settingsObj.put(KEYID_ADD_LOCK_TIME, BriefDate.fromNature(new Date()).toString());

                                dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                                    @Override
                                    public void dataUpateFail(int i, String s) {
                                        Log.d(TAG, "add master fail");
                                    }
                                    @Override
                                    public void dataUpdateSucc(String s) {
                                        Log.d(TAG, "add master success");
                                    }
                                });
                                JSONObject localObj = new JSONObject();
                                localObj.put(KEYID_PWD_MASTER, "");//清空本地数据
                                localObj.put(KEYID_FP_MASTER, "");//清空本地数据
                                localObj.put(KEYID_SM_LIST, "");//清空本地数据
                                localObj.put(KEYID_SKEY, "");//清空本地数据
                                dataManageUtil.saveDataToLocal(localObj);//清空本地数据
                            }else{//进来更新数据
                                settingsObj.put(KEYID_ADD_LOCK_TIME, resultObj.getString(KEYID_ADD_LOCK_TIME));
                                dataManageUtil.saveDataToLocal(settingsObj);
                            }
                            /* 提醒授时 begin */
                            if(resultObj.has(KEYID_SYNC_TIME) && !TextUtils.isEmpty(KEYID_SYNC_TIME)){
                                checkToShowSyncTimeTipsView(resultObj.getString(KEYID_SYNC_TIME));
                            }else{
                                checkToShowSyncTimeTipsView(null);
                            }
                            /* 提醒授时 end */
                        }catch (JSONException je){
                            je.printStackTrace();
                        }
                    }
                });
            }
            @Override
            public void onFailure(int i, String s) {
                Log.d(TAG, "get account info fail");
            }
        });
    }

    /**
     * 检查是否需要提醒同步时间布局
     * @param syncTimeStr: 上次授时的时间
     */
    private void checkToShowSyncTimeTipsView(String syncTimeStr){
        Log.d(TAG, "syncTimeStr: " + syncTimeStr);
        if(syncTimeStr == null){//未同步过时间
            longTimeNoSyncTimeTv.setText(R.string.never_synced_time);
            longTimeNoSyncTimeRel.setVisibility(View.VISIBLE);
        }else{
            SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date syncTime = sdFormat.parse(syncTimeStr);//上次授时的日期,也相当于上次授时时间
                Date nowTime = new Date();
                int dayInterval = (int) ((nowTime.getTime()-syncTime.getTime())/(60*60*1000*24));

                Log.d(TAG, "dayInterval: " + dayInterval);
                if(dayInterval > 30 && dayInterval <= 60){
                    longTimeNoSyncTimeTv.setText(R.string.no_sync_time_30);
                    longTimeNoSyncTimeRel.setVisibility(View.VISIBLE);
                }else if(dayInterval > 60){
                    longTimeNoSyncTimeTv.setText(R.string.no_sync_time_60);
                    longTimeNoSyncTimeRel.setVisibility(View.VISIBLE);
                } else{
                    longTimeNoSyncTimeRel.setVisibility(View.GONE);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 开锁失败界面
     */
    public void displayFailView(String errMsg){
        ZkUtil.stopAnima(openCircleImg);
        viewHanlder.removeMessages(MSG_CONNECT_TIMEOUT);
        openWarnTv.setText(errMsg);
        openWarnTv.setTextColor(activity.getResources().getColor(R.color.warn_color));
        openLockImg.setImageResource(R.drawable.icon_mensuoweikai);
        bashouImg.setImageResource(R.drawable.open_fail_animation);
        bashouImg.setVisibility(View.VISIBLE);
        AnimationDrawable animationDrawable = (AnimationDrawable) bashouImg.getDrawable();
        animationDrawable.start();
        viewHanlder.sendEmptyMessageDelayed(BASHOU_ANIMA_STOP_FAIL, 2100);
    }

    /**
     * 开锁成功界面
     */
    public void displaySuccView(String succMsg){
        ZkUtil.stopAnima(openCircleImg);
        viewHanlder.removeMessages(MSG_CONNECT_TIMEOUT);
        openLockImg.setImageResource(R.drawable.icon_mensuoyikai);
        bashouImg.setImageResource(R.drawable.bashou_succ_7);
        bashouImg.setVisibility(View.VISIBLE);
        //Rotate动画可以设置加速模式，恢复模式，停止位置
        final Animation rotate = AnimationUtils.loadAnimation(activity, R.anim.rotate_bashou_down_anim);
        bashouImg.setAnimation(rotate);
        viewHanlder.sendEmptyMessageDelayed(BASHOU_ANIMA_STOP_SUCC, 2100);
        openWarnTv.setText(succMsg);
        openWarnTv.setTextColor(activity.getResources().getColor(R.color.text_succ));
    }

    /**
     * 时序: 用于界面控制
     */
    Handler viewHanlder = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECT_TIMEOUT:
                    isOperating = false;
                    openWarnTv.setText(R.string.main_connect_fail);
                    ZkUtil.stopAnima(openCircleImg);
                    break;
                case BASHOU_ANIMA_STOP_FAIL:
                    bashouImg.setImageResource(R.drawable.bashou_open_fail);
                    bashouImg.setVisibility(View.VISIBLE);
                    viewHanlder.sendEmptyMessageDelayed(BASHOU_TO_NORMAL, 1000);
                    break;
                case BASHOU_ANIMA_STOP_SUCC:
                    bashouImg.clearAnimation();
                    bashouImg.setImageResource(R.drawable.bashou_open_succ);
                    bashouImg.setVisibility(View.VISIBLE);
                    viewHanlder.sendEmptyMessageDelayed(BASHOU_TO_NORMAL, 6000);
                    break;
                case BASHOU_TO_NORMAL:
                    bashouImg.setImageResource(R.drawable.bashou);
                    openLockImg.setImageResource(R.drawable.open_lock_selector);
                    bashouImg.setVisibility(View.GONE);
                    openWarnTv.setText(R.string.main_click_to_open);
                    openWarnTv.setTextColor(activity.getResources().getColor(R.color.colorPrimary));
                    isOperating = false;
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 刷新电量
     */
    private void refreshPower(final boolean backgroundRun){
        XmPluginHostApi.instance().getUserDeviceData(MyEntity.MODEL_ID, mDeviceStat.did, MyEntity.PROP_TYPE, MyEntity.PROP_POWER_KEY, 0, System.currentTimeMillis()/1000, new Callback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray jsonArray) {
                xqProgressDialog.dismiss();
                if(jsonArray.length() == 0) return;
                Log.d(TAG, "prop power :"+jsonArray.toString());
                ImageView powerImg = (ImageView) activity.findViewById(R.id.powerImg);
                TextView powerTv = (TextView) activity.findViewById(R.id.powerTv);
                powerImg.setVisibility(View.VISIBLE);
                powerTv.setVisibility(View.VISIBLE);
                int powerLevel = ZkUtil.getLockPower(jsonArray);
                if(powerLevel <= 10){
                    powerImg.setImageResource(R.drawable.icon_dianliangdi);
                    powerTv.setTextColor(activity.getResources().getColor(R.color.warn_color));
                }else{
                    powerImg.setImageResource(R.drawable.icon_dianlianggao);
                    powerTv.setTextColor(activity.getResources().getColor(R.color.black));
                }
                if(powerLevel > 80){
                    powerTv.setText(">80%");
                }else{
                    powerTv.setText(powerLevel+"%");
                }
            }

            @Override
            public void onFailure(int i, String s) {
                xqProgressDialog.dismiss();
                Log.d(TAG, "getUserDeviceData:"+i);
                if(!backgroundRun) Toast.makeText(activity, R.string.main_check_gateway, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 解析锁状态事件
     *
     * @param statusJsArray
     * @return
     * @throws JSONException
     */
    public JSONArray transformLockStatus(JSONArray statusJsArray) throws JSONException{

        for(int i=0; i<statusJsArray.length(); i++){
            JSONObject statusJsObj = statusJsArray.getJSONObject(i);

            String value = statusJsObj.getString("value").replace("[\"", "").replace("\"]", "");
            String uid = statusJsObj.getString("uid");
            String time = statusJsObj.getString("time");
            String did = statusJsObj.getString("did");
            Log.d("解析", "opTime:"+ZkUtil.getDateBySnd(Long.parseLong(time)));
            statusJsObj.put("opTime", ZkUtil.getDateBySnd(Long.parseLong(time)));//操作时间
            Log.d("解析 ", "status: "+ getStatus(value));
            statusJsObj.put("status", getStatus(value));//门锁状态
        }

        return statusJsArray;
    }
    /**
     * action 所状态
     * 0xff 异常
     * 0x00 开锁状态
     * 0x01 上锁状态
     * 0x02 反锁状态
     * @return
     */
    private String getStatus(String value){
        if(value.equals("00")) return activity.getResources().getString(R.string.main_status_changkai);
        if(value.equals("01")) return activity.getResources().getString(R.string.main_close_weishangti);
        if(value.equals("02")) return activity.getResources().getString(R.string.main_close_yishangti);

        return "开锁状态";
    }

    /**
     * 刷新状态
     */
    private void refreshStatus(final boolean backgroundRun){
        XmPluginHostApi.instance().getUserDeviceData(MyEntity.MODEL_ID, mDeviceStat.did, MyEntity.PROP_TYPE, MyEntity.PROP_STATUS_KEY, 0, System.currentTimeMillis()/1000, new Callback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray jsonArray) {
                xqProgressDialog.dismiss();
                try {
                    if(jsonArray.length() == 0){
                        if(!backgroundRun) Toast.makeText(activity, R.string.main_check_gateway, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    JSONArray statusJsArray = transformLockStatus(jsonArray);
                    Log.d(TAG, "statusJsArray: "+statusJsArray.toString());
                    try {
                        JSONObject statusObj = statusJsArray.getJSONObject(0);//第一个是最新的
                        Log.d(TAG, "statusObj: "+statusObj.toString());
                        ImageView lockStatusImg = (ImageView) activity.findViewById(R.id.lockStatusImg);
                        TextView lockStatusTv = (TextView) activity.findViewById(R.id.lockStatusTv);
                        lockStatusImg.setVisibility(View.VISIBLE);
                        lockStatusTv.setText(statusObj.getString("status"));
                        if(statusObj.getString("status").equals(activity.getResources().getString(R.string.main_close_yishangti)) || statusObj.getString("status").equals(activity.getResources().getString(R.string.main_close_weishangti))){
                            lockStatusImg.setImageResource(R.drawable.icon_yiguan);
                        }
                        if(statusObj.getString("status").equals(activity.getResources().getString(R.string.main_status_changkai))){
                            lockStatusImg.setImageResource(R.drawable.icon_changkai);
                        }

                        TextView refreshTimeTv = (TextView) activity.findViewById(R.id.refreshTimeTv);
                        refreshTimeTv.setText(activity.getResources().getString(R.string.main_update_time, statusObj.getString("opTime")));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "prop status:"+jsonArray.toString());
            }

            @Override
            public void onFailure(int i, String s) {
                xqProgressDialog.dismiss();
                Log.d(TAG, s);
                ImageView lockStatusImg = (ImageView) activity.findViewById(R.id.lockStatusImg);
                TextView lockStatusTv = (TextView) activity.findViewById(R.id.lockStatusTv);
                lockStatusImg.setVisibility(View.GONE);
                lockStatusTv.setText("");
                TextView refreshTimeTv = (TextView) activity.findViewById(R.id.refreshTimeTv);
                refreshTimeTv.setText("");
                Toast.makeText(activity, s, Toast.LENGTH_SHORT).show();
            }
        });
    }
    /**
     * 刷新门锁状态
     * @param backgroundRun :是否是后台刷新
     */
    public void refreshLockStatus(final boolean backgroundRun){
        if(!ZkUtil.isNetworkAvailable(activity)){
            if(!backgroundRun) Toast.makeText(activity, R.string.main_check_gateway, Toast.LENGTH_LONG).show();
            return;
        }

        if(!backgroundRun){
            xqProgressDialog.setMessage(activity.getResources().getString(R.string.main_refreshing));
            xqProgressDialog.setCancelable(false);
            xqProgressDialog.show();
        }
        //电量
        refreshPower(backgroundRun);
        //状态
        refreshStatus(backgroundRun);
    }

    /**
     * 被取消授权界面
     */
    protected void showNoShareKeyView(){
        openLockImg.setImageResource(R.drawable.btn_open_bukedianji);
        openLockImg.setOnClickListener(null);
        openWarnTv.setVisibility(View.VISIBLE);
        openWarnTv.setText("");
        keyPeriodTv.setVisibility(View.GONE);
        final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity);

        builder.setTitle(R.string.device_info_tip);
        builder.setMessage(activity.getString(R.string.main_key_ivalid));

        builder.setCancelable(false);
        builder.setNegativeButton(R.string.gloable_confirm, new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                activity.finish();
            }
        });
        builder.show();
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(result.getDevice().getAddress().equals(mDeviceStat.mac)){
                Log.d(TAG, "连接前扫描到："+result.getDevice().getAddress());
                //scanner.stopScan(mScanCallback);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "onBatchScanResults");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "onScanFailed");
        }
    };

   public void doScan() {
       if(ZkUtil.isBleOpen()){
           Log.d(TAG, "连接前启动扫描:"+mDeviceStat.isOnline);
           BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
           BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
           ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
           scanner.startScan(/*filters*/ null, settings, mScanCallback);

//           XmBluetoothManager.getInstance().startScan(8000, XmBluetoothManager.SCAN_BLE, new XmBluetoothManager.BluetoothSearchResponse() {
//               @Override
//               public void onSearchStarted() {
//
//               }
//
//               @Override
//               public void onDeviceFounded(XmBluetoothDevice xmBluetoothDevice) {
//
//               }
//
//               @Override
//               public void onSearchStopped() {
//
//               }
//
//               @Override
//               public void onSearchCanceled() {
//
//               }
//           });
       }
    }

    protected void setOperating(boolean flag){
       isOperating = flag;
    }

}
