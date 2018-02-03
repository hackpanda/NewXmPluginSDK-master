package com.xiaomi.zkplug.lockoperater;

/**
 * 作者：liwenqi on 17/8/4 15:14
 * 邮箱：liwenqi@zelkova.cn
 * 描述：开锁结果回调
 */

public interface LockOperateCallback {
    void lockOperateSucc(String value);//带参回调
    void lockOperateFail(String value);//带参回调
}
