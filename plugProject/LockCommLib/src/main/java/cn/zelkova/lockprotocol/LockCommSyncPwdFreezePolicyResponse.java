package cn.zelkova.lockprotocol;

/**
 * 设置同步开门密码指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/6/25.
 */
public class LockCommSyncPwdFreezePolicyResponse extends LockCommResponse {
    public final static short CMD_ID =0x0C;

    @Override
    public String getCmdName() {
        return "LockCommSyncPwdFreezePolicyResponse";
    }

    /**
     * 执行结果，成功执行返回0，否则可能的错误码：0x01、0x03、0x08、0x0E、0x10
     */
    public byte getResultCode(){
        return getVal(0x03)[0];
    }

    /**
     * 输错次数
     */
    public byte[] getVFrom(){
        return getVal(0x04);
    }
    /**
     * 锁定时长
     */
    public byte[] getVTo(){
        return getVal(0x05);
    }
    /**
     * 有效期终止
     */
    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }
}
