package cn.zelkova.lockprotocol;

/**
 * 获得锁内ROM版本号指令
 *
 * @author zp
 * Created by zp on 2016/6/15.
 */
public class LockCommGetVer extends LockCommBase {
    @Override
    public short getCmdId() {
        return 0x10;
    }

    @Override
    public String getCmdName() {
        return "getVer";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
