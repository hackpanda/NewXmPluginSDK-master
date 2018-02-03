package com.xiaomi.zkplug.blekey;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.common.ui.widget.SwitchButton;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.util.ZkUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：授权
 */
public class KeyGaveActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "AuthGaveActivity";
    private final int TAG_WEI_XUAN_ZHONG = 0x0;//未选中
    private final int TAG_XUAN_ZHONG = 0x1;//选中
    ImageView foreverImg, tempImg, periodImg;
    TextView nickNameTv;//用户nickName
    String userId;
    ImageView userImg;
    Bitmap userBitmap;
    String memberId;
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
                keyManager.keyGaveWork();//添加钥匙
                break;

            case R.id.foreverLayout:
                Toast.makeText(activity(), "foreverLayout", Toast.LENGTH_SHORT).show();
                this.foreverImg.setImageResource(R.drawable.btn_xuanzhong);
                this.foreverImg.setTag(TAG_XUAN_ZHONG);
                this.tempImg.setImageResource(R.drawable.btn_weixuan);
                this.tempImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.periodImg.setImageResource(R.drawable.btn_weixuan);
                this.periodImg.setTag(TAG_WEI_XUAN_ZHONG);
                break;
            case R.id.tempLayout:
                Log.d(TAG, "tempLayout");
                this.foreverImg.setImageResource(R.drawable.btn_weixuan);
                this.foreverImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.tempImg.setImageResource(R.drawable.btn_xuanzhong);
                this.tempImg.setTag(TAG_XUAN_ZHONG);
                this.periodImg.setImageResource(R.drawable.btn_weixuan);
                this.periodImg.setTag(TAG_WEI_XUAN_ZHONG);
                break;
            case R.id.periodLayout:
                this.foreverImg.setImageResource(R.drawable.btn_weixuan);
                this.foreverImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.tempImg.setImageResource(R.drawable.btn_weixuan);
                this.tempImg.setTag(TAG_WEI_XUAN_ZHONG);
                this.periodImg.setImageResource(R.drawable.btn_xuanzhong);
                this.periodImg.setTag(TAG_XUAN_ZHONG);
                break;
        }
    }






}
