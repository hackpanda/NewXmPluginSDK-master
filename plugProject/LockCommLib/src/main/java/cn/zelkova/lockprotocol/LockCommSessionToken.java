package cn.zelkova.lockprotocol;

/**
 * 获得session Token指令
 *
 * @author zp
 * Created by zp on 2016/6/14.
 */
public class LockCommSessionToken extends LockCommBase {
    @Override
    public short getCmdId() {
        return 0x01;
    }

    @Override
    public String getCmdName() {
        return "sessionToken";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
