package com.xiaomi.zkplug.member;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.xiaomi.zkplug.R;

import org.json.JSONArray;
import org.json.JSONException;

;

/**
 * 作者：liwenqi on 16/9/13 10:33
 * 邮箱：liwenqi@zelkova.cn
 */
public class FamilyAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    public JSONArray smArray;

    public FamilyAdapter(Context pContext, JSONArray smArray) {
        mInflater = LayoutInflater.from(pContext);
        this.smArray = smArray;

    }

    @Override
    public int getCount() {
        if (smArray == null) {
            return 0;
        } else {
            return smArray.length();
        }
    }

    @Override
    public Object getItem(int position) {
        try {
            return smArray.get(position);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FamilyHolder holder = null;
        // 如果缓存convertView为空，则需要创建View
        if (convertView == null) {
            holder = new FamilyHolder();
            // 根据自定义的Item布局加载布局
            convertView = mInflater.inflate(R.layout.family_item, null);
            holder.nickName = (TextView) convertView.findViewById(R.id.memberName);
            // 将设置好的布局保存到缓存中，并将其设置在Tag里，以便后面方便取出Tag
            convertView.setTag(holder);
        } else {
            holder = (FamilyHolder) convertView.getTag();
        }
        try {
            holder.nickName.setText(smArray.getJSONObject(position).getString("nn"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return convertView;
    }
}

