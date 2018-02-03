package cn.zelkova.lockprotocol;

/**
 * 设置同步开门密码指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/25.
 */
public class LockCommSyncPwd extends LockCommBase {


    /**
     * @param cmd 从服务器获得的设置开门密码命令
     */
    public LockCommSyncPwd(byte[] cmd) {
        super.mKLVList.add((byte) 0x01, cmd);
    }

    @Override
    public short getCmdId() {
        return 0x0A;
    }

    @Override
    public String getCmdName() {
        return "SyncPwd";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
