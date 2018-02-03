package cn.zelkova.lockprotocol;

/**
 * 获取锁内PIN信息的指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/24.
 */
public class LockCommGetPINInfo extends LockCommBase {
    @Override
    public short getCmdId() {
        return 0x07;
    }

    @Override
    public String getCmdName() {
        return "getPinInfo";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
