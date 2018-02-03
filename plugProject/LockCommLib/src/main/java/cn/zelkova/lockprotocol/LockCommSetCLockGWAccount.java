package cn.zelkova.lockprotocol;

/**
 *
 * @author lwq
 * Created by liwenqi on 2016/11/16.
 */
public class LockCommSetCLockGWAccount extends LockCommBase {

    /**
     * 设置C锁网关账号
     * @param lockMac
     * @param memberId : 锁的管理员Id
     */
    public LockCommSetCLockGWAccount(byte[] lockMac, String memberId) {
        super.mKLVList.add((byte) 0x01, lockMac);
        super.mKLVList.add((byte) 0x02, memberId);
    }

    @Override
    public short getCmdId() {
        return 0x33;
    }

    @Override
    public String getCmdName() {
        return "LockCommSetCLockGWAccount";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }

}

