/**
 *
 */
package cn.zelkova.lockprotocol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 负责与BLE设备通讯，发送指令和接收设备返回的指令
 *
 * @author zp
 */
public class BleLockConnector implements WhatHappenCallback {

    /**
     * 与BLE设备连接状态
     *
     * @author zp
     */
    public enum ConnectionStateEnum {

        /**
         * 设备断开状态
         */
        Disconnected(0),

        /**
         * 与设备连接状态
         */
        Connected(2);

        private int state;

        ConnectionStateEnum(int val) {
            this.state = val;
        }
    }


    /**
     * 用来通知外部与BLE设备交互和执行的结果，异步执行时使用此接口
     *
     * @author zp
     */
    public interface IResultCallback {

        /**
         * 与设备连接状态发生改变时被调用
         *
         * @param mac   设备的mac地址
         * @param state 连接状态
         * @param ex    执行过程中发生的异常信息，为null则表示成功执行
         */
        void onConnectionState(String mac, ConnectionStateEnum state, Exception ex);

        /**
         * 异步操作发送给设备指令，执行完成后被调用
         *
         * @param mac      设备的mac地址
         * @param ex       执行过程中发生的异常信息，为null则表示成功执行
         * @param response 执行指令后返回的信息
         */
        void onExecutionResult(String mac, Exception ex, LockCommResponse response);
    }


    private class BleGattCallback extends cn.zelkova.lockprotocol.BluetoothGattCallback {

        public BleGattCallback(WhatHappenCallback say) {
            super(say);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            /* 任何异常状态都做回收 by wenqi begin */
            if (newState == BluetoothProfile.STATE_CONNECTED) {//优化前是BluetoothAdapter.STATE_CONNECTED
                letMeKnow("\tdiscoverServices：" + gatt.discoverServices());

            } else {//优化前是BluetoothAdapter.STATE_DISCONNECTED
                String disMac;
                synchronized (BleLockConnector.this.callbackSignal) {
                    disMac = BleLockConnector.this.bleRemoteGatt.getDevice().getAddress();
                    BleLockConnector.this.bleRemoteGatt.close();
                    BleLockConnector.this.bleRemoteGatt = null;
                    BleLockConnector.this.bleRemoteDevice = null;

                    BleLockConnector.this.callbackSignal.notifyAll();
                    letMeKnow("连接已断开，线程同步notifyAll");
                }

                letMeKnow("+++++===== closed & cleared:" + disMac + " =====+++++");
                //记录断开时间
                LastDisconnectTime = new Date();
                BleLockConnector.this.lastException = null;
                onConnectionStateCallback(ConnectionStateEnum.Disconnected);


            }

            /* 任何异常状态都做回收 by wenqi end */
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            enableTXNotification();
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            synchronized (BleLockConnector.this.callbackSignal) {
                BleLockConnector.this.callbackSignal.connected = BleCallbackSignal.Done;
                BleLockConnector.this.callbackSignal.notifyAll();
                letMeKnow("连接状态就绪，线程同步notifyAll");
            }
        }

        //拆包命令发送同步
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            synchronized (BleLockConnector.this.callbackSignal) {
                BleLockConnector.this.callbackSignal.writeRX = BleCallbackSignal.Done;
                BleLockConnector.this.callbackSignal.notifyAll();
            }
        }

        //接收设备发来的数据
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            byte[] val = characteristic.getValue();
            Collections.addAll(inputBuffer, BitConverter.toPackaged(val));
            LockCommResponse result = LockCommBase.parse(inputBuffer);
            if (result == null) return;

            letMeKnow("解析出一个协议：[" + result.getCmdId() + "]" + result.getCmdName());

            if (result.getCmdId() == LockCommSessionTokenResponse.CMD_ID) {
                synchronized (BleLockConnector.this.callbackSignal) {

                    BleLockConnector.this.ssToken = (LockCommSessionTokenResponse) result;

                    BleLockConnector.this.callbackSignal.sessionToken = BleCallbackSignal.Done;
                    BleLockConnector.this.callbackSignal.notifyAll();

                    return;
                }
            }

            //由send发来的同步方式
            if (BleLockConnector.this.callbackSignal.command == BleCallbackSignal.Doing) {
                synchronized (BleLockConnector.this.callbackSignal) {
                    BleLockConnector.this.sendToClient = result;
                    BleLockConnector.this.callbackSignal.command = BleCallbackSignal.Done;
                    BleLockConnector.this.callbackSignal.notifyAll();
                }
                return;
            }

            cancelReceiveTimer();
            onResultCallbackForExecute(result);
        }
    }


    /***
     * 做连接扫描用，不需要实际执行内容
     */
    private class NoopLeScanCallback implements BluetoothAdapter.LeScanCallback {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            String logTag = BleLockConnector.TAG_BLE;
            if (BleLockConnector.this.macAddress.toLowerCase().equals(device.getAddress().toLowerCase())) {
                BleLockConnector.this.bleRemoteDevice = device;
                synchronized (BleLockConnector.this.callbackSignal) {
                    BleLockConnector.this.callbackSignal.connected = BleCallbackSignal.Done;
                    BleLockConnector.this.callbackSignal.notifyAll();
                }
                Log.i(logTag, "found it, stop bleScan, " + device.getAddress());
            } else {
                Log.d(logTag, "[" + device.getName() + "]" + device.getAddress());
            }
        }
    }

    private class BleCallbackSignal {
        public static final int IDLE = 0;
        public static final int Doing = 1;
        public static final int Done = 2;

        public int connected = IDLE;
        public int writeRX = IDLE;
        public int sessionToken = IDLE;
        public int command = IDLE;
    }

    public static UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");//发送
    public static UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");//接受
    public static UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final String TAG_BLE = "ZkBleLockConnector";


    /**
     * 记录上次断开断开时间
     */
    private static Date LastDisconnectTime = new Date(0);

    /**
     * 限制断开\连接的间隔时间:毫秒
     */
    static int IntervalTimeInTwoConnection = 0;

    /**
     * 已无用
     */
    @Deprecated
    public int CompatibleBleScanTimes = 0;

    /**
     * 已无用
     */
    @Deprecated
    public static int CompatibleBleScanSnd = 1000 * 15;

    /**
     * 每次与BLE设备通讯的最大字节数
     */
    public static final int MAX_MTU = 20;

    /**
     * 与BLE设备操作相关的超时时间，默认为8s
     */
    private static int BLE_TIMEOUT = 1000 * 8;

    /**
     * 与BLE设备连接失败重试次数，默认为3
     */
    private static int BLE_Retry_Times = 3;

    /**
     * 作连接扫描时，是否用Service UUID进行过滤。0:不用，1:用
     */
    private static int Enable_SvcUUID_ScanFilter = 0;

    private Context context;
    private BluetoothManager bleMng;
    private BluetoothAdapter bleLocalAdp;
    private BluetoothDevice bleRemoteDevice;
    private BluetoothGatt bleRemoteGatt;

    private String macAddress = "";

    private boolean selfShowMsg = true;  //要求自己也显示log
    private WhatHappenCallback whatHappenCallback = null;
    private IResultCallback resultCallback = null;

    private BleGattCallback myBleGattCallback = null;

    private ArrayDeque<Byte> outputBuffer = new ArrayDeque<Byte>();
    private ArrayDeque<Byte> inputBuffer = new ArrayDeque<Byte>();
    private LockCommSessionTokenResponse ssToken = null;
    private LockCommResponse sendToClient = null;

    private NoopLeScanCallback noopLeScanCallback = new NoopLeScanCallback();

    private ExecutorService cmdExecSvc;

    /**
     * 断开连接请求
     */
    private boolean disconnectReq = false;

    /**
     * 最后发生的异常
     */
    private Exception lastException = null;

    /**
     * 发送命令和ble回调线程的同步信号量
     */
    private final BleCallbackSignal callbackSignal = new BleCallbackSignal();

    /**
     * 用来执行超时任务的定时器
     */
    private Timer receiveTimer;

    protected BleLockConnector(Context ctx, String mac) {

        this.macAddress = mac;
        this.context = ctx;
        this.bleMng = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bleLocalAdp = this.bleMng.getAdapter(); // same as BluetoothAdapter.getDefaultAdapter();

        this.myBleGattCallback = new BleGattCallback(null);
    }

    /**
     * 设置超时时间，单位：秒
     *
     * @param snd 超时时间
     */
    public static void setTimeOut(int snd) {
        BLE_TIMEOUT = 1000 * snd;
    }

    /**
     * 获取超时时间，单位：秒
     *
     * @return 超时时间
     */
    public static int getTimeout() {
        return BLE_TIMEOUT / 1000;
    }


    /**
     * 与蓝牙设备连接失败时重试次数
     */
    public static int getConnRetryTimes() {
        return BLE_Retry_Times;
    }

    /**
     * 与蓝牙设备连接失败时重试次数
     */
    public static void setBLEConnRetryTimes(int val) {
        BleLockConnector.BLE_Retry_Times = val;
    }


    /**
     * 作连接扫描时，是否用Service UUID进行过滤。<br>
     *     有的安卓版本，或者BLE没有广播ServiceUUID时，将搜索不到。<br>
     *     使用此UUID效率更高些，不用也不影响。默认为0
     *
     */
    public static int getEnableSvcUUIDScanFilter() {
        return Enable_SvcUUID_ScanFilter;
    }

    /**
     * 作连接扫描时，是否用Service UUID进行过滤。<br>
     *     有的安卓版本，或者BLE没有广播ServiceUUID时，将搜索不到。<br>
     *     使用此UUID效率更高些，不用也不影响。默认为0
     *
     * @param val 0不用，1使用
     */
    public static void setEnableSvcUUIDScanFilter(int val) {
        if(val !=0 && val!=1) throw new IllegalArgumentException("只有0,1被接受");
        Enable_SvcUUID_ScanFilter = val;
    }

    /**
     * 获取当前的Mac地址
     */
    public String getMacAddress() {
        return this.macAddress;
    }


    /**
     * 确定当前的连接状态
     */
    public boolean isConnected() {
        if (this.bleRemoteGatt == null) return false;
        if (this.bleRemoteDevice == null) return false;

        //经过测试，这个方法和回调里事件是等价的
        int retVal = this.bleMng.getConnectionState(this.bleRemoteDevice, BluetoothProfile.GATT);

        return retVal == BluetoothProfile.STATE_CONNECTED;
    }


    /**
     * 当前存储的ssToken是否存在或者已经过期
     */
    public boolean isSessionTokenExpire() {
        if (this.ssToken == null) return true;
        return this.ssToken.isExpire();
    }

    /**
     * 以异步方式连接到设备。如果当前已是连接状态，将不再触发连接回调
     */
    public void connectAsync() {
        letMeKnow("接到UI连接命令");
        if (isConnected()) {
            letMeKnow("已连，不再执行：" + this.macAddress);
            Log.w(TAG_BLE, "已连，不再执行");
            return;
        }

        if (this.callbackSignal.connected == BleCallbackSignal.Doing) {
            letMeKnow("已在Ble连接中...");
            Log.w(TAG_BLE, "已在Ble连接中...");
            return;
        }

        this.cmdExecSvc = Executors.newSingleThreadExecutor();
        this.cmdExecSvc.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    doConnect();
                } catch (Exception ex) {
                    BleLockConnector.this.lastException = ex;
                }
                onConnectionStateCallback(
                        isConnected()
                                ? ConnectionStateEnum.Connected
                                : ConnectionStateEnum.Disconnected);
            }
        });
    }


    /**
     * 链接到BLE设备，连接结果通过回调返回
     */
    public void connect() {
        letMeKnow("接到UI连接命令");
        if (isConnected()) {
            letMeKnow("已连，不再执行：" + this.macAddress);
            return;
        }

        /* 在ui线程做连接操作, 可以提高连接成功率 begin */
        try {
            doConnect();
        } catch (Exception ex) {
            BleLockConnector.this.lastException = ex;
        }
        onConnectionStateCallback(
                isConnected()
                        ? ConnectionStateEnum.Connected
                        : ConnectionStateEnum.Disconnected);
        /* 在ui线程做连接操作 end */
    }


    /**
     * 断开与BLE设备的连接，连接结果通过回调返回。断开连接很快，完成断开连接后才返回。
     */
    public void disconnect() {
        if (isConnected()) {
            letMeKnow("接到断开请求");
        } else {
            letMeKnow("接到断开请求，当前已断：" + this.macAddress);
        }


        // TODO: 2017/5/15 感觉disconnect这部分还有些乱，需要再捋顺调用关系
        this.disconnectReq = true;
        this.bleLocalAdp.stopLeScan(this.noopLeScanCallback);

        synchronized (this.callbackSignal) {
            this.callbackSignal.notifyAll();
        }

        if (this.cmdExecSvc != null)
            this.cmdExecSvc.shutdownNow();

        this.ssToken = null;
        this.inputBuffer.clear();
        this.outputBuffer.clear();

        if (this.bleRemoteGatt == null) return;

        synchronized (this.callbackSignal) {

            this.bleRemoteGatt.disconnect();
            letMeKnow("发出断开指令:" + this.macAddress);

            try {
                this.callbackSignal.wait(1000);
                letMeKnow("断开BLE连接，同步信号正常结束");
            } catch (InterruptedException e) {
                letMeKnow("超时未正常中断BLE连接");
            }
        }

        if (this.bleRemoteGatt != null) {  //再次确认已经关闭干净
            Log.w(BleLockConnector.TAG_BLE, "BluetoothGATT.close again");

            this.bleRemoteGatt.close();
            this.bleRemoteGatt = null;
            this.bleRemoteDevice = null;

            BleLockConnector.this.lastException = null;
            onConnectionStateCallback(ConnectionStateEnum.Disconnected);
        }
    }


    /**
     * 以同步方式执行执行的通讯协议，直到得到结果，或者出现错误抛出异常。
     *
     * @param cmd 要执行的通讯协议。
     * @return 设备返回的结果 ，根据请求的指令通过类型转换可以得到相应的返回类
     */
    public LockCommResponse send(final LockCommBase cmd) {
        doExecute(cmd);

        //发送完指令，就开始接受消息了
        this.callbackSignal.command = BleCallbackSignal.Doing;
        synchronized (this.callbackSignal) {
            try {
                while (this.callbackSignal.command != BleCallbackSignal.Done) {
                    this.callbackSignal.wait(BLE_TIMEOUT);
                    if (this.callbackSignal.command == BleCallbackSignal.Done) {
                        LockCommResponse retVal = this.sendToClient;
                        return retVal;
                    } else {
                        throw new LockTimeoutException("超时未得到返回内容");
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new LockException("send时线程中断：" + e.getMessage(), e);
            } finally {
                this.callbackSignal.command = BleCallbackSignal.IDLE;
                this.sendToClient = null;
            }
        }

        return null;
    }


    /**
     * 以异步方式执行通讯指令，执行结果通过回调返回
     *
     * @param cmd 要执行的通讯协议
     */
    public void post(final LockCommBase cmd) {
        letMeKnow("接到UI命令：" + cmd.getCmdName());

        this.cmdExecSvc = Executors.newSingleThreadExecutor();
        this.cmdExecSvc.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    doExecute(cmd);
                    if (BleLockConnector.this.disconnectReq) return;
                    startReceiveTimer();
                } catch (Exception ex) {
                    BleLockConnector.this.lastException = ex;
                    onResultCallbackForExecute(null);
                }
            }
        });
    }



    /**
     * 获取断开\连接的休眠间隔
     */
    public static long getIntervalTimeInTwoConnection() {
        return IntervalTimeInTwoConnection;
    }

    /**
     * 设置断开\连接的休眠间隔
     */
    public static void setIntervalTimeInTwoConnection(int val) {
        IntervalTimeInTwoConnection = val;
    }

    /**
     * 描述：断开\连接之间的等待处理
     * @return
     */
    private static boolean waitInTwoConnection(){
        long diff = (new Date()).getTime()-LastDisconnectTime.getTime();
        if(diff > IntervalTimeInTwoConnection) return false;
        Log.d(BleLockConnector.TAG_BLE, "当前时间:"+BriefDate.fromNature(Calendar.getInstance().getTime()).toString()+",上次断开时间:"+BriefDate.fromNature(LastDisconnectTime).toString());
        //时间差小于IntervalTimeInTwoConnection
        try {
            Log.d(BleLockConnector.TAG_BLE, "需休眠"+(IntervalTimeInTwoConnection - diff));
            Thread.sleep(IntervalTimeInTwoConnection - diff);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void doExecute(LockCommBase cmd) {
        byte[] token = new byte[0];

        if (!isConnected()) {
            doConnect();
            onConnectionStateCallback(ConnectionStateEnum.Connected);
        }

        if (this.disconnectReq) return;

        if (cmd.needSessionToken()) {
            letMeKnow("需要ssToken：[" + cmd.getCmdId() + "]" + cmd.getCmdName());
            if (isSessionTokenExpire()) {
                doRefreshSsToken();
            }
            //doRefreshSsToken();
            DateFormat df = SimpleDateFormat.getTimeInstance();
            token = ssToken.getToken();
            letMeKnow("本次使用的Token：" + BitConverter.toHexString(token) + "; exp:" + df.format(ssToken.getExpire()));
        }

        byte[] buffer = cmd.getBytes(token);
        for (int idx = 0; idx < buffer.length; idx++) {
            outputBuffer.add(buffer[idx]);
        }

        //测试接收超时回调时，可以注释此语句模拟，for debug
        writeRXCharacteristic(buffer);

    }

    private void doRefreshSsToken() {

        synchronized (this.callbackSignal) {

            this.callbackSignal.sessionToken = BleCallbackSignal.IDLE;

            LockCommSessionToken st = new LockCommSessionToken();
            writeRXCharacteristic(st.getBytes());

            this.callbackSignal.sessionToken = BleCallbackSignal.Doing;

            try {
                while (true) {

                    this.callbackSignal.wait(BLE_TIMEOUT);

                    if (this.callbackSignal.sessionToken == BleCallbackSignal.Done) {
                        letMeKnow("得到了SessionToken");
                        return;
                    } else {
                        letMeKnow("超时未得到SessionToken");
                        throw new LockTimeoutException("获取ssToken超时");
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new LockException("获取ssToken时线程中断：" + e.getMessage(), e);
            } finally {
                this.callbackSignal.sessionToken = BleCallbackSignal.IDLE;
            }
        }
    }


    private boolean doCompatibleBleScan() {

        synchronized (this.callbackSignal) {

            UUID[] svcUUID = getEnableSvcUUIDScanFilter() == 0 ? null : new UUID[]{BleLockConnector.RX_SERVICE_UUID};
            this.bleLocalAdp.startLeScan(svcUUID, noopLeScanCallback);
            letMeKnow("bleScan is doing for " + BLE_TIMEOUT + "ms...");

            this.callbackSignal.connected = BleCallbackSignal.Doing;
            try {
                this.callbackSignal.wait(BLE_TIMEOUT);

                boolean found = this.callbackSignal.connected == BleCallbackSignal.Done;
                this.callbackSignal.connected = BleCallbackSignal.IDLE;  //doConnectCore里会判断此值
                return found;

            } catch (InterruptedException e) {
                this.callbackSignal.connected = BleCallbackSignal.IDLE;  //doConnectCore里会判断此值
                letMeKnow("bleScan not found, timeout");
                return false;

            } finally {
                BleLockConnector.this.bleLocalAdp.stopLeScan(noopLeScanCallback);
            }
        }
    }

    private void doConnect() {
        waitInTwoConnection();

        //用来处理断开重连，如果不需要此功能，直接用 doConnectCore
        int currentRetry = 0;
        this.disconnectReq = false;

        while (true) {
            if (this.disconnectReq) return;

            try {

                boolean findDevice = doCompatibleBleScan();
                if (!findDevice)
                    throw new LockTimeoutException("超时未发现");

                doConnectCore();//重连，通过catch里面的ex停止扫描
                return;

            } catch (LockTimeoutException ex) {

                currentRetry++;

                String msg = "发生异常[" + ex.getMessage() + "]";
                msg += "，第" + currentRetry + "/" + BLE_Retry_Times + "次";
                letMeKnow(msg);
                Log.w(BleLockConnector.TAG_BLE, msg);

                if (this.bleRemoteGatt != null) {
                    this.bleRemoteGatt.close();
                    this.bleRemoteGatt = null;
                    this.bleRemoteDevice = null;
                }

                if (currentRetry >= BLE_Retry_Times) {
                    msg = "重试 " + currentRetry + " 次后仍未成功连接，报连接超时";
                    letMeKnow(msg);
                    Log.e(BleLockConnector.TAG_BLE, msg);
                    throw ex;
                }
            }
        }

    }

    private void doConnectCore() {

        synchronized (this.callbackSignal) {

            //当前正在有连接
            if (this.callbackSignal.connected != BleCallbackSignal.IDLE) {
                letMeKnow("当前在执行连接动作，退出：" + this.callbackSignal.connected);
                return;
            }

            this.callbackSignal.connected = BleCallbackSignal.IDLE;

            letMeKnow("开始执行连接到：" + this.macAddress);
            if (this.bleRemoteDevice == null) { // blescan 中有初始化的机会
                this.bleRemoteDevice = this.bleLocalAdp.getRemoteDevice(this.macAddress);
                Log.w(TAG_BLE, "连接时初始化bluetoothDevice");
            }
            this.bleRemoteGatt = this.bleRemoteDevice.connectGatt(this.context, false, this.myBleGattCallback);
            letMeKnow("连接命令发送完成");

            this.callbackSignal.connected = BleCallbackSignal.Doing;

            try {
                while (true) {
                    this.callbackSignal.wait(BLE_TIMEOUT);

                    if (this.callbackSignal.connected == BleCallbackSignal.Done) {
                        return;
                    } else {
                        throw new LockTimeoutException("连接超时");//抛出异常，在doConnect（）中捕获
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new LockException("连接时线程中断：" + e.getMessage(), e);
            } finally {
                this.callbackSignal.connected = BleCallbackSignal.IDLE;
            }
        }
    }


    private boolean enableTXNotification() {
        BluetoothGattService gattRxSvc = this.bleRemoteGatt.getService(RX_SERVICE_UUID);
        if (gattRxSvc == null) {
            letMeKnow("\tRx gattSvc not found：" + RX_SERVICE_UUID);
            return false;
        }

        BluetoothGattCharacteristic gattTxChar = gattRxSvc.getCharacteristic(TX_CHAR_UUID);
        if (gattTxChar == null) {
            letMeKnow("\tTx gattChar not found：" + TX_CHAR_UUID);
            return false;
        }
        this.bleRemoteGatt.setCharacteristicNotification(gattTxChar, true);

        //去掉就收不到BLE的通知了
        BluetoothGattDescriptor descriptor = gattTxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        boolean result = this.bleRemoteGatt.writeDescriptor(descriptor);
        letMeKnow("\tenableTXNotification：" + result);

        return result;
    }


    private void writeRXCharacteristic(byte[] val) {
        //todo rom重上电后直接发送，会出现此问题，为了不吃掉异常，先注释掉
        //if(this.bleRemoteGatt==null) return false;
        BluetoothGattService gattRxSvc = this.bleRemoteGatt.getService(RX_SERVICE_UUID);
        if (gattRxSvc == null) {
            letMeKnow("Rx service not found：" + RX_SERVICE_UUID);
            throw new LockException("蓝牙底层失败(RxSvcNotFound)");
        }

        BluetoothGattCharacteristic gattRxChar = gattRxSvc.getCharacteristic(RX_CHAR_UUID);
        if (gattRxChar == null) {
            letMeKnow("Rx characteristic not found：" + RX_CHAR_UUID);
            throw new LockException("蓝牙底层失败(RxCharaNotFound)");
        }

        letMeKnow("******** Begin Tx[Len:" + val.length + "] ********");

        synchronized (BleLockConnector.this.callbackSignal) {
            try {
                int mBufferOffset = 0;
                int idx = 0;
                while (mBufferOffset < val.length) {

                    BleLockConnector.this.callbackSignal.writeRX = BleCallbackSignal.IDLE;

                    idx++;
                    final int length = Math.min(val.length - mBufferOffset, MAX_MTU);
                    final byte[] data = new byte[length];
                    System.arraycopy(val, mBufferOffset, data, 0, length);
                    gattRxChar.setValue(data);
                    boolean success = this.bleRemoteGatt.writeCharacteristic(gattRxChar);
                    mBufferOffset += length;
                    letMeKnow("[Tx:" + idx + ",status:" + success + "]" + BitConverter.toHexString(data));
                    if (!success) throw new LockException("未成功发送BLE指令");

                    BleLockConnector.this.callbackSignal.writeRX = BleCallbackSignal.Doing;
                    while (this.callbackSignal.writeRX != BleCallbackSignal.Done) {

                        this.callbackSignal.wait(BLE_TIMEOUT);

                        if (this.callbackSignal.writeRX == BleCallbackSignal.Done) {
                            //letMeKnow("等到了Rx写入成功通知");  成功就不用说了，消息太多了
                        } else {
                            letMeKnow("超时未等到Rx写入确认");
                            throw new LockException("超时未等到Rx写入确认");
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new LockException("发送命令时线程中断：" + e.getMessage(), e);
            }
        }

        letMeKnow("-------- finished Tx --------");
    }


    private void onConnectionStateCallback(ConnectionStateEnum state) {
        if (this.resultCallback == null) return;

        try {
            this.resultCallback.onConnectionState(this.macAddress, state, this.lastException);
            this.lastException = null;

        } catch (Exception ex) {
            Log.e(BleLockConnector.TAG_BLE, "连接回调，客户端发生异常：" + ex.getMessage());
            ex.printStackTrace();
        }
    }


    private void onResultCallbackForExecute(LockCommResponse response) {
        if (this.resultCallback == null) return;

        // TODO: 2016/7/25 启用线程池方式，不用同步等待，反正都是异步的
        //目前方式在回调里再用同步，会有发送指令相关的异常。
        //估计是等待回调客户代码的执行，导致connector里的错乱

        try {
            this.resultCallback.onExecutionResult(this.macAddress, this.lastException, response);
            this.lastException = null;
            this.sendToClient = null;

        } catch (Exception ex) {
            Log.e(BleLockConnector.TAG_BLE, "执行回调，客户端发生异常：" + ex.getMessage());
            ex.printStackTrace();
        }
    }


    /**
     * 启动一个接收命令超时定时器
     */
    private void startReceiveTimer() {
        if (BleLockConnector.this.receiveTimer != null)
            BleLockConnector.this.receiveTimer.cancel();

        BleLockConnector.this.receiveTimer = new Timer("receiveTimer");
        BleLockConnector.this.receiveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                letMeKnow("接收指令超时");
                if (resultCallback == null) return;

                LockTimeoutException ex = new LockTimeoutException("接收响应内容超时");
                lastException = ex;
                onResultCallbackForExecute(null);
            }
        }, BLE_TIMEOUT);
    }

    /**
     * 停止超时定时器
     */
    private void cancelReceiveTimer() {
        if (BleLockConnector.this.receiveTimer == null) return;

        BleLockConnector.this.receiveTimer.cancel();
    }

    /**
     * 设置执行结果的回调内容，当异步执行时，通过此处设置的接口获得返回内容
     */
    public void setResultCallback(IResultCallback callback) {
        this.resultCallback = callback;
    }


    /**
     * 设置内部运行的日志接口，可以通过此了解内部的运行结果
     */
    public void setWhatHappend(WhatHappenCallback callback) {
        this.whatHappenCallback = callback;
        this.selfShowMsg = true;
    }

    /**
     * 设置内部运行的日志接口，可以通过此了解内部的运行结果
     *
     * @param innerShowMsg 控制内部也要显示LogCat消息
     */
    public void setWhatHappend(boolean innerShowMsg, WhatHappenCallback callback) {
        this.whatHappenCallback = callback;
        this.selfShowMsg = innerShowMsg;
    }

    @Override
    public void letMeKnow(String msg) {

        if (this.selfShowMsg || this.whatHappenCallback == null)
            Log.i(BleLockConnector.TAG_BLE, msg);

        if (this.whatHappenCallback != null)
            this.whatHappenCallback.letMeKnow(msg);
    }


    private static Map<String, BleLockConnector> ConnectorPool = new HashMap<>();


    /**
     * 创建或返回与指定mac相关联的连接对象
     *
     * @param mac 要连接的BLE设备地址
     */
    public static BleLockConnector create(Context ctx, String mac) {

        if (ConnectorPool.containsKey(mac)) return ConnectorPool.get(mac);

        BleLockConnector retVal = new BleLockConnector(ctx, mac);
        ConnectorPool.put(mac, retVal);

        return retVal;
    }

    /**
     * 获得指定BLE地址的连接对象，如果没有则返回null
     *
     * @param mac 要连接的BLE设备地址
     */
    public static BleLockConnector get(String mac) {
        if (ConnectorPool.containsKey(mac)) return ConnectorPool.get(mac);
        return null;
    }

    /**
     * 删除指定地址的BLE设备连接对象
     */
    public static void clear(String mac) {
        if (!ConnectorPool.containsKey(mac)) return;
        BleLockConnector conn = ConnectorPool.remove(mac);
        conn.disconnect();
        conn.setResultCallback(null);
        conn.setWhatHappend(null);

        Log.i(BleLockConnector.TAG_BLE, "释放BleLockConnection对象：" + mac);
    }

    /**
     * 清空所有保留的BLE设备对象
     */
    public static void clearAll() {
        for (String curMac : ConnectorPool.keySet()) {
            clear(curMac);
        }
    }

}
