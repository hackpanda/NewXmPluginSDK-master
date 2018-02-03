package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 设置键盘锁定策略
 *
 * @author liwenqi
 * Created by liwenqi on 2016/6/25.
 */
public class LockCmdSetZotpPeriod extends LockCmdBase {


    private int period = 60;
    public LockCmdSetZotpPeriod(byte[] targetMac, BriefDate validFrom, BriefDate validTo, String lockid) {
        super(targetMac, validFrom, validTo, lockid);
    }
    @Override
    public byte getCmdId() {
        return 0x0d;
    }

    @Override
    public String getCmdName() {
        return "SetZotpPeriod";
    }

    /**
     * 设置有效周期参数，这是全局的参数
     * @param period 有效周期
     */
    public void setPeriod(int period) {
        this.period = period;
    }
    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.writeShort(this.period);

        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdSetZotpPeriod:" + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
