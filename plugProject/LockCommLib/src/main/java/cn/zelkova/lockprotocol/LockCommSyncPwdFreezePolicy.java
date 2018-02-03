package cn.zelkova.lockprotocol;

/**
 * 设置键盘锁定策略
 *
 * @author lwq
 * Created by liwenqi on 2016/12/06.
 */
public class LockCommSyncPwdFreezePolicy extends LockCommBase {

    /**
     * @param cmd 锁命令
     */
    public LockCommSyncPwdFreezePolicy(byte[] cmd) {
        super.mKLVList.add((byte) 0x01, cmd);
    }

    @Override
    public short getCmdId() {
        return 0x0C;
    }

    @Override
    public String getCmdName() {
        return "LockCommKeyboardFreezePolicy";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
