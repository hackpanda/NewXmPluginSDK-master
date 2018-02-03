package com.xiaomi.zkplug.lockoperater;

import cn.zelkova.lockprotocol.LockCommGetLogResponse;

/**
 * 作者：liwenqi on 17/8/4 15:14
 * 邮箱：liwenqi@zelkova.cn
 * 描述：开锁结果回调
 */

public interface LogReadCallback {
    void logReadSucc(LockCommGetLogResponse lockCommGetLogResponse);//带参回调
    void logReadFail(String value);//带参回调
}
