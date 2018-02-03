package com.xiaomi.zkplug.lockoperater;

/**
 * 作者：liwenqi on 17/7/21 11:03
 * 邮箱：liwenqi@zelkova.cn
 * 描述：锁数据交互
 */
public interface ILockDataOperator {
    void registerBluetoothReceiver();//注册广播
    void unregisterBluetoothReceiver();//监听广播
    void sendLockMsg(byte[] msg, LockOperateCallback lockOperateCallback);//发送锁命令
}
