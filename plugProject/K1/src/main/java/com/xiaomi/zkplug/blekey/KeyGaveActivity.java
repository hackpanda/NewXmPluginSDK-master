package com.xiaomi.zkplug.blekey;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xiaomi.smarthome.common.ui.widget.SwitchButton;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.util.ZkUtil;
import com.xiaomi.zkplug.view.TimeSelector.TimeSelector;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.zelkova.lockprotocol.BriefDate;


/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：授权
 */
public class KeyGaveActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "KeyGaveActivity";
    private final int TAG_WEI_XUAN_ZHONG = 0x0;//未选中
    private final int TAG_XUAN_ZHONG = 0x1;//选中
    ImageView foreverImg, tempImg, periodImg;
    TextView nickNameTv;//用户nickName
    String userId;
    ImageView userImg;
    Bitmap userBitmap;
    String memberId;
    LinearLayout keyInfoLayout;
    TextView keyInfoStartTv, keyInfoEndTv;//钥匙周期
    private TimeSelector timeSelector;
    KeyManager keyManager;//发送钥匙辅助类
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_gave);
        initView();
    }

    private void initView(){
        keyManager = new KeyManager(mDeviceStat, activity());
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText("添加手机钥匙");
        nickNameTv = (TextView) findViewById(R.id.nickNameTv);
        nickNameTv.setText(getIntent().getStringExtra("nickName"));
        this.userImg = (ImageView) findViewById(R.id.userImg);

        initUserImg();//设置帐号头像

        findViewById(R.id.keyGaveBtn).setOnClickListener(this);
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        SwitchButton readSwitchBtn = (SwitchButton) findViewById(R.id.btnSwitch);
        readSwitchBtn.setChecked(true);
        findViewById(R.id.foreverLayout).setOnClickListener(this);
        findViewById(R.id.tempLayout).setOnClickListener(this);
        findViewById(R.id.periodLayout).setOnClickListener(this);
        this.foreverImg = (ImageView) findViewById(R.id.foreverImg);
        this.tempImg = (ImageView) findViewById(R.id.tempImg);
        this.periodImg = (ImageView) findViewById(R.id.periodImg);

        this.keyInfoLayout = (LinearLayout) findViewById(R.id.keyInfoLayout);
        this.keyInfoStartTv = (TextView) findViewById(R.id.keyInfoStartTv);
        this.keyInfoEndTv = (TextView) findViewById(R.id.keyInfoEndTv);
        this.keyInfoLayout.setOnClickListener(this);
        this.keyInfoStartTv.setOnClickListener(this);
        this.keyInfoEndTv.setOnClickListener(this);
        this.timeSelector = new TimeSelector(this, new TimeSelector.ResultHandler() {
            @Override
            public void handle(String time) {
                if(timeSelector.getTitle().equals("请选择结束时间")){
                    if(time.indexOf("每周") != -1){
                        keyInfoEndTv.setText(" ~ "+time.substring(4));
                    }else{
                        keyInfoEndTv.setText(" ~ "+time);
                    }


                }else{
                    keyInfoStartTv.setText(time);
                }
            }
        }, "2018-01-30 00:00", "2030-12-31 00:00");

    }
    /**
     * 设置帐号头像
     */
    private void initUserImg(){
        if(!getIntent().getStringExtra("url").equals("null") && !getIntent().getStringExtra("url").equals("")){
            try{
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        userBitmap = ZkUtil.getHttpBitmap(getIntent().getStringExtra("url"));
                        if(userBitmap != null){
                            activity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    userImg.setImageBitmap(userBitmap);
                                }
                            });
                        }
                    }
                });

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.keyGaveBtn:
                //1：暂时有效，2：周期有效，3：永久有效
                if((int)this.tempImg.getTag() == TAG_XUAN_ZHONG){
                    keyManager.keyGaveWork(1);//添加钥匙
                }
                if((int)this.periodImg.getTag() == TAG_XUAN_ZHONG){
                    keyManager.keyGaveWork(2);//添加钥匙
                }
                if((int)this.foreverImg.getTag() == TAG_XUAN_ZHONG){
                    keyManager.keyGaveWork(3);//添加钥匙
                }
                break;
            case R.id.keyInfoLayout:
                showTimePickDialog("请选择结束时间");
                break;
            case R.id.keyInfoStartTv:
                Log.d(TAG,"keyInfoStartTv click");
                showTimePickDialog("请选择开始时间");
                break;
            case R.id.keyInfoEndTv:
                showTimePickDialog("请选择结束时间");
                Log.d(TAG,"keyInfoEndTv click");
                break;


            case R.id.foreverLayout:
                this.foreverImg.setImageResource(R.drawable.btn_xuanzhong);
                this.foreverImg.setTag(TAG_XUAN_ZHONG);
                this.tempImg.setImageResource(R.drawable.btn_weixuan);
                this.tempImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.periodImg.setImageResource(R.drawable.btn_weixuan);
                this.periodImg.setTag(TAG_WEI_XUAN_ZHONG);
                keyInfoStartTv.setText("永久");
                keyInfoEndTv.setText("");
                break;
            case R.id.tempLayout:
                Log.d(TAG, "tempLayout");
                this.foreverImg.setImageResource(R.drawable.btn_weixuan);
                this.foreverImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.tempImg.setImageResource(R.drawable.btn_xuanzhong);
                this.tempImg.setTag(TAG_XUAN_ZHONG);
                this.periodImg.setImageResource(R.drawable.btn_weixuan);
                this.periodImg.setTag(TAG_WEI_XUAN_ZHONG);

                Calendar calendar = Calendar.getInstance();
                String startTime = BriefDate.fromNature(calendar.getTime()).toString().substring(0, 16);
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                String endTime = BriefDate.fromNature(calendar.getTime()).toString().substring(0, 16);
                this.keyInfoStartTv.setText(startTime);
                this.keyInfoEndTv.setText(" ~ "+endTime);
                break;
            case R.id.periodLayout:
                this.foreverImg.setImageResource(R.drawable.btn_weixuan);
                this.foreverImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.tempImg.setImageResource(R.drawable.btn_weixuan);
                this.tempImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.periodImg.setImageResource(R.drawable.btn_xuanzhong);
                this.periodImg.setTag(TAG_XUAN_ZHONG);

                keyInfoStartTv.setText("每周三 12:00");
                keyInfoEndTv.setText(" ~ 13:00       ");
                break;
        }
    }

    /**
     * 显示不同的日期选择框
     */
    private void showTimePickDialog(String title){
        if((int)this.foreverImg.getTag() == TAG_XUAN_ZHONG){

        }
        if((int)this.tempImg.getTag() == TAG_XUAN_ZHONG){
            timeSelector.setTitle(title);
            this.timeSelector.setMode(TimeSelector.MODE.YMDHM);
            this.timeSelector.setSelectType("year");
            timeSelector.show();
        }
        if((int)this.periodImg.getTag() == TAG_XUAN_ZHONG){
            timeSelector.setTitle(title);
            this.timeSelector.setMode(TimeSelector.MODE.WHM);
            this.timeSelector.setSelectType("week");
            this.timeSelector.show();

        }
    }

}
