package cn.zelkova.lockprotocol;

/**
 * 更新开门密码有效周期
 *
 * @author lwq
 * Created by liwenqi on 2016/12/06.
 */
public class LockCommSyncPwdExpire extends LockCommBase {

    /**
     * @param cmd 锁命令
     */
    public LockCommSyncPwdExpire(byte[] cmd) {
        super.mKLVList.add((byte) 0x01, cmd);
    }

    @Override
    public short getCmdId() {
        return 0x0B;
    }

    @Override
    public String getCmdName() {
        return "LockCommSyncPwdExpire";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
