package cn.zelkova.lockprotocol;

/**
 * 重启网关命令
 *
 * @author lwq
 * Created by liwenqi on 2017/3/7.
 */
public class LockCommReBootGateway extends LockCommBase {
    @Override
    public short getCmdId() {
        return 0x35;
    }

    @Override
    public String getCmdName() {
        return "rebootGateway";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }
}
