package cn.zelkova.lockprotocol;

/**
 * 设置音量指令
 *
 * @author lwq
 * Created by liwenqi on 2017/8/21.
 */
public class LockCommSetVolumn extends LockCommBase {

    /**
     * @param cmd 设置音量命令
     */
    public LockCommSetVolumn(byte[] cmd) {
        super.mKLVList.add((byte) 0x02, cmd);
    }


    @Override
    public short getCmdId() {
        return 0x18;
    }

    @Override
    public String getCmdName() {
        return "SetVolumnComm";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
