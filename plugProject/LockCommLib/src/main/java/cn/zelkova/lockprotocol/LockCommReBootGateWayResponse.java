package cn.zelkova.lockprotocol;

/**
 * 重启网关返回值
 *
 * @author lwq
 * Created by liwenqi on 2016/6/23.
 */
public class LockCommReBootGateWayResponse extends LockCommResponse {
    public final static short CMD_ID =0x35;

    @Override
    public String getCmdName() {
        return "rebootGateWay";
    }


    private byte[] getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        byte[] retVal = klv.getVal();
        return retVal;
    }

    /**
     * 返回值
     */
    public byte getResultCode(){
        byte resultCode = getVal(0x01)[0];
        return resultCode;
    }

}
