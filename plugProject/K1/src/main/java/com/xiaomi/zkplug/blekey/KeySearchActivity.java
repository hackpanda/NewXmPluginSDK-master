package com.xiaomi.zkplug.blekey;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.common.ui.dialog.XQProgressDialog;
import com.xiaomi.smarthome.device.api.Callback;
import com.xiaomi.smarthome.device.api.SecurityKeyInfo;
import com.xiaomi.smarthome.device.api.UserInfo;
import com.xiaomi.smarthome.device.api.XmPluginHostApi;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.util.ZkUtil;

import java.util.List;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：授权查找
 */
public class KeySearchActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "AuthSearchActivity";
    TextView accountWarnTv;
    EditText accountEdit;
    Device mDevice;
    public static KeySearchActivity instance;
    XQProgressDialog xqProgressDialog;
    KeyManager keyManager;//发送钥匙辅助类
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_search);

        initView();
    }

    private void initView(){
        instance = this;
        this.keyManager = new KeyManager(mDeviceStat, activity());
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(R.string.member_add_key);
        mDevice = Device.getDevice(mDeviceStat);
        accountWarnTv = (TextView) findViewById(R.id.accountWarnTv);
        accountEdit = (EditText) findViewById(R.id.accountEdit);
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.searchBtn).setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.searchBtn:
                accountWarnTv.setVisibility(View.GONE);
                if(!ZkUtil.isNetworkAvailable(this)){
                    Toast.makeText(activity(), R.string.network_not_avilable, Toast.LENGTH_LONG).show();
                    return;
                }
                if(TextUtils.isEmpty(accountEdit.getText().toString())){
                    accountWarnTv.setText(R.string.blekey_empty_search);
                    accountWarnTv.setVisibility(View.VISIBLE);
                    return;
                }
                xqProgressDialog = new XQProgressDialog(activity());
                xqProgressDialog.setMessage(getResources().getString(R.string.blekey_searching));
                xqProgressDialog.setCancelable(false);
                xqProgressDialog.show();
                getUserInfo(accountEdit.getText().toString().trim());//根据帐号获取用户信息
                break;
        }
    }
    /**
     * 查找账户的钥匙信息
     */
    private void searchAccount(){

    }
    /**
     * @param accountId
     * 1. 判断账号是否为空
     * 2. 判断是否授权过
     */
    //根据帐号获取用户信息
    private void getUserInfo(final String accountId){
        XmPluginHostApi.instance().getUserInfo(accountId, new Callback<UserInfo>() {
            @Override
            public void onSuccess(UserInfo userInfo) {
                xqProgressDialog.dismiss();
                Log.d(TAG, "accountId:"+accountId + ",userInfo: userInfo.userId: "+userInfo.userId+", userInfo.phone: "+userInfo.phone+", userInfo.nickName: "+userInfo.nickName+", userInfo.url: "+userInfo.url);
                if(TextUtils.isEmpty(userInfo.nickName)){
                    accountWarnTv.setText(R.string.blekey_not_find);
                    accountWarnTv.setVisibility(View.VISIBLE);
                    return;
                }
                getSecurityKey(userInfo);
            }
            @Override
            public void onFailure(int i, String s) {
                accountWarnTv.setText(R.string.blekey_not_find);
                accountWarnTv.setVisibility(View.VISIBLE);
                xqProgressDialog.dismiss();
            }
        });
    }

    //获取指定人员的钥匙记录
    public void getSecurityKey(final UserInfo userInfo) {
        XmPluginHostApi.instance().getSecurityKey(mDevice.getModel(), mDevice.getDid(), new Callback<List<SecurityKeyInfo>>() {
            @Override
            public void onSuccess(List<SecurityKeyInfo> securityKeyInfos) {
                Log.d(TAG, "userInfo data：userInfo.url: "+userInfo.url+", userInfo.phone"+userInfo.phone+", userInfo.nickName"+userInfo.nickName+", userInfo.userId"+userInfo.userId);
                for (int i = 0; i < securityKeyInfos.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("blekey" + (i + 1) + ":" + "\n");
                    sb.append("keyId = " + securityKeyInfos.get(i).keyId + "\n");
                    sb.append("shareUid = " + securityKeyInfos.get(i).shareUid + "\n");
                    sb.append("status = " + securityKeyInfos.get(i).status + "\n");
                    sb.append("activeTime = " + securityKeyInfos.get(i).activeTime + "\n");
                    sb.append("expireTime = " + securityKeyInfos.get(i).expireTime + "\n");
                    sb.append("weekdays = " + securityKeyInfos.get(i).weekdays + "\n");
                    sb.append("isoutofdate = " + securityKeyInfos.get(i).isoutofdate + "\n");
                    Log.d(TAG, sb.toString()+", userId:"+userInfo.userId);
                    if(securityKeyInfos.get(i).shareUid.equals(userInfo.userId)){//已被授过权
                        Intent userIntent = new Intent();
                        userIntent.putExtra("nickName", userInfo.nickName);
                        userIntent.putExtra("url", userInfo.url);
                        userIntent.putExtra("msg", getResources().getString(R.string.ble_key_exist));
                        accountEdit.setText("");
                        startActivity(userIntent, KeyExistActivity.class.getName());
                        return;
                    }
                }
                if(userInfo.userId.equals(XmPluginHostApi.instance().getAccountId())){//管理员自己
                    Intent userIntent = new Intent();
                    userIntent.putExtra("nickName", userInfo.nickName);
                    userIntent.putExtra("url", userInfo.url);
                    userIntent.putExtra("msg", getResources().getString(R.string.blekey_admin));
                    startActivity(userIntent, KeyExistActivity.class.getName());
                    return;
                }else{
                    Intent userIntent = new Intent();
                    userIntent.putExtra("memberId", getIntent().getStringExtra("memberId"));
                    userIntent.putExtra("userId", userInfo.userId);//小米帐号
                    userIntent.putExtra("nickName", userInfo.nickName);
                    userIntent.putExtra("phone", userInfo.phone);
                    userIntent.putExtra("url", userInfo.url);
                    startActivity(userIntent, KeyGaveActivity.class.getName());
                }
            }

            @Override
            public void onFailure(int i, String s) {
                Toast.makeText(activity(), getResources().getString(R.string.blekey_info_query_failed)+"：" + s, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
