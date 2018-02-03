package cn.zelkova.lockprotocol;

/**
 * 执行开锁操作指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/23.
 */
public class LockCommFpAdd extends LockCommBase {

    /**
     * @param cmd 从服务器获取的添加指纹命令
     */
    public LockCommFpAdd(byte[] cmd) {
        super.mKLVList.add((byte) 0x02, cmd);
    }


    @Override
    public short getCmdId() {
        return 0x13;
    }

    @Override
    public String getCmdName() {
        return "LockCommFpAdd";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
