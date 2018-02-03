package com.xiaomi.zkplug.lockoperater;

import android.util.Log;

import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.zkplug.Device;

import cn.zelkova.lockprotocol.BitConverter;

/**
 * 作者：liwenqi on 17/7/19 17:25
 * 邮箱：liwenqi@zelkova.cn
 * 描述：开门
 */

public class SecureOpen {

    public static final int FLAG = 0;

    public static final String LOCK_UUID_FORMAT = "0000%04x-0065-6C62-2E74-6F696D2E696D";

    Device mDevice;

    /**
     * @param mDevice
     * 描述：赋值, 用于设备和Toast提示的activity
     */
    public SecureOpen(Device mDevice) {
        this.mDevice = mDevice;
    }

    public void sendLockMsg(final byte[] operator, final LockOperateCallback lockOperateCallback) {
        XmBluetoothManager.getInstance().securityChipOperate(
                mDevice.getMac(),
                XmBluetoothManager.SECURITY_CHIP_UNLOCK_OPERATOR,
                new Response.BleReadResponse() {
                    @Override
                    public void onResponse(int code, byte[] bytes) {
                        if (code == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                            lockOperateCallback.lockOperateSucc(""+ BitConverter.toHexString(bytes));
                        } else {
                            Log.d("MainActivity", "open_sendLockMsg code: "+code);
                            lockOperateCallback.lockOperateFail(""+code);
                        }
                    }
                });
    }
}
