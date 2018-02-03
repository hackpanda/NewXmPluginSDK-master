package com.xiaomi.zkplug.fpdel;

import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.smarthome.common.ui.dialog.MLAlertDialog;
import com.xiaomi.smarthome.common.ui.dialog.XQProgressDialog;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.CommonUtils;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.entity.MyEntity;
import com.xiaomi.zkplug.lockoperater.ILockDataOperator;
import com.xiaomi.zkplug.lockoperater.LockOperateCallback;
import com.xiaomi.zkplug.lockoperater.OperatorBuilder;
import com.xiaomi.zkplug.lockoperater.SecureFpDel;
import com.xiaomi.zkplug.util.BitConverter;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import cn.zelkova.lockprotocol.BriefDate;
import cn.zelkova.lockprotocol.LockCmdFpDel;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：删除指纹
 */
public class FpDelActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "FpDelActivity";
    private final int MSG_FP_DEL_TIMEOUT = 0;//删除超时
    TextView fpNameTv, mTitleView;
    RelativeLayout fpNameRel;
    XQProgressDialog xqProgressDialog;
    String fpBatchNo;//指纹批次号
    Device mDevice;
    String memberId;
    DataManageUtil dataManageUtil;
    ILockDataOperator iLockDataOperator;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fp_del);
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(getIntent().getStringExtra("fpName"));
        fpNameTv = ((TextView) findViewById(R.id.fpNameTv));
        fpNameTv.setText(getIntent().getStringExtra("fpName"));
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.btnFpDel).setOnClickListener(this);
        fpNameRel = (RelativeLayout) findViewById(R.id.fpNameRel);
        fpNameRel.setOnClickListener(this);
        findViewById(R.id.btnFpDel).setOnClickListener(this);
        this.xqProgressDialog = new XQProgressDialog(FpDelActivity.this);
        this.dataManageUtil = new DataManageUtil(mDeviceStat, this);
        this.memberId = getIntent().getStringExtra("memberId");
        this.mDevice = Device.getDevice(mDeviceStat);
        this.fpBatchNo = getIntent().getStringExtra("fpBatchNo");
        this.iLockDataOperator = OperatorBuilder.create(SecureFpDel.FLAG, activity(), mDevice);
        iLockDataOperator.registerBluetoothReceiver();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.fpNameRel:
                showDialog();
                break;
            case R.id.btnFpDel:
                delFp();
                break;
        }
    }

    //点击了删除指纹
    private void delFp(){

        final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
        builder.setTitle("是否删除指纹？");
        builder.setPositiveButton("确定", new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //确定
                if(!ZkUtil.isNetworkAvailable(activity())){
                    CommonUtils.toast(activity(), "网络未连接，请确保网络畅通");
                    return;
                }
                if(!ZkUtil.isBleOpen()){
                    CommonUtils.toast(activity(), "请打开手机蓝牙");
                    return;
                }

                viewHanlder.sendEmptyMessageDelayed(MSG_FP_DEL_TIMEOUT, MyEntity.OPERATE_TIMEOUT);
                xqProgressDialog.setMessage("正在删除");
                xqProgressDialog.setCancelable(false);
                xqProgressDialog.show();
                if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
                    delFpInLock();//删除锁内指纹
                }else{
                    XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                        @Override
                        public void onResponse(int i, Bundle bundle) {
                            Log.d(TAG, "回调状态:"+XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()));
                            if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                                delFpInLock();//删除锁内指纹
                            }else if(i == XmBluetoothManager.Code.REQUEST_NOT_REGISTERED){
                                Toast.makeText(activity(), "设备已被重置，请解除绑定后重新添加", Toast.LENGTH_LONG).show();
                                xqProgressDialog.dismiss();
                                viewHanlder.removeMessages(MSG_FP_DEL_TIMEOUT);
                            }else {
                                xqProgressDialog.dismiss();
                                CommonUtils.toast(activity(), "未发现门锁，请靠近门锁重试");
                                viewHanlder.removeMessages(MSG_FP_DEL_TIMEOUT);
                            }

                        }
                    });
                }
            }
        });
        builder.setNegativeButton("取消", new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }
    //修改家人名字dialog
    private void showDialog() {
        final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
        builder.setTitle("修改指纹名称");
        builder.setInputView(fpNameTv.getText().toString(), true);
        builder.setPositiveButton("确定", new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //确定
                if(!ZkUtil.isNetworkAvailable(activity())){
                    CommonUtils.toast(activity(), "网络未连接，请确保网络畅通");
                    return;
                }
                if(!ZkUtil.isNetworkAvailable(activity())){
                    CommonUtils.toast(activity(), "网络未连接，请确保网络畅通");
                    return;
                }
                if(!TextUtils.isEmpty(builder.getInputView().getText().toString()) && TextUtils.isEmpty(builder.getInputView().getText().toString().trim())){//全是空格
                    CommonUtils.toast(activity(), "请输入指纹名称");
                    return;
                }
                if(builder.getInputView().getText().toString().trim().equals(fpNameTv.getText().toString())){
                    CommonUtils.toast(activity(), "指纹名已存在，请重新输入");
                    return;
                }
                if(TextUtils.isEmpty(builder.getInputView().getText().toString())){//什么都没输入
                    CommonUtils.toast(activity(), "修改成功");
                    return;
                }
                String theFpKey = "";
                if(memberId.equals("")){
                    theFpKey = "keyid_master$fp_data";
                }else{
                    theFpKey = "keyid_sm$fp$"+memberId+"_data";
                }
                try{
                    JSONArray mJsArray = new JSONArray();
                    mJsArray.put(theFpKey);
                    JSONArray fpArray = new JSONArray();//指纹列表
                    JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);//查询多组数据
                    if(resultObj.has(theFpKey) && !resultObj.getString(theFpKey).equals("")) {
                        fpArray = new JSONArray(resultObj.getString(theFpKey));
                        if(checkFpNameAvalid(fpArray, builder.getInputView().getText().toString().trim())){
                            updateFpInMemory(false, builder.getInputView().getText().toString().trim());//去空格
                        }
                    }
                }catch (JSONException e){
                    e.printStackTrace();
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

    /**
     * 检查指纹名称是否重复
     * @param fpList
     * @param newFpName
     * @return true:有效，false：无效
     * @throws JSONException
     */
    private boolean checkFpNameAvalid(JSONArray fpList, String newFpName) throws JSONException{
        Log.d(TAG, "fpList: "+fpList.toString());
        for(int m =0; m<fpList.length(); m++){
            JSONObject memberFp = fpList.getJSONObject(m);
            if(memberFp.getString("name").equals(newFpName)){
                Toast.makeText(activity(), "指纹名称已存在", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    /**
     * 指纹删除命令
     *
     * @param fpBatchId
     * @return
     */
    private LockCmdFpDel getFpDelCmd(int fpBatchId){
        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.MINUTE, -60);
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.MINUTE, 60);
        BriefDate vTo = BriefDate.fromNature(cld.getTime());
        byte[] macByte = BitConverter.convertMacAdd(mDevice.getMac());
        LockCmdFpDel lockCmdFpDel = new LockCmdFpDel(macByte, vFrom, vTo);
        Log.d(TAG, "fpBatchId:"+fpBatchId);
        lockCmdFpDel.setBatchId(fpBatchId);
        return lockCmdFpDel;
    }

    /**
     * 删除指纹in lock
     */
    private void delFpInLock(){
        iLockDataOperator.sendLockMsg(getFpDelCmd(Integer.parseInt(fpBatchNo)).getBytes(), new LockOperateCallback() {
            @Override
            public void lockOperateSucc(String value) {
                xqProgressDialog.dismiss();
                viewHanlder.removeMessages(MSG_FP_DEL_TIMEOUT);
                updateFpInMemory(true, "");
            }

            @Override
            public void lockOperateFail(String value) {
                viewHanlder.removeMessages(MSG_FP_DEL_TIMEOUT);
                xqProgressDialog.dismiss();
                if(value.equals("命令不在有效期内(3)")){
                    ZkUtil.showCmdTimeOutView(activity());
                }else{
                    CommonUtils.toast(activity(), value);
                }

            }
        });
    }

    /**
     * 删除服务器和本地存储的指纹
     */
    private void updateFpInMemory(final boolean isDel, final String newFpName){
        //查找到fplist
        try {
            String theFpKey = "";
            if(memberId.equals("")){
                theFpKey = "keyid_master$fp_data";
            }else{
                theFpKey = "keyid_sm$fp$"+memberId+"_data";
            }
            JSONArray mJsArray = new JSONArray();
            mJsArray.put(theFpKey);
            JSONArray fpArray = new JSONArray();//指纹列表
            JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);//查询多组数据
            if(resultObj.has(theFpKey) && !resultObj.getString(theFpKey).equals("")){
                fpArray = new JSONArray(resultObj.getString(theFpKey));
                for(int m =0; m<fpArray.length(); m++){
                    JSONObject memberFp = fpArray.getJSONObject(m);
                    if(memberFp.getString("batchno").equals(fpBatchNo)){
                        //找到了指纹，更新
                        if(isDel){
                            fpArray.remove(m);
                        }else{//修改名字
                            memberFp.put("name", newFpName);
                        }
                        break;
                    }
                }
                JSONObject settingsObj = new JSONObject();
                Log.d(TAG, "fpArray: "+fpArray.toString());
                settingsObj.put(theFpKey, fpArray.toString());
                dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                    @Override
                    public void dataUpdateSucc(String s) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(isDel){
                                    CommonUtils.toast(activity(), "删除成功");
                                    finish();
                                }else{
                                    fpNameTv.setText(newFpName);
                                    CommonUtils.toast(activity(), "修改成功");
                                }

                                xqProgressDialog.dismiss();

                            }
                        });
                    }
                    @Override
                    public void dataUpateFail(int i, final String s) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                CommonUtils.toast(activity(), s);
                                xqProgressDialog.dismiss();
                            }
                        });
                    }
                });

            }
        } catch (JSONException e) {
            e.printStackTrace();
            CommonUtils.toast(activity(), "数据解析异常");
        }
    }
    Handler viewHanlder = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FP_DEL_TIMEOUT:
                    xqProgressDialog.dismiss();
                    Log.d(TAG, "启动超时，执行断开:"+mDeviceStat.mac);
                    XmBluetoothManager.getInstance().disconnect(mDeviceStat.mac);
                    CommonUtils.toast(activity(), "未发现门锁，请靠近门锁重试");
                    Log.d(TAG, "超时未发现门锁");
                    break;

                default:
                    break;
            }
        }
    };

}