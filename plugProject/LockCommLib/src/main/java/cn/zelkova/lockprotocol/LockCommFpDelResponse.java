package cn.zelkova.lockprotocol;

/**
 * 执行DelFp操作指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/6/23.
 */
public class LockCommFpDelResponse extends LockCommResponse {
    public final static short CMD_ID =0x15;
    @Override
    public String getCmdName() {
        return "LockCommDelFpResponse";
    }

    /**
     * 执行结果，0为成功执行，否则可能返回： 0x01、0x02、0x03、0x06、0x04、0x0D
     */
    public byte getResultCode(){
        return getVal(0x03);
    }

    private byte getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte retVal = klv.getVal()[0];
        return retVal;
    }
}
