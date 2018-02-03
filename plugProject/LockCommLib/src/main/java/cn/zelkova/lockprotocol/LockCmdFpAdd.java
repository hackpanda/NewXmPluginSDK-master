package cn.zelkova.lockprotocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 设置密码锁命令
 *
 * @author zp
 * Created by zp on 2016/6/25.
 */
public class LockCmdFpAdd extends LockCmdBase {
    private int count;
    private int overdue;
    /**
     * 启动扫描指纹
     */
    public LockCmdFpAdd(byte[] targetMac, BriefDate validFrom, BriefDate validTo) {
        super(targetMac, validFrom, validTo);
    }

    /**
     * 要新增的开门密码
     * @param count 采集总次数
     * @param overdue 确认超时
     */
    public void setFpParam(int count, int overdue) {
        this.count = count;
        this.overdue = overdue;
    }

    @Override
    public byte getCmdId() {
        return 0x13;
    }

    @Override
    public String getCmdName() {
        return "addNewFp";
    }

    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);

        try {
            out.writeByte(this.count);
            out.writeByte(this.overdue);

        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdAddNewFp:" + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
