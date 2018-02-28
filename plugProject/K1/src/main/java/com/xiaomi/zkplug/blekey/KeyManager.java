package com.xiaomi.zkplug.blekey;

import android.app.Activity;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.common.ui.widget.SwitchButton;
import com.xiaomi.smarthome.device.api.Callback;
import com.xiaomi.smarthome.device.api.DeviceStat;
import com.xiaomi.smarthome.device.api.SecurityKeyInfo;
import com.xiaomi.smarthome.device.api.XmPluginHostApi;
import com.xiaomi.zkplug.CommonUtils;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;
import com.xiaomi.zkplug.view.mutiselect.MultiSelectPopWindow;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.zelkova.lockprotocol.BriefDate;

/**
 * 作者：liwenqi on 18/2/2 11:12
 * 邮箱：liwenqi@zelkova.cn
 * 描述：发送钥匙辅助类
 */

public class KeyManager {
    private final String TAG = "KeyManager";

    Activity activity;
    Device mDevice;
    DataManageUtil dataManageUtil;
    String memberId, userId;
    TextView keyStartTv, keyEndTv, keyPeriodTv;

    List<Integer> weekdays = null; // 生效日期（星期几，例如周一和周三对应1和3，[1, 3]），仅在status=2时不可为空
    public KeyManager(DeviceStat mDeviceStat, Activity activity) {
        // 初始化device
        this.activity = activity;
        mDevice = Device.getDevice(mDeviceStat);
        this.dataManageUtil = new DataManageUtil(mDeviceStat, activity);
        this.memberId = activity.getIntent().getStringExtra("memberId");//成员Id
        this.userId = activity.getIntent().getStringExtra("userId");//小米账号
        this.keyPeriodTv = (TextView) activity.findViewById(R.id.keyPeriodTv);//重复周期
        this.keyStartTv = (TextView) activity.findViewById(R.id.keyStartTv);
        this.keyEndTv = (TextView) activity.findViewById(R.id.keyEndTv);
    }

    /**
     * 显示周期选择框
     */
    protected void showPeriodPickDialog(){
        weekdays = null;//初始化
        ArrayList<String> names = new ArrayList<>();
        names.add("星期一");
        names.add("星期二");
        names.add("星期三");
        names.add("星期四");
        names.add("星期五");
        names.add("星期六");
        names.add("星期日");

        new MultiSelectPopWindow.Builder(activity)
                .setNameArray(names)
                .setConfirmListener(new MultiSelectPopWindow.OnConfirmClickListener() {
                    @Override
                    public void onClick(ArrayList<Integer> indexList, ArrayList<String> selectedList) {

                        if(selectedList.size() > 0){
                            String result = "";
                            weekdays = new ArrayList<Integer>();
                            for(int i=0; i<selectedList.size(); i++){
                                if(selectedList.get(i).equals("星期一")){
                                    weekdays.add(1);
                                }else if(selectedList.get(i).equals("星期二")){
                                    weekdays.add(2);
                                }else if(selectedList.get(i).equals("星期三")){
                                    weekdays.add(3);
                                }else if(selectedList.get(i).equals("星期四")){
                                    weekdays.add(4);
                                }else if(selectedList.get(i).equals("星期五")){
                                    weekdays.add(5);
                                }else if(selectedList.get(i).equals("星期六")){
                                    weekdays.add(6);
                                }else if(selectedList.get(i).equals("星期日")){
                                    weekdays.add(0);
                                }
                                result += selectedList.get(i) +",";
                            }
                            result = result.substring(0, result.length() - 1);
                            keyPeriodTv.setText(result);
                            for(int m = 0; m<weekdays.size(); m++){
                                Log.d(TAG, weekdays.get(m).toString());
                            }

                        }
                    }
                })
                .setCancel("取消")
                .setConfirm("完成")
                .setTitle("周期列表")
                .build()
                .show(activity.findViewById(R.id.mBottom));
    }

    /**
     * 授权;分享
     * // 1：暂时有效，2：周期有效，3：永久有效
     */
    protected void keyGaveWork(int status){
        long activeTime = 0;
        long expireTime = 0;

        if(status == 3){
            Log.d(TAG, "发送永久有效钥匙");
            activeTime = ZkUtil.getUTCTime(); // 生效时间 UTC时间戳，单位为s
            expireTime = activeTime + 50 * DateUtils.YEAR_IN_MILLIS; // 过期时间 UTC时间戳，单位为s
            Log.d(TAG, "activeTime: "+activeTime+", expireTime: "+expireTime);
        }else if(status == 2){//周期钥匙，暂时屏蔽掉了
            SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            try {
                Date startTime = sdFormat.parse("2018-01-30 " + keyStartTv.getText().toString());//上次授时的日期,也相当于上次授时时间, 时间是随便写的，只要保证开始和结束24小时即可
                activeTime = startTime.getTime(); // 生效时间 UTC时间戳，
                Date endTime = sdFormat.parse("2018-01-30 " + keyEndTv.getText().toString());//上次授时的日期,也相当于上次授时时间
                expireTime = endTime.getTime(); // 生效时间 UTC时间戳，单位为s
                Log.d(TAG, BriefDate.fromNature(startTime).toString()+"~~~"+BriefDate.fromNature(endTime).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }else{//临时钥匙
            SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            try {
                Date startTime = new Date();
                activeTime = startTime.getTime(); // 生效时间 UTC时间戳，
                Date endTime = sdFormat.parse(keyEndTv.getText().toString());//上次授时的日期,也相当于上次授时时间
                expireTime = endTime.getTime(); // 生效时间 UTC时间戳，单位为s
                Log.d(TAG, BriefDate.fromNature(startTime).toString()+"~~~"+BriefDate.fromNature(endTime).toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            //周期钥匙的weekdays，已经通过showPeriodPickDialog()得到
        }

        boolean readonly = true; // true：被分享人不可接收锁push，false：被分享人可接收锁push
        SwitchButton readSwitchBtn = (SwitchButton) activity.findViewById(R.id.btnSwitch);
        if(readSwitchBtn.isChecked()){
            readonly = false;
        }
        Log.d(TAG, "readonly: "+readonly);

        // 分享钥匙的有效时间可以配置
        shareSecurityKey(mDevice.getModel(), mDevice.getDid(), userId, status, activeTime / 1000, expireTime / 1000, weekdays, readonly);
    }

    /**
     * ApiLevel:51
     * 分享电子钥匙
     *
     * @param model 设备model
     * @param did 分享者的did
     * @param shareUid 分享目标的uid
     * @param status 分享类别，1：暂时，2：周期，3：永久
     * @param activeTime 生效时间 UTC时间戳，单位为s
     * @param expireTime 过期时间 UTC时间戳，单位为s
     * @param weekdays 生效日期（星期几，例如周一和周三对应1和3，[1, 3]，星期天对应0），仅在status=2时不可为空
     * @param readonly true：被分享人不可接收锁push，false：被分享人可接收锁push，（family关系用户不受这个字段影响）
     * @param
     */
    protected void  shareSecurityKey(String model, String did, String shareUid, int status, final long activeTime, long expireTime, List<Integer> weekdays, final boolean readonly) {
        Date startTime = new Date();

        Date endTime = new Date(expireTime * 1000);
        if(BriefDate.fromNature(startTime).toString().substring(0, 16).equals(BriefDate.fromNature(endTime).toString().substring(0, 16))){//至少一分钟
            Toast.makeText(activity, "到期时间必须大于当前时间", Toast.LENGTH_LONG).show();
            return;
        }
        XmPluginHostApi.instance().shareSecurityKey(model, did, shareUid,
                status, activeTime, expireTime, weekdays, readonly, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updateAuthAccount(readonly);//分享成功后更新auth属性
                    }

                    @Override
                    public void onFailure(int i, String s) {
//                        if(i == -8){
//                            Toast.makeText(activity, "无效的钥匙周期", Toast.LENGTH_LONG).show();
//                        }else{
//                            Toast.makeText(activity, "分享钥匙失败, code = " + i + ", detail = " + s, Toast.LENGTH_SHORT).show();
//                        }

                        Toast.makeText(activity, "分享钥匙失败, code = " + i + ", detail = " + s, Toast.LENGTH_SHORT).show();
                        if (i == Callback.INVALID) {
                            Toast.makeText(activity, "用户id不存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    /**
     * 存储member的keyId
     * @param readonly false:可接收消息，true：不可接收消息
     */
    protected void updateAuthAccount(final boolean readonly) {
        XmPluginHostApi.instance().getSecurityKey(mDevice.getModel(), mDevice.getDid(), new Callback<List<SecurityKeyInfo>>() {
            @Override
            public void onSuccess(List<SecurityKeyInfo> securityKeyInfos) {
                Log.d(TAG, String.format("获取到%d把钥匙：", securityKeyInfos.size()));
                for (int i = 0; i < securityKeyInfos.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("钥匙" + (i + 1) + ":" + "\n");
                    sb.append("keyId = " + securityKeyInfos.get(i).keyId + "\n");
                    sb.append("shareUid = " + securityKeyInfos.get(i).shareUid + "\n");
                    sb.append("status = " + securityKeyInfos.get(i).status + "\n");
                    sb.append("activeTime = " + securityKeyInfos.get(i).activeTime + "\n");
                    sb.append("expireTime = " + securityKeyInfos.get(i).expireTime + "\n");
                    sb.append("weekdays = " + securityKeyInfos.get(i).weekdays + "\n");
                    sb.append("isoutofdate = " + securityKeyInfos.get(i).isoutofdate + "\n");
                    Log.d(TAG, sb.toString()+", userId:"+userId);
                    if(securityKeyInfos.get(i).shareUid.equals(userId)){
                        updateMemberAuth(securityKeyInfos.get(i), readonly);//更新小米账号、通知、钥匙
                        dataManageUtil.setKeyToName(securityKeyInfos.get(i).keyId, activity.getIntent().getStringExtra("nickName"));
                        return;
                    }
                }
            }
            @Override
            public void onFailure(int i, String s) {
                Toast.makeText(activity, "获取授权钥匙失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 更新授权信息
     * 小米账号、通知、钥匙
     * @param readonly
     */
    protected void updateMemberAuth(SecurityKeyInfo securityKeyInfo, boolean readonly){
        JSONObject settingsObj = new JSONObject();
        try{
            settingsObj.put("keyid_sm$mi$account$"+memberId+"_data", userId);
            settingsObj.put("keyid_sm$mi$notify$"+memberId+"_data", readonly == true ? "0" : "1");
            JSONObject bleObj = new JSONObject();
            bleObj.put("keyid", String.valueOf(securityKeyInfo.keyId));
            bleObj.put("bletype", String.valueOf(securityKeyInfo.status));//钥匙类型，永久、周期、时间段
            bleObj.put("activetime", String.valueOf(securityKeyInfo.activeTime));
            bleObj.put("expiretime", String.valueOf(securityKeyInfo.expireTime));
            /* 钥匙信息 begin */
            JSONArray weekArray = new JSONArray();
            if(securityKeyInfo.weekdays != null){
                for(int i=0; i<securityKeyInfo.weekdays.size(); i++){
                    weekArray.put(securityKeyInfo.weekdays.get(i));
                }
            }

            bleObj.put("week", weekArray.toString());
            settingsObj.put("keyid_sm$mi$blekey$"+memberId+"_data", bleObj.toString());
            /* 钥匙信息 end */
            dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                @Override
                public void dataUpateFail(int i, final String s) {
                    Log.d(TAG, "授权失败");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CommonUtils.toast(activity, s);
                        }
                    });
                }
                @Override
                public void dataUpdateSucc(String s) {
                    Log.d(TAG, "授权成功");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, "钥匙添加成功", Toast.LENGTH_SHORT).show();
                            activity.finish();
                            if(KeySearchActivity.instance != null){
                                KeySearchActivity.instance.finish();
                            }
                        }
                    });
                }
            });
        }catch (JSONException je){
            je.printStackTrace();
        }
    }

}
