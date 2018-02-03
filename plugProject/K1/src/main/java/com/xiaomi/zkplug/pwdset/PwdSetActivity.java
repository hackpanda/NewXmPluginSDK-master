package com.xiaomi.zkplug.pwdset;

import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.xiaomi.zkplug.lockoperater.SecurePwdAdd;
import com.xiaomi.zkplug.lockoperater.SecurePwdUpdate;
import com.xiaomi.zkplug.util.BitConverter;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import cn.zelkova.lockprotocol.BriefDate;
import cn.zelkova.lockprotocol.LockCmdSyncPwd;

public class PwdSetActivity extends BaseActivity implements View.OnClickListener {

    private final String TAG = "PwdSetActivity";
    private static final int MSG_PWD_SET_TIMEOUT = 0x1001;
    ImageView pwdToggleImg;//控制密码的显示与隐藏
    EditText pwdEdit;//密码
    TextView pwdWarnTv;
    TextView mTitleView;
    String nickName;
    String memberId;
    /*
    * 密码显示隐藏的标示
    * */
    private final int YIN_CANG = 0;
    private final int XIAN_SHI = 1;
    XQProgressDialog xqProgressDialog;//等待dialog
    ILockDataOperator iLockUpdateOperator;
    ILockDataOperator iLockAddOperator;
    Device mDevice;

    DataManageUtil dataManageUtil;
    private int retryCount;//服务器存储失败后重试次数
    LockCmdSyncPwd lockCmdSyncPwd;//锁命令
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pwd_set);
        initView();
    }

    //布局初始化
    private void initView(){
        dataManageUtil = new DataManageUtil(mDeviceStat, this);
        memberId = getIntent().getStringExtra("memberId");
        this.mDevice = Device.getDevice(mDeviceStat);
        nickName = getIntent().getStringExtra("nickName");
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(getIntent().getStringExtra("title"));//传来的title
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.pwdSetBtn).setOnClickListener(this);
        pwdToggleImg = (ImageView) findViewById(R.id.pwdToggleImg);
        pwdToggleImg.setOnClickListener(this);
        pwdToggleImg.setTag(YIN_CANG);
        pwdEdit = (EditText) findViewById(R.id.pwdEdit);
        pwdWarnTv = (TextView) findViewById(R.id.pwdWarnTv);
        iLockAddOperator = OperatorBuilder.create(SecurePwdAdd.FLAG, activity(), mDevice);
        iLockUpdateOperator = OperatorBuilder.create(SecurePwdUpdate.FLAG, activity(), mDevice);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(iLockAddOperator != null){
            iLockAddOperator.unregisterBluetoothReceiver();
        }
        if(iLockUpdateOperator != null){
            iLockUpdateOperator.unregisterBluetoothReceiver();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.pwdSetBtn:
                pwdWarnTv.setText("");
                if(!pwdCheck()){
                    return;
                }
                viewHanlder.sendEmptyMessageDelayed(MSG_PWD_SET_TIMEOUT, MyEntity.OPERATE_TIMEOUT);
                xqProgressDialog = new XQProgressDialog(PwdSetActivity.this);
                xqProgressDialog.setMessage("正在设置");
                xqProgressDialog.setCancelable(false);
                xqProgressDialog.show();

                retryCount = 0;//每次点击按钮都置位
                pwdWarnTv.setVisibility(View.GONE);
                dataManageUtil.checkMemberAvaliable(memberId, new DataUpdateCallback() {
                    @Override
                    public void dataUpateFail(int i, final String s) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                xqProgressDialog.dismiss();
                                Toast.makeText(activity(), s, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    @Override
                    public void dataUpdateSucc(final String s) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(s.equals("true")){//有效人员
                                    if(getIntent().getStringExtra("title").equals("设置密码")){//设置密码
                                        if(iLockAddOperator == null){
                                            iLockAddOperator = OperatorBuilder.create(SecurePwdAdd.FLAG, activity(), mDevice);
                                        }
                                        if(iLockUpdateOperator != null){
                                            iLockUpdateOperator.unregisterBluetoothReceiver();
                                        }
                                        iLockAddOperator.registerBluetoothReceiver();
                                        lockCmdSyncPwd = getPwdSetCmd();

                                        setMemberPwd(iLockAddOperator);
                                    }else{
                                        //修改密码
                                        try{
                                            if(iLockUpdateOperator == null){
                                                iLockUpdateOperator = OperatorBuilder.create(SecurePwdUpdate.FLAG, activity(), mDevice);
                                            }
                                            if(iLockAddOperator != null){
                                                iLockAddOperator.unregisterBluetoothReceiver();
                                            }
                                            iLockUpdateOperator.registerBluetoothReceiver();
                                            String pwdIdx = "";
                                            if(memberId.equals("")){
                                                JSONArray mJsArray = new JSONArray();
                                                mJsArray.put("keyid_master$pwd_data");
                                                JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);
                                                pwdIdx = resultObj.getString("keyid_master$pwd_data");
                                            }else{
                                                JSONArray mJsArray = new JSONArray();
                                                mJsArray.put("keyid_sm$pwd$"+memberId+"_data");
                                                JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);
                                                pwdIdx = resultObj.getString("keyid_sm$pwd$"+memberId+"_data");
                                            }

                                            Log.d(TAG, pwdIdx+"-- pwdidx short: "+Integer.parseInt(pwdIdx));
                                            lockCmdSyncPwd = getPwdUpdateCmd(Integer.parseInt(pwdIdx));

                                            Log.d(TAG, "PwdDel锁命令:"+cn.zelkova.lockprotocol.BitConverter.toHexString(lockCmdSyncPwd.getBytes()));
                                            iLockUpdateOperator.registerBluetoothReceiver();
                                            setMemberPwd(iLockUpdateOperator);
                                        }catch (JSONException e){
                                            e.printStackTrace();
                                            CommonUtils.toast(activity(), "数据解析异常");
                                        }
                                    }
                                }else{
                                    Toast.makeText(activity(), "成员已被删除", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    }
                });

                break;
            case R.id.pwdToggleImg:
                if((int)pwdToggleImg.getTag() == YIN_CANG){
                    pwdToggleImg.setBackgroundResource(R.drawable.icon_xianshi);
                    pwdToggleImg.setTag(XIAN_SHI);
                    pwdEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }else{
                    pwdToggleImg.setBackgroundResource(R.drawable.icon_yincang);
                    pwdToggleImg.setTag(YIN_CANG);
                    pwdEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
                break;
        }
    }

    /*
    * 密码规则检查
    * */
    private boolean pwdCheck(){
        if(!ZkUtil.isBleOpen()){
            CommonUtils.toast(activity(), "请打开手机蓝牙");
            return false;
        }

        if(pwdEdit.getText().toString().isEmpty()){
            pwdWarnTv.setText("请输入密码");
            pwdWarnTv.setVisibility(View.VISIBLE);
            return false;
        }
        if(pwdEdit.getText().toString().length() < 6){
            pwdWarnTv.setText("请输入6~8位数字");
            pwdWarnTv.setVisibility(View.VISIBLE);
            return false;
        }
        if(!ZkUtil.isNetworkAvailable(this)){
            Toast.makeText(activity(), "网络未连接，请确保网络畅通", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;//符合规则
    }


    /*
    * 添加密码
    * */
    private LockCmdSyncPwd getPwdSetCmd(){

        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.MINUTE, -60);
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.MINUTE, 180);
        BriefDate vTo = BriefDate.fromNature(cld.getTime());

        String pwdToAdd = this.pwdEdit.getText().toString().trim();

        cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, -60);
        BriefDate pwdVFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.YEAR, 100);
        BriefDate pwdVTo = BriefDate.fromNature(cld.getTime());
        Log.d(TAG, "mDevice.getMac():"+mDevice.getMac()+", "+BitConverter.convertMacAdd(mDevice.getMac()));

        LockCmdSyncPwd cmdSyncPwd = new LockCmdSyncPwd(BitConverter.convertMacAdd(mDevice.getMac()), vFrom, vTo);

        if (pwdToAdd.length() > 0) {
            cmdSyncPwd.addNewPwd(pwdToAdd, pwdVFrom, pwdVTo);
        }
        return cmdSyncPwd;
    }

    /*
    * 修改密码
    * pwdIdxToDel：要删除的密码别名
    * */
    private LockCmdSyncPwd getPwdUpdateCmd(int pwdIdxToDel){

        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, -60);
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, 180);
        BriefDate vTo = BriefDate.fromNature(cld.getTime());

        String pwdToAdd = this.pwdEdit.getText().toString().trim();

        cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, -60);
        BriefDate pwdVFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.YEAR, 100);
        BriefDate pwdVTo = BriefDate.fromNature(cld.getTime());


        Log.d(TAG, "mDevice.getMac():"+mDevice.getMac()+", "+BitConverter.convertMacAdd(mDevice.getMac()));

        LockCmdSyncPwd cmdSyncPwd = new LockCmdSyncPwd(BitConverter.convertMacAdd(mDevice.getMac()), vFrom, vTo);

        if (pwdToAdd.length() > 0) {
            cmdSyncPwd.updatePwd(pwdToAdd, pwdIdxToDel, pwdVFrom, pwdVTo);
        }
        return cmdSyncPwd;
    }

    /*
    * 更新家人密码
    * */
    private void setMemberPwd(final ILockDataOperator lockDataOperator){

        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            operateLockMsg(lockDataOperator, lockCmdSyncPwd);
            Log.d(TAG, "已连接，直接发送锁命令");
        } else {
            Log.d(TAG, "未连接，先连接");
            XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                @Override
                public void onResponse(int i, Bundle bundle) {
                    Log.d(TAG, "回调状态:"+XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()));
                    if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                        operateLockMsg(lockDataOperator, lockCmdSyncPwd);
                    }else if(i == XmBluetoothManager.Code.REQUEST_NOT_REGISTERED){
                        showResultView("设备已被重置，请解除绑定后重新添加");
                    }
                }
            });
        }
    }

    //发送锁命令,并更新数据
    private void operateLockMsg(ILockDataOperator lockDataOperator, LockCmdSyncPwd lockCmdSyncPwd){
        lockDataOperator.sendLockMsg(lockCmdSyncPwd.getBytes(), new LockOperateCallback() {
            @Override
            public void lockOperateSucc(String pwdIdx) {
                viewHanlder.removeMessages(MSG_PWD_SET_TIMEOUT);
                //更新数据到服务器
                savePwdIdxToServer(pwdIdx);//value是传过来的密码别名
            }

            @Override
            public void lockOperateFail(String value) {
                viewHanlder.removeMessages(MSG_PWD_SET_TIMEOUT);
                if(value.equals("命令不在有效期内(3)")){
                    ZkUtil.showCmdTimeOutView(PwdSetActivity.this);
                }else{
                    Toast.makeText(activity(), value, Toast.LENGTH_LONG).show();
                }
                showResultView("");
            }
        });
    }

    /**
     * 更新密码数据到服务器
     *
     * @param pwdIdx
     */
    private void savePwdIdxToServer(final String pwdIdx){
        //存储密码
        try {
            JSONObject settingsObj = new JSONObject();
            if(memberId.equals("")){//管理员
                settingsObj.put("keyid_master$pwd_data", pwdIdx);
            }else{
                settingsObj.put("keyid_sm$pwd$"+memberId+"_data", pwdIdx);
            }
            dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                @Override
                public void dataUpdateSucc(String s) {
                    Log.d(TAG, "设置成功");
                    viewHanlder.removeMessages(MSG_PWD_SET_TIMEOUT);
                    xqProgressDialog.dismiss();
                    //设置key与name的对应关系
                    dataManageUtil.setKeyToName(pwdIdx, getIntent().getStringExtra("nickName"));
                    showPwdSuccDialog();
                }
                @Override
                public void dataUpateFail(int i, String s) {
                    retryCount+=1;
                    Log.d(TAG, "设置失败, retryCount: "+retryCount);
                    if(retryCount > 3){
                        showResultView(s);
                    }else{
                        savePwdIdxToServer(pwdIdx);
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 谁成功后弹窗
     */
    private void showPwdSuccDialog(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
                builder.setTitle("设置成功");
                builder.setCancelable(false);
                builder.setMessage("请尝试用密码开一次锁");
                builder.setNeutralButton("知道了", new MLAlertDialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                builder.show();
            }
        });

    }
    private void showResultView(final String s){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                xqProgressDialog.dismiss();
                if(!TextUtils.isEmpty(s)){
                    CommonUtils.toast(activity(), s);
                }
                XmBluetoothManager.getInstance().disconnect(mDeviceStat.mac);
                Log.d(TAG, "超时未发现门锁, 执行断开");
                viewHanlder.removeMessages(MSG_PWD_SET_TIMEOUT);
            }
        });
    }

    /**
     * 时序: 用于界面控制
     */
    Handler viewHanlder = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PWD_SET_TIMEOUT:
                    showResultView("未发现门锁，请靠近门锁重试");
                    break;
                default:
                    break;
            }
        }
    };
}
