package cn.zelkova.lockprotocol;

import android.util.Log;

/**
 * 交换密钥指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/6/23.
 */
public class LockCommExchangeKeyResponse extends LockCommResponse {
    public final static short CMD_ID =0x05;
    @Override
    public String getCmdName() {
        return "exchangeKeyResp";
    }

    /**
     * 状态码，0为成功执行，否则可能返回：0x07、0x0E、0x0D、0x03、0x0C
     */
    public byte getResultCode(){
        return getVal(0x01)[0];
    }

    /**
     * 需要返回给服务器的交换密钥锁命令
     */
    public byte[] getSkey(){
        return getVal(0x02);
    }

    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }
}
