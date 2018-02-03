package cn.zelkova.lockprotocol;

/**
 * 执行DelFp操作指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/23.
 */
public class LockCommFpDel extends LockCommBase {

    /**
     * @param cmd 从服务器获取的添加指纹命令
     */
    public LockCommFpDel(byte[] cmd) {
        super.mKLVList.add((byte) 0x02, cmd);
    }


    @Override
    public short getCmdId() {
        return 0x15;
    }

    @Override
    public String getCmdName() {
        return "DelFpComm";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
