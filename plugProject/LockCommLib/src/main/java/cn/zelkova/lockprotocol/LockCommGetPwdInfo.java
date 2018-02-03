package cn.zelkova.lockprotocol;

/**
 * 获得锁内开门密码信息指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/24.
 */
public class LockCommGetPwdInfo extends LockCommBase {
    @Override
    public short getCmdId() {
        return 0x09;
    }

    @Override
    public String getCmdName() {
        return "getPwdInfo";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
