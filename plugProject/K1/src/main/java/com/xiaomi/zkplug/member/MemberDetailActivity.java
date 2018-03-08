package com.xiaomi.zkplug.member;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.common.ui.dialog.MLAlertDialog;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.CommonUtils;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.blekey.KeyDetailActivity;
import com.xiaomi.zkplug.blekey.KeySearchActivity;
import com.xiaomi.zkplug.fpdel.FpDelActivity;
import com.xiaomi.zkplug.fpinput.FpInputActivity;
import com.xiaomi.zkplug.member.slideview.ListViewCompat;
import com.xiaomi.zkplug.member.slideview.PwdUnlockItem;
import com.xiaomi.zkplug.member.slideview.SlideView;
import com.xiaomi.zkplug.pwdset.PwdSetActivity;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：成员详情
 */
public class MemberDetailActivity extends BaseActivity implements View.OnClickListener, IPwdSet, SlideView.OnSlideListener{

    private final String TAG = "MemberDetailActivity";
    private SlideView mLastSlideViewWithStatusOn;//解锁密码的滑动删除布局
    List<PwdUnlockItem> pwdUnlockItems;
    TextView nickNameTv;
    ListViewCompat pwdUnlockView;//解锁密码项:有密码时显示
    RelativeLayout pwdSetRel, memberDelRel, keyRel;//添加密码,无密码时显示; 删除成员；授权
    RelativeLayout fpRel_0, fpRel_1, fpRel_2;//指纹
    TextView fpName_0, fpName_1, fpName_2;//指纹名字
    TextView fpBatchNo_0, fpBatchNo_1, fpBatchNo_2;//指纹名字
    TextView keyTv, keyLabel;//手机钥匙栏
    RelativeLayout fpAddRel;//添加指纹
    String memberId;//成员ID
    Device mDevice;
    MemberHelper memberHelper;//人员详情辅助类
    DataManageUtil dataManageUtil;
    String miAccount = "";//小米账号
    JSONArray fpArray = new JSONArray();//指纹列表
    String thePwdKey = "";
    String theFpKey = "";
    String theAccountKey = "";//钥匙
    String theNotifyKey = "";//接收推送

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_detail);
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(R.string.member_detail);
        initView();
    }

    private void initView() {
        this.dataManageUtil = new DataManageUtil(mDeviceStat, this);
        this.mDevice = Device.getDevice(mDeviceStat);
        memberId = getIntent().getStringExtra("memberId");
        fpRel_0 = (RelativeLayout) findViewById(R.id.fpRel_0);
        fpRel_1 = (RelativeLayout) findViewById(R.id.fpRel_1);
        fpRel_2 = (RelativeLayout) findViewById(R.id.fpRel_2);
        fpRel_0.setOnClickListener(this);
        fpRel_1.setOnClickListener(this);
        fpRel_2.setOnClickListener(this);
        fpName_0 = (TextView) findViewById(R.id.fpName_0);
        fpName_1 = (TextView) findViewById(R.id.fpName_1);
        fpName_2 = (TextView) findViewById(R.id.fpName_2);
        fpBatchNo_0 = (TextView) findViewById(R.id.fpBatchNo_0);
        fpBatchNo_1 = (TextView) findViewById(R.id.fpBatchNo_1);
        fpBatchNo_2 = (TextView) findViewById(R.id.fpBatchNo_2);

        fpAddRel = (RelativeLayout) findViewById(R.id.fpAddRel);
        fpAddRel.setOnClickListener(this);
        findViewById(R.id.title_bar_return).setOnClickListener(this);

        keyTv = (TextView)findViewById(R.id.keyTv);
        keyLabel = (TextView) findViewById(R.id.keyLabel);

        pwdSetRel = (RelativeLayout) findViewById(R.id.pwdSetRel);
        pwdSetRel.setOnClickListener(this);
        findViewById(R.id.nameRel).setOnClickListener(this);
        keyRel = (RelativeLayout) findViewById(R.id.keyRel);
        keyRel.setOnClickListener(this);
        memberDelRel = (RelativeLayout) findViewById(R.id.memberDelRel);
        memberDelRel.setOnClickListener(this);
        nickNameTv = (TextView) findViewById(R.id.memberNickNameTv);
        nickNameTv.setText(getIntent().getStringExtra("nickname"));
        memberHelper = new MemberHelper(activity(), mDeviceStat, pluginPackage(), memberId);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkMemberProperty();//根据有没有密码更新界面
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.nameRel:
                showNameEditDialog();
                break;
            case R.id.keyRel:
                Intent intent = new Intent();
                intent.putExtra("memberId", memberId);//member的id
                //根据标题进入不同的页面
                if(keyTv.getText().toString().equals(this.getResources().getString(R.string.member_add_key))){
                    startActivity(intent, KeySearchActivity.class.getName());
                }else{
                    intent.putExtra("miAccount", miAccount);//小米帐号传进去
                    intent.putExtra("nickName", nickNameTv.getText().toString());
                    startActivity(intent, KeyDetailActivity.class.getName());
                }
                break;
            case R.id.fpRel_0:
                Intent fpIntent_0 = new Intent();
                fpIntent_0.putExtra("memberId", memberId);
                fpIntent_0.putExtra("fpName", fpName_0.getText().toString());
                fpIntent_0.putExtra("fpBatchNo", fpBatchNo_0.getText().toString());
                startActivity(fpIntent_0, FpDelActivity.class.getName());
                break;
            case R.id.fpRel_1:
                Intent fpIntent_1 = new Intent();
                fpIntent_1.putExtra("memberId", memberId);
                fpIntent_1.putExtra("fpName", fpName_1.getText().toString());
                fpIntent_1.putExtra("fpBatchNo", fpBatchNo_1.getText().toString());
                startActivity(fpIntent_1, FpDelActivity.class.getName());
                break;
            case R.id.fpRel_2:
                Intent fpIntent_2 = new Intent();
                fpIntent_2.putExtra("memberId", memberId);
                fpIntent_2.putExtra("fpName", fpName_2.getText().toString());
                fpIntent_2.putExtra("fpBatchNo", fpBatchNo_2.getText().toString());
                startActivity(fpIntent_2, FpDelActivity.class.getName());
                break;
            case R.id.fpAddRel:
                Intent fppAddIntent = new Intent();
                fppAddIntent.putExtra("memberId", memberId);
                fppAddIntent.putExtra("nickName", nickNameTv.getText().toString());
                startActivity(fppAddIntent, FpInputActivity.class.getName());
                break;
            case R.id.memberDelRel:
                memberHelper.delFamilyMember();
                break;
            case R.id.pwdSetRel:
                Intent pwdSetIntent = new Intent();
                pwdSetIntent.putExtra("memberId", memberId);
                pwdSetIntent.putExtra("nickName", nickNameTv.getText().toString());
                pwdSetIntent.putExtra("title", getString(R.string.member_pwd_add_title));
                startActivity(pwdSetIntent, PwdSetActivity.class.getName());
                break;
            case R.id.delHolder:
                memberHelper.delPwd();
                break;
        }
    }

    @Override
    public void goToPwdModify() {
        Intent pwdUnlockIntent = new Intent();
        pwdUnlockIntent.putExtra("memberId", memberId);
        pwdUnlockIntent.putExtra("nickName", nickNameTv.getText().toString());
        pwdUnlockIntent.putExtra("title", "修改密码");
        startActivity(pwdUnlockIntent, PwdSetActivity.class.getName());
    }

    @Override
    public void onSlide(View view, int status) {
        if (mLastSlideViewWithStatusOn != null && mLastSlideViewWithStatusOn != view) {
            mLastSlideViewWithStatusOn.shrink();
        }

        if (status == SLIDE_STATUS_ON) {
            mLastSlideViewWithStatusOn = (SlideView) view;
        }
    }

    class PwdUnlockHolder {
        public TextView title;
        public ViewGroup deleteHolder;
        PwdUnlockHolder(View view) {
            title = (TextView) view.findViewById(R.id.title);
            deleteHolder = (ViewGroup)view.findViewById(R.id.delHolder);
        }
    }
    private class PwdUnlockAdapter extends BaseAdapter {

        private LayoutInflater mInflater;
        PwdUnlockAdapter() {
            mInflater = getLayoutInflater();
        }

        @Override
        public int getCount() {
            return pwdUnlockItems.size();
        }

        @Override
        public Object getItem(int position) {
            return pwdUnlockItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PwdUnlockHolder holder;
            SlideView slideView = (SlideView) convertView;
            if (slideView == null) {
                View itemView = mInflater.inflate(R.layout.pwd_unlock_item, null);

                slideView = new SlideView(MemberDetailActivity.this);
                slideView.setContentView(itemView);

                holder = new PwdUnlockHolder(slideView);
                slideView.setOnSlideListener(MemberDetailActivity.this);
                slideView.setTag(holder);
            } else {
                holder = (PwdUnlockHolder) slideView.getTag();
            }
            PwdUnlockItem item = pwdUnlockItems.get(position);
            item.slideView = slideView;
            item.slideView.shrink();

            holder.title.setText(item.title);
            holder.deleteHolder.setOnClickListener(MemberDetailActivity.this);

            return slideView;
        }

    }

    /**
     * 修改家人名字dialog
     */
    private void showNameEditDialog() {
        final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
        builder.setTitle("修改家人名称");
        builder.setInputView(nickNameTv.getText().toString(), true);
        builder.setPositiveButton(R.string.gloable_confirm, new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //确定
                if(!ZkUtil.isNetworkAvailable(activity())){
                    Toast.makeText(activity(), R.string.network_not_avilable, Toast.LENGTH_LONG).show();
                    return;
                }
                if(!TextUtils.isEmpty(builder.getInputView().getText().toString()) && TextUtils.isEmpty(builder.getInputView().getText().toString().trim())){//全是空格
                    Toast.makeText(activity(), R.string.member_name_empty, Toast.LENGTH_LONG).show();
                    return;
                }
                if(TextUtils.isEmpty(builder.getInputView().getText().toString())){//什么都没输入
                    CommonUtils.toast(activity(), "修改成功");
                    return;
                }
                updateMemberName(builder.getInputView().getText().toString().trim());//去空格
            }
        });
        builder.setNegativeButton("取消", new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();

        builder.getInputView().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String editable = builder.getInputView().getText().toString();
                String str = ZkUtil.stringFilter(editable.toString());
                if(str.length() > 20){
                    str = str.substring(0,20);
                }
                if(!editable.equals(str)){
                    builder.getInputView().setText(str);
                    //设置新的光标所在位置
                    builder.getInputView().setSelection(str.length());
                }
            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * 修改家人名字后，更新key name
     * @param newName
     */
    private void setKeyToNameAfterUpateMemberName(String newName){
        try{
            JSONObject settingsObj = new JSONObject();
            if(memberId.equals("")){
                JSONArray jsonArray = new JSONArray();
                jsonArray.put("keyid_master$pwd_data");
                jsonArray.put("keyid_master$fp_data");
                JSONObject resultObj = dataManageUtil.getDataFromLocal(jsonArray);


                if(resultObj.has("keyid_master$pwd_data") && !TextUtils.isEmpty(resultObj.getString("keyid_master$fp_data"))){
                    settingsObj.put("keyid_"+resultObj.getString("keyid_master$pwd_data")+"_ali", newName);
                }
                if(resultObj.has("keyid_master$fp_data") && !TextUtils.isEmpty(resultObj.getString("keyid_master$fp_data"))){
                    JSONArray fpArray = new JSONArray(resultObj.getString("keyid_master$fp_data"));
                    for(int i=0; i<fpArray.length(); i++){
                        JSONObject fpObj = fpArray.getJSONObject(i);
                        settingsObj.put("keyid_"+fpObj.getString("batchno")+"_ali", newName);
                    }
                }
            }else{
                JSONArray jsonArray = new JSONArray();
                Log.d(TAG, memberId);
                jsonArray.put("keyid_sm$pwd$"+memberId+"_data");
                jsonArray.put("keyid_sm$fp$"+memberId+"_data");
                jsonArray.put("keyid_sm$blekey$"+memberId+"_data");
                JSONObject resultObj = dataManageUtil.getDataFromLocal(jsonArray);

                Log.d(TAG, resultObj.toString());
                if(resultObj.has("keyid_sm$pwd$"+memberId+"_data") && !TextUtils.isEmpty(resultObj.getString("keyid_sm$pwd$"+memberId+"_data"))){
                    settingsObj.put("keyid_"+resultObj.getString("keyid_sm$pwd$"+memberId+"_data")+"_ali", newName);
                }
                if(resultObj.has("keyid_sm$fp$"+memberId+"_data") && !TextUtils.isEmpty(resultObj.getString("keyid_sm$fp$"+memberId+"_data"))){
                    JSONArray fpArray = new JSONArray(resultObj.getString("keyid_sm$fp$"+memberId+"_data"));
                    for(int i=0; i<fpArray.length(); i++){
                        JSONObject fpObj = fpArray.getJSONObject(i);
                        settingsObj.put("keyid_"+fpObj.getString("batchno")+"_ali", newName);
                    }
                }
                if(resultObj.has("keyid_sm$blekey$"+memberId+"_data") && !TextUtils.isEmpty(resultObj.getString("keyid_sm$blekey$"+memberId+"_data"))){
                    JSONObject blekeyObj = new JSONObject(resultObj.getString("keyid_sm$blekey$"+memberId+"_data"));
                    settingsObj.put("keyid_"+blekeyObj.getString("keyid")+"_ali", newName);
                }
            }

            dataManageUtil.setKeyToName(settingsObj);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }
    /**
     * 修改家人名字
     * @param newName
     */
    private void updateMemberName(final String newName){
        Log.d(TAG, "memberId: "+memberId);
        if(memberId.equals("")){//管理员
            try{
                JSONObject settingsObj = new JSONObject();
                settingsObj.put("keyid_master$nickname_data", newName);
                dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                    @Override
                    public void dataUpateFail(int i, String s) {
                        activity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                CommonUtils.toast(activity(), "修改失败");
                            }
                        });
                    }

                    @Override
                    public void dataUpdateSucc(String s) {
                        activity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                nickNameTv.setText(newName);
                                CommonUtils.toast(activity(), "修改成功");

                                setKeyToNameAfterUpateMemberName(newName);

                            }
                        });
                    }
                });
            }catch (JSONException je){
                je.printStackTrace();
            }
        }else{
            //查询服务器数据，存储进服务器
            JSONArray mJsArray = new JSONArray();
            mJsArray.put("keyid_smlist_data");
            dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
                @Override
                public void dataUpateFail(int i, String s) {
                    activity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity(), R.string.data_query_fail, Toast.LENGTH_LONG).show();
                        }
                    });
                }
                @Override
                public void dataUpdateSucc(String s) {
                    try{
                        JSONObject resultObj = new JSONObject(s);
                        JSONArray smArray = new JSONArray(resultObj.getString("keyid_smlist_data"));
                        Log.d(TAG, "smArray: "+smArray.toString());
                        boolean isMemberAvalid = false;//人员无效
                        for(int i=0; i<smArray.length(); i++){
                            if(smArray.getJSONObject(i).getString("id").equals(memberId)){
                                smArray.getJSONObject(i).put("nn", newName);
                                isMemberAvalid = true;
                                break;
                            }
                        }
                        if(!isMemberAvalid){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    CommonUtils.toast(activity(), "修改失败");
                                }
                            });
                            return;
                        }

                        JSONObject settingsObj = new JSONObject();
                        settingsObj.put("keyid_smlist_data", smArray.toString());
                        dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                            @Override
                            public void dataUpateFail(int i, String s) {
                                Log.d(TAG, "修改失败");
                                activity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CommonUtils.toast(activity(), "修改失败");
                                    }
                                });
                            }
                            @Override
                            public void dataUpdateSucc(String s) {
                                Log.d(TAG, "修改成功");
                                activity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        nickNameTv.setText(newName);
                                        CommonUtils.toast(activity(), "修改成功");
                                        setKeyToNameAfterUpateMemberName(newName);
                                    }
                                });
                            }
                        });
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    /**
     * 根据数据初始化界面
     */
    private void checkMemberProperty(){

        pwdUnlockView = (ListViewCompat) findViewById(R.id.pwdUnlockView);
        pwdUnlockItems = new ArrayList<PwdUnlockItem>();
        PwdUnlockItem item = new PwdUnlockItem();
        item.title = "密码";
        pwdUnlockItems.add(item);
        pwdUnlockView.setAdapter(new PwdUnlockAdapter());

        JSONArray mJsArray = new JSONArray();//数据查询参数
        if(memberId.equals("")){//管理员
            memberDelRel.setVisibility(View.GONE);//管理员隐藏删除按钮
            keyRel.setVisibility(View.GONE);//管理员隐藏授权按钮
            keyLabel.setVisibility(View.GONE);
            String theNickNameKey = "keyid_master$nickname_data";
            thePwdKey = "keyid_master$pwd_data";
            theFpKey = "keyid_master$fp_data";
            mJsArray.put(thePwdKey);
            mJsArray.put(theFpKey);
            mJsArray.put(theNickNameKey);
        }else{//家庭成员
            memberDelRel.setVisibility(View.VISIBLE);
            keyRel.setVisibility(View.VISIBLE);
            keyLabel.setVisibility(View.VISIBLE);
            thePwdKey = "keyid_sm$pwd$"+memberId+"_data";
            theFpKey = "keyid_sm$fp$"+memberId+"_data";
            theAccountKey = "keyid_sm$mi$account$"+memberId+"_data";
            theNotifyKey = "keyid_sm$mi$notify$"+memberId+"_data";
            mJsArray.put(thePwdKey);
            mJsArray.put(theFpKey);
            mJsArray.put(theAccountKey);
            mJsArray.put(theNotifyKey);
        }

        initMemberViewByLocal(mJsArray);
        initMemberViewByServer(mJsArray);
    }

    private void initMemberViewByServer(JSONArray mJsArray){
        try{
            dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
                @Override
                public void dataUpateFail(int i, String s) {

                }
                @Override
                public void dataUpdateSucc(String s) {
                    try {
                        final JSONObject memberObj = new JSONObject(s);
                        dataManageUtil.saveDataToLocal(memberObj);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "memberObj服务器: "+memberObj.toString());
                                initMemberView(memberObj);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initMemberViewByLocal(JSONArray mJsArray){
        try{
            JSONObject memberObj = dataManageUtil.getDataFromLocal(mJsArray);//查询多组数据
            Log.d(TAG, "memberObj本地: "+memberObj.toString());
            initMemberView(memberObj);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void initMemberView(JSONObject memberObj){
        fpRel_0.setVisibility(View.GONE);
        fpRel_1.setVisibility(View.GONE);
        fpRel_2.setVisibility(View.GONE);
        fpAddRel.setVisibility(View.VISIBLE);
        try{
            /* 手机钥匙布局加载 begin */
            if(memberObj.has(theAccountKey) && !memberObj.getString(theAccountKey).equals("")){//有钥匙
                miAccount = memberObj.getString(theAccountKey);
                keyTv.setTextColor(getResources().getColor(R.color.black));
                keyTv.setText("手机钥匙");
            }else{
                keyTv.setTextColor(getResources().getColor(R.color.colorPrimary));
                keyTv.setText(R.string.member_add_key);
            }
            /* 手机钥匙布局加载 end */

            /* 密码布局加载 begin */
            if(memberObj.has(thePwdKey) && !memberObj.getString(thePwdKey).equals("")){
                pwdSetRel.setVisibility(View.GONE);
                pwdUnlockView.setVisibility(View.VISIBLE);
            }else{
                pwdSetRel.setVisibility(View.VISIBLE);
                pwdUnlockView.setVisibility(View.GONE);
            }
            /* 密码布局加载 end */
            if(memberObj.has(theFpKey) && !memberObj.getString(theFpKey).equals("")){
                fpArray = new JSONArray(memberObj.getString(theFpKey));
            }

            //指纹布局加载
            if(fpArray.length() > 0){//设置过指纹了
                for(int m=0; m<fpArray.length(); m++){
                    JSONObject fpObj = fpArray.getJSONObject(m);
                    Log.d(TAG, "fp详情:"+fpObj.toString());
                    if(m == 0){
                        fpRel_0.setVisibility(View.VISIBLE);
                        fpName_0.setText(fpObj.getString("name"));
                        fpBatchNo_0.setText(fpObj.getString("batchno"));
                    }
                    if(m == 1){
                        fpRel_1.setVisibility(View.VISIBLE);
                        fpName_1.setText(fpObj.getString("name"));
                        fpBatchNo_1.setText(fpObj.getString("batchno"));
                    }
                    if(m == 2){
                        fpRel_2.setVisibility(View.VISIBLE);
                        fpName_2.setText(fpObj.getString("name"));
                        fpBatchNo_2.setText(fpObj.getString("batchno"));
                        findViewById(R.id.addDivider).setVisibility(View.VISIBLE);
                    }
                }
                if(fpArray.length() > 2){
                    findViewById(R.id.addDivider).setVisibility(View.GONE);
                    fpAddRel.setVisibility(View.GONE);
                }
            }else{
                findViewById(R.id.addDivider).setVisibility(View.GONE);
                fpRel_0.setVisibility(View.GONE);
                fpRel_1.setVisibility(View.GONE);
                fpRel_2.setVisibility(View.GONE);
                fpAddRel.setVisibility(View.VISIBLE);
            }
        }catch (JSONException e){
            e.printStackTrace();
        }


    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
