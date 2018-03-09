package com.xiaomi.zkplug.util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.xiaomi.smarthome.common.ui.dialog.MLAlertDialog;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.entity.MyEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cn.zelkova.lockprotocol.BitConverter;

/**
 * 作者：liwenqi on 17/6/13 13:32
 * 邮箱：liwenqi@zelkova.cn
 * 描述：
 */

public class ZkUtil {
    /**
     * 生成一个随机字符串
     */
    public static String generateTrackId(int maxLen) {
        //String baseChar = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String baseChar = "0123456789abcdefghijklmnopqrstuvwxyz";
        int baseTxtLen = baseChar.length();

        String retVal = "";

        long la = UUID.randomUUID().getLeastSignificantBits();
        long lb = UUID.randomUUID().getMostSignificantBits();
        long ready = Math.abs(la ^ lb);

        for (int idx = 0; idx < maxLen; idx++) {
            int remainder = (int) (ready % baseTxtLen);  //余数
            ready = ready / baseTxtLen;
            retVal += baseChar.substring(remainder, remainder + 1);

            if (ready == 0) break;
        }

        return retVal;
    }
    /**
     * 检测当的网络（WLAN、3G/2G）状态
     * @param context Context
     * @return true 表示网络可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()){
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED){
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }
    //检查蓝牙是否开启
    /*
    * 蓝牙检查:true已打开，false未打开
    * */
    public static boolean isBleOpen(){
        BluetoothAdapter blueadapter=BluetoothAdapter.getDefaultAdapter();
        if(blueadapter.isEnabled()){
            return true;
        }
        return false;
    }
    /**
     * @param context
     * @return 获取屏幕内容高度不包括虚拟按键
     */
    public int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }
    /*
    * 家庭成员：jsonObject 转换为map，用于存储
    * {
   "deviceList":{
           did: [ {
               "id": xx;
               "memberType": xxx,
               "nickName": xx;
               "pwdIdx": xx;
               "auth": xx;
               "sharer"：
               "fpList": [
                   {
                       "id": xx;"fpName": xx;"fpBatchNo": xx;
                   }
                ]
           } ]
   }
    version:xxx;
}
    * */
    public static Map<String, Object> dataObject2Map(JSONObject data) throws JSONException {
        Map<String, Object> dataMap = new HashMap<String, Object>();
        Iterator it = data.keys();
        while (it.hasNext()) {
            String key = (String) it.next();
            dataMap.put(key, data.get(key));
        }
        return dataMap;
    }

    public static long getUTCTime() {
        Calendar cal = Calendar.getInstance();
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
        int dstOffset = cal.get(Calendar.DST_OFFSET);
        cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        return cal.getTimeInMillis();
    }

    /*
    * 启动动画
    * */
    public static void startAnima(Context context, ImageView circleImg){
        circleImg.setVisibility(View.VISIBLE);
		/* 启动动画 begin */
        Animation operatingAnim = AnimationUtils.loadAnimation(context, R.anim.tip);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        if (operatingAnim != null) {
            circleImg.startAnimation(operatingAnim);
        }
		 /* 启动动画 end */
    }
    /*
    * 停止动画
    * */
    public static void stopAnima(ImageView circleImg){
        circleImg.setVisibility(View.GONE);
        circleImg.clearAnimation();//停止动画
    }

    /**
     * 从服务器取图片
     *http://bbs.3gstdy.com
     * @param url
     * @return
     */
    public static Bitmap getHttpBitmap(String url) {
        URL myFileUrl = null;
        Bitmap bitmap = null;
        try {
            myFileUrl = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            if(myFileUrl != null){
                HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
                conn.setConnectTimeout(0);
                conn.setDoInput(true);
                conn.connect();
                InputStream is = conn.getInputStream();
                bitmap = BitmapFactory.decodeStream(is);
                is.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    /**
     * @param str
     * @return
     * @throws PatternSyntaxException
     *
     * 只允许字母、数字和汉字
     */
    public static String stringFilter(String str)throws PatternSyntaxException {
        // 只允许字母、数字和汉字
        String regEx = "[^a-zA-Z0-9\u4E00-\u9FA5\\s]";//正则表达式
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.replaceAll("");
    }


    /**
     * 大头输出
     *
     * 12345678
     *
     * @return78563412
     */
    private static String getReverseStr(String value){
        StringBuilder sb = new StringBuilder();
        sb.append(value.substring(6));
        sb.append(value.substring(4,6));
        sb.append(value.substring(2,4));
        sb.append(value.substring(0,2));
        return sb.toString();
    }
    /**
     * 根据毫秒数计算时间
     * @param snd
     * @return
     */
    public static String getDateBySnd(long snd){
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(snd * 1000);
        return formatter.format(calendar.getTime());
    }

    public static Date getDateByStr(String dStr){
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date theDate = null;
        try {
            theDate = formatter.parse(dStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return theDate;
    }

    /**
     * 查询操作人员人员名字
     *
     * method
     * 0x00:蓝牙方式
     0x01:密码方式
     0x02:指纹方式
     0x03:钥匙方式
     0x04:转盘方式
     0x10:人工反锁
     0x20:自动反锁
     0xFF:异常

     管理员创建成功时调用
     */
    public static JSONObject getMsgInfo(String method, String keyId, JSONArray mDeviceMemberArray, Activity activity){
        JSONObject resObj = new JSONObject();
        JSONObject memberObj = null;
        try{
            Log.d("msg", "keyId: "+keyId+", method: "+method);
            if(method.equals("00")){//蓝牙
                if(keyId.equals("00000000")){//锁的拥有者
                    for(int i=0; i<mDeviceMemberArray.length(); i++){
                        memberObj = mDeviceMemberArray.getJSONObject(i);
                        Log.d("解析", "memberObj:"+memberObj.toString());
                        if(memberObj.getString("memberType").equals("admin")){
                            if(memberObj.getString("nickName").length() > 6){
                                resObj.put("msgTitle", activity.getString(R.string.msg_ble_open_long, memberObj.getString("nickName").substring(0, 6)));
                            }else{
                                resObj.put("msgTitle", activity.getString(R.string.msg_ble_open_normal, memberObj.getString("nickName")));
                            }
                            String msgContent = activity.getString(R.string.msg_ble_open_normal, memberObj.getString("nickName"));
                            resObj.put("msgContent", msgContent);
                            resObj.put("msgType", MyEntity.MSG_LEVEL_HUIJIA);
                            return resObj;
                        }
                    }
                }else if(keyId.equals("ffffffff")){
                    resObj.put("msgTitle", activity.getString(R.string.msg_unknown_open));
                    resObj.put("msgContent", activity.getString(R.string.msg_unknown_open));
                    resObj.put("msgType", MyEntity.MSG_LEVEL_HUIJIA);
                    return resObj;
                }else{//被分享者开门
                    Log.d("解析", "被分享者开门，keyId: "+keyId+"， mDeviceMemberArray.length: "+mDeviceMemberArray.length()+", long keyId:"+BitConverter.toInt32(BitConverter.fromHexString(keyId), 0));
                    for(int i=0; i<mDeviceMemberArray.length(); i++){
                        memberObj = mDeviceMemberArray.getJSONObject(i);
                        Log.d("解析", memberObj.toString());
                        if(memberObj.has("auth") && !memberObj.get("auth").toString().equals("") && !memberObj.get("auth").toString().equals("null")){
                            JSONObject authObj = memberObj.getJSONObject("auth");

                            if(authObj.has("keyId") && !TextUtils.isEmpty(authObj.getString("keyId")) && BitConverter.toInt32(BitConverter.fromHexString(keyId), 0) == Integer.parseInt(authObj.getString("keyId"))){

                                if(memberObj.getString("nickName").length() > 6){
                                    resObj.put("msgTitle", activity.getString(R.string.msg_ble_open_long, memberObj.getString("nickName").substring(0, 6)));
                                }else{
                                    resObj.put("msgTitle", activity.getString(R.string.msg_ble_open_normal, memberObj.getString("nickName")));
                                }

                                resObj.put("msgContent", activity.getString(R.string.msg_ble_open_normal, memberObj.getString("nickName")));
                                resObj.put("msgType", MyEntity.MSG_LEVEL_HUIJIA);
                                return resObj;
                            }
                        }
                    }
                    resObj.put("msgTitle", activity.getString(R.string.msg_sm_ble_open));
                    resObj.put("msgContent", activity.getString(R.string.msg_sm_ble_open));
                    resObj.put("msgType", MyEntity.MSG_LEVEL_HUIJIA);
                    Log.d("解析", "----2-------"+resObj.toString());
                    return resObj;
                }
            }

            if(method.equals("01")){//密码
                Log.d("解析", "keyId： "+keyId);
                for(int i=0; i<mDeviceMemberArray.length(); i++){
                    memberObj = mDeviceMemberArray.getJSONObject(i);
                    if(memberObj.has("pwdIdx") && memberObj.getString("pwdIdx").equals(String.valueOf(BitConverter.toInt32(BitConverter.fromHexString(keyId), 0)))){
                        if(memberObj.getString("nickName").length() > 6){
                            resObj.put("msgTitle", activity.getString(R.string.msg_pwd_open_long, memberObj.getString("nickName").substring(0, 6)));
                        }else{
                            resObj.put("msgTitle", activity.getString(R.string.msg_pwd_open_normal, memberObj.getString("nickName")));
                        }
                        resObj.put("msgContent", activity.getString(R.string.msg_pwd_open_normal, memberObj.getString("nickName")));
                        resObj.put("msgType", MyEntity.MSG_LEVEL_HUIJIA);
                        return resObj;
                    }
                }
                if(keyId.equals("ffffffff")){
                    resObj.put("msgTitle", activity.getString(R.string.msg_sm_otp_open));
                    resObj.put("msgContent", activity.getString(R.string.msg_sm_otp_open));
                    resObj.put("msgType", MyEntity.MSG_LEVEL_HUIJIA);
                    return resObj;
                }
                resObj.put("msgTitle", activity.getString(R.string.msg_sm_pwd_open));
                resObj.put("msgContent", activity.getString(R.string.msg_sm_pwd_open));
                resObj.put("msgType", MyEntity.MSG_LEVEL_HUIJIA);
                return resObj;
            }
            if(method.equals("02")){//指纹
                for(int i=0; i<mDeviceMemberArray.length(); i++){
                    memberObj = mDeviceMemberArray.getJSONObject(i);
                    if(memberObj.has("fpArray")){
                        JSONArray fpList = memberObj.getJSONArray("fpArray");
                        Log.d("msg", "fpList: "+fpList.toString());
                        for(int m =0; m<fpList.length(); m++){
                            JSONObject memberFp = fpList.getJSONObject(m);
                            if(memberFp.getString("batchno").equals(String.valueOf(BitConverter.toInt32(BitConverter.fromHexString(keyId), 0)))){
                                if(memberObj.getString("nickName").length() > 6){
                                    resObj.put("msgTitle", activity.getString(R.string.msg_fp_open_long, memberObj.getString("nickName").substring(0, 6)));
                                }else{
                                    resObj.put("msgTitle", activity.getString(R.string.msg_fp_open_normal, memberObj.getString("nickName")));
                                }

                                resObj.put("msgContent", activity.getString(R.string.msg_fp_open_normal, memberObj.getString("nickName")));
                                resObj.put("msgType", MyEntity.MSG_LEVEL_HUIJIA);
                                return resObj;
                            }
                        }
                    }
                }
                resObj.put("msgTitle", activity.getString(R.string.msg_sm_fp_open));
                resObj.put("msgContent", activity.getString(R.string.msg_fp_open_content));
                resObj.put("msgType", MyEntity.MSG_LEVEL_HUIJIA);
                return resObj;
            }
            if(method.equals("10")){//人工反锁
                resObj.put("msgTitle", activity.getString(R.string.msg_fansuo_notify));
                resObj.put("msgContent", activity.getString(R.string.msg_find_fansuo));
                resObj.put("msgType", MyEntity.MSG_LEVEL_FANSUO);
                return resObj;
            }
            if(method.equals("20")){//自动反锁
                resObj.put("msgTitle", activity.getString(R.string.msg_fansuo_notify));
                resObj.put("msgContent", activity.getString(R.string.msg_find_fansuo));
                resObj.put("msgType", MyEntity.MSG_LEVEL_FANSUO);
                return resObj;
            }
            if(keyId.equals("0100dec0")){
                resObj.put("msgTitle", activity.getString(R.string.msg_keyb_freezon));
                resObj.put("msgContent", activity.getString(R.string.msg_keyb_freezon_reason));
                resObj.put("msgType", MyEntity.MSG_LEVEL_FREEZE_KEYBOARD);
                return resObj;
            }
            if(keyId.equals("0200dec0")){
                resObj.put("msgTitle", activity.getString(R.string.msg_operate_timeout));
                resObj.put("msgContent", activity.getString(R.string.msg_pwd_input_timeout));
                resObj.put("msgType", MyEntity.MSG_LEVEL_FREEZE_KEYBOARD);
                return resObj;
            }
            if(keyId.equals("0300dec0")){
                resObj.put("msgTitle", activity.getString(R.string.msg_peo_qiaosuo));
                resObj.put("msgContent", activity.getString(R.string.msg_qiaosuo_content));
                resObj.put("msgType", MyEntity.MSG_LEVEL_BAOJING);
                return resObj;
            }
            if(keyId.equals("0010dec0")){
                resObj.put("msgTitle", activity.getString(R.string.msg_low_power));
                resObj.put("msgContent", activity.getString(R.string.msg_power_10_percent));
                resObj.put("msgType", MyEntity.MSG_LEVEL_LOW_POWER);
                return resObj;
            }
            if(keyId.equals("0110dec0")){
                resObj.put("msgTitle", activity.getString(R.string.msg_low_power));
                resObj.put("msgContent", activity.getString(R.string.msg_power_5_percent));
                resObj.put("msgType", MyEntity.MSG_LEVEL_LOW_POWER);
                return resObj;
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
        return resObj;
    }
    /**
     * 解析锁事件
     *[
         {
         "uid": "1139628820",
         "time": 1505293216,
         "value": "[\"0000000000009df3b859\"]",
         "did": "1010018460",
         "type": "event",
         "key": "5"
         },
         {
         "uid": "1139628820",
         "time": 1505220729,
         "value": "[\"00000000000070d8b759\"]",
         "did": "1010018460",
         "type": "event",
         "key": "5"
         }
     ]
     * @param eventJsArray
     * @return
     */
    public static JSONArray transformLockEvent(JSONArray eventJsArray, JSONArray mDeviceMemberArray, Activity activity) throws JSONException{
        for(int i=0; i<eventJsArray.length(); i++){
            JSONObject eventJsObj = eventJsArray.getJSONObject(i);

            String value = eventJsObj.getString("value").replace("[\"", "").replace("\"]", "");
            String uid = eventJsObj.getString("uid");
            String time = eventJsObj.getString("time");
            String did = eventJsObj.getString("did");
            String method = value.substring(2,4);
            String keyId = value.substring(4,12);

            JSONObject msgInfoObj = getMsgInfo(method, keyId, mDeviceMemberArray, activity);
            eventJsObj.put("opTime", getDateBySnd(Long.parseLong(time)));
            eventJsObj.put("msgTitle", msgInfoObj.getString("msgTitle"));
            eventJsObj.put("msgContent", msgInfoObj.getString("msgContent"));
            eventJsObj.put("msgType", msgInfoObj.getInt("msgType"));
        }

        Log.d("解析", "解析后eventJsArray: "+eventJsArray.toString());
        return eventJsArray;
    }

    /**
     * 解析得出最新电量
     *
     * @param powerJsArray
     * @return
     * @throws JSONException
     */
    public static int getLockPower(JSONArray powerJsArray){
        String value = "";
        try {
            JSONObject powerJsObj = powerJsArray.getJSONObject(0);
            value = powerJsObj.getString("value").replace("[\"", "").replace("\"]", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return Integer.parseInt(value.substring(0, 2),16);
    }
    /**
     * 命令不在有效期，进行特殊显示
     * 方法内的变量都是局部变量，不会造成内存泄漏的
     */
    public static void showCmdTimeOutView(Activity mActivity){

        final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(mActivity);
        builder.setTitle(R.string.device_cmd_timeout);
        builder.setMessage(R.string.cmd_out_of_time);
        builder.setPositiveButton(R.string.gloable_confirm, new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public static String[] delete(int index, String array[]){
        //根据删除索引，把数组后面的向前移一位
        for(int i=index;i<array.length;i++){
            if(i!=array.length-1){
                array[i]=array[i+1];
            }else{//处理最后一位超出情况
                array[i]=array[i];
            }
        }
        return array;
    }


}
