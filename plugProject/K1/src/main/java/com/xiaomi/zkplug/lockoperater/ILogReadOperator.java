package com.xiaomi.zkplug.lockoperater;


/**
 * 作者：liwenqi on 17/7/21 11:03
 * 邮箱：liwenqi@zelkova.cn
 * 描述：读取日志接口
 */
public interface ILogReadOperator {
    void registerBluetoothReceiver();//注册广播
    void unregisterBluetoothReceiver();//监听广播
    void sendLockMsg(byte[] msg, LogReadCallback logReadCallback);//发送锁命令
}
