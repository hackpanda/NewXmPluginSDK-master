package com.xiaomi.zkplug.intelligent;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.view.TimeSelector.Utils.TextUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * 作者：liwenqi on 18/3/5 11:22
 * 邮箱：liwenqi@zelkova.cn
 * 描述：逻辑数据管理类
 */

public class InteManager {
    private final String TAG = "InteManager";
    DataManageUtil dataManageUtil;
    Activity activity;
    ExpandableListView mExpandableListView;
    private final String theMasterPwdKey = "keyid_master$pwd_data";//管理员密码
    private final String theMasterFpKey = "keyid_master$fp_data";//管理员指纹
    private String theSmFpKey = "";//成员指纹
    private String theSmPwdKey = "";//成员密码
    private String theAccountKey = "";//成员蓝牙钥匙
    public String[] IDS = {"1", "2", "3", "4"};
    public String[] MEMBERS = null;
    public String[][] KEYS = null;//蓝牙、密码、指纹

    public InteManager(DataManageUtil dataManageUtil, Activity activity) {
        this.dataManageUtil = dataManageUtil;
        this.activity = activity;
    }

    protected void initView(){
        mExpandableListView = (ExpandableListView) activity.findViewById(R.id.expandable_list);

        queryFamilyData();//查询数据
    }
    // 每次展开一个分组后，关闭其他的分组
    private boolean expandOnlyOne(int expandedPosition) {
        boolean result = true;
        int groupLength = mExpandableListView.getExpandableListAdapter().getGroupCount();
        for (int i = 0; i < groupLength; i++) {
            if (i != expandedPosition && mExpandableListView.isGroupExpanded(i)) {
                result &= mExpandableListView.collapseGroup(i);
            }
        }
        return result;
    }
    /**
     * 初始化家庭成员列表,包括管理员
     * @return
     */
    protected void queryFamilyData(){
        try{
            JSONArray mJsArray = new JSONArray();
            mJsArray.put("keyid_smlist_data");//里面放的是key, 因为家人keyid是动态的，先查到家人数据
            JSONObject resultObj = dataManageUtil.getDataFromLocal(mJsArray);

            if(resultObj.has("keyid_smlist_data") && !resultObj.get("keyid_smlist_data").equals("")){
                JSONArray smArray = new JSONArray(resultObj.getString("keyid_smlist_data"));//本地存储的都成了string
                queryFamilyDataByLocal(smArray);//本地方式
            }else{
                queryFamilyDataByLocal(new JSONArray());
            }
            queryFamilyDataByServer(mJsArray);//服务器方式
        }catch (JSONException je){
            je.printStackTrace();
        }
    }

    /**
     * Loacal方式：得到家庭成员或者管理员的钥匙数据
     */
    private void queryFamilyDataByLocal(final JSONArray smArray){
        MEMBERS = new String[smArray.length()+1];
        MEMBERS[0] = "管理员";

        JSONArray mJsArray = new JSONArray();//数据查询参数
        mJsArray.put(theMasterPwdKey);//管理员密码
        mJsArray.put(theMasterFpKey);//管理员指纹

        try {
            for(int n=0; n<smArray.length(); n++){
                JSONObject memberObj = smArray.getJSONObject(n);
                String memberId = memberObj.getString("id");
                mJsArray.put("keyid_sm$pwd$"+memberId+"_data");
                mJsArray.put("keyid_sm$fp$"+memberId+"_data");
                mJsArray.put("keyid_sm$mi$blekey$"+memberId+"_data");
                MEMBERS[n+1] = memberObj.getString("nn");
            }
            final JSONObject data = dataManageUtil.getDataFromLocal(mJsArray);//




            Log.d(TAG, "memberObj本地: "+data.toString());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initViewByFamilyData(smArray, data);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 服务器放肆: 获取家庭成员列表
     * @return
     */
    private void queryFamilyDataByServer(JSONArray mJsArray){
        dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
            @Override
            public void dataUpdateSucc(final String s) {
                JSONObject resultObj = null;
                try {
                    resultObj = new JSONObject(s);
                    if(!resultObj.isNull("keyid_smlist_data")){
                        queryFamilyKeysByServer(new JSONArray(resultObj.getString("keyid_smlist_data")));
                    }else{
                        queryFamilyKeysByServer(new JSONArray());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            @Override
            public void dataUpateFail(int i, final String s) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity,s, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 服务器方式：得到家庭成员或者管理员的钥匙数据
     */
    private void queryFamilyKeysByServer(final JSONArray smArray){
        MEMBERS = new String[smArray.length()+1];//更新MEMBERS
        MEMBERS[0] = "管理员";

        JSONArray mJsArray = new JSONArray();//数据查询参数
        mJsArray.put(theMasterPwdKey);//管理员密码
        mJsArray.put(theMasterFpKey);//管理员指纹
        try {
            for(int n=0; n<smArray.length(); n++){
                JSONObject memberObj = smArray.getJSONObject(n);
                String memberId = memberObj.getString("id");
                mJsArray.put("keyid_sm$pwd$"+memberId+"_data");
                mJsArray.put("keyid_sm$fp$"+memberId+"_data");
                mJsArray.put("keyid_sm$mi$blekey$"+memberId+"_data");

                MEMBERS[n+1] = memberObj.getString("nn");
            }
            dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
                @Override
                public void dataUpateFail(int i, final String s) {

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, s, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override
                public void dataUpdateSucc(String s) {
                    try {
                        final JSONObject data = new JSONObject(s);
                        Log.d(TAG, "服务器数据: "+data.toString());
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                initViewByFamilyData(smArray, data);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initViewByFamilyData(JSONArray smArray, JSONObject data){
        try {
            Log.d(TAG, "nnnnnnnnnnn");
            KEYS = new String[MEMBERS.length][];
            ArrayList<String> keys = new ArrayList<String>();
            keys.add("手机钥匙开锁");

            if(data.has(theMasterPwdKey) && !TextUtil.isEmpty(data.getString(theMasterPwdKey))){
                Log.d(TAG, "xxxxxxxxxxx");
                keys.add("密码开锁");
            }
            if(data.has(theMasterFpKey) && !TextUtil.isEmpty(data.getString(theMasterFpKey))){
                Log.d(TAG, "yyyyyyyyyyyyyy");
                JSONArray fpArray = new JSONArray(data.getString(theMasterFpKey));
                Log.d(TAG, "zzzzzzzzzzzzz");
                for(int i=0; i<fpArray.length(); i++){
                    JSONObject fpObj = fpArray.getJSONObject(i);
                    keys.add(fpObj.getString("name")+"开锁");
                }
            }

            KEYS[0] = new String[keys.size()];
            for(int n=0; n < keys.size(); n++){
                KEYS[0][n] = keys.get(n);
            }

            for(int j=0; j<smArray.length(); j++){
                keys.clear();
                String smid = smArray.getJSONObject(j).getString("id");
                theSmPwdKey = "keyid_sm$pwd$"+smid+"_data";
                theSmFpKey = "keyid_sm$fp$"+smid+"_data";
                theAccountKey = "keyid_sm$mi$account$"+smid+"_data";
                if(data.has(theAccountKey) && !TextUtil.isEmpty(data.getString(theAccountKey))){//有钥匙
                    keys.add("手机钥匙开锁");
                }

                if(data.has(theSmPwdKey) && !TextUtil.isEmpty(data.getString(theSmPwdKey))){
                    keys.add("密码开锁");
                }
                if(data.has(theSmFpKey) && !TextUtil.isEmpty(data.getString(theSmFpKey))){
                    JSONArray fpArray = new JSONArray(data.getString(theSmFpKey));
                    for(int i=0; i<fpArray.length(); i++){
                        JSONObject fpObj = fpArray.getJSONObject(i);
                        keys.add(fpObj.getString("name")+"开锁");
                    }
                }
                KEYS[j+1] = new String[keys.size()];//成员+1，因为第一位管理员占了
                for(int n=0; n < keys.size(); n++){
                    KEYS[j+1][n] = keys.get(n);
                }
            }

            loadDataToListView();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    /**
     * ListView加载数据
     */
    private void loadDataToListView(){
        NormalExpandableListAdapter adapter = new NormalExpandableListAdapter(MEMBERS, KEYS);
        mExpandableListView.setAdapter(adapter);
        adapter.setOnGroupExpandedListener(new OnGroupExpandedListener() {
            @Override
            public void onGroupExpanded(int groupPosition) {
                expandOnlyOne(groupPosition);
            }
        });

        //  设置分组项的点击监听事件
        mExpandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                Log.d(TAG, "onGroupClick: groupPosition:" + groupPosition + ", id:" + id);
                // 请务必返回 false，否则分组不会展开
                return false;
            }
        });

        //  设置子选项点击监听事件
        mExpandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                return true;
            }
        });
    }
}
