package com.xiaomi.zkplug.intelligent;


import com.xiaomi.smarthome.device.api.MessageCallback;

/**
 * 作者：liwenqi on 18/3/9 11:03
 * 邮箱：liwenqi@zelkova.cn
 * 描述：
 */
public interface IntelCallback {
    void doSuccess(MessageCallback messageCallback);//设置成功
    void doFail(MessageCallback messageCallback);//设置失败
}
