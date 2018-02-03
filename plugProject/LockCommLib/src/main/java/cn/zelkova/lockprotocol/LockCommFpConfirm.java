package cn.zelkova.lockprotocol;

/**
 * 执行FpConfirm操作指令
 *
 * @author lwq
 * Created by liwenqi on 2016/6/23.
 */
public class LockCommFpConfirm extends LockCommBase {

    /**
     * @param cmd 从服务器获取的添加指纹命令
     */
    public LockCommFpConfirm(byte[] cmd) {
        super.mKLVList.add((byte) 0x02, cmd);
    }


    @Override
    public short getCmdId() {
        return 0x14;
    }

    @Override
    public String getCmdName() {
        return "AddNewFpConfirmComm";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
