package com.xiaomi.zkplug.blekey;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.common.ui.dialog.MLAlertDialog;
import com.xiaomi.smarthome.device.api.Callback;
import com.xiaomi.smarthome.device.api.UserInfo;
import com.xiaomi.smarthome.device.api.XmPluginHostApi;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.CommonUtils;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;
import com.xiaomi.zkplug.view.TimeSelector.Utils.TextUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.zelkova.lockprotocol.BriefDate;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：已经授权
 */
public class KeyDetailActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "KeyDetail";

    TextView nickNameTv;
    Device mDevice;
    String miAccount;//小米帐号
    Bitmap userBitmap;
    ImageView userImg;
    String memberId;
    DataManageUtil dataManageUtil;
    KeyManager keyManager;
    TextView keyValidPeriodTv, recMsgTv;
    private String theAccountKey;//字段名
    private String theBleKey;//字段名
    private String theNotifyKey;//字段名
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_detail);
        initView();
    }

    private void initView(){
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();


        this.dataManageUtil = new DataManageUtil(mDeviceStat, this);
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(R.string.blekey_key_info);
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.authRemoveBtn).setOnClickListener(this);
        nickNameTv = (TextView) findViewById(R.id.nickNameTv);
        mDevice = Device.getDevice(mDeviceStat);
        nickNameTv.setText(getIntent().getStringExtra("nickName"));
        userImg = (ImageView) findViewById(R.id.userImg);
        keyValidPeriodTv = ((TextView) findViewById(R.id.keyValidPeriodTv));
        recMsgTv = ((TextView) findViewById(R.id.recMsgTv));
        this.miAccount = getIntent().getStringExtra("miAccount");
        this.memberId = getIntent().getStringExtra("memberId");
        theAccountKey = "keyid_sm$mi$account$"+memberId+"_data";
        theNotifyKey = "keyid_sm$mi$notify$"+memberId+"_data";
        theBleKey = "keyid_sm$mi$blekey$"+memberId+"_data";
        getUserInfo(miAccount);
        getSecurityKey(false);
    }
    /**
     * 查询账号信息并初始化头像
     * @param miAccount
     */
    private void getUserInfo(String miAccount){
        XmPluginHostApi.instance().getUserInfo(miAccount, new Callback<UserInfo>() {
            @Override
            public void onSuccess(UserInfo userInfo) {
                initUserImg(userInfo.url);
            }
            @Override
            public void onFailure(int i, String s) {
                Toast.makeText(activity(), R.string.blekey_userinfo_query_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 设置帐号头像
     */
    private void initUserImg(final String url){

        if(!ZkUtil.isNetworkAvailable(this)) return;
        if(!url.equals("null") && !TextUtils.isEmpty(url)){
            Log.d(TAG, "initUserImg:"+url);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try{
                        userBitmap = ZkUtil.getHttpBitmap(url);
                        Log.d(TAG, "userBitmap:"+userBitmap.toString());
                        activity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "initUseruserImg.setImageBitmap(userBitmap)mg");
                                userImg.setImageBitmap(userBitmap);
                            }
                        });
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * @param dataKey
     * @param isDelete
     * 根据数据加载界面
     */
    private void initKeyDetailView(JSONObject dataKey, boolean isDelete){
        try {
            String account = dataKey.getString(theAccountKey);
            String notify = dataKey.getString(theNotifyKey);
            if(dataKey.has(theBleKey) && !TextUtil.isEmpty(dataKey.getString(theBleKey))){
                JSONObject bleKey = new JSONObject(dataKey.getString(theBleKey));
                String bletype = bleKey.getString("bletype");
                String keyId = bleKey.getString("keyid");
                Long activeTime = Long.parseLong(bleKey.getString("activetime")) * 1000;
                Long expireTime = Long.parseLong(bleKey.getString("expiretime")) * 1000;
                if(isDelete){
                    deleteSecurityKey(mDeviceStat.model, mDeviceStat.did, keyId);
                }else{
                    recMsgTv.setText(notify.equals("0") ? R.string.blekey_msg_not : R.string.blekey_msg_receive);
                    Date startTime = new Date(activeTime);
                    Date endTime = new Date(expireTime);
                    if(bletype.equals("3")){//永久
                        keyValidPeriodTv.setText(R.string.blekey_valid_forever);
                    }else if(bletype.equals("1")){//临时
                        keyValidPeriodTv.setText(BriefDate.fromNature(startTime).toString().substring(0, 16) +" ~ "+ BriefDate.fromNature(endTime).toString().substring(0, 16));
                    }else{
                        JSONArray weekdays = new JSONArray(bleKey.getString("week"));
                        String result = getResources().getString(R.string.blekey_period_per);
                        for(int m=0; m<weekdays.length(); m++){
                            if(weekdays.get(m).toString().equals("1")){
                                result += getResources().getString(R.string.blekey_period_monday)+"，";
                            }else if(weekdays.get(m).toString().equals("2")){
                                result += getResources().getString(R.string.blekey_period_tuesday)+"，";
                            }else if(weekdays.get(m).toString().equals("3")){
                                result += getResources().getString(R.string.blekey_period_wednesday)+"，";
                            }else if(weekdays.get(m).toString().equals("4")){
                                result += getResources().getString(R.string.blekey_period_thursday)+"，";
                            }else if(weekdays.get(m).toString().equals("5")){
                                result += getResources().getString(R.string.blekey_period_friday)+"，";
                            }else if(weekdays.get(m).toString().equals("6")){
                                result += getResources().getString(R.string.blekey_period_saturday)+"，";
                            }else if(weekdays.get(m).toString().equals("0")){
                                result += getResources().getString(R.string.blekey_period_sunday)+"，";
                            }
                        }
                        result = result.substring(0, result.length() - 1);
                        keyValidPeriodTv.setText(result+getResources().getString(R.string.blekey_period_de)+BriefDate.fromNature(startTime).toString().substring(11, 16) +" - "+ BriefDate.fromNature(endTime).toString().substring(11, 16)+getResources().getString(R.string.blekey_period_valid));
                    }
                }
            }else{
                Toast.makeText(KeyDetailActivity.this.activity(), R.string.blekey_null, Toast.LENGTH_SHORT).show();
                return;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
    //获取指定人员的钥匙记录
    public void getSecurityKey(final boolean isDelete) {
        JSONArray mJsArray = new JSONArray();//数据查询参数
        mJsArray.put(theAccountKey);
        mJsArray.put(theNotifyKey);
        mJsArray.put(theBleKey);
        Log.d(TAG, "getSecurityKey: "+mJsArray.toString());
        try {
            dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
                @Override
                public void dataUpateFail(int i, final String s) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity(), s, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override
                public void dataUpdateSucc(String s) {
                    try {
                        final JSONObject data = new JSONObject(s);
                        Log.d(TAG, "服务器数据: "+data.toString());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                initKeyDetailView(data, isDelete);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
//        XmPluginHostApi.instance().getSecurityKey(model, did, new Callback<List<SecurityKeyInfo>>() {
//            @Override
//            public void onSuccess(List<SecurityKeyInfo> securityKeyInfos) {
//                if(securityKeyInfos.size() == 0) {
//                    Toast.makeText(KeyDetailActivity.this.activity(), R.string.blekey_null, Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                for (int i = 0; i < securityKeyInfos.size(); i++) {
//                    if(securityKeyInfos.get(i).shareUid.equals(miAccount)){//已被授过权
//                        if(isDelete){
//                            deleteSecurityKey(model, did, securityKeyInfos.get(i).keyId);
//                        }else{
//                            //recMsgTv.setText();
//                            Log.d(TAG, "securityKeyInfos.get(i): "+securityKeyInfos.get(i).toString());
//                            Date startTime = new Date(securityKeyInfos.get(i).activeTime * 1000);
//                            Date endTime = new Date(securityKeyInfos.get(i).expireTime * 1000);
//                            if(securityKeyInfos.get(i).status == 3){//永久
//                                keyValidPeriodTv.setText(R.string.blekey_valid_forever);
//                            }else if(securityKeyInfos.get(i).status == 1){//临时
//                                keyValidPeriodTv.setText(BriefDate.fromNature(startTime).toString().substring(0, 16) +" ~ "+ BriefDate.fromNature(endTime).toString().substring(0, 16));
//                            }else{
//                                List<Integer> weekdays = securityKeyInfos.get(i).weekdays;
//                                String result = getResources().getString(R.string.blekey_period_per);
//                                for(int m=0; m<weekdays.size(); m++){
//                                    if(weekdays.get(m) == 1){
//                                        result += getResources().getString(R.string.blekey_period_monday)+"，";
//                                    }else if(weekdays.get(m) == 2){
//                                        result += getResources().getString(R.string.blekey_period_tuesday)+"，";
//                                    }else if(weekdays.get(m) == 3){
//                                        result += getResources().getString(R.string.blekey_period_wednesday)+"，";
//                                    }else if(weekdays.get(m) == 4){
//                                        result += getResources().getString(R.string.blekey_period_thursday)+"，";
//                                    }else if(weekdays.get(m) == 5){
//                                        result += getResources().getString(R.string.blekey_period_friday)+"，";
//                                    }else if(weekdays.get(m) == 6){
//                                        result += getResources().getString(R.string.blekey_period_saturday)+"，";
//                                    }else if(weekdays.get(m) == 0){
//                                        result += getResources().getString(R.string.blekey_period_sunday)+"，";
//                                    }
//                                }
//                                result = result.substring(0, result.length() - 1);
//                                keyValidPeriodTv.setText(result+getResources().getString(R.string.blekey_period_de)+BriefDate.fromNature(startTime).toString().substring(11, 16) +" - "+ BriefDate.fromNature(endTime).toString().substring(11, 16)+getResources().getString(R.string.blekey_period_valid));
//                            }
//                        }
//                        return;
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(int i, String s) {
//                Toast.makeText(KeyDetailActivity.this.activity(), activity().getResources().getString(R.string.blekey_info_query_failed)+"：" + s, Toast.LENGTH_SHORT).show();
//
//            }
//        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.authRemoveBtn:
                if(!ZkUtil.isNetworkAvailable(this)){
                    Toast.makeText(activity(), R.string.network_not_avilable, Toast.LENGTH_LONG).show();
                    return;
                }
                final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
                builder.setTitle(R.string.blekey_del_dialog_title);
                builder.setPositiveButton(R.string.gloable_confirm, new MLAlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getSecurityKey(true);
                    }
                });
                builder.setNegativeButton(R.string.gloable_cancel, new MLAlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
                break;
        }
    }

    //删除分享的钥匙：即取消授权
    public void deleteSecurityKey(String model, String did, final String keyId) {
        XmPluginHostApi.instance().deleteSecurityKey(model, did, keyId, new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateMemberAuth();//删除钥匙成功后，更新auth属性
            }
            @Override
            public void onFailure(int i, String s) {
                Toast.makeText(KeyDetailActivity.this.activity(), getResources().getString(R.string.blekey_del_fail)+" Code="+i, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 清除授权信息
     *
     * 小米账号、通知、钥匙
     */
    private void updateMemberAuth(){
        JSONObject settingsObj = new JSONObject();
        try{
            settingsObj.put("keyid_sm$mi$account$"+memberId+"_data", "");
            settingsObj.put("keyid_sm$mi$notify$"+memberId+"_data", "0");
            settingsObj.put("keyid_sm$mi$blekey$"+memberId+"_data", "");
            /* 钥匙信息 end */
            dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                @Override
                public void dataUpateFail(int i, final String s) {
                    activity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CommonUtils.toast(activity(), s);
                        }
                    });
                }
                @Override
                public void dataUpdateSucc(String s) {
                    activity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(KeyDetailActivity.this.activity(), R.string.blekey_del_succ, Toast.LENGTH_SHORT).show();
                            finish();
                            if(KeySearchActivity.instance != null){
                                KeySearchActivity.instance.finish();
                            }
                        }
                    });
                }
            });
        }catch (JSONException je){
            je.printStackTrace();
        }
    }
}
