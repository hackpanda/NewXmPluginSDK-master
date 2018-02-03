package cn.zelkova.lockprotocol;

/**
 * 设置临时密码有效周期
 *
 * @author lwq
 * Created by liwenqi on 2017/01/11.
 */
public class LockCommSetZotpPeriod extends LockCommBase {
    /**
     *仅获取锁内时间
     */
    public LockCommSetZotpPeriod() {
        this(new byte[0]);

    }

    /**
     * @param cmd 设置临时密码周期锁命令
     */
    public LockCommSetZotpPeriod(byte[] cmd) {
        super.mKLVList.add((byte) 0x01, cmd);

    }

    @Override
    public short getCmdId() {
        return 0x0D;
    }

    @Override
    public String getCmdName() {
        return "LockCommSetZotpPeriod";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }

}

