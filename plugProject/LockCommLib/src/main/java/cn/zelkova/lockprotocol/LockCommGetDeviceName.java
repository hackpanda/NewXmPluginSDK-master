package cn.zelkova.lockprotocol;

/**
 * 获得deviceName指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/22.
 */
public class LockCommGetDeviceName extends LockCommBase {
    @Override
    public short getCmdId() {
        return 0x30;
    }

    @Override
    public String getCmdName() {
        return "getDevicename";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
