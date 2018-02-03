package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 删除指纹锁命令
 *
 * @author zp
 * Created by zp on 2016/6/25.
 */
public class LockCmdFpDel extends LockCmdBase {
    private int batchId;
    /**
     * 启动扫描指纹
     */
    public LockCmdFpDel(byte[] targetMac, BriefDate validFrom, BriefDate validTo) {
        super(targetMac, validFrom, validTo);
    }
    /**
     * 要删除的开门密码
     * @param batchId 批次号
     */
    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    @Override
    public byte getCmdId() {
        return 0x15;
    }

    @Override
    public String getCmdName() {
        return "LockCmdFpDel";
    }

    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.writeInt(this.batchId);

        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdFpDel:" + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
