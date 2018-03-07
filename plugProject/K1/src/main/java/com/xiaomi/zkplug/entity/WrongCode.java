package com.xiaomi.zkplug.entity;

import android.app.Activity;

import com.xiaomi.zkplug.R;

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
    public static String get(String message, Activity activity) {
        map.put("1", activity.getString(R.string.gloable_wrongcode_1));
        map.put("2", activity.getString(R.string.gloable_wrongcode_2));
        map.put("3", activity.getString(R.string.gloable_wrongcode_3));
        map.put("4", activity.getString(R.string.gloable_wrongcode_4));
        map.put("5", activity.getString(R.string.gloable_wrongcode_5));
        map.put("6", activity.getString(R.string.gloable_wrongcode_6));
        map.put("7", activity.getString(R.string.gloable_wrongcode_7));//等待回转或者提前放置了手指
        map.put("8", activity.getString(R.string.gloable_wrongcode_8));
        map.put("9", activity.getString(R.string.gloable_wrongcode_9));
        map.put("10", activity.getString(R.string.gloable_wrongcode_10));
        map.put("11", activity.getString(R.string.gloable_wrongcode_11));
        map.put("12", activity.getString(R.string.gloable_wrongcode_12));
        map.put("13", activity.getString(R.string.gloable_wrongcode_13));
        map.put("14", activity.getString(R.string.gloable_wrongcode_14));
        map.put("15", activity.getString(R.string.gloable_wrongcode_15));
        map.put("16", activity.getString(R.string.gloable_wrongcode_16));
        map.put("17", activity.getString(R.string.gloable_wrongcode_17));
        map.put("18", activity.getString(R.string.gloable_wrongcode_18));
        map.put("19", activity.getString(R.string.gloable_wrongcode_19));
        map.put("20", activity.getString(R.string.gloable_wrongcode_20));
        map.put("64", activity.getString(R.string.gloable_wrongcode_64));
        String result = activity.getString(R.string.gloable_wrongcode_undifine)+"("+message+")";
        if(map.get(message) != null){
            result = map.get(message)+"("+message+")";
        }
        return result;
    }
}
