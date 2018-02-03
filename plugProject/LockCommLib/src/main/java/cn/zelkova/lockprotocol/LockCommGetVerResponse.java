package cn.zelkova.lockprotocol;

/**
 * * 获得锁内密钥设置信息指令返回
 *
 * @author zp
 * Created by zp on 2016/6/15.
 */
public class LockCommGetVerResponse extends LockCommResponse {
    public final static short CMD_ID =0x10;

    @Override
    public String getCmdName() {
        return "getVerResp";
    }

    /**
     * 锁内ROM版本信息，格式 x.y.b
     */
    public String getRomVerText(){
        byte b0 =this.mKLVList.getKLV(0x01).getVal()[0];
        byte b1 =this.mKLVList.getKLV(0x01).getVal()[1];

        byte x = (byte) ((b0>>>4) & 0x0F);
        byte y = (byte) (b0 & 0x0F);

        String retVal= x +"." +y +"." +b1;

        return retVal;
    }

    /**
     * 锁内ROM版本信息原始值
     */
    public int getRomVer(){return getVal(0x01); }

    /**
     * 锁内DFU版本
     */
    public int getDFUVer(){return getVal(0x02); }

    /**
     * 锁内ROM支持协议的版本
     */
    public int getLockCmdVer(){return getVal(0x03); }

    @Deprecated
    public int getLockCommL1Ver(){return getVal(0x04); }

    /**
     * 硬件PCB版本
     */
    public byte getPCBVer(){ return (byte) getVal(0x04); }

    private int getVal(int key){
        KLVContainer klv =this.mKLVList.getKLV(key);
        int retVal =BitConverter.toUInt16(klv.getVal(), 0);
        return retVal;
    }
}
