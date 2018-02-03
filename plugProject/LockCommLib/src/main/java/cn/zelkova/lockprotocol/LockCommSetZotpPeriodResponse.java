package cn.zelkova.lockprotocol;

import android.util.Log;

/**
 * 设置临时密码有效周期指令返回
 *
 * @author lwq
 * Created by liwenqi on 2017/01/11.
 */
public class LockCommSetZotpPeriodResponse extends LockCommResponse {

    public final static short CMD_ID =0x0D;
    @Override
    public String getCmdName() {
        return "LockCommSetZotpPeriodResponse";
    }

    /**
     * 执行结果，成功执行返回0，否则可能的错误码：0x01、0x03、0x06、0x09、0x0E
     */
    public byte getResultCode(){
        return getVal(0x03)[0];
    }

    /**
     * 设置的有效期
     */
    public int getZotpPeriod(){
        int zotpInterval = BitConverter.toInt16(getVal(0x04), 0);
        return zotpInterval;
    }

    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }
}
