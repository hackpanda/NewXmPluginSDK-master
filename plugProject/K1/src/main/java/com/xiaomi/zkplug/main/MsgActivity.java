package com.xiaomi.zkplug.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.smarthome.device.api.Callback;
import com.xiaomi.smarthome.device.api.XmPluginHostApi;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.entity.LockMsg;
import com.xiaomi.zkplug.entity.MyEntity;
import com.xiaomi.zkplug.main.msg.MsgAdapter;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;
import com.xiaomi.zkplug.view.LoadMoreListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.zelkova.lockprotocol.BriefDate;

public class MsgActivity extends BaseActivity implements OnClickListener{

    private final String TAG = "MsgActivity";

    long timeDiffStart;//消息查询的启始事件
    Device mDevice;
    TextView mTitleView;
    private MsgAdapter adapter;
    private List<LockMsg> msgList = new ArrayList<LockMsg>();
    private LoadMoreListView listView;
    long lastTime = 0;//最后一条msg的time(秒)
    JSONObject msgLastObj = null;
    ImageView xialaImg;
    JSONArray mDeviceMemberArray = null;
    ImageView checkImg;//转圈img
    LinearLayout checkView;
    DataManageUtil dataManageUtil;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msg);
        // 初始化device
        mDevice = Device.getDevice(mDeviceStat);
        initView();
    }

    private void initView(){
        registerDeviceReceiver();
        this.dataManageUtil = new DataManageUtil(mDeviceStat, this);
        checkImg = (ImageView) findViewById(R.id.checkImg);
        checkView = (LinearLayout) findViewById(R.id.checkView);
        ZkUtil.startAnima(this, checkImg);//旋转
        xialaImg = (ImageView) findViewById(R.id.xialaImg);//下拉尖头
        xialaImg.setOnClickListener(this);
        mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(mDeviceStat.name);
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableBlackTranslucentStatus();
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.title_bar_more).setVisibility(View.GONE);
        initMsgView();
    }

    /**
     * 查找添加设备的时间，从前添加的不用解析了
     */
    protected void queryAddLockTime(){
        if(!ZkUtil.isNetworkAvailable(activity())) return;
        JSONArray mJsArray = new JSONArray();
        mJsArray.put("keyid_addLockTime_data");
        dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
            @Override
            public void dataUpateFail(int i, String s) {
                Toast.makeText(activity(), "数据查询失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void dataUpdateSucc(String s) {
                try{
                    JSONObject resultObj = new JSONObject(s);
                    Log.d("msg", "resultObj: "+resultObj.toString());
                    if(resultObj.has("keyid_addLockTime_data") && !TextUtils.isEmpty(resultObj.getString("keyid_addLockTime_data"))){
                        String addLockTime = resultObj.getString("keyid_addLockTime_data");
                        SimpleDateFormat sdFormat = new SimpleDateFormat(BriefDate.DATE_FORMAT);
                        Date theAddLockDate = null;
                        try {
                            theAddLockDate = sdFormat.parse(addLockTime);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        timeDiffStart = (theAddLockDate.getTime()/1000);
                        Log.d(TAG, "addLockTime: "+addLockTime + ", timeDiffStart: "+timeDiffStart);
                    }
                    getMsgList(timeDiffStart, System.currentTimeMillis()/1000, true);
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
        JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);

    }

    /**
     * 初始化listView
     */
    private void initMsgView(){
        mDeviceMemberArray = new JSONArray();
        JSONArray mJsArray = new JSONArray();
        mJsArray.put("keyid_master$nickname_data");
        mJsArray.put("keyid_master$pwd_data");
        mJsArray.put("keyid_master$fp_data");
        mJsArray.put("keyid_smlist_data");

        dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
            @Override
            public void dataUpateFail(int i, String s) {

                Toast.makeText(activity(), "数据查询失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void dataUpdateSucc(String s) {
                try{
                    JSONObject resultObj =  new JSONObject(s);
                    JSONObject masterMember = new JSONObject();
                    masterMember.put("memberType", "admin");
                    masterMember.put("nickName", resultObj.getString("keyid_master$nickname_data"));
                    if(resultObj.has("keyid_master$pwd_data") && !TextUtils.isEmpty(resultObj.getString("keyid_master$pwd_data"))){
                        masterMember.put("pwdIdx", resultObj.getString("keyid_master$pwd_data"));
                    }
                    if(resultObj.has("keyid_master$fp_data") && !TextUtils.isEmpty(resultObj.getString("keyid_master$fp_data"))){
                        masterMember.put("fpArray",new JSONArray(resultObj.getString("keyid_master$fp_data")));
                    }
                    mDeviceMemberArray.put(masterMember);
                    Log.d(TAG, "resultObj:"+resultObj.toString());
                    if(resultObj.has("keyid_smlist_data") && !TextUtils.isEmpty(resultObj.getString("keyid_smlist_data"))){
                        final JSONArray smArray = new JSONArray(resultObj.getString("keyid_smlist_data"));
                        if(smArray.length() == 0){
                            queryAddLockTime();
                            return;
                        }
                        JSONArray jsonArray = new JSONArray();//家人数据参数
                        for(int i=0; i<smArray.length(); i++){
                            String smid = smArray.getJSONObject(i).getString("id");
                            jsonArray.put("keyid_sm$pwd$"+smid+"_data");
                            jsonArray.put("keyid_sm$fp$"+smid+"_data");
                            jsonArray.put("keyid_sm$mi$account$"+smid+"_data");
                            jsonArray.put("keyid_sm$mi$blekey$"+smid+"_data");
                        }

                        dataManageUtil.queryDataFromServer(jsonArray, new DataUpdateCallback() {
                            @Override
                            public void dataUpateFail(int i, String s) {
                                Toast.makeText(activity(), "数据查询失败", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void dataUpdateSucc(String s) {
                                try{
                                    JSONObject smObj = new JSONObject(s);
                                    for(int m=0; m<smArray.length(); m++){
                                        String smid = smArray.getJSONObject(m).getString("id");
                                        JSONObject memberObj = new JSONObject();
                                        memberObj.put("memberType", "member");//家人
                                        memberObj.put("nickName", smArray.getJSONObject(m).getString("nn"));//家人
                                        if(smObj.has("keyid_sm$pwd$"+smid+"_data") && !TextUtils.isEmpty(smObj.getString("keyid_sm$pwd$"+smid+"_data"))){
                                            memberObj.put("pwdIdx", smObj.getString("keyid_sm$pwd$"+smid+"_data"));
                                        }
                                        if(smObj.has("keyid_sm$fp$"+smid+"_data") && !TextUtils.isEmpty(smObj.getString("keyid_sm$fp$"+smid+"_data"))){
                                            memberObj.put("fpArray", new JSONArray(smObj.getString("keyid_sm$fp$"+smid+"_data")));
                                        }
                                        JSONObject authObj = new JSONObject();
                                        authObj.put("userId", smObj.has("keyid_sm$mi$account$"+smid+"_data") ? smObj.getString("keyid_sm$mi$account$"+smid+"_data") : "");
                                        if(smObj.has("keyid_sm$mi$blekey$"+smid+"_data") && !TextUtils.isEmpty(smObj.getString("keyid_sm$mi$blekey$"+smid+"_data"))){
                                            JSONObject blekeyObj = new JSONObject(smObj.getString("keyid_sm$mi$blekey$"+smid+"_data"));
                                            authObj.put("keyId", blekeyObj.getString("keyid"));
                                        }
                                        authObj.put("notify", smObj.has("keyid_sm$mi$notify$"+smid+"_data") ? smObj.getString("keyid_sm$mi$notify$"+smid+"_data") : "");
                                        memberObj.put("auth", authObj);
                                        mDeviceMemberArray.put(memberObj);
                                        Log.d(TAG, "mDeviceMemberArray: "+mDeviceMemberArray.toString());
                                    }
                                    queryAddLockTime();
                                }catch (JSONException e){
                                    e.printStackTrace();
                                }
                            }
                        });
                    }else{
                        queryAddLockTime();
                        Log.d(TAG, "xxxxxxx家人列表为空bbb");
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
        LinearLayout emptyView= (LinearLayout)findViewById(R.id.emptyView);
        listView = (LoadMoreListView) findViewById(R.id.msgListView);
        listView.setEmptyView(emptyView);
        listView.setOnLoadMoreListener(new LoadMoreListView.OnLoadMoreListener() {
            @Override
            public void onloadMore() {
                getMsgList(timeDiffStart, lastTime-1, false);//加载更多
            }
        });
    }

    /**
     * 获取message信息
     */
    private void getMsgList(long timeDiffStart, long timeDiffSnd, final boolean firstRefresh){
        Log.d(TAG, timeDiffStart+", "+timeDiffSnd);
        XmPluginHostApi.instance().getUserDeviceData(MyEntity.MODEL_ID, mDeviceStat.did, MyEntity.EVENT_TYPE, MyEntity.EVENT_KEY, timeDiffStart, timeDiffSnd, new Callback<JSONArray>() {
            @Override
            public void onSuccess(JSONArray jsonArray) {
                try {
                    Log.d(TAG, "jsonArray条数:"+jsonArray.length());
                    if(jsonArray.length() == 0){
                        checkView.setVisibility(View.GONE);
                        Log.d(TAG, "jsonArray.length() == 0");
                        listView.setLoadCompleted();
                        return;
                    }

                    JSONArray msgJsArray = ZkUtil.transformLockEvent(jsonArray, mDeviceMemberArray);
                    Log.d(TAG, "消息条数:"+msgJsArray.length());
                    for(int i=0; i<msgJsArray.length(); i++){
                        JSONObject msgJsObj = msgJsArray.getJSONObject(i);
                        Log.d(TAG,"msgJsObj:" + msgJsObj.toString());

                        if(i > 0){
                            if(!msgJsObj.getString("opTime").substring(0, 10).equals(msgJsArray.getJSONObject(i-1).getString("opTime").substring(0, 10))){//新的一天
                                LockMsg lockMsg = new LockMsg();
                                lockMsg.setMsgDate(msgJsObj.getString("opTime").substring(0, 10));
                                msgList.add(lockMsg);
                            }

                        }else{
                            if(msgLastObj == null || !msgLastObj.getString("opTime").substring(0, 10).equals(msgJsArray.getJSONObject(0).getString("opTime").substring(0, 10))){//第一条
                                LockMsg lockMsg = new LockMsg();
                                lockMsg.setMsgDate(msgJsObj.getString("opTime").substring(0, 10));
                                msgList.add(lockMsg);
                            }
                        }
                        LockMsg lockMsg = new LockMsg();
                        lockMsg.setMsgTitle(msgJsObj.getString("msgTitle"));
                        lockMsg.setMsgContent(msgJsObj.getString("msgContent"));
                        lockMsg.setMsgType(msgJsObj.getInt("msgType"));
                        lockMsg.setMsgTime(msgJsObj.getString("opTime").substring(11));
                        msgList.add(lockMsg);
                    }
                    lastTime =  Long.parseLong(msgJsArray.getJSONObject(msgJsArray.length()-1).getString("time"));
                    msgLastObj = msgJsArray.getJSONObject(msgJsArray.length()-1);
                    Log.d(TAG, "解析完成");
                    if(firstRefresh){
                        adapter = new MsgAdapter(MsgActivity.this, msgList);
                        listView.setAdapter(adapter);
                    }

                    adapter.notifyDataSetChanged();
                    listView.setLoadCompleted();
                    ZkUtil.stopAnima(checkImg);
                    checkView.setVisibility(View.GONE);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "JSON异常");
                    ZkUtil.stopAnima(checkImg);
                    checkView.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(int i, String s) {
                Log.d(TAG, "消息读取失败"+s);
                ZkUtil.stopAnima(checkImg);
                checkView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterDeviceReceiver();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.xialaImg:
                finish();
                overridePendingTransition(R.anim.activity_open, R.anim.activity_close);
                break;
        }
    }

    /**
     * 插件页修改设备名字
     */
    private void registerDeviceReceiver() {
        if (mReceiver == null) {
            mReceiver = new PluginReceiver();
            IntentFilter filter = new IntentFilter("action.more.rename.notify");
            filter.addAction(XmBluetoothManager.ACTION_ONLINE_STATUS_CHANGED);
            registerReceiver(mReceiver, filter);
        }
    }

    private void unregisterDeviceReceiver() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private BroadcastReceiver mReceiver;

    private class PluginReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "action: "+intent.getAction());
            if (intent != null && "action.more.rename.notify".equals(intent.getAction())) {

                String name = intent.getStringExtra("extra.name");
                int result = intent.getIntExtra("extra.result", 0);
                Log.d(TAG, "miio-bluetooth"+String.format("name: %s, result = %d", name, result));
                mTitleView.setText(name);
            }
            if (intent != null && XmBluetoothManager.ACTION_ONLINE_STATUS_CHANGED.equals(intent.getAction())) {
                Log.d(TAG, "登录状态广播: 登录成功");
            }
        }
    }
}

