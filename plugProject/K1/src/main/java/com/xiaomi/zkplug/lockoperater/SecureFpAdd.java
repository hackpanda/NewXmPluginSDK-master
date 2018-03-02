package com.xiaomi.zkplug.lockoperater;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.zkplug.CommonUtils;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.entity.MyEntity;
import com.xiaomi.zkplug.entity.WrongCode;
import com.xiaomi.zkplug.fpinput.FpInputHelper;
import com.xiaomi.zkplug.util.BitConverter;

import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Collections;
import java.util.UUID;

import cn.zelkova.lockprotocol.BriefDate;
import cn.zelkova.lockprotocol.LockCmdFpConfirm;
import cn.zelkova.lockprotocol.LockCommBase;
import cn.zelkova.lockprotocol.LockCommFpAdd;
import cn.zelkova.lockprotocol.LockCommFpAddResponse;
import cn.zelkova.lockprotocol.LockFormatException;

/**
 * 作者：liwenqi on 17/7/19 17:25
 * 邮箱：liwenqi@zelkova.cn
 * 描述：录入指纹
 */

public class SecureFpAdd implements ILockDataOperator {

    private final String TAG = "SecureFpInput";
    public static final int FLAG = 5;
    ImageView fpImg;//指纹动画
    private BroadcastReceiver mBluetoothReceiver;
    ArrayDeque<Byte> inputBuffer = new ArrayDeque<Byte>();//缓存返回数据
    LockOperateCallback lockOperateCallback;//操作锁的回调
    int index = 0;//拆包起始index
    Activity activity;
    Device mDevice;
    int fpBatchId;//指纹批次号
    /**
     * @param activity
     * @param mDevice
     * 描述：赋值, 用于设备和Toast提示的activity
     */
    public SecureFpAdd(Activity activity, Device mDevice) {
        this.activity = activity;
        this.mDevice = mDevice;
        this.fpImg = (ImageView) activity.findViewById(R.id.fpImg);

    }
    /*
    * 指纹确认
    * */
    private LockCmdFpConfirm getFpConfirmCmd(){
        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.MINUTE, -60);
        Log.d(TAG, "Calendar start:"+BriefDate.fromNature(cld.getTime()));
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.YEAR, 20);
        Log.d(TAG, "Calendar end:"+BriefDate.fromNature(cld.getTime()));
        BriefDate vTo = BriefDate.fromNature(cld.getTime());
        byte[] macByte = BitConverter.convertMacAdd(mDevice.getMac());
        LockCmdFpConfirm lockCmdFpConfirm = new LockCmdFpConfirm(macByte, vFrom, vTo);
        Log.d(TAG, "fpBatchId:"+fpBatchId);
        lockCmdFpConfirm.setFpConfirm(fpBatchId, vFrom, vTo);
        return lockCmdFpConfirm;
    }
    @Override
    public void sendLockMsg(final byte[] cmd, final LockOperateCallback lockOperateCallback) {
        inputBuffer.clear();//清空缓存
        this.lockOperateCallback = lockOperateCallback;
        XmBluetoothManager.getInstance().securityChipEncrypt(mDevice.getMac(), cmd, new Response.BleReadResponse() {
            @Override
            public void onResponse(int i, byte[] bytes) {//bytes加密后的数据
                if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                    index = 0;//置位
                    Log.d(TAG, "cmdData:"+cn.zelkova.lockprotocol.BitConverter.toHexString(bytes));

                    LockCommFpAdd lockCommFpAdd = new LockCommFpAdd(bytes);//封装进锁通讯
                    byte[] commBytes = lockCommFpAdd.getBytes();//锁通讯命令
                    Log.d(TAG, "commData:"+cn.zelkova.lockprotocol.BitConverter.toHexString(bytes));
                    do{
                        //分包begin
                        byte[] surplusData = new byte[commBytes.length - index];
                        byte[] currentData;
                        System.arraycopy(commBytes, index, surplusData, 0, commBytes.length - index);
                        if (surplusData.length <= 20) {
                            currentData = new byte[surplusData.length];
                            System.arraycopy(surplusData, 0, currentData, 0, surplusData.length);
                            index += surplusData.length;
                            Log.d(TAG, "currentData:"+cn.zelkova.lockprotocol.BitConverter.toHexString(currentData));
                            XmBluetoothManager.getInstance().write(mDevice.getMac(), MyEntity.RX_SERVICE_UUID, MyEntity.RX_CHAR_UUID, currentData, new Response.BleWriteResponse() {
                                @Override
                                public void onResponse(int code, Void aVoid) {
                                    if (code == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                                        Log.d(TAG, "写数据成功："+code+", "+inputBuffer.size());
                                        XmBluetoothManager.getInstance().notify(mDevice.getMac(), MyEntity.RX_SERVICE_UUID,MyEntity.TX_CHAR_UUID, null);
                                    } else {
                                        CommonUtils.toast(activity, "写数据失败，code:"+code);
                                    }
                                }
                            });
                        } else {
                            currentData = new byte[20];
                            System.arraycopy(commBytes, index, currentData, 0, 20);
                            index += 20;
                            Log.d(TAG, "currentData:"+cn.zelkova.lockprotocol.BitConverter.toHexString(currentData));

                            XmBluetoothManager.getInstance().write(mDevice.getMac(), MyEntity.RX_SERVICE_UUID, MyEntity.RX_CHAR_UUID, currentData, new Response.BleWriteResponse() {
                                @Override
                                public void onResponse(int code, Void aVoid) {
                                    if (code == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                                        //正在发送
                                        Log.d(TAG, "数据发送中");
                                    } else {
                                        Log.d(TAG, "数据发送失败");
                                    }
                                }
                            });
                        }
                        //分包end

                    }while(index < commBytes.length);

                } else {
                    Log.d(TAG, "通讯数据加密失败:"+i);
                    lockOperateCallback.lockOperateFail("通讯数据加密失败:"+i);
                }
            }
        });
    }
    @Override
    public void registerBluetoothReceiver() {
        if (mBluetoothReceiver == null) {
            mBluetoothReceiver = new BluetoothChangeReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(XmBluetoothManager.ACTION_CONNECT_STATUS_CHANGED);
            filter.addAction(XmBluetoothManager.ACTION_CHARACTER_CHANGED);
            activity.registerReceiver(mBluetoothReceiver, filter);
            Log.d(TAG, "注册广播");
        }
    }

    @Override
    public void unregisterBluetoothReceiver() {
        if (mBluetoothReceiver != null) {
            activity.unregisterReceiver(mBluetoothReceiver);
            mBluetoothReceiver = null;
        }
    }

    private class BluetoothChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null
                    || !intent.hasExtra(XmBluetoothManager.KEY_DEVICE_ADDRESS)) {
                return;
            }

            String mac = intent.getStringExtra(XmBluetoothManager.KEY_DEVICE_ADDRESS);
            if (TextUtils.equals(mac, mDevice.getMac())) {
                String action = intent.getAction();
                if (TextUtils.equals(action, XmBluetoothManager.ACTION_CONNECT_STATUS_CHANGED)) {
                    processConnectStatusChanged(intent);
                } else if (TextUtils.equals(action, XmBluetoothManager.ACTION_CHARACTER_CHANGED)) {
                   processCharacterChanged(intent);
                }
            }
        }
    }

    private void processConnectStatusChanged(Intent intent) {
        int status = intent.getIntExtra(XmBluetoothManager.KEY_CONNECT_STATUS, 0);
        if (status == XmBluetoothManager.STATUS_CONNECTED) {
            Log.d(TAG, "已连接上");
            XmBluetoothManager.getInstance().notify(mDevice.getMac(), MyEntity.RX_SERVICE_UUID,MyEntity.TX_CHAR_UUID, null);
        } else if (status == XmBluetoothManager.STATUS_DISCONNECTED) {
            Log.d(TAG, "未连接上 ");
        }
    }

    private void processCharacterChanged(Intent intent) {
        UUID service = (UUID) intent.getSerializableExtra(XmBluetoothManager.KEY_SERVICE_UUID);
        UUID character = (UUID) intent.getSerializableExtra(XmBluetoothManager.KEY_CHARACTER_UUID);
        final byte[] value = intent.getByteArrayExtra(XmBluetoothManager.KEY_CHARACTER_VALUE);
        if (MyEntity.RX_SERVICE_UUID.equals(service) && MyEntity.TX_CHAR_UUID.equals(character)) {
            Collections.addAll(inputBuffer, BitConverter.toPackaged(value));
            final byte[] buffer = BitConverter.toPrimitive(inputBuffer.toArray(new Byte[0]));
            try{
                LockCommFpAddResponse lockCommFpAddResponse = (LockCommFpAddResponse) LockCommBase.parse(inputBuffer);
                if(lockCommFpAddResponse == null){
                    Log.d(TAG, "lockCommBase == null");
                    return;
                }
                Log.d(TAG, "锁返回数据:"+BitConverter.toHexString(buffer));
                Collections.addAll(inputBuffer, BitConverter.toPackaged(buffer));
                Log.d(TAG, "指纹命令进锁成功:inputBuffer.size()"+inputBuffer.size());
                lockCommFpAddResponse = (LockCommFpAddResponse) LockCommBase.parse(inputBuffer);
                if(inputBuffer.size() == 38) inputBuffer.clear();
                if(lockCommFpAddResponse.getResultCode() != 0){
                    FpInputHelper.getXqProgressDialog().dismiss();
                    Log.d(TAG, "错误码："+lockCommFpAddResponse.getResultCode());
                    if(lockCommFpAddResponse.getResultCode() == 3){
                        LinearLayout fpTipLayout = (LinearLayout) activity.findViewById(R.id.fpTipLayout);
                        if(fpTipLayout.getVisibility() == View.GONE){
                            lockOperateCallback.lockOperateFail("超时未放置手指，请重新添加");
                        }else{
                            lockOperateCallback.lockOperateFail(WrongCode.get(String.valueOf(lockCommFpAddResponse.getResultCode())));
                        }

                    }else if(lockCommFpAddResponse.getResultCode() == 0x17){//0x17
                        //什么都不要做，不要退出
                    }else{
                        lockOperateCallback.lockOperateFail(WrongCode.get(String.valueOf(lockCommFpAddResponse.getResultCode())));
                    }
                }else{
                    Log.d(TAG, "请录入指纹getLuruNum():"+lockCommFpAddResponse.getLuruNum());
                    if(lockCommFpAddResponse.getLuruNum() == 0){
                        FpInputHelper.getXqProgressDialog().dismiss();
                        LinearLayout fpTipLayout = (LinearLayout) activity.findViewById(R.id.fpTipLayout);
                        fpTipLayout.setVisibility(View.GONE);
                        LinearLayout fpInputBuzhouLayout = (LinearLayout) activity.findViewById(R.id.fpInputBuzhouLayout);
                        fpInputBuzhouLayout.setVisibility(View.VISIBLE);
                        fpImg.setImageResource(R.drawable.fp_input_animation_1);
                    }else if(lockCommFpAddResponse.getLuruNum() == 1){
                        AnimationDrawable animationDrawable = (AnimationDrawable) fpImg.getDrawable();
                        animationDrawable.start();
                    }else if(lockCommFpAddResponse.getLuruNum() == 2){
                        fpImg.setImageResource(R.drawable.fp_input_animation_2);
                        AnimationDrawable animationDrawable = (AnimationDrawable) fpImg.getDrawable();
                        animationDrawable.start();
                    }else if(lockCommFpAddResponse.getLuruNum() == 3){
                        fpImg.setImageResource(R.drawable.fp_input_animation_3);
                        AnimationDrawable animationDrawable = (AnimationDrawable) fpImg.getDrawable();
                        animationDrawable.start();
                    }else if(lockCommFpAddResponse.getLuruNum() == 4){
                        TextView fpPutTv = (TextView) activity.findViewById(R.id.fpPutTv);
                        fpPutTv.setText("采集手指边缘指纹");
                        TextView fpCompleteTv = (TextView) activity.findViewById(R.id.fpCompleteTv);
                        fpCompleteTv.setText("将手指边缘按在指纹头上再抬起，重复此步骤");
                        fpImg.setImageResource(R.drawable.fp_input_animation_4);
                        AnimationDrawable animationDrawable = (AnimationDrawable) fpImg.getDrawable();
                        animationDrawable.start();

                    }else if(lockCommFpAddResponse.getLuruNum() == 5){
                        fpImg.setImageResource(R.drawable.fp_input_animation_5);
                        AnimationDrawable animationDrawable = (AnimationDrawable) fpImg.getDrawable();
                        animationDrawable.start();
                    }else if(lockCommFpAddResponse.getLuruNum() == 6){
                        fpImg.setImageResource(R.drawable.fp_input_animation_6);
                        AnimationDrawable animationDrawable = (AnimationDrawable) fpImg.getDrawable();
                        animationDrawable.start();
                    }else if(lockCommFpAddResponse.getLuruNum() == 7){
                        fpImg.setImageResource(R.drawable.fp_input_animation_7);
                        AnimationDrawable animationDrawable = (AnimationDrawable) fpImg.getDrawable();
                        animationDrawable.start();
                    }else if(lockCommFpAddResponse.getLuruNum() == 8){
                        fpImg.setImageResource(R.drawable.fp_input_animation_8);
                        AnimationDrawable animationDrawable = (AnimationDrawable) fpImg.getDrawable();
                        animationDrawable.start();
                        fpBatchId = lockCommFpAddResponse.getBatchId();
                        Log.d(TAG, "fpBatchId:"+fpBatchId);
                        //发送指纹确认
                        unregisterBluetoothReceiver();
                        SecureFpConfirm secureFpConfirm = new SecureFpConfirm(activity, mDevice, fpBatchId);
                        Log.d(TAG, "指纹确认命令:"+ cn.zelkova.lockprotocol.BitConverter.toHexString(getFpConfirmCmd().getBytes()));
                        secureFpConfirm.sendLockMsg(getFpConfirmCmd().getBytes(), lockOperateCallback);
                    }
                }

            }catch (LockFormatException e){
                Log.d(TAG, "LockFormatException");
                e.printStackTrace();
            }
        }
    }
}
