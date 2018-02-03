package com.xiaomi.zkplug.lockoperater;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.zkplug.CommonUtils;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.entity.MyEntity;
import com.xiaomi.zkplug.entity.WrongCode;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.UUID;

import cn.zelkova.lockprotocol.BitConverter;
import cn.zelkova.lockprotocol.LockCommBase;
import cn.zelkova.lockprotocol.LockCommSyncPwd;
import cn.zelkova.lockprotocol.LockCommSyncPwdResponse;
import cn.zelkova.lockprotocol.LockFormatException;

/**
 * 作者：liwenqi on 17/7/19 17:25
 * 邮箱：liwenqi@zelkova.cn
 * 描述：添加密码
 */

public class SecurePwdAdd implements ILockDataOperator {

    private final String TAG = "SecurePwdSet";
    public static final int FLAG = 1;
    private BroadcastReceiver mBluetoothReceiver;

    int index = 0;//拆包起始index
    Activity activity;
    Device mDevice;
    ArrayDeque<Byte> inputBuffer = new ArrayDeque<Byte>();//缓存返回数据
    LockOperateCallback lockOperateCallback;//操作锁的回调
    /**
     * @param activity
     * @param mDevice
     * 描述：赋值, 用于设备和Toast提示的activity
     */
    public SecurePwdAdd(Activity activity, Device mDevice) {
        this.activity = activity;
        this.mDevice = mDevice;
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

                    LockCommSyncPwd lockCommSyncPwd = new LockCommSyncPwd(bytes);//封装进锁通讯
                    byte[] commBytes = lockCommSyncPwd.getBytes();//锁通讯命令
                    Log.d(TAG, "commData:"+cn.zelkova.lockprotocol.BitConverter.toHexString(commBytes));
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
                                        Log.d("SecurePwdSet", "写数据成功："+code);
                                        XmBluetoothManager.getInstance().notify(mDevice.getMac(), MyEntity.RX_SERVICE_UUID, MyEntity.TX_CHAR_UUID, null);
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
                    Toast.makeText(activity, "通讯数据加密失败", Toast.LENGTH_SHORT).show();
                    lockOperateCallback.lockOperateFail("通讯数据加密失败");
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
            Log.d("MainActivity", "注册广播");
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
        byte[] value = intent.getByteArrayExtra(XmBluetoothManager.KEY_CHARACTER_VALUE);
        Log.d(TAG, "锁返回数据1:"+BitConverter.toHexString(value));
        if (MyEntity.RX_SERVICE_UUID.equals(service) && MyEntity.TX_CHAR_UUID.equals(character)) {
            Collections.addAll(inputBuffer, BitConverter.toPackaged(value));
            final byte[] buffer = BitConverter.toPrimitive(inputBuffer.toArray(new Byte[0]));

            try{
                Log.d(TAG, "锁返回数据2:"+BitConverter.toHexString(buffer));
                LockCommSyncPwdResponse lockCommSyncPwdResponse = (LockCommSyncPwdResponse) LockCommBase.parse(inputBuffer);
                if(lockCommSyncPwdResponse == null){
                    Log.d(TAG, "lockCommBase == null");
                    return;
                }
                Collections.addAll(inputBuffer, BitConverter.toPackaged(buffer));
                Log.d(TAG, "密码进锁成功:inputBuffer.size()"+inputBuffer.size());
                lockCommSyncPwdResponse = (LockCommSyncPwdResponse) LockCommBase.parse(inputBuffer);
                if(lockCommSyncPwdResponse.getResultCode() != 0){
                    Log.d(TAG, "错误码："+lockCommSyncPwdResponse.getResultCode());
                    lockOperateCallback.lockOperateFail(WrongCode.get(String.valueOf(lockCommSyncPwdResponse.getResultCode())));
                }else{
                    Log.d(TAG, "密码别名:"+lockCommSyncPwdResponse.getPwdIdx());
                    lockOperateCallback.lockOperateSucc(String.valueOf(lockCommSyncPwdResponse.getPwdIdx()));
                }
            }catch (LockFormatException e){
                Log.d(TAG, "LockFormatException");
                e.printStackTrace();
            }catch (ClassCastException cce){
                lockOperateCallback.lockOperateFail("密码设置失败");
            }
        }
    }
}
