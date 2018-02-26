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
import com.xiaomi.smarthome.device.api.SecurityKeyInfo;
import com.xiaomi.smarthome.device.api.UserInfo;
import com.xiaomi.smarthome.device.api.XmPluginHostApi;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.CommonUtils;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.zelkova.lockprotocol.BriefDate;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：已经授权
 */
public class KeyDetailActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "AuthRemoveActivity";

    TextView nickNameTv;
    Device mDevice;
    String miAccount;//小米帐号
    Bitmap userBitmap;
    ImageView userImg;
    String memberId;
    DataManageUtil dataManageUtil;
    KeyManager keyManager;
    TextView keyValidPeriodTv, recMsgTv;
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
        mTitleView.setText("钥匙详情");
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
        getUserInfo(miAccount);
        getSecurityKey(mDeviceStat.model, mDeviceStat.did, false);
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
                Toast.makeText(activity(), "用户信息查询失败", Toast.LENGTH_LONG).show();
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

    //获取指定人员的钥匙记录
    public void getSecurityKey(final String model, final String did, final boolean isDelete) {
        XmPluginHostApi.instance().getSecurityKey(model, did, new Callback<List<SecurityKeyInfo>>() {
            @Override
            public void onSuccess(List<SecurityKeyInfo> securityKeyInfos) {
                Log.d(TAG, String.format("获取到%d把钥匙：", securityKeyInfos.size()));
                if(securityKeyInfos.size() == 0) {
                    Toast.makeText(KeyDetailActivity.this.activity(), "没有钥匙，授权取消失败", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (int i = 0; i < securityKeyInfos.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("钥匙" + (i + 1) + ":" + "\n");
                    sb.append("keyId = " + securityKeyInfos.get(i).keyId + "\n");
                    sb.append("shareUid = " + securityKeyInfos.get(i).shareUid + "\n");
                    sb.append("status = " + securityKeyInfos.get(i).status + "\n");
                    sb.append("activeTime = " + securityKeyInfos.get(i).activeTime + "\n");
                    sb.append("expireTime = " + securityKeyInfos.get(i).expireTime + "\n");
                    sb.append("weekdays = " + securityKeyInfos.get(i).weekdays + "\n");
                    sb.append("isoutofdate = " + securityKeyInfos.get(i).isoutofdate + "\n");
                    if(securityKeyInfos.get(i).shareUid.equals(miAccount)){//已被授过权
                        if(isDelete){
                            deleteSecurityKey(model, did, securityKeyInfos.get(i).keyId);
                        }else{
                            //recMsgTv.setText();
                            Date startTime = new Date(securityKeyInfos.get(i).activeTime * 1000);
                            Date endTime = new Date(securityKeyInfos.get(i).expireTime * 1000);
                            if(securityKeyInfos.get(i).status == 3){//永久
                                keyValidPeriodTv.setText("手机钥匙永久有效");
                            }else if(securityKeyInfos.get(i).status == 1){//临时
                                keyValidPeriodTv.setText(BriefDate.fromNature(startTime).toString().substring(0, 16) +" - "+ BriefDate.fromNature(endTime).toString().substring(0, 16));
                            }else{
                                List<Integer> weekdays = securityKeyInfos.get(i).weekdays;
                                String result = "每";
                                for(int m=0; m<weekdays.size(); m++){
                                    if(weekdays.get(m) == 1){
                                        result += "周一，";
                                    }else if(weekdays.get(m) == 2){
                                        result += "周二，";
                                    }else if(weekdays.get(m) == 3){
                                        result += "周三，";
                                    }else if(weekdays.get(m) == 4){
                                        result += "周四，";
                                    }else if(weekdays.get(m) == 5){
                                        result += "周五，";
                                    }else if(weekdays.get(m) == 6){
                                        result += "周六，";
                                    }else if(weekdays.get(m) == 0){
                                        result += "周日，";
                                    }
                                }
                                result = result.substring(0, result.length() - 1);
                                keyValidPeriodTv.setText(result+"的"+BriefDate.fromNature(startTime).toString().substring(11, 16) +" - "+ BriefDate.fromNature(endTime).toString().substring(11, 16)+"有效");
                            }
                        }
                        return;
                    }
                }
            }

            @Override
            public void onFailure(int i, String s) {
                Toast.makeText(KeyDetailActivity.this.activity(), "授权取消失败：" + s, Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.authRemoveBtn:
                if(!ZkUtil.isNetworkAvailable(this)){
                    Toast.makeText(activity(), "网络未连接，请确保网络畅通", Toast.LENGTH_SHORT).show();
                    return;
                }
                final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
                builder.setTitle("是否删除钥匙 ");
                builder.setPositiveButton("确定", new MLAlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getSecurityKey(mDeviceStat.model, mDeviceStat.did, true);
                    }
                });
                builder.setNegativeButton("取消", new MLAlertDialog.OnClickListener() {
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
                Toast.makeText(KeyDetailActivity.this.activity(), String.format("删除钥匙(keyId=%s)失败, code = %d, detail = %s", keyId, i, s), Toast.LENGTH_SHORT).show();
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
                    Log.d(TAG, "取消授权失败");
                    activity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CommonUtils.toast(activity(), s);
                        }
                    });
                }
                @Override
                public void dataUpdateSucc(String s) {
                    Log.d(TAG, "取消授权成功");
                    activity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(KeyDetailActivity.this.activity(), "取消授权成功", Toast.LENGTH_SHORT).show();
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
