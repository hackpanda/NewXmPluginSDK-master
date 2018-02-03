package com.xiaomi.zkplug.entity;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.xiaomi.plugin.core.XmPluginPackage;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.util.DataManageUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import cn.zelkova.lockprotocol.BitConverter;

/**
 * 作者：liwenqi on 16/8/12 20:26
 * 邮箱：liwenqi@zelkova.cn
 */
public class MyProvider extends cn.zelkova.lockprotocol.ProfileProvider implements Serializable{

    private final String TAG = "MyProvider";
    public static byte[] ssKey = new byte[]{
            0x30,0x30,0x30,0x30,
            0x30,0x30,0x30,0x30,
            0x30,0x30,0x30,0x30,
            0x30,0x30,0x30,0x30};

    Activity activity;
    XmPluginPackage xmPluginPackage;
    Device mDevice;
    public MyProvider(Activity activity, XmPluginPackage xmPluginPackage, Device mDevice) {
        this.activity = activity;
        this.xmPluginPackage = xmPluginPackage;
        this.mDevice = mDevice;
    }

    @Override
    public byte[] getSecurityKey(String did) {
        JSONObject userDataObj = DataManageUtil.getDataFromLocal(activity);
        try {
            String skey = userDataObj.getJSONObject(mDevice.deviceStat().did).getString("skey");
            ssKey = BitConverter.fromHexString(skey);
            Log.d(TAG, "查询本地密钥为： "+BitConverter.fromHexString(skey));
        } catch (JSONException je) {
            je.printStackTrace();
            Toast.makeText(activity, "数据解析异常", Toast.LENGTH_SHORT).show();
        } catch (Exception e){
            e.printStackTrace();
            Log.d(TAG, "初始无数据");
            Toast.makeText(activity, "初始无密钥数据", Toast.LENGTH_SHORT).show();
        }
        return ssKey;
    }

    byte[] getSecurityKey(byte[] macByte) {
        return this.getSecurityKey(BitConverter.convertMacAdd(macByte));
    }


}
