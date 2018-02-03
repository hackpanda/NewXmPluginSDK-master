package cn.zelkova.lockprotocol;

/**
 * 设置同步开门密码指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/6/25.
 */
public class LockCommSyncPwdResponse extends LockCommResponse {
    public final static short CMD_ID =0x0A;
    @Override
    public String getCmdName() {
        return "SyncPwdResp";
    }

    /**
     * 执行结果，成功执行返回0，否则可能的错误码：0x01、0x03、0x08、0x0E、0x10
     */
    public byte getResultCode(){
        return getVal(0x03)[0];
    }
    /**
     * 密码别名
     */

    public int getPwdIdx(){
        String pwdStr = BitConverter.toHexString(getVal(0x04), "");
        return Integer.parseInt(pwdStr.substring(2)+pwdStr.substring(0,2), 16);
    }
    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }
}
