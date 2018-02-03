package cn.zelkova.lockprotocol;

/**
 * 设置省电模式返回
 *
 * @author lwq
 * Created by liwenqi on 2017/01/11.
 */
public class LockCommSetPwrSaveResponse extends LockCommResponse {
    public final static short CMD_ID =0x17;
    @Override
    public String getCmdName() {
        return "LockCommSetPwrSaveResponse";
    }

    /**
     * 执行结果，成功执行返回0，否则可能的错误码：0x01、0x03、0x08、0x0E、0x10
     */
    public byte getResultCode(){
        return getVal(0x03)[0];
    }

    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }
}
