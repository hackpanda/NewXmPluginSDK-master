package cn.zelkova.lockprotocol;

/**
 * 设置省电间隔
 *
 * @author lwq
 * Created by liwenqi on 2017/01/11.
 */
public class LockCommSetPwrSave extends LockCommBase {


    /**
     * @param cmd 设置命令
     */
    public LockCommSetPwrSave(byte[] cmd) {
        super.mKLVList.add((byte) 0x02, cmd);
    }

    @Override
    public short getCmdId() {
        return 0x17;
    }

    @Override
    public String getCmdName() {
        return "LockCommSetPwrSave";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
