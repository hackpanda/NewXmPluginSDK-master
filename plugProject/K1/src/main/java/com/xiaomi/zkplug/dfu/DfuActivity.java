package com.xiaomi.zkplug.dfu;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qindachang.bluetoothle.BluetoothLe;
import com.qindachang.bluetoothle.OnLeConnectListener;
import com.qindachang.bluetoothle.OnLeScanListener;
import com.qindachang.bluetoothle.exception.ConnBleException;
import com.qindachang.bluetoothle.exception.ScanBleException;
import com.qindachang.bluetoothle.scanner.ScanRecord;
import com.qindachang.bluetoothle.scanner.ScanResult;
import com.xiaomi.smarthome.bluetooth.Response;
import com.xiaomi.smarthome.bluetooth.XmBluetoothManager;
import com.xiaomi.smarthome.common.ui.dialog.MLAlertDialog;
import com.xiaomi.smarthome.common.ui.dialog.XQProgressDialog;
import com.xiaomi.smarthome.device.api.BtFirmwareUpdateInfo;
import com.xiaomi.smarthome.device.api.Callback;
import com.xiaomi.smarthome.device.api.DeviceUpdateInfo;
import com.xiaomi.smarthome.device.api.XmPluginHostApi;
import com.xiaomi.zkplug.BaseActivity;
import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;
import com.xiaomi.zkplug.entity.MyEntity;
import com.xiaomi.zkplug.lockoperater.ILockDataOperator;
import com.xiaomi.zkplug.lockoperater.LockOperateCallback;
import com.xiaomi.zkplug.lockoperater.OperatorBuilder;
import com.xiaomi.zkplug.lockoperater.SecureStatus;
import com.xiaomi.zkplug.util.BitConverter;
import com.xiaomi.zkplug.util.DataManageUtil;
import com.xiaomi.zkplug.util.DataUpdateCallback;
import com.xiaomi.zkplug.util.ZkUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.zelkova.lockprotocol.BriefDate;
import cn.zelkova.lockprotocol.LockCmdGetStatus;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;


public class DfuActivity extends BaseActivity implements View.OnClickListener{

    private final String TAG = "DfuActivity";
    private static final String SERVICE_UUID = "6E401001-B5A3-F393-E0A9-E50E24DCCA0E";
    private static final String WRITE_UUID = "6E401003-B5A3-F393-E0A9-E50E24DCCA0E";
    private static final String DEVICE_NAME = "ZkDFU";

    private final int MSG_GET_VER_TIME_OUT = 0x10;//超时
    private final int MSG_CHECK_UPDATE_INFO = 0x11;
    private final int MSG_ENTER_DFU_TIME_OUT = 0x20;//启动DFU超时
    private final int MSG_CONNECT_DFU = 0x21;//连接DFU设备
    private boolean isDisvocerService = false;
    TextView curRomVer, newestRomVer;
    ImageView checkImg;
    LinearLayout dfuProgressView;//更新进程View,用来做布局判断
    Button checkVersionBtn;//用来做布局判断
    private BluetoothLe mBluetoothLe;
    private BluetoothDevice mBluetoothDevice;
    static final int MSG_UPDATE_FIRM = 1;
    ILockDataOperator iStatusOperator;
    Device mDevice;
    TextView dfuProgressTv;
    DataManageUtil dataManageUtil;
    XQProgressDialog xqProgressDialog;//下载rom包和扫描dfu进度条
    DfuManager dfuManager;
    String mFilePath;//Rom下载后路径
    private DeviceUpdateInfo updateInfo;//服务器Rom版本信息
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dfu);

        // 设置titlebar在顶部透明显示时的顶部padding
        mHostActivity.setTitleBarPadding(findViewById(R.id.title_bar));
        mHostActivity.enableWhiteTranslucentStatus();
        TextView mTitleView = ((TextView) findViewById(R.id.title_bar_title));
        mTitleView.setText(R.string.main_menu_dfu_check);
        Button updateBtn = (Button) findViewById(R.id.updateBtn);
        xqProgressDialog = new XQProgressDialog(this);
        xqProgressDialog.setCancelable(false);
        dfuProgressTv = (TextView) findViewById(R.id.dfuProgressTv);
        curRomVer = (TextView) findViewById(R.id.curRomVer);
        newestRomVer = (TextView) findViewById(R.id.newestRomVer);
        dfuProgressView = (LinearLayout) findViewById(R.id.dfuProgressView);
        checkVersionBtn = (Button) findViewById(R.id.checkVersionBtn);
        this.dataManageUtil = new DataManageUtil(mDeviceStat, this);
        dfuManager = new DfuManager(activity());
        this.mDevice = Device.getDevice(mDeviceStat);
        mBluetoothLe = BluetoothLe.getDefault();
        mBluetoothLe.init(this);

        if (!mBluetoothLe.isBluetoothOpen()) {
            mBluetoothLe.enableBluetooth(this);
        }
        updateBtn.setOnClickListener(this);
        findViewById(R.id.reupdateBtn).setOnClickListener(this);
        findViewById(R.id.title_bar_return).setOnClickListener(this);
        findViewById(R.id.checkVersionBtn).setOnClickListener(this);
        checkImg = (ImageView) findViewById(R.id.checkImg);


        ZkUtil.startAnima(this, checkImg);//旋转
        XmBluetoothManager.getInstance().disconnect(mDevice.getMac());

        viewHanlder.sendEmptyMessageDelayed(MSG_CHECK_UPDATE_INFO, 2000);

    }
    /**
     * 时序: 用于界面控制
     */
    Handler viewHanlder = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_GET_VER_TIME_OUT:
                    Log.d(TAG, "MSG_GET_VER_TIME_OUT");
                    showGetVerFailView("");
                    break;
                case MSG_CHECK_UPDATE_INFO:
                    initDfuView();//检查固件版本
                    break;
                case MSG_ENTER_DFU_TIME_OUT:
                    Log.d(TAG, "MSG_ENTER_DFU_TIME_OUT");
                    Button checkVersionBtn = (Button) findViewById(R.id.checkVersionBtn);
                    LinearLayout  dfuFailView = (LinearLayout) findViewById(R.id.dfuFailView);
                    if(checkVersionBtn.getVisibility() == View.VISIBLE || dfuFailView.getVisibility() == View.VISIBLE){

                        XmBluetoothManager.getInstance().disconnect(mDeviceStat.mac);
                        if(iStatusOperator != null) iStatusOperator.unregisterBluetoothReceiver();
                        xqProgressDialog.dismiss();
                        Toast.makeText(activity(), R.string.connect_time_out, Toast.LENGTH_LONG).show();
                    }
                    break;
                case MSG_CONNECT_DFU:
                    connect();
                    break;
                default:
                    break;
            }
        }
    };


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_bar_return:
                finish();
                break;
            case R.id.reupdateBtn:
                if(!ZkUtil.isNetworkAvailable(this)){
                    Toast.makeText(activity(), R.string.network_not_avilable, Toast.LENGTH_LONG).show();
                    return;
                }
                if(!ZkUtil.isBleOpen()){
                    Toast.makeText(activity(), R.string.open_bluetooth, Toast.LENGTH_LONG).show();
                    return;
                }
                xqProgressDialog.setMessage(getString(R.string.dfu_send_request));
                xqProgressDialog.show();
                scan();
                break;
            case R.id.updateBtn:
                if(!ZkUtil.isNetworkAvailable(this)){
                    Toast.makeText(activity(), R.string.network_not_avilable, Toast.LENGTH_LONG).show();
                    return;
                }
                if(!ZkUtil.isBleOpen()){
                    Toast.makeText(activity(), R.string.open_bluetooth, Toast.LENGTH_LONG).show();
                    return;
                }
                xqProgressDialog.setMessage(getString(R.string.dfu_send_request));
                xqProgressDialog.show();
                downLoadRom();//先下载文件
                break;
            case R.id.checkVersionBtn:
                showCheckVerDialog();
                break;
        }
    }

    /**
     * 检查锁内固件版本号
     */
    private void showCheckVerDialog(){
        MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
        builder.setTitle(R.string.dfu_is_newest);
        builder.setMessage(R.string.dfu_want_check);
        builder.setPositiveButton(R.string.gloable_confirm, new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getLockStatus(true);
            }
        });
        builder.setNegativeButton(R.string.gloable_cancel, new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }
    /**
     * 检查后锁内固件仍是最新
     */
    private void showConfirmDialog(){
        viewHanlder.removeMessages(MSG_GET_VER_TIME_OUT);
        MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
        builder.setTitle(R.string.dfu_check_result);
        builder.setMessage(R.string.dfu_not_need_update);
        builder.setNegativeButton(R.string.gloable_confirm, new MLAlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }
    @Override
    public void onResume() {
        super.onResume();
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothLe.getConnected()) {
            mBluetoothLe.destroy();
            mBluetoothLe.close();
        }
    }

    private void scan() {
        Log.d(TAG, "begin scan.");
        viewHanlder.sendEmptyMessageDelayed(MSG_ENTER_DFU_TIME_OUT, 25*1000);
        mBluetoothLe.setScanPeriod(18000)
                .setScanWithServiceUUID(SERVICE_UUID)
                .setScanWithDeviceName(DEVICE_NAME)
                .setReportDelay(0)
                .startScan(this, new OnLeScanListener() {
                    @Override
                    public void onScanResult(BluetoothDevice bluetoothDevice, int rssi, ScanRecord scanRecord) {
                        Log.d(TAG, "发现设备，开始连接.");
                        XmBluetoothManager.getInstance().disconnect(mDevice.getMac());//主动断开一下默认的连接
                        mBluetoothDevice = bluetoothDevice;
                        mBluetoothLe.stopScan();
                        //viewHanlder.sendEmptyMessageDelayed(MSG_CONNECT_DFU, 3 * 1000);

                        connect();
                    }

                    @Override
                    public void onBatchScanResults(List<ScanResult> results) {

                    }

                    @Override
                    public void onScanCompleted() {
                        Log.d(TAG, "停止扫描.");
                        if (mBluetoothDevice == null) {
                            Log.d(TAG, "没有发现设备.");
                            viewHanlder.removeMessages(MSG_ENTER_DFU_TIME_OUT);
                            Toast.makeText(activity(), R.string.dfu_reboot_ble, Toast.LENGTH_LONG).show();
                            XmBluetoothManager.getInstance().disconnect(mDeviceStat.mac);
                            if(iStatusOperator != null) iStatusOperator.unregisterBluetoothReceiver();
                            xqProgressDialog.dismiss();
                        }
                    }

                    @Override
                    public void onScanFailed(ScanBleException e) {
                        Log.d(TAG, "连接成功，扫描错误."+e.getDetailMessage());
                        Toast.makeText(activity(), R.string.dfu_not_reset_fount, Toast.LENGTH_LONG).show();
                        XmBluetoothManager.getInstance().disconnect(mDeviceStat.mac);
                        iStatusOperator.unregisterBluetoothReceiver();
                        xqProgressDialog.dismiss();
                        viewHanlder.removeMessages(MSG_ENTER_DFU_TIME_OUT);
                    }
                });
    }

    private void connect() {
        Log.d(TAG,mBluetoothDevice.getAddress()+", "+mBluetoothDevice.getName());
        mBluetoothLe.startConnect(mBluetoothDevice, new OnLeConnectListener() {
            @Override
            public void onDeviceConnecting() {

            }

            @Override
            public void onDeviceConnected() {
                Log.d(TAG, "连接成功，正在发现服务.");
            }

            @Override
            public void onDeviceDisconnected() {
                Log.d(TAG, "断开连接.");
                //Toast.makeText(activity(), "连接断开", Toast.LENGTH_LONG).show();
                //iStatusOperator.unregisterBluetoothReceiver();
                //xqProgressDialog.dismiss();
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt) {
                viewHanlder.removeMessages(MSG_ENTER_DFU_TIME_OUT);
                Log.d(TAG, "已发现服务，可以升级了.");
                isDisvocerService = true;
                Log.d(TAG, "正在升级，请等待升级成功");
                mBluetoothLe.writeDataToCharacteristic(new byte[]{-1, -2, -3}, SERVICE_UUID, WRITE_UUID);
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        dfuManager.startDFU(mBluetoothDevice, true, false, true, 0, mFilePath);
                    }
                });

            }

            @Override
            public void onDeviceConnectFail(ConnBleException e) {
                Log.d(TAG, "连接失败.");
                Toast.makeText(activity(), R.string.dfu_connect_fail, Toast.LENGTH_LONG).show();
                xqProgressDialog.dismiss();
                iStatusOperator.unregisterBluetoothReceiver();
                viewHanlder.removeMessages(MSG_ENTER_DFU_TIME_OUT);
            }
        });
    }

    DfuProgressListener mDfuProgressListener = new DfuProgressListener() {

        @Override
        public void onDeviceConnecting(String deviceAddress) {
            //当DFU服务开始与DFU目标连接时调用的方法
            Log.d(TAG, "升级服务开始与硬件设备连接.");
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            //方法在服务成功连接时调用，发现服务并在DFU目标上找到DFU服务。
            Log.d(TAG, "升级服务连接成功.");
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            //当DFU进程启动时调用的方法。 这包括读取DFU版本特性，发送DFU START命令以及Init数据包（如果设置）。
            Log.d(TAG, "升级进程启动.");
            xqProgressDialog.dismiss();
            dfuManager.showDfuProgressView();//界面准备
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            //当DFU进程启动和要发送的字节时调用的方法。
            Log.d(TAG, "DFU进程启动和要发送的字节" + deviceAddress);
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            //当服务发现DFU目标处于应用程序模式并且必须切换到DFU模式时调用的方法。 将发送开关命令，并且DFU过程应该再次开始。 此调用后不会有onDeviceDisconnected（String）事件。
            Log.d(TAG, "硬件设备切换到升级模式.");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            //在上传固件期间调用的方法。 它不会使用相同的百分比值调用两次，但是在小型固件文件的情况下，可能会省略一些值。\
            Log.d(TAG, "进度：" + percent + "%");
            if(checkVersionBtn.getVisibility() == View.VISIBLE){
                dfuManager.showDfuProgressView();
            }
            dfuProgressTv.setText(percent + "%");
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            //在目标设备上验证新固件时调用的方法。
            Log.d(TAG, "硬件设备正在验证新固件！");
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            //服务开始断开与目标设备的连接时调用的方法。
            Log.d(TAG, "服务开始断开设备连接！");
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            //当服务从设备断开连接时调用的方法。 设备已重置。
            Log.d(TAG, "硬件设备已重置！");
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            //Method called when the DFU process succeeded.
            Log.d(TAG, "升级成功！");
            dfuManager.showDfuSuccView(getString(R.string.dfu_cur_version)+updateInfo.mNewVersion);
            try {
                JSONObject settingsObj = new JSONObject();
                //settingsObj.put("keyid_romver_data", newestRomVer.getText().toString().replace("最新版本: ", ""));
                settingsObj.put("keyid_romver_data", updateInfo.mNewVersion);
                dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                    @Override
                    public void dataUpateFail(int i, String s) {
                        Log.d(TAG, "数据存储失败！");
                    }

                    @Override
                    public void dataUpdateSucc(String s) {
                        Log.d(TAG, "数据存储成功！");
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            //当DFU进程已中止时调用的方法。
            Log.d(TAG, "升级进程已中止");
            dfuManager.showDfuFailView();
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            //发生错误时调用的方法。
            Log.d(TAG, "升级发生错误");
            dfuManager.showDfuFailView();
        }
    };

    /**
     * 更新Rom：dfu
     * think:已经是DFU模式了
     *
     */
    private void doDfuWork(){
        scan();
        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "启动DFU模式");
            dfuManager.enterDfuMode(mDevice, true, false, true, 0);//启动DFU模式

        }else{
            Log.d(TAG, "未连接，先进行连接----");
            XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                @Override
                public void onResponse(int i, Bundle bundle) {
                    if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                        dfuManager.enterDfuMode(mDevice, true, false, true, 0);//启动DFU模式
                    }else if(i == XmBluetoothManager.Code.REQUEST_NOT_REGISTERED){
                        Toast.makeText(activity(), R.string.device_has_been_reset, Toast.LENGTH_LONG).show();
                        xqProgressDialog.dismiss();
                    }else {
                        //Toast.makeText(activity(), "未发现门锁，请靠近门锁重试", Toast.LENGTH_LONG).show();
                        //xqProgressDialog.dismiss();
                        //XmBluetoothManager.getInstance().disconnect(mDevice.getMac());
                    }
                }
            });
        }
    }

    /**
     * 检查固件版本, 加载不同布局
     */
    private void initDfuView(){
        mDevice.checkDeviceUpdateInfo(new Callback<DeviceUpdateInfo>() {

            @Override
            public void onSuccess(DeviceUpdateInfo updateInfo) {
                Message.obtain(mHandler, MSG_UPDATE_FIRM, updateInfo).sendToTarget();
            }

            @Override
            public void onFailure(int arg0, String arg1) {

            }
        });
    }
    /**
     * 下载更新包
     */
    private void downLoadRom(){
        XmPluginHostApi.instance().getBluetoothFirmwareUpdateInfo(MyEntity.MODEL_ID, new Callback<BtFirmwareUpdateInfo>() {
            @Override
            public void onSuccess(BtFirmwareUpdateInfo btFirmwareUpdateInfo) {
                XmPluginHostApi.instance().downloadBleFirmware(btFirmwareUpdateInfo.url, new Response.BleUpgradeResponse() {
                    @Override
                    public void onProgress(int i) {
                        Log.d(TAG, "download_onProgress:"+i);
                    }

                    @Override
                    public void onResponse(int i, String s) {

                        Log.d(TAG, "file path:"+ s+", i: "+i);
                        mFilePath = s.replace("lock.c1.bin", "zip");
                        File file = new File(s);
                        File newFile = new File(mFilePath);
                        file.renameTo(newFile);
                        Log.d(TAG, "file new path:"+newFile.getAbsolutePath());
                        doDfuWork();//文件下载成功开始DFU流程
                    }
                });
            }

            @Override
            public void onFailure(int i, String s) {
                Log.d("debug", "getBluetoothFirmwareUpdateInfo:  Fail");
                Toast.makeText(activity(), R.string.dfu_ver_download_fail, Toast.LENGTH_LONG);
                xqProgressDialog.dismiss();
            }
        });
    }

    /**
     * 获取版本失败时提示
     */
    private void showGetVerFailView(String value){
        Log.d(TAG, "showGetVerFailView");
        xqProgressDialog.dismiss();
        XmBluetoothManager.getInstance().disconnect(mDevice.getMac());
        iStatusOperator.unregisterBluetoothReceiver();
        viewHanlder.removeMessages(MSG_GET_VER_TIME_OUT);

        if(value.indexOf(getString(R.string.device_cmd_timeout)) != -1){//需要同步时间
            ZkUtil.showCmdTimeOutView(activity());
        }else if(value.equals(getString(R.string.device_has_been_reset))){
            Toast.makeText(activity(), value, Toast.LENGTH_LONG).show();
            finish();
        }else{
            iStatusOperator.unregisterBluetoothReceiver();
            final MLAlertDialog.Builder builder = new MLAlertDialog.Builder(activity());
            builder.setTitle(R.string.dfu_ver_read_fail);
            builder.setMessage(R.string.dfu_update_need);
            builder.setPositiveButton(getString(R.string.dfu_update_want), new MLAlertDialog.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(!ZkUtil.isNetworkAvailable(activity())){
                        Toast.makeText(activity(), R.string.network_not_avilable, Toast.LENGTH_LONG).show();
                        return;
                    }
                    if(!ZkUtil.isBleOpen()){
                        Toast.makeText(activity(), R.string.open_bluetooth, Toast.LENGTH_LONG).show();
                        return;
                    }
                    xqProgressDialog.setMessage(getString(R.string.dfu_send_request));
                    xqProgressDialog.show();
                    downLoadRom();//先下载文件

                }
            });
            builder.setNegativeButton(R.string.device_info_exit, new MLAlertDialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                }
            });
            builder.show();
        }
    }
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_UPDATE_FIRM:
                // 刷新固件升级状态
                if (!mDevice.isOwner()) {//被分享者
                    return;
                }
                updateInfo = (DeviceUpdateInfo) msg.obj;
                JSONArray mJsArray = new JSONArray();
                mJsArray.put("keyid_romver_data");
                dataManageUtil.queryDataFromServer(mJsArray, new DataUpdateCallback() {
                    @Override
                    public void dataUpateFail(int i, String s) {
                        Toast.makeText(activity(), R.string.dfu_query_fail, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void dataUpdateSucc(final String s) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    JSONObject resultObj = new JSONObject(s);
                                    Log.d(TAG, "resultObj:"+resultObj.toString());
                                    if(dfuProgressView.getVisibility() == View.GONE){//用来处理异常情况，在更新中，就不用显示初始布局了
                                        if(resultObj.has("keyid_romver_data") && !TextUtils.isEmpty(resultObj.getString("keyid_romver_data"))){
                                            showDfuUpdateView(resultObj.getString("keyid_romver_data"), updateInfo);//展示更新界面
                                        }else{
                                            getLockStatus(false);
                                            return;
                                        }
                                    }

                                }catch (JSONException e){
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
                break;
            default:
                break;
        }
    }

    /**
     * 根据当前版本号，展示不同的布局
     *
     * @param romver
     * @param updateInfo
     */
    private void showDfuUpdateView(String romver, DeviceUpdateInfo updateInfo){
        if(iStatusOperator != null){
            iStatusOperator.unregisterBluetoothReceiver();
        }
        viewHanlder.removeMessages(MSG_GET_VER_TIME_OUT);
        Log.d(TAG, "读取Rom版本成功:"+romver);
        ZkUtil.stopAnima(checkImg);
        curRomVer.setText(getString(R.string.dfu_cur_version)+romver);
        newestRomVer.setText(getString(R.string.dfu_newest_version)+updateInfo.mNewVersion);
        if(romver.equals(updateInfo.mNewVersion) || romver.equals(getString(R.string.dfu_is_newest))){
            dfuManager.showDfuSuccView(getString(R.string.dfu_cur_version)+romver);
            Log.d(TAG, "已是最新版本");
        }else{
            curRomVer.setText(getString(R.string.dfu_cur_version)+romver);
            newestRomVer.setText(getString(R.string.dfu_newest_version)+updateInfo.mNewVersion);
            TextView featureListTv = (TextView) findViewById(R.id.featureListTv);
            featureListTv.setText(updateInfo.mUpdateDes);
            LinearLayout dfuInitView = (LinearLayout) findViewById(R.id.dfuInitView);
            dfuInitView.setVisibility(View.VISIBLE);
            LinearLayout emptyView = (LinearLayout) findViewById(R.id.emptyView);
            emptyView.setVisibility(View.GONE);
        }
    }
    /**
     *
     * 获取锁状态命令
     * @return
     */
    private LockCmdGetStatus getLockStatusCmd(){
        Calendar cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, -180);
        BriefDate vFrom = BriefDate.fromNature(cld.getTime());
        cld = Calendar.getInstance();
        cld.add(Calendar.SECOND, 180);
        BriefDate vTo = BriefDate.fromNature(cld.getTime());
        LockCmdGetStatus lockCmdGetStatus = new LockCmdGetStatus(BitConverter.convertMacAdd(mDevice.getMac()), vFrom, vTo);
        Log.d(TAG, "getLockStatusCmd()");
        return lockCmdGetStatus;
    }
    /**
     * 进入首届面获取门锁状态
     * 进行更新门锁状态和授时
     */
    private void getLockStatus(final boolean isFromCheckVerClick){
        Log.d(TAG, "getLockStatus开始");
        viewHanlder.sendEmptyMessageDelayed(MSG_GET_VER_TIME_OUT, 25* 1000);
        iStatusOperator = OperatorBuilder.create(SecureStatus.FLAG, activity(), mDevice);
        iStatusOperator.registerBluetoothReceiver();
        if(isFromCheckVerClick){
            xqProgressDialog.setMessage(getString(R.string.dfu_ver_checking));
            xqProgressDialog.show();
        }
        if (XmBluetoothManager.getInstance().getConnectStatus(mDevice.getMac()) == BluetoothProfile.STATE_CONNECTED) {
            iStatusOperator.sendLockMsg(getLockStatusCmd().getBytes(), new LockOperateCallback() {
                @Override
                public void lockOperateSucc(String value) {//value为时间差
                    //判断锁内时间偏差

                    xqProgressDialog.dismiss();
                    try {

                        JSONObject statusObj = new JSONObject(value);
                        JSONObject settingsObj = new JSONObject();
                        settingsObj.put("keyid_romver_data", statusObj.getString("romver"));
                        dataManageUtil.saveDataToServer(settingsObj, new DataUpdateCallback() {
                            @Override
                            public void dataUpateFail(int i, String s) {
                                Log.d(TAG, "数据存储失败！");
                            }

                            @Override
                            public void dataUpdateSucc(String s) {
                                Log.d(TAG, "数据存储成功！");
                            }
                        });
                        if(statusObj.getString("romver").equals(updateInfo.mNewVersion) && isFromCheckVerClick){
                            iStatusOperator.unregisterBluetoothReceiver();
                            showConfirmDialog();//检查后仍是最新的
                        }else{
                            showDfuUpdateView(statusObj.getString("romver"), updateInfo);//展示更新界面
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void lockOperateFail(String value) {
                    Log.d(TAG, "getLockStatus:"+value);
                    showGetVerFailView(value);
                    Log.d(TAG, "lockOperateFail-status");
                }
            });
            Log.d(TAG, "getLockStatus：已连接，直接发送锁命令");
        } else {
            //未连接就先建立连接
            Log.d(TAG, "getLockStatus：未连接就先建立连接");
            XmBluetoothManager.getInstance().securityChipConnect(mDevice.getMac(), new Response.BleConnectResponse() {
                @Override
                public void onResponse(int i, Bundle bundle) {
                    Log.d(TAG, "回调状态xxxx:" + i);
                    if (i == XmBluetoothManager.Code.REQUEST_SUCCESS) {
                        iStatusOperator.sendLockMsg(getLockStatusCmd().getBytes(), new LockOperateCallback() {
                            @Override
                            public void lockOperateSucc(String value) {
                                //判断锁内时间偏差
                                xqProgressDialog.dismiss();
                                try {
                                    JSONObject statusObj = new JSONObject(value);
                                    dataManageUtil.saveDataToServer(statusObj, new DataUpdateCallback() {
                                        @Override
                                        public void dataUpateFail(int i, String s) {
                                            Log.d(TAG, "状态数据存储失败！");
                                        }

                                        @Override
                                        public void dataUpdateSucc(String s) {
                                            Log.d(TAG, "状态数据存储成功！");
                                        }
                                    });
                                    if(statusObj.getString("romver").equals(updateInfo.mNewVersion) && isFromCheckVerClick){
                                        iStatusOperator.unregisterBluetoothReceiver();
                                        showConfirmDialog();//检查后仍是最新的
                                    }else{
                                        showDfuUpdateView(statusObj.getString("romver"), updateInfo);//展示更新界面
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            @Override
                            public void lockOperateFail(String value) {
                                Log.d(TAG, "getLockStatus:"+value);
                                showGetVerFailView(value);
                            }
                        });
                    }else if(i == XmBluetoothManager.Code.REQUEST_NOT_REGISTERED){
                        showGetVerFailView(getResources().getString(R.string.device_has_been_reset));
                    }
//                    else{
//                        Log.d(TAG, "xxxxxxxxxxxxxxx");
//                        showGetVerFailView("");
//                    }
                }
            });
        }
    }
}
