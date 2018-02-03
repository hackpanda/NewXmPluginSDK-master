package com.xiaomi.zkplug.util;

/**
 * 作者：liwenqi on 17/8/4 15:14
 * 邮箱：liwenqi@zelkova.cn
 * 描述：
 */

public interface DataUpdateCallback {
    void dataUpateFail(int i, String s);
    void dataUpdateSucc(String s);
}
