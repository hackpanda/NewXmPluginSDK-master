package com.xiaomi.zkplug.entity;

import java.text.SimpleDateFormat;
import java.util.UUID;

/**
 * 作者：liwenqi on 17/7/25 14:29
 * 邮箱：liwenqi@zelkova.cn
 * 描述：SharedPreference文件名
 */
public class MyEntity {
    public static final String UserConfigFile = "UserConfigFile";//存储家庭成员

    public static final int MEMBER_QUERY_KEY = 1000; //userConfig里面的参数可以

    public static final String MODEL_ID = "shjszn.lock.c1";//model_id

    public static final String VERSION = "0.4.0.802271";//version，正常读取读到的是米家自己的版本号

    // 开锁超时时间，可以根据实际情况自己定义
    public static final long OPERATE_TIMEOUT = 15 * 1000;

    public static UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");//发送
    public static UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");//接受

    public static final String LOCK_UUID_FORMAT = "0000%04x-0065-6C62-2E74-6F696D2E696D";

    public static UUID makeLockUUID(int value) {
        return UUID.fromString(String.format(LOCK_UUID_FORMAT, value));
    }
    static UUID UUID_LOCK_SERVICE = makeLockUUID(0x1000);
    static UUID UUID_LOCK_STATE_CHARACTER = makeLockUUID(0x1002);

    SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public final static int MSG_LEVEL_HUIJIA = 1;//回家
    public final static int MSG_LEVEL_LOW_POWER = 2;//电量低
    public final static int MSG_LEVEL_FREEZE_KEYBOARD = 3;//键盘被锁
    public final static int MSG_LEVEL_BAOJING =4;//报警
    public final static int MSG_LEVEL_FANSUO =5;//反锁

    public final static String PROP_POWER_KEY = String.valueOf(Integer.parseInt("100A",16));//查询锁电量
    public final static String PROP_STATUS_KEY = String.valueOf(Integer.parseInt("100E",16));//查询锁状态
    public final static String PROP_TYPE = "prop";//状态

    public final static String EVENT_TYPE = "event";//查询锁开门事件
    public final static String EVENT_KEY = "5";//查询锁状态

    //门锁状态
    public final static String STATUS_CLOSE_WEISHANGTI = "关门(未上提)";
    public final static String STATUS_CLOSE_YISHANGTI = "关门(已上提)";
    public final static String STATUS_CHANGKAI = "敞开";
}
