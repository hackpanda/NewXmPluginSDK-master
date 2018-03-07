package com.xiaomi.zkplug.member;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.common.ui.dialog.MLAlertDialog;
import com.xiaomi.smarthome.common.ui.dialog.XQProgressDialog;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.CommonUtils;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.entity.MyEntity;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：成员管理
 */
public class MemberManageActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener{

    private final String TAG = "MemberManage";
    TextView masterNickNameTv;
    ListView memListView;
    JSONArray smArray = new JSONArray();//此device下的家庭成员
    ImageView addMemberImg;//titlebar添加家人
    String adminId;//管理员Id
    XQProgressDialog xqProgressDialog;
    DataManageUtil dataManageUtil;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_manage);
        initContentView();
    }

    //titlebar初始化
    private void initTitalBar(){
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText("成员管理");
        addMemberImg = (ImageView) findViewById(R.id.title_bar_more);//没有add按钮，有more替换
        addMemberImg.setVisibility(View.VISIBLE);
        addMemberImg.setImageResource(R.drawable.titlebar_add_selector);
        addMemberImg.setOnClickListener(this);
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.ownerRel).setOnClickListener(this);
    }

    //content布局
    private void initContentView(){
        initTitalBar();
        dataManageUtil = new DataManageUtil(mDeviceStat, MemberManageActivity.this);
        masterNickNameTv = (TextView) findViewById(R.id.masterNickNameTv);
        memListView = (ListView) findViewById(R.id.memListView);
        memListView.setOnItemClickListener(this);
        TextView versionTv = (TextView) findViewById(R.id.versionTv);

        versionTv.setText(MyEntity.VERSION);
        if(!ZkUtil.isNetworkAvailable(this)){
            Toast.makeText(activity(), R.string.network_not_avilable, Toast.LENGTH_LONG).show();
            return;
        }

    }

    //修改完名字回来需要查询，所以放在onResume里面
    @Override
    public void onResume() {
        super.onResume();
        initMemberArray();//初始化家庭成员和管理员
    }

    //更新成员列表
    private void refreshMemberListView(){
        Log.d(TAG, "smArray-refresh: "+ smArray.toString());
        FamilyAdapter familyAdapter = new FamilyAdapter(this, smArray);
        memListView.setAdapter(familyAdapter);
        familyAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.ownerRel:
                Intent intent = new Intent();
                intent.putExtra("memberId", "");//管理员memberId设为空
                intent.putExtra("nickname", masterNickNameTv.getText().toString());
                startActivity(intent, MemberDetailActivity.class.getName());
                break;
            case R.id.title_bar_more:
                showDialog();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent();
        try {
            intent.putExtra("memberId", smArray.getJSONObject(position).getString("id"));
            intent.putExtra("nickname", smArray.getJSONObject(position).getString("nn"));
            startActivity(intent, MemberDetailActivity.class.getName());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    /**
     * 获取家庭成员列表
     * @return
     */
    protected void initMemberArray(){
        try{
            JSONArray mJsArray = new JSONArray();
            mJsArray.put("keyid_master$nickname_data");//里面放的是key
            mJsArray.put("keyid_smlist_data");//里面放的是key
            JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);
            masterNickNameTv.setText(resultObj.getString("keyid_master$nickname_data"));
            Log.d(TAG, "resultObj-local: "+resultObj.toString());
            if(resultObj.has("keyid_smlist_data") && !resultObj.get("keyid_smlist_data").equals("")){
                smArray = new JSONArray(resultObj.getString("keyid_smlist_data"));//本地存储的都成了string
                refreshMemberListView();
            }
            dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
                @Override
                public void dataUpdateSucc(final String s) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d(TAG, "成员列表:"+s);
                                JSONObject resultObj = new JSONObject(s);
                                masterNickNameTv.setText(resultObj.getString("keyid_master$nickname_data"));
                                if(resultObj.isNull("keyid_smlist_data")){
                                    smArray = new JSONArray();
                                    resultObj.put("keyid_smlist_data", "");
                                }else{
                                    smArray = new JSONArray(resultObj.getString("keyid_smlist_data"));
                                }
                                refreshMemberListView();
                                dataManageUtil.saveDataToLocal(resultObj);//保存
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                @Override
                public void dataUpateFail(int i, final String s) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity(),s, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }catch (JSONException je){
            je.printStackTrace();
        }
    }
    //添加家人dialog
    private void showDialog() {
        if(smArray.length() == 15){
            CommonUtils.toast(activity(), "当前已添加15个成员，不可再添加");
            return;
        }
        final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
        builder.setTitle("添加成员");
        builder.setInputView("成员姓名", true);
        builder.setPositiveButton(R.string.gloable_confirm, new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //确定
                if(!ZkUtil.isNetworkAvailable(activity())){
                    Toast.makeText(activity(), R.string.network_not_avilable, Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    if(TextUtils.isEmpty(builder.getInputView().getText().toString().trim())){
                        CommonUtils.toast(activity(), "请输入姓名");
                        return;
                    }
                    xqProgressDialog = new XQProgressDialog(activity());
                    xqProgressDialog.setMessage("正在添加");
                    xqProgressDialog.setCancelable(false);
                    xqProgressDialog.show();

                    JSONObject smJsObj = new JSONObject();//家庭成员
                    String smid = ZkUtil.generateTrackId(10);
                    String nickname = builder.getInputView().getText().toString().trim();
                    smJsObj.put("id", smid);
                    smJsObj.put("nn", nickname);
                    smArray.put(smJsObj);
                    Log.d(TAG, "smArray: "+smArray.toString());
                    JSONObject settingsObj = new JSONObject();
                    settingsObj.put("keyid_smlist_data", smArray.toString());//存储时，value只能是string
                    dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                        @Override
                        public void dataUpateFail(int i, final String s) {
                            Log.d(TAG, "添加成员失败");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    initMemberArray();//添加失败后重新初始化smArray列表
                                    CommonUtils.toast(activity(), s);
                                    xqProgressDialog.dismiss();
                                }
                            });
                        }
                        @Override
                        public void dataUpdateSucc(String s) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshMemberListView();
                                    CommonUtils.toast(activity(), "添加成功");
                                    xqProgressDialog.dismiss();
                                }
                            });

                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    CommonUtils.toast(activity(), "数据格式解析失败");
                    xqProgressDialog.dismiss();
                }
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
}