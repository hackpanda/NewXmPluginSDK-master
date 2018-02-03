package com.xiaomi.zkplug.main;

import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.smarthome.device.api.IXmPluginHostActivity;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.deviceinfo.DeviceInfoActivity;
import com.xiaomi.zkplug.dfu.DfuActivity;
import com.xiaomi.zkplug.entity.MyProvider;
import com.xiaomi.zkplug.log.LogReadActivity;
import com.xiaomi.zkplug.member.MemberManageActivity;
import com.xiaomi.zkplug.otp.OtpActivity;
import com.xiaomi.zkplug.view.GifView;

import org.json.JSONArray;

import java.util.ArrayList;

import cn.zelkova.lockprotocol.ProfileProvider;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends BaseActivity implements OnClickListener{
    private final String TAG = "MainActivity";
    ArrayList<IXmPluginHostActivity.MenuItemBase> menus = new ArrayList<>();
    JSONArray mDeviceMemberArray;// 家人列表
    //手指按下的点为(x1, y1)手指离开屏幕的点为(x2, y2)
    float x1 = 0;
    float x2 = 0;
    float y1 = 0;
    float y2 = 0;
    Device mDevice;
    View mNewFirmView;
    TextView mTitleView;
    MainViewControl mainViewControl;
    GifView openLockGif;//等待三秒动画
    ImageView refreshLsImg, memberImg, shanglaImg, openLockImg;
    private BroadcastReceiver mDeviceReceiver;//修改插件名字广播接收器
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open);
        mDevice = Device.getDevice(mDeviceStat);
        Log.d(TAG, "Mac地址： "+mDeviceStat.mac);
        mHostActivity.enableVerifyPincode();
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView(){
        shanglaImg = (ImageView) findViewById(R.id.shanglaImg);//信息条数img
        shanglaImg.setOnClickListener(this);
        mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(mDeviceStat.name);
        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableBlackTranslucentStatus();
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.title_bar_more).setOnClickListener(this);

        openLockGif = (GifView) findViewById(R.id.openLockGif);
        openLockImg = (ImageView) findViewById(R.id.openLockImg);
        refreshLsImg = (ImageView) findViewById(R.id.refreshLsImg);
        refreshLsImg.setOnClickListener(this);
        openLockImg.setOnClickListener(this);
        if(mDevice.isOwner()){
            initOwnerView();
        }else{
            initCustomerView();
        }
    }
    /**
     * 分享者界面
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCustomerView(){
        memberImg.setVisibility(View.GONE);
        mainViewControl = new MainViewControl(activity(), mDeviceStat, pluginPackage());
        mainViewControl.refreshLockStatus(true);//刷新状态
        /* 检查家人的钥匙是否可用 begin */
        boolean isSecurityChipSharedKeyValid = XmBluetoothManager.getInstance().isSecurityChipSharedKeyValid(mDevice.getMac());
        Log.d(TAG, "isSecurityChipSharedKeyValid: "+isSecurityChipSharedKeyValid);
        if (!isSecurityChipSharedKeyValid) {
            //Toast.makeText(activity, "没有被分享的钥匙", Toast.LENGTH_SHORT).show();
            mainViewControl.showNoShareKeyView();
        }else{
            findViewById(R.id.keyPeriodTv).setVisibility(View.VISIBLE);
            mainViewControl.connectSharedLock();//自动连接门锁
        }
        /* 检查家人的钥匙是否可用 end */

    }
    /**
     * owner界面
     */
    private void initOwnerView(){
        mTitleView.setOnClickListener(this);
        mNewFirmView = findViewById(R.id.title_bar_redpoint);
        /* use share icon instead of member begin */
        memberImg = (ImageView) findViewById(R.id.title_bar_share);
        memberImg.setVisibility(View.VISIBLE);
        memberImg.setOnClickListener(this);
        memberImg.setImageResource(R.drawable.member_guanli_selector);
        /* use share icon instead of member end */
        mDeviceMemberArray = new JSONArray();
        /*
        * menus
        * 临时密码
        * 设备信息
        * 检查固件升级
        * */
        IXmPluginHostActivity.IntentMenuItem intentMenuOtp = new IXmPluginHostActivity.IntentMenuItem();
        intentMenuOtp.name = "临时密码";
        intentMenuOtp.intent = mHostActivity.getActivityIntent(null, OtpActivity.class.getName());
        menus.add(intentMenuOtp);

        IXmPluginHostActivity.IntentMenuItem intentMenuDeviceInfo = new IXmPluginHostActivity.IntentMenuItem();
        intentMenuDeviceInfo.name = "设备信息";
        intentMenuDeviceInfo.intent = mHostActivity.getActivityIntent(null, DeviceInfoActivity.class.getName());
        menus.add(intentMenuDeviceInfo);

        IXmPluginHostActivity.IntentMenuItem intentMenuDfu = new IXmPluginHostActivity.IntentMenuItem();
        intentMenuDfu.name = "检查固件升级";
        intentMenuDfu.intent = mHostActivity.getActivityIntent(null, DfuActivity.class.getName());
        menus.add(intentMenuDfu);

        /*
        * 注册修改名字的广播监听
        * 准备一次性密码准备秘钥
        * 初始化数据，添加管理员
        * 刷新状态
        * */
        registerDeviceReceiver();//1.注册修改名字的广播监听
        MyProvider myProvider = new MyProvider(activity(), pluginPackage(), mDevice);
        ProfileProvider.getIns().setProvider(myProvider);//2.一次性密码准备秘钥
        mainViewControl = new MainViewControl(activity(), mDeviceStat, pluginPackage());
        mainViewControl.checkMasterNickName();//3.初始化数据，添加管理员
        mainViewControl.refreshLockStatus(true);//4.刷新状态
        mainViewControl.conneMasterctLock();//5.自动连接门锁
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_title:
                onDisplayLogReadView();
                break;
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.title_bar_more:
                //mHostActivity.openMoreMenu(null, true, -1);
                //跳转到插件下一个activity的菜单
                Intent scenceIntent = new Intent();//第一级
                scenceIntent.putExtra("scence_enable", false);
                Intent commonSettingParams = new Intent();
                commonSettingParams.putExtra("firmware_enable", false);
                commonSettingParams.putExtra("share_enable", false);
                if(mDevice.isOwner()){
                    commonSettingParams.putExtra("security_setting_enable",true);
                }
                mHostActivity.openMoreMenu2(menus, true, 0, scenceIntent,  commonSettingParams);//还有个openMenu
                break;
            case R.id.openLockImg:
                mainViewControl.openLock();
                break;
            case R.id.refreshLsImg:
                mainViewControl.refreshLockStatus(false);
                break;
            case R.id.title_bar_share:
                Intent memberIntent = new Intent();
                memberIntent.putExtra("mDeviceMemberArray", mDeviceMemberArray.toString());
                startActivity(memberIntent, MemberManageActivity.class.getName());
                break;
            case R.id.shanglaImg:
                startActivity(new Intent(), MsgActivity.class.getName());
                overridePendingTransition(R.anim.activity_open, R.anim.activity_open);//结束的动画
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            mainViewControl.showConnecSuccView();
        }else{
            mainViewControl.showConnecFailView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterDeviceReceiver();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //继承了Activity的onTouchEvent方法，直接监听点击事件
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            //当手指按下的时候
            x1 = event.getX();
            y1 = event.getY();
        }
        if(event.getAction() == MotionEvent.ACTION_UP) {
            //当手指离开的时候
            x2 = event.getX();
            y2 = event.getY();
            if(y1 - y2 > 100) {
                if(mDevice.isOwner()){
                    startActivity(new Intent(), MsgActivity.class.getName());
                    overridePendingTransition(R.anim.activity_open, R.anim.activity_open);//结束的动画
                }
            }
        }
        return super.onTouchEvent(event);
    }
    /**
     * 插件页修改设备名字
     */
    private void registerDeviceReceiver() {
        if (mDeviceReceiver == null) {
            mDeviceReceiver = new PluginReceiver();
            IntentFilter filter = new IntentFilter("action.more.rename.notify");
            filter.addAction(XmBluetoothManager.ACTION_ONLINE_STATUS_CHANGED);
            registerReceiver(mDeviceReceiver, filter);
        }
    }

    private void unregisterDeviceReceiver() {
        if (mDeviceReceiver != null) {
            unregisterReceiver(mDeviceReceiver);
            mDeviceReceiver = null;
        }
    }

    private class PluginReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "action: "+intent.getAction());
            if (intent != null && "action.more.rename.notify".equals(intent.getAction())) {

                String name = intent.getStringExtra("extra.name");
                int result = intent.getIntExtra("extra.result", 0);
                Log.d(TAG, "miio-bluetooth"+String.format("name: %s, result = %d", name, result));
                mTitleView.setText(name);
            }
            if (intent != null && XmBluetoothManager.ACTION_ONLINE_STATUS_CHANGED.equals(intent.getAction())) {

                Log.d(TAG, "登录状态广播: 登录成功");
            }
        }
    }

    // 需要点击几次 就设置几
    long [] mHits = null;
    boolean mShow = false;
    public void onDisplayLogReadView() {
        if (mHits == null) {
            mHits = new long[5];
        }
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);//把从第二位至最后一位之间的数字复制到第一位至倒数第一位
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();//记录一个时间
        if (SystemClock.uptimeMillis() - mHits[0] <= 1000) {//一秒内连续点击。
            mHits = null;	//这里说明一下，我们在进来以后需要还原状态，否则如果点击过快，第六次，第七次 都会不断进来触发该效果。重新开始计数即可
            mShow = true;
            if (mShow) {
                Toast.makeText(activity(), "已开放日志读取功能", Toast.LENGTH_SHORT).show();
                //这里是你具体的操作
                if(mDevice.isOwner() && menus.size() == 3){
                    /* begin add by wenqi for test begin */
                    IXmPluginHostActivity.IntentMenuItem intentMenuItemLog = new IXmPluginHostActivity.IntentMenuItem();
                    intentMenuItemLog.name = "日志读取";
                    intentMenuItemLog.intent = mHostActivity.getActivityIntent(null, LogReadActivity.class.getName());
                    menus.add(intentMenuItemLog);
                    /* begin add by wenqi for test end */
                }
                mShow = false;
            }
        }
    }

//    private void leScan() {
//        Log.d(TAG, "开始扫描.");
//        mBluetoothLe.setScanPeriod(15000)
//                .setReportDelay(0)
//                .startScan(this, new OnLeScanListener() {
//                    @Override
//                    public void onScanResult(BluetoothDevice bluetoothDevice, int rssi, ScanRecord scanRecord) {
//                        Log.d(TAG, "发现设备，开始连接.");
//                    }
//
//                    @Override
//                    public void onBatchScanResults(List<ScanResult> results) {
//
//                    }
//
//                    @Override
//                    public void onScanCompleted() {
//                        Log.d(TAG, "停止扫描.");
//                    }
//
//                    @Override
//                    public void onScanFailed(ScanBleException e) {
//                        Log.d(TAG, "扫描错误.");
//                    }
//                });
//    }
}

