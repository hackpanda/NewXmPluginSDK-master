package cn.zelkova.lockprotocol;

/**
 * 获取锁内PIN信息的指令返回
 *
 * @author lwq
 * Created by liwenqi on 2016/6/24.
 */
public class LockCommGetPinInfoResponse extends LockCommResponse {
    public final static short CMD_ID =0x07;
    @Override
    public String getCmdName() {
        return "getPinInfoResp";
    }

    /**
     * 预留存放PIN的个数
     */
    public int getReservedStoragePinCount(){return getVal(0x01); }

    /**
     *每个PIN的字节长度,目前固定为4bytes
     */
    public int getPerPinLength(){return getVal(0x02); }
    /**
     *当前已有PIN的数量
     */
    public int getCurrentAlreadyPinCount(){return getVal(0x03); }
    /**
     *已绑定开门设备的数量
     */
    public int getBoundedDoorCount(){return getVal(0x04); }

    private int getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        int retVal = (int)klv.getVal()[0];
        return retVal;
    }
}
