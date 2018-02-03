package cn.zelkova.lockprotocol;

/**
 * 设置PIN的指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/23.
 */
public class LockCommSyncPINs extends LockCommBase {

    /**
     * 从服务器获得的同步PIN命令
     */
    public LockCommSyncPINs(byte[] cmd) {
        super.mKLVList.add((byte) 0x01, cmd);
    }

    @Override
    public short getCmdId() {
        return 0x08;
    }

    @Override
    public String getCmdName() {
        return "SyncPINs";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
