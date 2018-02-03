package com.xiaomi.zkplug.otp;

import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.plugin.core.XmPluginPackage;
import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.smarthome.common.ui.dialog.XQProgressDialog;
import com.xiaomi.zkplug.CommonUtils;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.entity.MyEntity;
import com.xiaomi.zkplug.lockoperater.ILockDataOperator;
import com.xiaomi.zkplug.lockoperater.LockOperateCallback;
import com.xiaomi.zkplug.lockoperater.OperatorBuilder;
import com.xiaomi.zkplug.lockoperater.SecureExchangeKey;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;
import com.xiaomi.zkplug.view.AutoScrollTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import cn.zelkova.lockprotocol.AESService;
import cn.zelkova.lockprotocol.BitConverter;
import cn.zelkova.lockprotocol.BriefDate;

import static cn.zelkova.lockprotocol.BriefDate.fromNature;

/**
 * 作者：liwenqi on 17/9/11 17:10
 * 邮箱：liwenqi@zelkova.cn
 * 描述：OTP 之 M
 */

public class OtpManager {
    boolean isUsedOut;//临时密码用完了
    private final static String TAG = "ZOTP";
    private AutoScrollTextView mAstv_0, mAstv_1, mAstv_2, mAstv_3, mAstv_4, mAstv_5;
    private final int MSG_CACULATE_ZOTP = 0;//开始计算
    private final int MSG_OTP_KAITONG_TIMEOUT = 0x11;//开通超时
    private final int MSG_OTP_CLOSE_TIMEOUT = 0x12;//关闭超时
    Activity activity;
    ILockDataOperator iLockDataOperator;
    XQProgressDialog xqProgressDialog;
    XmPluginPackage xmPluginPackage;
    LinearLayout emptyView;
    ImageView checkImg;
    Device mDevice;
    String skey = "";//本地存储的密钥
    DataManageUtil dataManageUtil;
    JSONObject otpIdxsObj;//zotp数据存储
    TextView invalidTime;
    public OtpManager(Activity activity, Device mDevice, XmPluginPackage xmPluginPackage) {
        this.activity = activity;
        this.mDevice = mDevice;
        isUsedOut = false;//临时密码用完了
        invalidTime = (TextView) activity.findViewById(R.id.invalidTime);
        this.mAstv_0 = (AutoScrollTextView) activity.findViewById(R.id.scrollText_0);
        this.mAstv_1 = (AutoScrollTextView) activity.findViewById(R.id.scrollText_1);
        this.mAstv_2 = (AutoScrollTextView) activity.findViewById(R.id.scrollText_2);
        this.mAstv_3 = (AutoScrollTextView) activity.findViewById(R.id.scrollText_3);
        this.mAstv_4 = (AutoScrollTextView) activity.findViewById(R.id.scrollText_4);
        this.mAstv_5 = (AutoScrollTextView) activity.findViewById(R.id.scrollText_5);
        this.otpIdxsObj = new JSONObject();
        this.emptyView = (LinearLayout) activity.findViewById(R.id.emptyView);
        this.checkImg = (ImageView) activity.findViewById(R.id.checkImg);
        this.xmPluginPackage = xmPluginPackage;
        this.dataManageUtil = new DataManageUtil(mDevice.deviceStat(), activity);
        this.iLockDataOperator = OperatorBuilder.create(SecureExchangeKey.FLAG, activity, mDevice);
    }

    /**
     * 对应不同的布局
     */
    private void showOtpView(String mSkey){
        this.skey = mSkey;
        Log.d(TAG, "skey: "+skey);
        emptyView.setVisibility(View.GONE);
        if(TextUtils.isEmpty(skey)){//无密钥, 则需要显示开通界面
            activity.findViewById(R.id.title_bar_share).setVisibility(View.GONE);
            RelativeLayout otpKaitongView = (RelativeLayout) activity.findViewById(R.id.otpKaitongView);
            otpKaitongView.setVisibility(View.VISIBLE);
            LinearLayout optRefreshView = (LinearLayout) activity.findViewById(R.id.optRefreshView);
            optRefreshView.setVisibility(View.GONE);
            mAstv_0.setNumber(0);
            mAstv_1.setNumber(0);
            mAstv_2.setNumber(0);
            mAstv_3.setNumber(0);
            mAstv_4.setNumber(0);
            mAstv_5.setNumber(0);
            mAstv_0.setTargetNumber(0);
            mAstv_1.setTargetNumber(0);
            mAstv_2.setTargetNumber(0);
            mAstv_3.setTargetNumber(0);
            mAstv_4.setTargetNumber(0);
            mAstv_5.setTargetNumber(0);
        }else{
            //显示关闭功能的三个点
            activity.findViewById(R.id.title_bar_share).setVisibility(View.VISIBLE);
            RelativeLayout otpKaitongView = (RelativeLayout) activity.findViewById(R.id.otpKaitongView);
            otpKaitongView.setVisibility(View.GONE);
            LinearLayout optRefreshView = (LinearLayout) activity.findViewById(R.id.optRefreshView);
            optRefreshView.setVisibility(View.VISIBLE);
            activity.findViewById(R.id.usedOutContent).setVisibility(View.GONE);
            activity.findViewById(R.id.content).setVisibility(View.VISIBLE);
        }
    }


    /**
     * 初始化布局
     */
    protected void initView(){
        ZkUtil.startAnima(activity, checkImg);//旋转
        JSONArray mJsArray = new JSONArray();
        mJsArray.put("keyid_skey_data");
        //查询skey
        if(ZkUtil.isNetworkAvailable(activity)){
            dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
                @Override
                public void dataUpateFail(int i, String s) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, "密钥数据查询失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override
                public void dataUpdateSucc(String s) {
                    try{
                        JSONObject resultObj = new JSONObject(s);
                        if(resultObj.has("keyid_skey_data")) skey = resultObj.getString("keyid_skey_data");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showOtpView(skey);
                            }
                        });

                    }catch (JSONException je){
                        je.printStackTrace();
                    }
                }
            });
        }else{
            try{
                JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);
                skey = resultObj.getString("keyid_skey_data");
                showOtpView(skey);
            }catch (JSONException je){
                je.printStackTrace();
                Toast.makeText(activity, "密钥数据查询失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void skeyExchange(final boolean isKaiTong){
        //交换密钥
        iLockDataOperator.sendLockMsg(null, new LockOperateCallback() {
            @Override
            public void lockOperateSucc(String value) {
                iLockDataOperator.unregisterBluetoothReceiver();
                Log.d(TAG, "交换密钥成功:"+value);
                JSONObject settingsObj = new JSONObject();
                try {
                    if(isKaiTong){
                        skey = value;//重新赋值
                    }else{
                        skey = "";//重新赋值
                        JSONObject emptyOtpIdxsObj = new JSONObject();//空字典赋值
                        settingsObj.put("keyid_otpidxs_data", emptyOtpIdxsObj.toString());
                    }
                    settingsObj.put("keyid_skey_data", skey);
                    dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                        @Override
                        public void dataUpateFail(int i, String s) {
                            Log.d(TAG, "存储密钥失败");
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(isKaiTong){
                                        viewHandler.removeMessages(MSG_OTP_KAITONG_TIMEOUT);
                                    }else{
                                        viewHandler.removeMessages(MSG_OTP_CLOSE_TIMEOUT);
                                    }
                                    Toast.makeText(activity, "存储密钥失败", Toast.LENGTH_SHORT).show();
                                    xqProgressDialog.dismiss();
                                }
                            });
                        }
                        @Override
                        public void dataUpdateSucc(String s) {
                            Log.d(TAG, "存储密钥成功: "+s);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(isKaiTong){
                                        viewHandler.removeMessages(MSG_OTP_KAITONG_TIMEOUT);
                                    }else{
                                        viewHandler.removeMessages(MSG_OTP_CLOSE_TIMEOUT);
                                    }
                                    xqProgressDialog.dismiss();
                                    if(isKaiTong){
                                        showOtpView(skey);
                                    }else{
                                        activity.finish();
                                    }
                                }
                            });
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    if(isKaiTong){
                        viewHandler.removeMessages(MSG_OTP_KAITONG_TIMEOUT);
                    }else{
                        viewHandler.removeMessages(MSG_OTP_CLOSE_TIMEOUT);
                    }
                    xqProgressDialog.dismiss();
                    CommonUtils.toast(activity, "数据格式异常");
                }
            }
            @Override
            public void lockOperateFail(String value) {
                if(isKaiTong){
                    viewHandler.removeMessages(MSG_OTP_KAITONG_TIMEOUT);
                }else{
                    viewHandler.removeMessages(MSG_OTP_CLOSE_TIMEOUT);
                }
                xqProgressDialog.dismiss();
                if(value.equals("命令不在有效期内(3)")){
                    ZkUtil.showCmdTimeOutView(activity);
                }else{
                    CommonUtils.toast(activity, value);
                }
            }
        });
    }
    /**
     * 交换密钥
     * isKaiTong : true:开通， false：关闭
     */
    protected void doSsKeyExchange(final boolean isKaiTong){
        iLockDataOperator.registerBluetoothReceiver();
        /*交换密钥*/
        if(!isKaiTong){
            viewHandler.sendEmptyMessageDelayed(MSG_OTP_CLOSE_TIMEOUT, MyEntity.OPERATE_TIMEOUT);
            xqProgressDialog = new XQProgressDialog(activity);
            xqProgressDialog.setMessage("正在关闭");
            xqProgressDialog.setCancelable(false);
            xqProgressDialog.show();
        }
        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            skeyExchange(isKaiTong);
        }else{
            XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                @Override
                public void onResponse(int i, Bundle bundle) {
                    Log.d(TAG, "回调状态:"+XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()));
                    if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                        skeyExchange(isKaiTong);
                    }else if(i == XmBluetoothManager.Code.REQUEST_NOT_REGISTERED){
                        if(isKaiTong){
                            viewHandler.removeMessages(MSG_OTP_KAITONG_TIMEOUT);
                        }else{
                            viewHandler.removeMessages(MSG_OTP_CLOSE_TIMEOUT);
                        }
                        Toast.makeText(activity, "设备已被重置，请解除绑定后重新添加", Toast.LENGTH_LONG).show();
                        xqProgressDialog.dismiss();
                    }
                }
            });
        }
    }
    /**
     * 开通临时密码时需要检查密钥是否存在
     */
    protected void checkSkeyIsExist(){
        isUsedOut = false;
        viewHandler.sendEmptyMessageDelayed(MSG_OTP_KAITONG_TIMEOUT, MyEntity.OPERATE_TIMEOUT);
        xqProgressDialog = new XQProgressDialog(activity);
        xqProgressDialog.setMessage("正在开通");
        xqProgressDialog.setCancelable(false);
        xqProgressDialog.show();
        JSONArray mJsArray = new JSONArray();
        mJsArray.put("keyid_skey_data");
        dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
            @Override
            public void dataUpateFail(int i, String s) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        viewHandler.removeMessages(MSG_OTP_KAITONG_TIMEOUT);
                        Toast.makeText(activity, "密钥数据查询失败", Toast.LENGTH_SHORT).show();
                        xqProgressDialog.dismiss();
                    }
                });
            }
            @Override
            public void dataUpdateSucc(String s) {
                try{
                    JSONObject resultObj = new JSONObject(s);
                    if(!resultObj.has("keyid_skey_data") || TextUtils.isEmpty(resultObj.getString("keyid_skey_data"))){
                        //没有密钥,运行开通
                        Log.d(TAG, "没有密钥");
                        doSsKeyExchange(true);
                    }else{//已经开通
                        viewHandler.removeMessages(MSG_OTP_KAITONG_TIMEOUT);
                        xqProgressDialog.dismiss();
                        skey = resultObj.getString("keyid_skey_data");
                        showOtpView(skey);
                    }

                }catch (JSONException je){
                    viewHandler.removeMessages(MSG_OTP_KAITONG_TIMEOUT);
                    xqProgressDialog.dismiss();
                    je.printStackTrace();
                }
            }
        });
    }
    /**idx合并去重并生成一个新的序号，放在数组末尾
     * @param idxsArrayServer
     * @param idxsArrayLocal
     * @return
     */
    private JSONArray combinJsonArray(JSONArray idxsArrayServer, JSONArray idxsArrayLocal){
        JSONArray resultArray = new JSONArray();
        //idx合并去重
        Set<Integer> set=new HashSet();
        if(idxsArrayLocal.length() > 0) {
            for (int i = 0; i < idxsArrayServer.length(); i++) {
                try {
                    set.add(idxsArrayServer.getInt(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if(idxsArrayLocal.length() > 0){
            for(int i=0; i<idxsArrayLocal.length();i++){
                try {
                    set.add(idxsArrayLocal.getInt(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        List<Integer> idxTotal = new ArrayList<>();
        for(int m=0; m<100; m++){
            idxTotal.add(m);
        }
        List<Integer> idxUsedList = new ArrayList<>();//已经使用过的
        for (Integer idxUsed : set) {
            idxUsedList.add(idxUsed);
        }
        idxTotal.removeAll(idxUsedList);//得到了未用过的list
        if(idxTotal.size() == 0){//用完了
            Log.d(TAG, "id已经用完了");
            return resultArray;//为空
        }else{
            Random random = new Random();
            int otpIndex = idxTotal.get(random.nextInt(idxTotal.size()));
            idxUsedList.add(otpIndex);
            for(int n=0; n<idxUsedList.size(); n++){
                resultArray.put(idxUsedList.get(n));
            }
        }

        return resultArray;
    }

    /**
     * 得到格式化zotp时间
     * @return
     */
    private BriefDate getNowZotpDate(){
        /*周期为30分钟*/
        Calendar calendar = Calendar.getInstance();
        int minute = calendar.get(Calendar.MINUTE);
        Log.d(TAG, minute+"  "+(minute / 30) * 30);
        calendar.set(Calendar.MINUTE, (minute / 30) * 30);
        calendar.set(Calendar.SECOND, 0);
        Log.d(TAG, calendar.getTime()+"minute"+calendar.get(Calendar.MINUTE)+"second"+calendar.get(Calendar.SECOND));
        BriefDate nowZotpDate = fromNature(calendar.getTime());
        return nowZotpDate;
    }
    /**
     * 计算zotp密码
     *
     * @return
     */
    @SuppressWarnings("WrongConstant")
    public void calculateZotp(){
        Map<String, String> result = new HashMap<>();
        try{

            String starttime = otpIdxsObj.getString("starttime");
            JSONArray otpIndexArray = otpIdxsObj.getJSONArray("idxs");
            int num = otpIndexArray.getInt(otpIndexArray.length()-1);

            BriefDate nowZotpDate = BriefDate.fromNature(ZkUtil.getDateByStr(starttime));
            Log.d(TAG, "nowZotpDate: "+nowZotpDate.toString());
            long dateDiffLong = (nowZotpDate.toProtocol() / 1800);
            byte[] macByte8 = BitConverter.convertMacAdd(mDevice.getMac());//
            byte[] numByte = BitConverter.getBytesBig((short) num);//xx
            byte[] macByte = new byte[6];
            System.arraycopy(macByte8, 0, macByte, 0, 6);
            byte[] zeroByte = new byte[]{0x00,0x00,0x00,0x00};
            String str = (BitConverter.toHexString(BitConverter.getBytesBig((int) dateDiffLong), "")+ BitConverter.toHexString(macByte)+ BitConverter.toHexString(numByte)+ BitConverter.toHexString(zeroByte)).replace(",","");
            byte[] ssKey = BitConverter.fromHexString(skey);//xxxx
            Log.d(TAG, "拼接得字符串: "+str+", 密钥:"+skey);
            Log.d(TAG, "starttime: "+starttime+", 序号: "+num);
            byte[] dddByte = AESService.encrypt(ssKey, BitConverter.fromHexString(str));
            Log.d(TAG, "字符串加密后："+BitConverter.toHexString(dddByte));
            byte[] subDDDByte = new byte[12];
            System.arraycopy(dddByte, 0, subDDDByte, 0, 12);
            String pwd = String.valueOf(num);//序号
            for(int i = 0; i < subDDDByte.length; i = i+2){
                byte p1 = (byte) (subDDDByte[i] ^ subDDDByte[i+1]);
                pwd +=(p1&0xff)%10;
            }
            Log.d(TAG, "密码："+pwd);
            result.put("pwd", pwd.length() > 7 ? pwd.substring(2, pwd.length()) : pwd.substring(1, pwd.length()));
            result.put("starttime", nowZotpDate.toString());//下次刷新时间
        }catch (Exception e){
            e.printStackTrace();
        }
        Log.d(TAG, result.toString());
        refreshOtp(result);//页面刷新
    }
    /**
     * 先计算密码序号
     * 再计算pwd
     * @return
     */

    public void initTheZotp(){
        try{

            if(isUsedOut) return;
            JSONArray idxsArrayLocal = new JSONArray();//索引数组
            otpIdxsObj = new JSONObject();
            JSONArray mJsArray = new JSONArray();
            mJsArray.put("keyid_otpidxs_data");
            mJsArray.put("keyid_skey_data");
            BriefDate nowZotpDate = getNowZotpDate();
            final String starttimeNow = nowZotpDate.toString();//starttime字符串
            Log.d(TAG, "starttimeNow: "+starttimeNow);
            JSONObject resultLocalObj = dataManageUtil.getDataFromLocal(mJsArray);
            final String skeyLocal = resultLocalObj.getString("keyid_skey_data");
            Log.d(TAG, "skeyLocal: "+skeyLocal);
            Log.d(TAG, "resultLocalObj: "+resultLocalObj.toString());
            if(resultLocalObj.has("keyid_otpidxs_data") && !resultLocalObj.getString("keyid_otpidxs_data").equals("")){//本地有存储
                JSONObject otpIdxsObjLocal = new JSONObject(resultLocalObj.getString("keyid_otpidxs_data"));
                if(otpIdxsObjLocal.has("idxs")){
                    String startTimeLocal = otpIdxsObjLocal.getString("starttime");
                    if(startTimeLocal.equals(starttimeNow)){//时段相同就保留idxs
                        idxsArrayLocal = new JSONArray(otpIdxsObjLocal.getString("idxs"));
                    }
                }
            }
            otpIdxsObj.put("starttime", starttimeNow);//默认去本地时间
            otpIdxsObj.put("idxs", idxsArrayLocal);
            Log.d(TAG, "idxsArrayLocal: "+idxsArrayLocal.toString());
            if(ZkUtil.isNetworkAvailable(activity)){
                dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
                    @Override
                    public void dataUpateFail(int i, final String s) {
                        Log.d(TAG, "查询otpidxs失败");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity, s, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    @Override
                    public void dataUpdateSucc(String result) {
                        try{
                            Log.d(TAG, "keyid_otpidxs_data: "+result);
                            String starttimeServer = "";//starttime字符串
                            JSONArray idxsArrayServer = new JSONArray();//索引数组
                            JSONObject resultServerObj = new JSONObject(result);
                            String skeyServer = resultServerObj.getString("keyid_skey_data");
                            skey = skeyServer;//skey用服务器的值
                            Log.d(TAG, "skeyServer: "+skeyServer);
                            if(skeyServer.equals("")){
                                showOtpView(skey);
                                return;
                            }
                            JSONObject otpIdxsObjServer = new JSONObject();
                            if(resultServerObj.has("keyid_otpidxs_data")){//服务器有存储
                                otpIdxsObjServer = new JSONObject(resultServerObj.getString("keyid_otpidxs_data"));
                                if(otpIdxsObjServer.has("starttime")){
                                    starttimeServer = otpIdxsObjServer.getString("starttime");
                                    idxsArrayServer = new JSONArray(otpIdxsObjServer.getString("idxs"));
                                }
                            }
                            Log.d(TAG, "idxsArrayServer: "+idxsArrayServer.toString());
                            if(!skeyLocal.equals("") && !skeyLocal.equals(skeyServer)){//服务器密钥与本地不通，则以服务器数据为主
                                Log.d(TAG, "密钥不通，以服务器为准");
                                otpIdxsObj.put("idxs", combinJsonArray(idxsArrayServer, new JSONArray()));//以服务器数据为主
                                otpIdxsObj.put("starttime", starttimeServer);
                            }else{
                                Log.d(TAG, "密钥相同");
                                JSONArray idxsLocalTemp = otpIdxsObj.getJSONArray("idxs");
                                if(starttimeNow.equals(starttimeServer)){//同一时段，合并
                                    otpIdxsObj.put("idxs", combinJsonArray(idxsArrayServer, idxsLocalTemp));//合并去重，并生成新序号
                                }else{
                                    otpIdxsObj.put("idxs", combinJsonArray(new JSONArray(), idxsLocalTemp));//时间段变化，以本地为准
                                }
                            }
                            initInvalidTime();
                            Log.d(TAG, "otpIdxsObj: "+otpIdxsObj.toString());
                            if(otpIdxsObj.getJSONArray("idxs").length() == 0){//次数用完了
                                activity.findViewById(R.id.usedOutContent).setVisibility(View.VISIBLE);
                                activity.findViewById(R.id.content).setVisibility(View.GONE);
                                isUsedOut = true;
                            }else{
                                isUsedOut = false;
                                activity.findViewById(R.id.usedOutContent).setVisibility(View.GONE);
                                activity.findViewById(R.id.content).setVisibility(View.VISIBLE);
                                viewHandler.sendEmptyMessage(MSG_CACULATE_ZOTP);
                            }
                        }catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                });
            }else{
                JSONArray tempArray = combinJsonArray(new JSONArray(), idxsArrayLocal);
                if(tempArray.length() == 0){
                    activity.findViewById(R.id.usedOutContent).setVisibility(View.VISIBLE);
                    activity.findViewById(R.id.content).setVisibility(View.GONE);
                    isUsedOut = true;
                }else{
                    isUsedOut = false;
                    activity.findViewById(R.id.usedOutContent).setVisibility(View.GONE);
                    activity.findViewById(R.id.content).setVisibility(View.VISIBLE);
                    otpIdxsObj.put("idxs", combinJsonArray(new JSONArray(), idxsArrayLocal));//合并去重，并生成新序号
                    viewHandler.sendEmptyMessage(MSG_CACULATE_ZOTP);
                }

            }
        }catch (JSONException e){
            e.printStackTrace();
        }
    }


    private int parseCharToInt(char c){
        return Integer.parseInt(String.valueOf(c));
    }

    @SuppressWarnings("WrongConstant")
    private void initInvalidTime(){
        String starttime = "";
        try {
            starttime = otpIdxsObj.getString("starttime");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        DateFormat df = new SimpleDateFormat(BriefDate.DATE_FORMAT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ZkUtil.getDateByStr(starttime));
        if(calendar.get(Calendar.MINUTE) == 30){
            int hour = calendar.get(Calendar.HOUR);
            calendar.set(Calendar.HOUR, hour+1);
            calendar.set(Calendar.MINUTE, 0);
        }else{
            int min = calendar.get(Calendar.MINUTE);
            calendar.set(Calendar.MINUTE, min+30);
        }

        invalidTime.setText("密码生成于"+starttime+"，将于"+(df.format(calendar.getTime())).substring(11,16)+"失效");
    }
    /**
     * @param otpMap
     * {pwd=00071606, dateDiff=404890, starttime=2017-09-12 17:50:00}
     */

    protected void refreshOtp(Map<String, String> otpMap){

        //initNextRefreshTime();
        String pwd = otpMap.get("pwd");
        Log.d(TAG, "---1----");
        if(parseCharToInt(pwd.charAt(0)) - 1 < 0){
            Log.d(TAG, "---4----");
            mAstv_0.setNumber(parseCharToInt(pwd.charAt(0)) + 9);
            Log.d(TAG, "---5----");
        }else{
            mAstv_0.setNumber(parseCharToInt(pwd.charAt(0)) - 1);
        }
        Log.d(TAG, "---6---");
        mAstv_0.setTargetNumber(parseCharToInt(pwd.charAt(0)));
        if(parseCharToInt(pwd.charAt(1)) - 1 < 0){
            mAstv_1.setNumber(parseCharToInt(pwd.charAt(1)) + 9);
        }else{
            mAstv_1.setNumber(parseCharToInt(pwd.charAt(1)) - 1);
        }
        mAstv_1.setTargetNumber(parseCharToInt(pwd.charAt(1)));

        if(parseCharToInt(pwd.charAt(2)) - 1 < 0){
            mAstv_2.setNumber(parseCharToInt(pwd.charAt(2)) + 9);
        }else{
            mAstv_2.setNumber(parseCharToInt(pwd.charAt(2)) - 1);
        }
        mAstv_2.setTargetNumber(parseCharToInt(pwd.charAt(2)));

        if(parseCharToInt(pwd.charAt(3)) - 1 < 0){
            mAstv_3.setNumber(parseCharToInt(pwd.charAt(3)) + 9);
        }else{
            mAstv_3.setNumber(parseCharToInt(pwd.charAt(3)) - 1);
        }
        mAstv_3.setTargetNumber(parseCharToInt(pwd.charAt(3)));

        if(parseCharToInt(pwd.charAt(4)) - 1 < 0){
            mAstv_4.setNumber(parseCharToInt(pwd.charAt(4)) + 9);
        }else{
            mAstv_4.setNumber(parseCharToInt(pwd.charAt(4)) - 1);
        }
        mAstv_4.setTargetNumber(parseCharToInt(pwd.charAt(4)));
        if(parseCharToInt(pwd.charAt(5)) - 1 < 0){
            mAstv_5.setNumber(parseCharToInt(pwd.charAt(5)) + 9);
        }else{
            mAstv_5.setNumber(parseCharToInt(pwd.charAt(5)) - 1);
        }
        mAstv_5.setTargetNumber(parseCharToInt(pwd.charAt(5)));
        mAstv_0.setMode(AutoScrollTextView.Mode.UP);//三种模式
        mAstv_1.setMode(AutoScrollTextView.Mode.DOWN);//三种模式
        mAstv_2.setMode(AutoScrollTextView.Mode.UP);//三种模式
        mAstv_3.setMode(AutoScrollTextView.Mode.DOWN);//三种模式
        mAstv_4.setMode(AutoScrollTextView.Mode.UP);//三种模式
        mAstv_5.setMode(AutoScrollTextView.Mode.DOWN);//三种模式
    }

    Handler viewHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_OTP_CLOSE_TIMEOUT:
                    XmBluetoothManager.getInstance().disconnect(mDevice.deviceStat().mac);
                    xqProgressDialog.dismiss();
                    Toast.makeText(activity, "未发现门锁，请靠近门锁重试", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_OTP_KAITONG_TIMEOUT:
                    XmBluetoothManager.getInstance().disconnect(mDevice.deviceStat().mac);
                    xqProgressDialog.dismiss();
                    Toast.makeText(activity, "未发现门锁，请靠近门锁重试", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_CACULATE_ZOTP:
                    calculateZotp();
                    try {
                        JSONObject settingsObj = new JSONObject();
                        settingsObj.put("keyid_otpidxs_data", otpIdxsObj.toString());

                        Log.d(TAG, otpIdxsObj.toString());
                        if(ZkUtil.isNetworkAvailable(activity)){
                            dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                                @Override
                                public void dataUpateFail(int i, String s) {
                                    Toast.makeText(activity, "数据存储失败", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void dataUpdateSucc(String s) {

                                    Log.d(TAG, "ZOTP数据存储成功");
                                }
                            });
                        }else{
                            dataManageUtil.saveDataToLocal(settingsObj);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    };
}
