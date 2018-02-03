package cn.zelkova.lockprotocol;

/**
 * 获取说内mac地址返回值
 *
 * @author lwq
 * Created by liwenqi on 2016/6/23.
 */
public class LockCommGetBleMacResponse extends LockCommResponse {
    public final static short CMD_ID =0x02;

    @Override
    public String getCmdName() {
        return "getBleMacResp";
    }


    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }

    /**
     * 获得mac地址
     */
    public byte[] getBleMacByte(){
        byte[] bleMac = getVal(0x01);
        return bleMac;
    }

    /**
     * 获得mac地址文本，格式b5:b4:b3:b2:b1:b0
     */
    public String getBleMacText(){
        byte[] bleMac = getBleMacByte();
        return BitConverter.convertMacAdd(bleMac);
    }


}
