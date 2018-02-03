package cn.zelkova.lockprotocol;

import android.util.Log;

/**
 * 获得deviceName指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/6/22.
 */
public class LockCommGetDeviceNameResponse extends LockCommResponse {
    public final static short CMD_ID =0x30;


    @Override
    public String getCmdName() {
        return "getDeviceNameResp";
    }

    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }

    /**
     * 获得deviceName
     */
    public byte[] getBleDeviceNameByte(){
        byte[] bleName = getVal(0x01);
        return bleName;
    }

    /**
     * 获得deviceName文本
     */
    public String getBleNameText(){
        byte[] bleName = getBleDeviceNameByte();
        Log.d("oooooo", BitConverter.toHexString(bleName));
        return BitConverter.convertMacAdd(bleName);
    }
}
