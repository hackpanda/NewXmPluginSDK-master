package com.xiaomi.zkplug.dfu;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xiaomi.zkplug.Device;
import com.xiaomi.zkplug.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import no.nordicsemi.android.dfu.DfuBaseService;
import no.nordicsemi.android.dfu.DfuServiceInitiator;

import static com.xiaomi.zkplug.R.id.checkVerDfuSuccTipTv;


/**
 * 作者：liwenqi on 17/9/1 14:58
 * 邮箱：liwenqi@zelkova.cn
 * 描述：Dfu辅助逻辑类
 */

public class DfuManager {
    Activity context;

    public DfuManager(Activity context) {
        this.context = context;
    }


    /**
     * 启动DFU升级服务
     *
     * @param bluetoothDevice 蓝牙设备
     * @param keepBond        升级后是否保持连接
     * @param force           将DFU设置为true将防止跳转到DFU Bootloader引导加载程序模式
     * @param PacketsReceipt  启用或禁用数据包接收通知（PRN）过程。
     *                        默认情况下，在使用Android Marshmallow或更高版本的设备上禁用PEN，并在旧设备上启用。
     * @param numberOfPackets 如果启用分组接收通知过程，则此方法设置在接收PEN之前要发送的分组数。 PEN用于同步发射器和接收器。
     * @param filePath        约定匹配的ZIP文件的路径。
     */
    protected void startDFU(BluetoothDevice bluetoothDevice, boolean keepBond, boolean force,
                          boolean PacketsReceipt, int numberOfPackets, String filePath) {
        Log.d("debug", bluetoothDevice.getAddress()+":"+bluetoothDevice.getName());
        final DfuServiceInitiator stater = new DfuServiceInitiator(bluetoothDevice.getAddress())
                .setDeviceName(bluetoothDevice.getName())
                .setKeepBond(keepBond)
                .setForceDfu(force)
                .setPacketsReceiptNotificationsEnabled(PacketsReceipt)
                .setPacketsReceiptNotificationsValue(numberOfPackets);
        stater.setZip(filePath);
        //stater.setZip(R.raw.zkcupdate012);//这个方法可以传入raw文件夹中的文件、也可以是文件的string或者url路径。
        stater.start(context, DfuBaseService.class);
        Log.d("debug", "isDfuServiceRunning(): "+isDfuServiceRunning());
    }


    /**
     * @param mDevice
     * @param keepBond
     * @param force
     * @param PacketsReceipt
     * @param numberOfPackets
     * 描述：进入DFU模式
     */
    protected void enterDfuMode(final Device mDevice, final boolean keepBond,final  boolean force,
                                final boolean PacketsReceipt, final int numberOfPackets) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final DfuServiceInitiator stater = new DfuServiceInitiator(mDevice.getMac())
                        .setKeepBond(keepBond)
                        .setForceDfu(force)
                        .setPacketsReceiptNotificationsEnabled(PacketsReceipt)
                        .setPacketsReceiptNotificationsValue(numberOfPackets);
                stater.enterDfu(context, DfuBaseService.class);
            }
        });
    }

    private boolean isDfuServiceRunning() {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DfuService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * DFU过程中界面效果
     */
    protected void showDfuProgressView(){
        LinearLayout emptyView = (LinearLayout) context.findViewById(R.id.emptyView);
        emptyView.setVisibility(View.GONE);
        //隐藏初始布局
        LinearLayout dfuInitView = (LinearLayout) context.findViewById(R.id.dfuInitView);
        dfuInitView.setVisibility(View.GONE);
        //显示过程布局
        LinearLayout dfuProgressView = (LinearLayout) context.findViewById(R.id.dfuProgressView);
        dfuProgressView.setVisibility(View.VISIBLE);
        ImageView dfuProgressImg = (ImageView) context.findViewById(R.id.dfuProgressImg);
        dfuProgressImg.setImageResource(R.drawable.icon_yuan);

        ImageView confirmBtn = (ImageView)context.findViewById(R.id.confirmBtn);
        confirmBtn.setVisibility(View.GONE);
        //隐藏失败布局
        LinearLayout dfuFailView = (LinearLayout) context.findViewById(R.id.dfuFailView);
        dfuFailView.setVisibility(View.GONE);
        TextView dfuProgressTv = (TextView) context.findViewById(R.id.dfuProgressTv);
        dfuProgressTv.setText("0%");
        TextView curVerDfuSuccTv = (TextView) context.findViewById(R.id.curVerDfuSuccTv);
        curVerDfuSuccTv.setText("更新中...");
        TextView checkVerDfuSuccTv = (TextView) context.findViewById(R.id.checkVerDfuSuccTv);
        checkVerDfuSuccTv.setText("更新过程中请勿使用门锁");
        checkVerDfuSuccTv.setTextColor(context.getResources().getColor(R.color.dfu_updateing_color));
        TextView checkVerDfuSuccTipTv = (TextView) context.findViewById(R.id.checkVerDfuSuccTipTv);
        checkVerDfuSuccTipTv.setText("手机与门锁距离不能超过1米");

        checkVerDfuSuccTipTv.setVisibility(View.VISIBLE);

    }

    /**
     * DFU更新成功界面效果
     */
    protected void showDfuSuccView(String version){
        LinearLayout emptyView = (LinearLayout) context.findViewById(R.id.emptyView);
        emptyView.setVisibility(View.GONE);
        LinearLayout dfuInitView = (LinearLayout) context.findViewById(R.id.dfuInitView);
        dfuInitView.setVisibility(View.GONE);
        //显示过程布局
        LinearLayout dfuProgressView = (LinearLayout) context.findViewById(R.id.dfuProgressView);
        dfuProgressView.setVisibility(View.VISIBLE);
        ImageView dfuProgressImg = (ImageView) context.findViewById(R.id.dfuProgressImg);
        dfuProgressImg.setImageResource(R.drawable.icon_duihao);
        TextView dfuProgressTv = (TextView) context.findViewById(R.id.dfuProgressTv);
        dfuProgressTv.setText("");
        TextView curVerDfuSuccTv = (TextView) context.findViewById(R.id.curVerDfuSuccTv);
        curVerDfuSuccTv.setText(version);
        TextView checkVerDfuSuccTv = (TextView) context.findViewById(R.id.checkVerDfuSuccTv);
        checkVerDfuSuccTv.setText("固件已是最新版本");
        context.findViewById(checkVerDfuSuccTipTv).setVisibility(View.GONE);
        checkVerDfuSuccTv.setTextColor(context.getResources().getColor(R.color.colorDivider));
        ImageView confirmBtn = (ImageView)context.findViewById(R.id.confirmBtn);
        confirmBtn.setVisibility(View.VISIBLE);
    }

    /**
     * DFU更新失败界面效果
     */
    protected void showDfuFailView(){
        LinearLayout emptyView = (LinearLayout) context.findViewById(R.id.emptyView);
        emptyView.setVisibility(View.GONE);
        //隐藏过程布局
        LinearLayout dfuProgressView = (LinearLayout) context.findViewById(R.id.dfuProgressView);
        dfuProgressView.setVisibility(View.GONE);
        //显示失败布局
        LinearLayout dfuFailView = (LinearLayout) context.findViewById(R.id.dfuFailView);
        dfuFailView.setVisibility(View.VISIBLE);
    }


}
