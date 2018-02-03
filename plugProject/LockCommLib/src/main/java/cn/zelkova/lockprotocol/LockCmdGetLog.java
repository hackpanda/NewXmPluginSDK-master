package cn.zelkova.lockprotocol;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 获取日志锁命令
 *
 * @author zp
 * Created by zp on 2016/6/25.
 */
public class LockCmdGetLog extends LockCmdBase {

    public LockCmdGetLog(byte[] targetMac, BriefDate validFrom, BriefDate validTo) {
        super(targetMac, validFrom, validTo);
    }
    /*
    * 0x00：从最新的开始、或从大到小号
    * 0x01：从最旧的开始、或从小到大号
    * */
    private byte orderType;
    /* 日志索引号”0 base
     * 0x00*4：第一条，0base
     * 0xNN：从指定的日志索引号开始
     * 0xFF*4：最新的一条
     */
    private long startIdx;
    /*
    * 指定每次返回的条数
    * */
    private int pageSize;

    public void setLogGetParam(byte orderType, long startIdx, int pageSize){
        this.orderType = orderType;
        this.startIdx = startIdx;
        this.pageSize = pageSize;
    }

    @Override
    public byte getCmdId() {
        return 0x16;
    }

    @Override
    public String getCmdName() {
        return "LockCmdGetLog";
    }

    @Override
    protected byte[] getPayload() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LittleEndianOutputStream out = new LittleEndianOutputStream(baos);
        try {
            out.writeByte(this.orderType);
            out.writeInt((int)this.startIdx);
            out.writeByte(this.pageSize);
        } catch (IOException e) {
            e.printStackTrace();
            throw new LockFormatException("LockCmdGetLog:" + e.getMessage(), e);
        }
        return baos.toByteArray();
    }
}
