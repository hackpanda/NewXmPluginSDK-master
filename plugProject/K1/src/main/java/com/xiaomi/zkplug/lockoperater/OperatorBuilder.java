package com.xiaomi.zkplug.lockoperater;

import android.app.Activity;

import com.xiaomi.zkplug.Device;

/**
 * 作者：liwenqi on 17/7/21 11:08
 * 邮箱：liwenqi@zelkova.cn
 * 描述：
 */

public class OperatorBuilder {
    private final static String TAG = "OperatorBuilder";
    public static ILockDataOperator create(int operatorFlag, Activity activity, Device mDevice) {

        ILockDataOperator iLockDataOperater;
        switch (operatorFlag) {
//            case SecureOpen.FLAG://暂定为开门0
//                iLockDataOperater = new SecureOpen(activity, mDevice);
//                break;
            case SecurePwdAdd.FLAG://1
                iLockDataOperater = new SecurePwdAdd(activity, mDevice);
                break;
            case SecurePwdUpdate.FLAG://2
                iLockDataOperater = new SecurePwdUpdate(activity, mDevice);
                break;
            case SecureSyncTime.FLAG://3
                iLockDataOperater = new SecureSyncTime(activity, mDevice);
                break;
            case SecurePwdDel.FLAG://4
                iLockDataOperater = new SecurePwdDel(activity, mDevice);
                break;
            case SecureFpAdd.FLAG://5
                iLockDataOperater = new SecureFpAdd(activity, mDevice);
                break;
            case SecureFpDel.FLAG://6
                iLockDataOperater = new SecureFpDel(activity, mDevice);
                break;
            case SecureStatus.FLAG://7
                iLockDataOperater = new SecureStatus(activity, mDevice);
                break;
            case SecureExchangeKey.FLAG://8
                iLockDataOperater = new SecureExchangeKey(activity, mDevice);
                break;
            case SecureSetVolumn.FLAG://9
                iLockDataOperater = new SecureSetVolumn(activity, mDevice);
                break;
            default:
                return null;
        }
        return iLockDataOperater;
    }
}
