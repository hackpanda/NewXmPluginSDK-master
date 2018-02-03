package com.xiaomi.zkplug.util;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述：管理所有的activity
 * 作者：liwenqi on 17/4/25 19:19
 * 邮箱：liwenqi@zelkova.cn
 */

public class ActivityCollector {
    public static List<Activity> activities = new ArrayList<Activity>();
    public static void addActivity(Activity activity){
        activities.add(activity);
    }
    public static void removeActivity(Activity activity){
        activities.remove(activity);
    }
    public static void finishAll(){
        for(Activity activity : activities){
            if(!activity.isFinishing()){
                activity.finish();
            }
        }
    }
}
