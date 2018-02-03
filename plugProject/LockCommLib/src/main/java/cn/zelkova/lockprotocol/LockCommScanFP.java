package cn.zelkova.lockprotocol;

/**
 * 启动指纹扫描
 *
 * @author zp
 * Created by zp on 2016/8/15.
 */
public class LockCommScanFP extends LockCommBase {
    public LockCommScanFP(){
        super.mKLVList.add((byte) 0x01, (byte)0x01);
    }

    @Override
    public short getCmdId() {
        return 0x13;
    }

    @Override
    public String getCmdName() {
        return "scanFP";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
