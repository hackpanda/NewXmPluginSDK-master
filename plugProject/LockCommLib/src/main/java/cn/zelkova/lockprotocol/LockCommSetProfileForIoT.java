package cn.zelkova.lockprotocol;

/**
 * 设置网关服务器授权参数
 * @author lwq
 * Created by liwenqi on 2016/11/16.
 */
public class LockCommSetProfileForIoT extends LockCommBase {

    /**
     * @param deviceId
     * @param deviceSecret
     */
    public LockCommSetProfileForIoT(String deviceId, String deviceSecret) {
        super.mKLVList.add((byte) 0x01, deviceId);
        super.mKLVList.add((byte) 0x02, deviceSecret);
    }

    @Override
    public short getCmdId() {
        return 0x32;
    }

    @Override
    public String getCmdName() {
        return "SetProfileForIoT";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }

}

