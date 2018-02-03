package cn.zelkova.lockprotocol;

/**
 * 获取网关联网状态
 *
 * @author lwq
 * Created by liwenqi on 2017/7/11.
 */
public class LockCommGetGwNetState extends LockCommBase {
    @Override
    public short getCmdId() {
        return 0x36;
    }

    @Override
    public String getCmdName() {
        return "getGWNetState";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
