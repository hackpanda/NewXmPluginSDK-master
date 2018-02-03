package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 指纹确认
 *
 * @author liwenqi
 * Created by liwenqi on 2016/12/08.
 */
public class LockCmdFpConfirm extends LockCmdBase {
    private int batchId;
    private BriefDate fpValidFrom;
    private BriefDate fpValidTo;

    /**
     * 指纹确认
     */
    public LockCmdFpConfirm(byte[] targetMac, BriefDate validFrom, BriefDate validTo) {
        super(targetMac, validFrom, validTo);
    }
    /**
     * 要新增的开门密码
     * @param batchId 批次号
     * @param fpValidFrom 有效期起始
     * @param fpValidTo 有效期终止
     */
    public void setFpConfirm(int batchId, BriefDate fpValidFrom, BriefDate fpValidTo) {
        this.batchId = batchId;
        this.fpValidFrom = fpValidFrom;
        this.fpValidTo = fpValidTo;
    }

    @Override
    public byte getCmdId() {
        return 0x14;
    }

    @Override
    public String getCmdName() {
        return "FpConfirm";
    }

    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.writeInt(this.batchId);
            out.write(this.fpValidFrom.toProtocolByte());
            out.write(this.fpValidTo.toProtocolByte());

        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdAddNewFpConfirm:" + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
