package cn.zelkova.lockprotocol;

/**
 * 获得锁内日志指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/27.
 */
public class LockCommGetLog extends LockCommBase {

    /**
     * @param cmd getLog命令
     */
    public LockCommGetLog(byte[] cmd) {
        super.mKLVList.add((byte) 0x01, cmd);
    }

    @Override
    public short getCmdId() {
        return 0x16;
    }

    @Override
    public String getCmdName() {
        return "LockCommGetLog";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
