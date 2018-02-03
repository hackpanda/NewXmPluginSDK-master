/**
 *
 */
package cn.zelkova.lockprotocol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.List;

/**
 * 输出各个回调事件的日志信息，其他没有更多的功能
 * @author zp
 */
class BluetoothGattCallback extends android.bluetooth.BluetoothGattCallback {

    public BluetoothGattCallback(WhatHappenCallback say) {
        super();
        this.sayCallback = say;
    }

    private WhatHappenCallback sayCallback;

    public static final String TAG_BLE = "ZkBluetoothGattCallback";

    public Integer rxPackages = 0;
    public Integer txPackages = 0;

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        rxPackages++;
        byte[] val = characteristic.getValue();
        showMsg("Characteristic_Changed[Len:" + val.length + ",rxPkg:" + rxPackages + "]");
        showMsg("\tUuid:" + characteristic.getUuid());
        showMsg("\tVal:" + BitConverter.toHexString(characteristic.getValue()));
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        txPackages++;
        byte[] val = characteristic.getValue();
        showMsg("Characteristic_Write[status:" + status + ",Len:" + val.length + ",txPkg:" + txPackages + "]");
        showMsg("\tUuid:" + characteristic.getUuid());
        showMsg("\tVal:" + BitConverter.toHexString(val));
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        showMsg("Connection_State_Change[status:" + status + "]");
        showMsg("\tnewState:" + getConnectionStateName(newState));
        showMsg("\tmac:" + gatt.getDevice().getAddress());

        if (newState == BluetoothAdapter.STATE_DISCONNECTED) {
            this.rxPackages = 0;
            this.txPackages = 0;
        }

        if (status != 0)
            Log.e(TAG_BLE, "onConnectionStateChange.Status:" + status + ", newState:" + getConnectionStateName(newState));
        /*
         * status 有时候会出现133、257请搜索，或者见下面的链接
		 * http://stackoverflow.com/questions/25330938/android-bluetoothgatt-status-133-register-callback
		 * http://stackoverflow.com/questions/20069507/gatt-callback-fails-to-register
		 * */
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        showMsg("Descriptor_Write[status:" + status + "]");
        showMsg("\tUuid:" + descriptor.getUuid());
        showMsg("\tVal:" + BitConverter.toHexString(descriptor.getValue()));
    }


    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        showMsg("Services_Discovered[status:" + status + "]");

//		displayGattServices(gatt);
    }

    private void showMsg(String msg) {
        if (this.sayCallback == null)
            Log.i(TAG_BLE, msg);
        else
            this.sayCallback.letMeKnow(msg);
    }

    public static String getConnectionStateName(int state) {
        String result;

        //不会出现ing的情况
        switch (state) {
            case BluetoothAdapter.STATE_CONNECTED:
                result = "Connected";
                break;
            case BluetoothAdapter.STATE_DISCONNECTED:
                result = "Disconnected";
                break;
            default:
                result = String.valueOf(state);
        }

        result = "[" + state + "]" + result;

        return result;
    }

    public static void displayGattServices(BluetoothGatt gatt) {
        List<BluetoothGattService> gattSvcList = gatt.getServices();
        if (gattSvcList == null) return;

        for (BluetoothGattService gattService : gattSvcList) {
            Log.i(TAG_BLE, "Svc:" + gattService.getUuid().toString());

            List<BluetoothGattCharacteristic> gattChaList = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic curCha : gattChaList) {
                Log.i(TAG_BLE, "\t Cha:" + curCha.getUuid().toString());

                List<BluetoothGattDescriptor> gattDescList = curCha.getDescriptors();
                for (BluetoothGattDescriptor curDesc : gattDescList) {
                    Log.i(TAG_BLE, "\t\t Desc:" + curDesc.getUuid());
                    Log.i(TAG_BLE, "\t\t\t Val:" + BitConverter.toHexString(curDesc.getValue()));
                    Log.i(TAG_BLE, "\t\t\t Permission:" + curDesc.getPermissions());
                }
            }
        }
    }
}
