package cn.zelkova.lockprotocol;

/**
 * 设置PIN的指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/6/24.
 */
public class LockCommSyncPINsResponse extends LockCommResponse {
    public final static short CMD_ID =0x08;
    @Override
    public String getCmdName() {
        return "SyncPINsResp";
    }

    /**
     * 执行结果，成功执行返回0，否则可能的错误码：0x01、0x03、0x08、0x0E
     */
    public byte getResultCode(){
        return getVal(0x03)[0];
    }


    /**
     * 锁命令密文，需要将结果返回给服务器
     */
    public byte[] getCmdCipher(){
        return getVal(0x04);
    }


    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }
}
