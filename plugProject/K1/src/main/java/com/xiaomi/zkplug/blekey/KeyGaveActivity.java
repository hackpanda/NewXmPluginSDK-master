package com.xiaomi.zkplug.blekey;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.common.ui.widget.SwitchButton;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.util.ZkUtil;
import com.xiaomi.zkplug.view.TimeSelector.TimeSelector;
import com.xiaomi.zkplug.view.TimeSelector.Utils.TextUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
    TextView keyInfoTv, keyStartTv, keyEndTv, keyPeriodTv;//钥匙周期
    LinearLayout keyStartLayout, keyEndLayout, keyPeriodLayout;
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
        mTitleView.setText(R.string.member_add_key);
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
        this.foreverImg.setTag(TAG_XUAN_ZHONG);
        this.tempImg.setTag(TAG_WEI_XUAN_ZHONG);
        this.periodImg.setTag(TAG_WEI_XUAN_ZHONG);
        this.keyInfoTv = (TextView) findViewById(R.id.keyInfoTv);
        this.keyPeriodTv = (TextView) findViewById(R.id.keyPeriodTv);
        this.keyStartTv = (TextView) findViewById(R.id.keyStartTv);
        this.keyEndTv = (TextView) findViewById(R.id.keyEndTv);

        keyPeriodLayout = (LinearLayout) findViewById(R.id.keyPeriodLayout);
        keyStartLayout = (LinearLayout) findViewById(R.id.keyStartLayout);
        keyEndLayout = (LinearLayout) findViewById(R.id.keyEndLayout);
        keyPeriodLayout.setOnClickListener(this);
        keyStartLayout.setOnClickListener(this);
        keyEndLayout.setOnClickListener(this);

        this.keyEndTv.setOnClickListener(this);



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
                if(TextUtil.isEmpty(keyEndTv.getText().toString())){
                    Toast.makeText(activity(), R.string.blekey_end_select, Toast.LENGTH_LONG).show();
                    return;
                }
                //1：暂时有效，2：周期有效，3：永久有效
                if((int)this.tempImg.getTag() == TAG_XUAN_ZHONG){
                    Log.d("KeyManager", "临时");
                    keyManager.keyGaveWork(1);//添加钥匙
                }
                if((int)this.periodImg.getTag() == TAG_XUAN_ZHONG){
                    Log.d("KeyManager", "周期");
                    keyManager.keyGaveWork(2);//添加钥匙
                }
                if((int)this.foreverImg.getTag() == TAG_XUAN_ZHONG){
                    Log.d("KeyManager", "永久");
                    keyManager.keyGaveWork(3);//添加钥匙
                }
                break;
            case R.id.keyPeriodLayout:

                keyManager.showPeriodPickDialog();
                break;
            case R.id.keyStartLayout:
                showTimePickDialog("请选择生效时间");
                break;
            case R.id.keyEndLayout:
                showTimePickDialog(getString(R.string.blekey_end_select));
                break;


            case R.id.foreverLayout:
                this.foreverImg.setImageResource(R.drawable.btn_xuanzhong);
                this.foreverImg.setTag(TAG_XUAN_ZHONG);
                this.tempImg.setImageResource(R.drawable.btn_weixuan);
                this.tempImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.periodImg.setImageResource(R.drawable.btn_weixuan);
                this.periodImg.setTag(TAG_WEI_XUAN_ZHONG);
                keyInfoTv.setText(R.string.blekey_valid_forever);
                keyPeriodLayout.setVisibility(View.GONE);
                keyStartLayout.setVisibility(View.GONE);
                keyEndLayout.setVisibility(View.GONE);
                break;
            case R.id.tempLayout:
                Log.d(TAG, "tempLayout");
                this.foreverImg.setImageResource(R.drawable.btn_weixuan);
                this.foreverImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.tempImg.setImageResource(R.drawable.btn_xuanzhong);
                this.tempImg.setTag(TAG_XUAN_ZHONG);
                this.periodImg.setImageResource(R.drawable.btn_weixuan);
                this.periodImg.setTag(TAG_WEI_XUAN_ZHONG);

                keyInfoTv.setText(R.string.blekey_temp_valid);
                keyPeriodLayout.setVisibility(View.GONE);
                keyStartLayout.setVisibility(View.GONE);
                keyEndLayout.setVisibility(View.VISIBLE);

                break;
            case R.id.periodLayout:
                this.foreverImg.setImageResource(R.drawable.btn_weixuan);
                this.foreverImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.tempImg.setImageResource(R.drawable.btn_weixuan);
                this.tempImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.periodImg.setImageResource(R.drawable.btn_xuanzhong);
                this.periodImg.setTag(TAG_XUAN_ZHONG);
                keyInfoTv.setText("钥匙在所选周期的时间段内重复有效（例如：每周一和周三的12：10~13：10有效）");
                keyPeriodLayout.setVisibility(View.VISIBLE);
                keyStartLayout.setVisibility(View.VISIBLE);
                keyEndLayout.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * 显示不同的日期选择框
     */
    private void showTimePickDialog(String title){

        if((int)this.tempImg.getTag() == TAG_XUAN_ZHONG){
            SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String startTime = sdFormat.format(new Date());
            this.timeSelector = new TimeSelector(this, new TimeSelector.ResultHandler() {
                @Override
                public void handle(String time) {
                    keyEndTv.setText(time);
                }
            }, startTime, "2030-12-31 23:59");
            timeSelector.setTitle(title);
            this.timeSelector.setMode(TimeSelector.MODE.YMDHM);
            timeSelector.show();
        }
//        if((int)this.periodImg.getTag() == TAG_XUAN_ZHONG){
//            this.timeSelector = new TimeSelector(this, new TimeSelector.ResultHandler() {
//                @Override
//                public void handle(String time) {
//
//                    if(timeSelector.getTitle().equals("请选择到期时间")){
//                        if((int)periodImg.getTag() == TAG_XUAN_ZHONG){
//                            keyStartTv.setText(time.substring(11));
//                        }else{
//                            keyStartTv.setText(time);
//                        }
//                    }else{
//                        if((int)periodImg.getTag() == TAG_XUAN_ZHONG){
//                            keyEndTv.setText(time.substring(11));
//                        }else{
//                            keyEndTv.setText(time);
//                        }
//                    }
//                }
//            }, "2018-01-30 00:00", "2018-01-30 23:59");
//            timeSelector.setTitle(title);
//            this.timeSelector.setMode(TimeSelector.MODE.HM);
//            this.timeSelector.show();
//        }


    }


}
