package cn.zelkova.lockprotocol;

/**
 * 执行SetVolumn操作指令返回
 *
 * @author lwq
 * Created by liwenqi on 201/8/21.
 */
public class LockCommSetVolumnResponse extends LockCommResponse {
    public final static short CMD_ID =0x18;
    @Override
    public String getCmdName() {
        return "LockCommSetVolumnResponse";
    }

    /**
     * 执行结果，0为成功执行，否则可能返回： 0x01、0x02、0x03、0x06、0x04、0x0D
     */
    public byte getResultCode(){
        return getVal(0x03);
    }
    /**
     * 当前音量设置
     */
    public int getVolumn(){
        int volumn = (int)getVal(0x04);
        return volumn;
    }
    private byte getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte retVal = klv.getVal()[0];
        return retVal;
    }
}
