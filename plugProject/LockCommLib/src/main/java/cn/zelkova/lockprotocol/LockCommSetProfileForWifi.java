package cn.zelkova.lockprotocol;

/**
 * 设置网关WIFI参数
 * @author lwq
 * Created by liwenqi on 2016/11/16.
 */
public class LockCommSetProfileForWifi extends LockCommBase {

    /**
     * @param ssid
     * @param pwd
     */
    public LockCommSetProfileForWifi(String ssid, String pwd, byte authMode) {
        super.mKLVList.add((byte) 0x01, ssid);
        super.mKLVList.add((byte) 0x02, pwd);
        super.mKLVList.add((byte) 0x03, authMode);
    }

    @Override
    public short getCmdId() {
        return 0x31;
    }

    @Override
    public String getCmdName() {
        return "SetProfileForWifi";
    }

    @Override
    public boolean needSessionToken() {
        return false;
    }

}

