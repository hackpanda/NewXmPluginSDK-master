package com.xiaomi.zkplug.main.msg;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.entity.LockMsg;
import com.xiaomi.zkplug.entity.MyEntity;

import java.util.ArrayList;
import java.util.List;

import static com.xiaomi.zkplug.R.id.msgTitleItemView;

/**
 * 
 * @author ywl5320
 *
 */
public class MsgAdapter extends BaseAdapter {

	protected Context context;
	protected LayoutInflater mlayoutInflate;
	protected List<LockMsg> mDatas = new ArrayList<LockMsg>();

	public MsgAdapter(Context context, List<LockMsg> mDatas) {
		this.context = context;
		this.mDatas = mDatas;
		mlayoutInflate = LayoutInflater.from(this.context);
	}

	@Override
	public int getCount() {
		return mDatas.size();
	}

	@Override
	public Object getItem(int position) {
		return mDatas.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		MsgHolder holder = null;
		if (convertView == null) {
			holder = new MsgHolder();
			// 根据自定义的Item布局加载布局
			convertView = mlayoutInflate.inflate(R.layout.msg_item, parent, false);
			holder.msgTitleItemView = (RelativeLayout) convertView.findViewById(msgTitleItemView);
			holder.msgContentItemView = (RelativeLayout) convertView.findViewById(R.id.msgContentItemView);
			holder.dateTv = (TextView) convertView.findViewById(R.id.dateTv);
			holder.titleTv = (TextView) convertView.findViewById(R.id.titleTv);
			holder.timeTv = (TextView) convertView.findViewById(R.id.timeTv);
			holder.contentTv = (TextView) convertView.findViewById(R.id.contentTv);
			holder.typeImg = (ImageView) convertView.findViewById(R.id.typeImg);
			holder.wdImg = (ImageView) convertView.findViewById(R.id.wdImg);
			convertView.setTag(holder);
		} else {
			holder = (MsgHolder) convertView.getTag();
		}
		if(mDatas.get(position).getMsgDate() != null){
			holder.dateTv.setText(mDatas.get(position).getMsgDate());
			holder.msgContentItemView.setVisibility(View.GONE);
			holder.msgTitleItemView.setVisibility(View.VISIBLE);
		}else{
			holder.msgContentItemView.setVisibility(View.VISIBLE);
			holder.msgTitleItemView.setVisibility(View.GONE);
			holder.titleTv.setText(mDatas.get(position).getMsgTitle());
			holder.timeTv.setText(mDatas.get(position).getMsgTime());
			holder.contentTv.setText(mDatas.get(position).getMsgContent());
			if(mDatas.get(position).getMsgType() == MyEntity.MSG_LEVEL_BAOJING){
				holder.typeImg.setBackgroundResource(R.drawable.icon_baojing);
			}else if(mDatas.get(position).getMsgType() == MyEntity.MSG_LEVEL_FREEZE_KEYBOARD){
				holder.typeImg.setBackgroundResource(R.drawable.icon_jianpanbeisuo);
			}else if(mDatas.get(position).getMsgType() == MyEntity.MSG_LEVEL_LOW_POWER){
				holder.typeImg.setBackgroundResource(R.drawable.icon_dianliangdi_msg);
			}else if(mDatas.get(position).getMsgType() == MyEntity.MSG_LEVEL_FANSUO){
				holder.typeImg.setBackgroundResource(R.drawable.icon_fansuo_msg);
			}else{
				holder.typeImg.setBackgroundResource(R.drawable.icon_huijia);
			}
		}

		return convertView;
	}
}
