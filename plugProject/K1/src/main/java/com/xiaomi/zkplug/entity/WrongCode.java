package com.xiaomi.zkplug.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者：liwenqi on 16/9/29 15:25
 * 邮箱：liwenqi@zelkova.cn
 */
public class WrongCode {
    public static Map<String,String> map = new HashMap<String, String>();
    /**
     * @param message
     * @return
     */
    public static String get(String message) {
        map.put("1", "处理服务器发来的锁命令时，没有能解密出来");
        map.put("2", "无效钥匙");
        map.put("3", "命令不在有效期内");
        map.put("4", "虽然锁命令未过期，但是不在指定的周期内Week，month，包括所处的周期时间");
        map.put("5", "次数使用完毕");
        map.put("6", "Open指令要求绑定到首次使用的设备，当前设备不是首次使用设备的Id");
        map.put("7", "指纹头不在状态，请稍后重试");//等待回转或者提前放置了手指
        map.put("8", "所执行的操作导致超过既定容量");
        map.put("9", "命令中的时间，与锁体时间误差过大");
        map.put("10", "未知错误");
        map.put("11", "得到的数据不能正确解析出协议内容");
        map.put("12", "锁命令中MAC与锁不匹配");
        map.put("13", "手机与锁Rom的通讯Token已经过期，或无效");
        map.put("14", "与控制端交换密钥的方式，不被ROM支持");
        map.put("15", "ROM不支持协议，或无法识别");
        map.put("16", "给的的参数值不在范围内");
        map.put("17", "数据包丢失");
        map.put("18", "该密码已存在，请重新设置");
        map.put("19", "未找到指定的值");
        map.put("20", "录入指纹出错");
        map.put("64", "设置参数有误");
        String result = "未定义的错误码("+message+")";
        if(map.get(message) != null){
            result = map.get(message)+"("+message+")";
        }
        return result;
    }
}
