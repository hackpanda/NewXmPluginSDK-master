package com.xiaomi.zkplug.lockoperater;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.entity.MyEntity;
import com.xiaomi.zkplug.entity.WrongCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.UUID;

import cn.zelkova.lockprotocol.BitConverter;
import cn.zelkova.lockprotocol.LockCommBase;
import cn.zelkova.lockprotocol.LockCommGetStatus;
import cn.zelkova.lockprotocol.LockCommGetStatusResponse;
import cn.zelkova.lockprotocol.LockFormatException;

/**
 * 作者：liwenqi on 17/7/19 17:25
 * 邮箱：liwenqi@zelkova.cn
 * 描述：门锁状态
 */

public class SecureStatus implements ILockDataOperator {

    private final String TAG = "SecureStatus";
    public static final int FLAG = 7;
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
    public SecureStatus(Activity activity, Device mDevice) {
        this.activity = activity;
        this.mDevice = mDevice;

    }

    @Override
    public void sendLockMsg(final byte[] cmd, final LockOperateCallback lockOperateCallback) {

        inputBuffer.clear();//清空缓存
        this.lockOperateCallback = lockOperateCallback;
        Log.d(TAG, "cmd:"+BitConverter.toHexString(cmd));
        XmBluetoothManager.getInstance().securityChipEncrypt(mDevice.getMac(), cmd, new Response.BleReadResponse() {
            @Override
            public void onResponse(int i, byte[] bytes) {//bytes加密后的数据
                if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                    index = 0;//置位
                    Log.d(TAG, "cmdData:"+ BitConverter.toHexString(bytes));

                    LockCommGetStatus lockCommGetStatus = new LockCommGetStatus(bytes);//封装进锁通讯
                    byte[] commBytes = lockCommGetStatus.getBytes();//锁通讯命令
                    Log.d(TAG, "commData:"+ BitConverter.toHexString(commBytes));
                    do{
                        //分包begin
                        byte[] surplusData = new byte[commBytes.length - index];
                        byte[] currentData;
                        System.arraycopy(commBytes, index, surplusData, 0, commBytes.length - index);
                        if (surplusData.length <= 20) {
                            currentData = new byte[surplusData.length];
                            System.arraycopy(surplusData, 0, currentData, 0, surplusData.length);
                            index += surplusData.length;
                            Log.d(TAG, "currentData:"+ BitConverter.toHexString(currentData));
                            XmBluetoothManager.getInstance().write(mDevice.getMac(), MyEntity.RX_SERVICE_UUID, MyEntity.RX_CHAR_UUID, currentData, new Response.BleWriteResponse() {
                                @Override
                                public void onResponse(int code, Void aVoid) {
                                    if (code == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                                        Log.d(TAG, "写数据成功："+code);
                                        XmBluetoothManager.getInstance().notify(mDevice.getMac(), MyEntity.RX_SERVICE_UUID, MyEntity.TX_CHAR_UUID, null);
                                    } else {
                                        Log.d(TAG, "写数据失败，code:"+code);
                                    }
                                }
                            });
                        } else {
                            currentData = new byte[20];
                            System.arraycopy(commBytes, index, currentData, 0, 20);
                            index += 20;
                            Log.d(TAG, "currentData:"+ BitConverter.toHexString(currentData));

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
                    //Toast.makeText(activity, "通讯数据加密失败", Toast.LENGTH_SHORT).show();
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
                }else if (TextUtils.equals(action, XmBluetoothManager.ACTION_CHARACTER_CHANGED)) {
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

        if (MyEntity.RX_SERVICE_UUID.equals(service) && MyEntity.TX_CHAR_UUID.equals(character)) {
            Collections.addAll(inputBuffer, BitConverter.toPackaged(value));
            final byte[] buffer = BitConverter.toPrimitive(inputBuffer.toArray(new Byte[0]));
            try{
                Log.d(TAG, "符合条件锁返回数据:"+BitConverter.toHexString(value));
                LockCommGetStatusResponse lockCommGetStatusResponse = (LockCommGetStatusResponse) LockCommBase.parse(inputBuffer);
                if(lockCommGetStatusResponse == null){
                    Log.d(TAG, "lockCommBase == null");
                    return;
                }
                Collections.addAll(inputBuffer, BitConverter.toPackaged(buffer));
                Log.d(TAG, "状态进锁成功:inputBuffer.size()"+inputBuffer.size());
                try{
                    lockCommGetStatusResponse = (LockCommGetStatusResponse) LockCommBase.parse(inputBuffer);
                    inputBuffer.clear();
                    if(lockCommGetStatusResponse.getResultCode() != 0){
                        unregisterBluetoothReceiver();
                        Log.d(TAG, "错误码："+lockCommGetStatusResponse.getResultCode());
                        lockOperateCallback.lockOperateFail(WrongCode.get(String.valueOf(lockCommGetStatusResponse.getResultCode()), activity));
                    }else{
                        unregisterBluetoothReceiver();
                        Log.d(TAG, "锁内时间:"+lockCommGetStatusResponse.getLockBriefTime());

                        int power = lockCommGetStatusResponse.getPower();//电量
                        Log.d(TAG, "锁内电量:"+power);
                        String romver = lockCommGetStatusResponse.getVerFrameware();//rom版本
                        JSONObject vObj = new JSONObject();
                        try {
                            vObj.put("power", power);
                            vObj.put("seclevel", lockCommGetStatusResponse.getSecurityLevel());//安全等级
                            vObj.put("volumn", lockCommGetStatusResponse.getVolumn());//音量
                            vObj.put("fpcount", lockCommGetStatusResponse.getFpStock());
                            vObj.put("pwdcount", lockCommGetStatusResponse.getPwdStock());
                            vObj.put("romver", castRomVer(romver));
                            vObj.put("pluginver", MyEntity.VERSION);//插件版本号
                            vObj.put("locktime", lockCommGetStatusResponse.getLockBriefTime());
                            vObj.put("timdiffsnd", Math.abs(lockCommGetStatusResponse.getDiffSnd()));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        lockOperateCallback.lockOperateSucc(vObj.toString());
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    Log.d(TAG, "获取状态异常，多半为CastClassException");
                }

            }catch (LockFormatException e){
                Log.d(TAG, "LockFormatException");
                e.printStackTrace();
            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            Log.d(TAG, "不符合数据返回:"+BitConverter.toHexString(value));
        }
    }

    /**
     * @param strRomVer 200000004
     * @return 2.0.0.0004
     */
    private String castRomVer(String strRomVer){
        String result = "";
        if(strRomVer.length() == 9){//
            result += Integer.parseInt(strRomVer.substring(0,1));
            result += ".";
            result += Integer.parseInt(strRomVer.substring(1,3));
            result += ".";
            result += Integer.parseInt(strRomVer.substring(3,5));
            result += "_";
            result += strRomVer.substring(5);
        }else{
            result += Integer.parseInt(strRomVer.substring(0,2));
            result += ".";
            result += Integer.parseInt(strRomVer.substring(2,4));
            result += ".";
            result += Integer.parseInt(strRomVer.substring(4,6));
            result += "_";
            result += strRomVer.substring(6);
        }
        Log.d(TAG, "rom版本:" + result);
        return result;
    }
}
