package com.xiaomi.zkplug.blekey;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.util.ZkUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 作者：liwenqi on 17/6/13 13:34
 * 邮箱：liwenqi@zelkova.cn
 * 描述：已经被授权
 */
public class KeyExistActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "AuthExistActivity";

    ImageView userImg;


    TextView msgTv;//信息提示
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_exist);
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText("添加手机钥匙");
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        TextView nickNameTv = (TextView) findViewById(R.id.nickNameTv);
        nickNameTv.setText(getIntent().getStringExtra("nickName"));
        userImg = (ImageView) findViewById(R.id.userImg);
        initUserImg(getIntent().getStringExtra("url"));
        findViewById(R.id.confirmBtn).setOnClickListener(this);
        msgTv = (TextView) findViewById(R.id.msgTv);
        msgTv.setText(getIntent().getStringExtra("msg"));
    }

    /**
     * 设置帐号头像
     */
    private void initUserImg(String url){
        if(!ZkUtil.isNetworkAvailable(this)) return;
        if(!url.equals("null") && !TextUtils.isEmpty(url)){
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try{
                        final Bitmap userBitmap = ZkUtil.getHttpBitmap(getIntent().getStringExtra("url"));
                        Log.d(TAG, "userBitmap:"+userBitmap.toString());
                        activity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "initUseruserImg.setImageBitmap(userBitmap)mg");
                                userImg.setImageBitmap(userBitmap);
                            }
                        });
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.confirmBtn:
                finish();
                break;
        }
    }
}
