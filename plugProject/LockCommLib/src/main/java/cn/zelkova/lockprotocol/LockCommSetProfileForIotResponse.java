package cn.zelkova.lockprotocol;

/**
 * setProfileForIoT指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/11/16.
 */
public class LockCommSetProfileForIotResponse extends LockCommResponse {

    public final static short CMD_ID =0x32;
    @Override
    public String getCmdName() {
        return "setProfileForIoT";
    }

    /**
     * 执行结果，成功执行返回0，否则可能的错误码：0x01、0x03、0x06、0x09、0x0E
     */
    public byte getResultCode(){
        return getVal(0x01)[0];
    }

    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }
}
