package cn.zelkova.lockprotocol;

/**
 * 获得锁内mac地址的指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/23.
 */
public class LockCommGetBleMac extends LockCommBase {
    @Override
    public short getCmdId() {
        return 0x02;
    }

    @Override
    public String getCmdName() {
        return "getBleMac";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
