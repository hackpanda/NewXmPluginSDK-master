package com.xiaomi.zkplug.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.xiaomi.smarthome.device.api.Callback;
import com.xiaomi.smarthome.device.api.DeviceStat;
import com.xiaomi.smarthome.device.api.Parser;
import com.xiaomi.smarthome.device.api.XmPluginHostApi;
import com.xiaomi.zkplug.entity.MyEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * 作者：liwenqi on 17/8/4 12:13
 * 邮箱：liwenqi@zelkova.cn
 * 描述：
 */

public class DataManageUtil {

    private final String TAG = "DataManageUtil";
    DeviceStat mDeviceStat;
    Context mContext;
    /**
     * 构造函数
     * @param mDeviceStat
     */
    public DataManageUtil(DeviceStat mDeviceStat, Context mContext) {
        this.mDeviceStat = mDeviceStat;
        this.mContext = mContext;
    }
    //是否创建过管理员
    private static boolean hasAdminMember(JSONArray mDeviceMemberArray){
        try{
            for(int n=0;n<mDeviceMemberArray.length();n++){
                if(mDeviceMemberArray.getJSONObject(n).getString("memberType").equals("admin")){
                    return true;
                }
            }
        }catch (JSONException e){
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 获取本地数据
     */
    public static JSONObject getDataFromLocal(Activity activity){
        JSONObject userDataObj = null;
        SharedPreferences spConfig = activity.getSharedPreferences(MyEntity.UserConfigFile, Context.MODE_PRIVATE);
        String userData = spConfig.getString("UserData", "{}");
        try{
            JSONObject dataObject = new JSONObject(userData);//data居然在服务器存成了字符串
            userDataObj = dataObject.getJSONObject("deviceList");//deviceList
        }catch (JSONException e){
            e.printStackTrace();
            Toast.makeText(activity, "数据解析异常", Toast.LENGTH_SHORT).show();
        }
        return userDataObj;
    }


    /**
     * 查询did，进入主界面时判断是否清空userconfig里面多数据
     *
     * 这个方法竟然会返回两次, 第一次是缓存，只能使用第二次返回的
     */



    //--------------------------------------------新的存储格式－－－－－－－－－－－－－－－－－－－－－－－

    /**
     * 批量存储数据到服务器: 支持批量
     */
    public void saveDataToServer(final JSONObject settingsObj, final DataUpdateCallback dataUpdateCallback){
        JSONObject dataObj = new JSONObject();
        try {
            dataObj.put("did", mDeviceStat.did);
            dataObj.put("settings", settingsObj);
        } catch (JSONException var12) {
            var12.printStackTrace();
        }
        Callback<String> callback = new Callback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "存储server成功回调回来了");
                saveDataToLocal(settingsObj);//直接存进本地
                dataUpdateCallback.dataUpdateSucc("");
            }

            @Override
            public void onFailure(int i, String s) {
                Log.d(TAG, "存储server失败回调回来了");
                dataUpdateCallback.dataUpateFail(i, s);
            }
        };

        XmPluginHostApi.instance().callSmartHomeApi(MyEntity.MODEL_ID, "/device/setsetting", dataObj, callback, new Parser() {
            public String parse(String result) throws JSONException {
                return result;
            }
        });
    }
    /**
     * 存储数据到本地: 支持批量
     */
    public void saveDataToLocal(JSONObject settingsObj){
        SharedPreferences dataConfig = mContext.getSharedPreferences(mDeviceStat.did, Context.MODE_PRIVATE);//文件名：did
        SharedPreferences.Editor editor = dataConfig.edit();
        Iterator iterator = settingsObj.keys();
        while(iterator.hasNext()){
            try {
                String key = (String) iterator.next();
                String value = settingsObj.getString(key);
                Log.d(TAG, "key: "+key+", value: "+value);
                editor.putString(key, value);
            }catch (JSONException je){
                je.printStackTrace();
            }
        }
        editor.commit();
    }

    /**
     * 查询数据: 支持批量
     * JSONArray mJsArray = new JSONArray();
     * mJsArray.put("keyid_master$nickname_data");//里面放的是key
     */
    public void queryDataFromServer(JSONArray mJsArray, final DataUpdateCallback dataUpdateCallback){
        Log.d(TAG, "开始查询");
        JSONObject dataObj = new JSONObject();
        try {
            dataObj.put("did", mDeviceStat.did);
            dataObj.put("settings", mJsArray);
        } catch (JSONException var12) {
            var12.printStackTrace();
        }
        Callback<String> callback = new Callback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "查询server成功回调回来了: "+result);//注意，这里服务器直接返回了：已是最新版本
                dataUpdateCallback.dataUpdateSucc(result);
            }

            @Override
            public void onFailure(int i, String s) {
                Log.d(TAG, "查询server失败回调回来了");
                dataUpdateCallback.dataUpateFail(i, s);
            }
        };
        XmPluginHostApi.instance().callSmartHomeApi(MyEntity.MODEL_ID, "/device/getsetting", dataObj, callback, new Parser() {
            public String parse(String result) throws JSONException {
                return result;
            }
        });

    }

    public void checkMemberAvaliable(final String memberId, final DataUpdateCallback dataUpdateCallback){
        //查询服务器数据，存储进服务器
        if(memberId.equals("")){
            dataUpdateCallback.dataUpdateSucc("true");
            return;
        }

        JSONArray mJsArray = new JSONArray();
        mJsArray.put("keyid_smlist_data");
        queryDataFromServer(mJsArray, new DataUpdateCallback() {
            @Override
            public void dataUpateFail(int i, String s) {
                dataUpdateCallback.dataUpateFail(i, "数据查询失败");
            }
            @Override
            public void dataUpdateSucc(String s) {
                try{
                    JSONObject resultObj = new JSONObject(s);
                    JSONArray smArray = new JSONArray(resultObj.getString("keyid_smlist_data"));
                    Log.d(TAG, "smArray: "+smArray.toString());
                    String isMemberAvalid = "false";//人员无效
                    for(int i=0; i<smArray.length(); i++){
                        if(smArray.getJSONObject(i).getString("id").equals(memberId)){
                            isMemberAvalid = "true";
                            break;
                        }
                    }
                    dataUpdateCallback.dataUpdateSucc(isMemberAvalid);
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }
    /**
     * 查询本地存储的数据
     */
    public JSONObject getDataFromLocal(JSONArray mJsArray){
        JSONObject resultObj = new JSONObject();
        SharedPreferences spConfig = mContext.getSharedPreferences(mDeviceStat.did, Context.MODE_PRIVATE);//文件名：did
        for(int i = 0; i < mJsArray.length(); i++){
            try{
                String key = mJsArray.getString(i);
                resultObj.put(key, spConfig.getString(key, ""));
            }catch (JSONException je){
                je.printStackTrace();
            }
        }
        return resultObj;
    }

    public void setKeyToName(String value, String nickName){
        try {
            JSONObject settingsObj = new JSONObject();
            settingsObj.put("keyid_"+value+"_ali", nickName);
            saveDataToServer(settingsObj, new DataUpdateCallback() {
                @Override
                public void dataUpateFail(int i, String s) {
                    Log.d(TAG, "setKeyToName失败");
                }

                @Override
                public void dataUpdateSucc(String s) {
                    Log.d(TAG, "setKeyToName成功");
                }
            });
        }catch (JSONException je){
            je.printStackTrace();
        }
    }

    /**
     * 批量
     * @param settingsObj
     */
    public void setKeyToName(JSONObject settingsObj){
        saveDataToServer(settingsObj, new DataUpdateCallback() {
            @Override
            public void dataUpateFail(int i, String s) {
                Log.d(TAG, "setKeyToName失败");
            }

            @Override
            public void dataUpdateSucc(String s) {
                Log.d(TAG, "setKeyToName成功");
            }
        });
    }
}