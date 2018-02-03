package cn.zelkova.lockprotocol;

/**
 * 获得锁内密钥设置信息指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/22.
 */
public class LockCommGetStatus extends LockCommBase {
    /**
     * @param cmd 从服务器获取的添加指纹命令
     */
    public LockCommGetStatus(byte[] cmd) {
        super.mKLVList.add((byte) 0x01, cmd);
    }
    @Override
    public short getCmdId() {
        return 0x03;
    }

    @Override
    public String getCmdName() {
        return "getStatus";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
