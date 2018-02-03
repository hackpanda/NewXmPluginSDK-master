package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 设置省电间隔
 *
 * @author liwenqi
 * Created by liwenqi on 2017/01/11.
 */
public class LockCmdSetPwrSave extends LockCmdBase {

    int workTime = 0;
    /**
     * @param workTime  工作时间
     */
    public LockCmdSetPwrSave(byte[] targetMac, BriefDate validFrom, BriefDate validTo, int workTime, String lockid) {
        super(targetMac, validFrom, validTo, lockid);
        this.workTime = workTime;
    }

    @Override
    public byte getCmdId() {
        return 0x17;
    }

    @Override
    public String getCmdName() {
        return "LockCmdSetPwrSave";
    }

    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.writeShort(this.workTime);
        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdSetPwrSave:" + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
