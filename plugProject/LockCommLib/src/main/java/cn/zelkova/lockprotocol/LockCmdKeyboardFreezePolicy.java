package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 设置键盘锁定策略
 *
 * @author liwenqi
 * Created by liwenqi on 2016/6/25.
 */
public class LockCmdKeyboardFreezePolicy extends LockCmdBase {

    private int errTimes = 10;
    private int freezeSnd = 90;
    /**
     * 设置键盘锁定策略
     */
    public LockCmdKeyboardFreezePolicy(byte[] targetMac, BriefDate validFrom, BriefDate validTo, String lockid) {
        super(targetMac, validFrom, validTo, lockid);
    }
    @Override
    public byte getCmdId() {
        return 0x0C;
    }

    @Override
    public String getCmdName() {
        return "SyncPwdFreezePolicy";
    }

    /**
     * 设置输错锁定参数，这是全局的参数
     * @param errTimes 输错次数
     * @param freezeSnd 锁定秒数
     */
    public void setFreeze(int errTimes, int freezeSnd) {
        this.errTimes = errTimes;
        this.freezeSnd = freezeSnd;
    }
    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.writeByte(this.errTimes);
            out.writeByte(this.freezeSnd);

        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdSyncPwd:" + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
