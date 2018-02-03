package cn.zelkova.lockprotocol;


/**
 * 获得锁内开门密码信息指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/6/24.
 */
public class LockCommGetPwdInfoResponse extends LockCommResponse {
    public final static short CMD_ID =0x09;
    @Override
    public String getCmdName() {
        return "getPwdInfoResp";
    }

    /**
     * 预留存放开门密码的个数
     * @return
     */
    public int getReservedStoragePwdCount(){return getVal(0x01); }

    /**
     *每个开门密码最大字节长度，目前固定为8 bytes
     * @return
     */
    public int getPerPwdLength(){return getVal(0x02); }
    /**
     *当前已有开门密码的数量
     * @return
     */
    public int getCurrentAlreadyPwdCount(){return getVal(0x03); }

    private int getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        int retVal = (int)klv.getVal()[0];
        return retVal;
    }
}
