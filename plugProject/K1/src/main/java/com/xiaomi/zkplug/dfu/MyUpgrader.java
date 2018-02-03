package com.xiaomi.zkplug.dfu;

import android.os.Bundle;
import android.os.RemoteException;

import com.xiaomi.smarthome.bluetooth.BleUpgrader;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;

/**
 * 作者：liwenqi on 17/9/12 10:30
 * 邮箱：liwenqi@zelkova.cn
 * 描述：
 */

public class MyUpgrader extends BleUpgrader {

    @Override
    public String getCurrentVersion() {
        // 返回当前固件版本
        return null;
    }

    @Override
    public String getLatestVersion() {
        // 返回最新固件版本
        return null;
    }

    @Override
    public String getUpgradeDescription() {
        // 返回最新固件升级描述
        return null;
    }

    @Override
    public void startUpgrade() {
        // 开始固件升级
    }

    @Override
    public void onActivityCreated(Bundle bundle) throws RemoteException {
        // 固件升级页初始化完成
        showPage(XmBluetoothManager.PAGE_CURRENT_DEPRECATED, null);
    }
}